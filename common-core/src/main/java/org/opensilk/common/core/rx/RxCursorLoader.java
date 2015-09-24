/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.common.core.rx;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.opensilk.common.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/19/14.
 */
public abstract class RxCursorLoader<T> implements RxLoader<T> {

    class UriObserver extends ContentObserver {
        UriObserver(Handler handler) {
            super(handler);
        }
        @Override
        public void onChange(boolean selfChange) {
            reset();
            for (ContentChangedListener l : contentChangedListeners) {
                l.reload();
            }
        }
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            onChange(selfChange);
        }
    }

    protected final List<ContentChangedListener> contentChangedListeners;
    protected final Context context;

    protected Scheduler subscribeScheduler = Schedulers.io();
    protected Scheduler observeOnScheduler = AndroidSchedulers.mainThread();

    protected Uri uri;
    protected String[] projection;
    protected String selection;
    protected String[] selectionArgs;
    protected String sortOrder;

    private UriObserver uriObserver;
    protected Observable<T> cachedObservable;

    public RxCursorLoader(Context context) {
        this(context, null, null, null, null, null);
    }

    public RxCursorLoader(Context context,
                          Uri uri, String[] projection, String selection,
                          String[] selectionArgs, String sortOrder) {
        contentChangedListeners = new ArrayList<>();
        this.context = context;
        this.uri = uri;
        this.projection = projection;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.sortOrder = sortOrder;
    }

    /**
     * Do your thing. Any thrown exceptions will be passed to onError() unless
     * using {@link #getObservable()} or {@link #getListObservable()} as they will
     * capture any exception then finish.
     */
    protected abstract T makeFromCursor(Cursor c) throws Exception;

    /**
     * @return Observable that collects all items into a List and emits that
     *         in a single onNext(List) call.
     */
    public Observable<List<T>> getListObservable() {
        return getObservable().toList();
    }

    /**
     * Note:<br>
     *     The observable is cached and must be {@link #reset()} before resubscribing to requery.
     *     The observable does not emit errors, on error the cached observable is reset
     *     then calls onComplete.
     *
     * @return Observable
     */
    public Observable<T> getObservable() {
        registerContentObserver();
        if (cachedObservable == null) {
            cachedObservable = createObservable()
                    .doOnError(new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            reset();
                            dump(throwable);
                        }
                    })
                    .onErrorResumeNext(Observable.<T>empty())
                    .subscribeOn(subscribeScheduler)
                    .observeOn(observeOnScheduler)
                    .cache();
        }
        return cachedObservable;
    }

    /**
     * @return the raw producer, suitable for chaining.
     */
    public Observable<T> createObservable() {
        return Observable.create(new Observable.OnSubscribe<T>() {
            // Querys the mediastore
            @Override
            public void call(Subscriber<? super T> subscriber) {
//                Timber.v("OnSubscribe %s", Thread.currentThread().getName());
                Cursor c = null;
                try {
                    if (context == null || uri == null) {
                        emmitError(new NullPointerException("Context and Uri must not be null"), subscriber);
                        return;
                    }
                    c = getCursor();
                    if (c == null) {
                        emmitError(new NullPointerException("Unable to obtain cursor"), subscriber);
                        return;
                    }
                    if (c.moveToFirst()) {
                        do {
                            T item = makeFromCursor(c);
                            if (item == null) continue; //TODO throw this?
                            if (subscriber.isUnsubscribed()) return;
                            subscriber.onNext(item);
                        } while (c.moveToNext());
                    }
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    emmitError(e, subscriber);
                } finally {
                    if (c != null) c.close();
                }
            }
        });
    }

    public void reset() {
        cachedObservable = null;
    }

    protected Cursor getCursor() {
        return context.getContentResolver().query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    protected void registerContentObserver() {
        if (uriObserver == null) {
            uriObserver = new UriObserver(new Handler(Looper.getMainLooper()));
            context.getContentResolver().registerContentObserver(uri, true, uriObserver);
        }
    }

    public void addContentChangedListener(ContentChangedListener l) {
        contentChangedListeners.add(l);
    }

    public void removeContentChangedListener(ContentChangedListener l) {
        contentChangedListeners.remove(l);
    }

    public RxCursorLoader<T> setUri(Uri uri) {
        this.uri = Preconditions.checkNotNull(uri, "Uri must not be null");
        return this;
    }

    public RxCursorLoader<T> setProjection(String[] projection) {
        this.projection = projection;
        return this;
    }

    public RxCursorLoader<T> setSelection(String selection) {
        this.selection = selection;
        return this;
    }

    public RxCursorLoader<T> setSelectionArgs(String[] selectionArgs) {
        this.selectionArgs = selectionArgs;
        return this;
    }

    public RxCursorLoader<T> setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public RxCursorLoader<T> setSubscribeScheduler(Scheduler scheduler) {
        this.subscribeScheduler = Preconditions.checkNotNull(scheduler, "Scheduler must not be null");
        return this;
    }

    public RxCursorLoader<T> setObserveOnScheduler(Scheduler scheduler) {
        this.observeOnScheduler = Preconditions.checkNotNull(scheduler, "Scheduler must not be null");
        return this;
    }

    protected void emmitError(Throwable t, Subscriber<? super T> subscriber) {
        if (subscriber.isUnsubscribed()) return;
        subscriber.onError(t);
        dump(new Throwable(t));
    }

    protected void dump(Throwable throwable) {
        Timber.e(throwable, "RxCursorLoader(uri=%s\nprojection=%s\nselection=%s\nselectionArgs=%s\nsortOrder=%s",
                uri,
                projection != null ? Arrays.toString(projection) : null,
                selection,
                selectionArgs != null ? Arrays.toString(selectionArgs) : null,
                sortOrder);
    }
}
