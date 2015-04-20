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

package org.opensilk.music.ui2.library;

import android.os.Bundle;
import android.os.RemoteException;

import org.opensilk.music.BuildConfig;
import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.api.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.opensilk.common.rx.RxUtils.observeOnMain;
import static org.opensilk.common.util.UncheckedThrow.rethrow;

/**
 * Created by drew on 10/20/14.
 */
public class LibraryConnection {

    public static class Result {
        public final List<Bundleable> items;
        public final Bundle token;

        public Result(List<Bundleable> items, Bundle token) {
            this.items = items;
            this.token = token;
        }

        /** Copies result for use in cache */
        public Result copy() {
            return new Result(new ArrayList<>(items),
                    token != null ? new Bundle(token) : null);
        }
    }

    class Callback extends org.opensilk.music.api.callback.Result.Stub {

        final LibraryInfo libraryInfo;
        final Subscriber<? super Result> subscriber;
        final boolean cacheable;

        public Callback(LibraryInfo libraryInfo, Subscriber<? super Result> subscriber, boolean cacheable) {
            this.libraryInfo = libraryInfo;
            this.subscriber = subscriber;
            this.cacheable = cacheable;
        }

        @Override
        public void onNext(List<Bundle> items, Bundle paginationBundle) throws RemoteException {
            List<Bundleable> list = new ArrayList<>(items.size());
            for (Bundle b : items) {
                try {
                    list.add(OrpheusApi.materializeBundle(b));
                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed()) subscriber.onError(e);
                    return;
                }
            }
            final Result result = new Result(list, paginationBundle);
            if (cacheable) updateCache(libraryInfo, result);
            if (subscriber.isUnsubscribed()) return;
            subscriber.onNext(result);
            subscriber.onCompleted();
        }

        @Override
        public void onError(ParcelableException e) throws RemoteException {
            if (subscriber.isUnsubscribed()) return;
            subscriber.onError(e);
        }
    }

    public static final int STEP = BuildConfig.DEBUG ? 4 : 30;

    final Map<LibraryInfo, Result> browseCache = new LinkedHashMap<>();
    final PluginConnectionManager connectionManager;

    public LibraryConnection(PluginConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /*
     * Helpers
     */

    Observable<RemoteLibrary> getObservable(PluginInfo pluginInfo) {
        return connectionManager
                .bind(pluginInfo.componentName)
                // subject emmits on main so push it onto a background thread
                .observeOn(Schedulers.io());
    }

    public synchronized boolean hasCache(LibraryInfo libraryInfo) {
        return browseCache.containsKey(libraryInfo);
    }

    public synchronized Result getCache(LibraryInfo libraryInfo) {
        return browseCache.get(libraryInfo).copy();
    }

    synchronized void updateCache(LibraryInfo libraryInfo, Result result) {
        Result resultCopy = result.copy();
        Result cacheResult = browseCache.put(libraryInfo, resultCopy);
        if (cacheResult != null) {
            resultCopy.items.addAll(0, cacheResult.items);
        }
    }

    public synchronized void clearCache(LibraryInfo libraryInfo) {
        if (hasCache(libraryInfo)) browseCache.remove(libraryInfo);
    }

    /*
     * Start API
     */

    public Observable<Result> browse(final PluginInfo pluginInfo, final LibraryInfo libraryInfo, final Bundle previousBundle) {
        return getObservable(pluginInfo)
                .flatMap(new Func1<RemoteLibrary, Observable<Result>>() {
                    @Override
                    public Observable<Result> call(final RemoteLibrary remoteLibrary) {
                        return Observable.create(new Observable.OnSubscribe<Result>() {
                            @Override
                            public void call(final Subscriber<? super Result> subscriber) {
                                try {
                                    remoteLibrary.browseFolders(libraryInfo.libraryId,
                                            libraryInfo.folderId, STEP, previousBundle,
                                            new Callback(libraryInfo, subscriber, true));
                                } catch (RemoteException e) {
                                    connectionManager.onException(pluginInfo.componentName);
                                    if (!subscriber.isUnsubscribed()) subscriber.onError(e);
                                }
                            }
                        });
                    }
                })
                // the Result callback will produce on a binder thread
                // push the results back to main.
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Result> listSongsInFolder(final PluginInfo pluginInfo,
                                                final LibraryInfo libraryInfo,
                                                final Bundle previousBundle) {
        return getObservable(pluginInfo)
                .flatMap(new Func1<RemoteLibrary, Observable<Result>>() {
                    @Override
                    public Observable<Result> call(final RemoteLibrary remoteLibrary) {
                        return Observable.create(new Observable.OnSubscribe<Result>() {
                            @Override
                            public void call(final Subscriber<? super Result> subscriber) {
                                try {
                                    remoteLibrary.listSongsInFolder(libraryInfo.libraryId,
                                            libraryInfo.folderId, STEP, previousBundle,
                                            new Callback(libraryInfo, subscriber, false));
                                } catch (RemoteException e) {
                                    connectionManager.onException(pluginInfo.componentName);
                                    if (!subscriber.isUnsubscribed()) subscriber.onError(e);
                                }
                            }
                        });
                    }
                });
    }

    public Observable<Result> search(final PluginInfo pluginInfo, final LibraryInfo libraryInfo, final String query) {
        return getObservable(pluginInfo)
                .flatMap(new Func1<RemoteLibrary, Observable<Result>>() {
                    @Override
                    public Observable<Result> call(final RemoteLibrary remoteLibrary) {
                        return Observable.create(new Observable.OnSubscribe<Result>() {
                            @Override
                            public void call(Subscriber<? super Result> subscriber) {
                                try {
                                    remoteLibrary.search(libraryInfo.libraryId,
                                            query,
                                            100,
                                            null,
                                            new Callback(libraryInfo, subscriber, false)
                                    );
                                } catch (RemoteException e) {
                                    connectionManager.onException(pluginInfo.componentName);
                                    if (!subscriber.isUnsubscribed()) subscriber.onError(e);
                                }
                            }
                        });
                    }
                });
    }

    public Observable<PluginConfig> getConfig(final PluginInfo pluginInfo) {
        return observeOnMain(getObservable(pluginInfo).map(new Func1<RemoteLibrary, PluginConfig>() {
            @Override
            public PluginConfig call(RemoteLibrary remoteLibrary) {
                try {
                    return PluginConfig.materialize(remoteLibrary.getConfig());
                } catch (RemoteException e) {
                    connectionManager.onException(pluginInfo.componentName);
                    throw rethrow(e);
                }
            }
        }));
    }

}
