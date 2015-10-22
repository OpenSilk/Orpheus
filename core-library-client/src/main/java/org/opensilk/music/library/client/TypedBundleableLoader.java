/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.library.client;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.bundleable.BundleableListSlice;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxLoader;
import org.opensilk.common.core.util.Preconditions;
import org.opensilk.music.library.internal.IBundleableObserver;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryExtras;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

import static org.opensilk.music.library.provider.LibraryMethods.LIST;

/**
 * Created by drew on 9/28/15.
 */
public class TypedBundleableLoader<T extends Bundleable> implements RxLoader<T> {

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

    private final Set<ContentChangedListener> contentChangedListeners = new LinkedHashSet<>();
    private final Context context;

    private Uri uri;
    private String sortOrder;
    private String method = LIST;

    private Scheduler observeOnScheduler = AndroidSchedulers.mainThread();

    private UriObserver uriObserver;
    private Observable<List<T>> cachedObservable;

    @Inject
    public TypedBundleableLoader(@ForApplication Context context) {
        this.context = context;
    }

    public static <T extends Bundleable> TypedBundleableLoader<T> create(Context context) {
        return new TypedBundleableLoader<>(context.getApplicationContext());
    }

    public Observable<List<T>> getListObservable() {
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
                    .onErrorResumeNext(Observable.<List<T>>empty())
                    .observeOn(observeOnScheduler)
                    .cache();
        }
        return cachedObservable;
    }

    public Observable<T> getObservable() {
        return getListObservable().flatMap(new Func1<List<T>, Observable<T>>() {
            @Override
            public Observable<T> call(List<T> bundleables) {
                return Observable.from(bundleables);
            }
        });
    }

    public Observable<List<T>> createObservable() {
        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(final Subscriber<? super List<T>> subscriber) {
                final IBundleableObserver o = new IBundleableObserver.Stub() {
                    @Override
                    public void onNext(BundleableListSlice slice) throws RemoteException {
                        List<T> list = new ArrayList<>(slice.getList());
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(list);
                        }
                    }

                    @Override
                    public void onError(LibraryException e) throws RemoteException {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(e);
                        }
                    }

                    @Override
                    public void onCompleted() throws RemoteException {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                        }
                    }
                };

                final Bundle extras = LibraryExtras.b()
                        .putUri(uri)
                        .putSortOrder(sortOrder)
                        .putBundleableObserverCallback(o)
                        .get();

                Bundle ok = context.getContentResolver().call(uri, method, null, extras);
                if (!LibraryExtras.getOk(ok)) {
                    subscriber.onError(LibraryExtras.getCause(ok));
                }
            }
        });
    }

    public void reset() {
        cachedObservable = null;
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

    public TypedBundleableLoader<T> setUri(Uri uri) {
        this.uri = uri;
        return this;
    }

    public TypedBundleableLoader<T> setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public TypedBundleableLoader<T> setMethod(String method) {
        this.method = method;
        return this;
    }

    public TypedBundleableLoader<T> setObserveOnScheduler(Scheduler scheduler) {
        this.observeOnScheduler = Preconditions.checkNotNull(scheduler, "Scheduler must not be null");
        return this;
    }

    protected void emmitError(Throwable t, Subscriber<? super Bundleable> subscriber) {
        if (subscriber.isUnsubscribed()) return;
        subscriber.onError(t);
        dump(t);
    }

    protected void dump(Throwable throwable) {
        Timber.e(throwable, "BundleableLoader(\nuri=%s\nsortOrder=%s\n) ex=",
                uri, sortOrder);
    }

}
