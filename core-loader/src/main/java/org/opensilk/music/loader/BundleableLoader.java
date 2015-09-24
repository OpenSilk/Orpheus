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

package org.opensilk.music.loader;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.rx.RxLoader;
import org.opensilk.common.core.util.Preconditions;
import org.opensilk.music.library.internal.BundleableListSlice;
import org.opensilk.music.library.internal.IBundleableObserver;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

import static org.opensilk.music.library.provider.LibraryMethods.*;

/**
 * Created by drew on 5/2/15.
 */
public class BundleableLoader implements RxLoader<Bundleable> {

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

    final Set<ContentChangedListener> contentChangedListeners = new LinkedHashSet<>();
    final Context context;

    Uri uri;
    String sortOrder;
    String method = LIST;

    Scheduler observeOnScheduler = AndroidSchedulers.mainThread();

    private UriObserver uriObserver;
    Observable<List<Bundleable>> cachedObservable;

    @Inject
    public BundleableLoader(@ForApplication Context context) {
        this.context = context;
    }

    public static BundleableLoader create(Context context) {
        return new BundleableLoader(context.getApplicationContext());
    }

    public Observable<List<Bundleable>> getListObservable() {
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
                    .onErrorResumeNext(Observable.<List<Bundleable>>empty())
                    .observeOn(observeOnScheduler)
                    .cache();
        }
        return cachedObservable;
    }

    public Observable<Bundleable> getObservable() {
        return getListObservable().flatMap(new Func1<List<Bundleable>, Observable<Bundleable>>() {
            @Override
            public Observable<Bundleable> call(List<Bundleable> bundleables) {
                return Observable.from(bundleables);
            }
        });
    }

    public Observable<List<Bundleable>> createObservable() {
        return Observable.create(new Observable.OnSubscribe<List<Bundleable>>() {
            @Override
            public void call(final Subscriber<? super List<Bundleable>> subscriber) {
                final IBundleableObserver o = new IBundleableObserver.Stub() {
                    @Override
                    public void onNext(BundleableListSlice slice) throws RemoteException {
                        List<Bundleable> list = new ArrayList<>(slice.getList());
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

    public BundleableLoader setUri(Uri uri) {
        this.uri = uri;
        return this;
    }

    public BundleableLoader setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public BundleableLoader setMethod(String method) {
        this.method = method;
        return this;
    }

    public BundleableLoader setObserveOnScheduler(Scheduler scheduler) {
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
