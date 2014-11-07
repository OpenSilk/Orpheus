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

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.RemoteLibrary;
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
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.opensilk.common.rx.RxUtils.observeOnMain;
import static org.opensilk.common.util.UncheckedThrow.rethrow;
import static org.opensilk.music.ui2.main.MusicServiceConnection.wrapForRetry;

/**
 * Created by drew on 10/20/14.
 */
public class LibraryConnection {

    public static class Result {
        final List<Bundleable> items;
        final Bundle token;

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

    public static class ResultException extends Exception {
        final int code;

        public ResultException(String detailMessage, int code) {
            super(detailMessage);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    public static final int STEP = 30;

    final Map<LibraryInfo, Result> CACHE = new LinkedHashMap<>();
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
        return CACHE.containsKey(libraryInfo);
    }

    public synchronized Result getCache(LibraryInfo libraryInfo) {
        return CACHE.get(libraryInfo).copy();
    }

    synchronized void updateCache(LibraryInfo libraryInfo, Result result) {
        Result resultCopy = result.copy();
        Result cacheResult = CACHE.put(libraryInfo, resultCopy);
        if (cacheResult != null) {
            resultCopy.items.addAll(0, cacheResult.items);
        }
    }

    public synchronized void clearCache(LibraryInfo libraryInfo) {
        if (hasCache(libraryInfo)) CACHE.remove(libraryInfo);
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
                                            new org.opensilk.music.api.callback.Result.Stub() {
                                                @Override
                                                public void success(List<Bundle> items, Bundle paginationBundle) throws RemoteException {
                                                    List<Bundleable> list = new ArrayList<>(items.size());
                                                    for (Bundle b : items) {
                                                        try {
                                                            list.add(OrpheusApi.transformBundle(b));
                                                        } catch (Exception e) {
                                                            if (!subscriber.isUnsubscribed())
                                                                subscriber.onError(e);
                                                            return;
                                                        }
                                                    }
                                                    final Result result = new Result(list, paginationBundle);
                                                    updateCache(libraryInfo, result);
                                                    if (subscriber.isUnsubscribed()) return;
                                                    subscriber.onNext(result);
                                                    subscriber.onCompleted();
                                                }

                                                @Override
                                                public void failure(int code, String reason) throws RemoteException {
                                                    if (subscriber.isUnsubscribed()) return;
                                                    subscriber.onError(new ResultException(reason, code));
                                                }
                                            });
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

    public Observable<Integer> getApiVersion(final PluginInfo pluginInfo) {
        return observeOnMain(getObservable(pluginInfo).map(new Func1<RemoteLibrary, Integer>() {
            @Override
            public Integer call(RemoteLibrary remoteLibrary) {
                try {
                    return remoteLibrary.getApiVersion();
                } catch (RemoteException e) {
                    connectionManager.onException(pluginInfo.componentName);
                    throw rethrow(e);
                }
            }
        }));
    }

    public Observable<Integer> getCapabilities(final PluginInfo pluginInfo) {
        return observeOnMain(wrapForRetry(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return _capabilities(pluginInfo);
            }
        }));
    }

    public Observable<Intent> getLibraryChooserIntent(final PluginInfo pluginInfo) {
        return observeOnMain(wrapForRetry(new Func0<Observable<Intent>>() {
            @Override
            public Observable<Intent> call() {
                return _libraryChooserIntent(pluginInfo);
            }
        }));
    }

    public Observable<Intent> getSettingsIntent(final PluginInfo pluginInfo) {
        return observeOnMain(wrapForRetry(new Func0<Observable<Intent>>() {
            @Override
            public Observable<Intent> call() {
                return _settingsIntent(pluginInfo);
            }
        }));
    }

    public Observable<LibraryInfo> getDefaultLibraryInfo(final PluginInfo pluginInfo) {
        return observeOnMain(wrapForRetry(new Func0<Observable<LibraryInfo>>() {
            @Override
            public Observable<LibraryInfo> call() {
                return _defaultLibraryInfo(pluginInfo);
            }
        }));
    }

    /*
     * Wrapped calls
     */

    public Observable<Integer> _capabilities(final PluginInfo pluginInfo) {
        return getObservable(pluginInfo).map(new Func1<RemoteLibrary, Integer>() {
            @Override
            public Integer call(RemoteLibrary remoteLibrary) {
                try {
                    return remoteLibrary.getCapabilities();
                } catch (RemoteException e) {
                    connectionManager.onException(pluginInfo.componentName);
                    throw rethrow(e);
                }
            }
        });
    }

    Observable<Intent> _libraryChooserIntent(final PluginInfo pluginInfo) {
        return getObservable(pluginInfo).map(new Func1<RemoteLibrary, Intent>() {
            @Override
            public Intent call(RemoteLibrary remoteLibrary) {
                try {
                    Intent intent = new Intent();
                    remoteLibrary.getLibraryChooserIntent(intent);
                    if (intent.getComponent() != null) return intent;
                } catch (RemoteException e) {
                    connectionManager.onException(pluginInfo.componentName);
                    throw rethrow(e);
                }
                throw new NullPointerException("Library chooser intent not populated");
            }
        });
    }

    Observable<Intent> _settingsIntent(final PluginInfo pluginInfo) {
        return getObservable(pluginInfo).map(new Func1<RemoteLibrary, Intent>() {
            @Override
            public Intent call(RemoteLibrary remoteLibrary) {
                try {
                    Intent intent = new Intent();
                    remoteLibrary.getSettingsIntent(intent);
                    return intent;
                } catch (RemoteException e) {
                    connectionManager.onException(pluginInfo.componentName);
                    throw rethrow(e);
                }
            }
        });
    }

    Observable<LibraryInfo> _defaultLibraryInfo(final PluginInfo pluginInfo) {
        return getObservable(pluginInfo).map(new Func1<RemoteLibrary, LibraryInfo>() {
            @Override
            public LibraryInfo call(RemoteLibrary remoteLibrary) {
                try {
                    LibraryInfo info = remoteLibrary.getDefaultLibraryInfo();
                    if (info != null) return info;
                } catch (RemoteException e) {
                    connectionManager.onException(pluginInfo.componentName);
                    throw rethrow(e);
                }
                throw new NullPointerException("No default libraryInfo");
            }
        });
    }

}
