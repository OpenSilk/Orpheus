/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.loader;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Created by drew on 10/5/14.
 */
public class LibraryLoader implements EndlessRemoteAsyncLoader<Bundleable>, ServiceConnection {

    static final int STEP = 30;

    final Context context;
    final PluginInfo plugin;
    final LibraryInfo library;
    final Handler handler;

    RemoteLibrary libraryConnection;
    boolean connectCalled = false;
    boolean connected =false;
    Callback<Bundleable> connectionCallback;

    boolean isLoading = false;
    boolean isFirstLoad = true;
    Bundle pagination;


    @Inject
    public LibraryLoader(@ForApplication Context context, PluginInfo plugin, LibraryInfo library) {
        this.context = new ContextWrapper(context);
        this.plugin = plugin;
        this.library = library;
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void loadAsync(Callback<Bundleable> callback) {
        if (isLoading) return;
        if (ensureConnection(callback)) {
            pagination = null;
            fetch(callback, true);
            isFirstLoad = false;
        }
    }

    @Override
    public void loadMoreAsync(Callback<Bundleable> callback) {
        if (isLoading) return;
        if (ensureConnection(callback)) {
            if (isFirstLoad) {
                loadAsync(callback);
                return;
            }
            fetch(callback, false);
        }
    }

    private void fetch(final Callback<Bundleable> callback, final boolean append) {
        try {
            libraryConnection.browseFolders(library.libraryId, library.currentFolderId, STEP, pagination, new Result.Stub() {
                @Override
                public void success(List<Bundle> items, Bundle paginationBundle) throws RemoteException {
                    Timber.v("success()");
                    pagination = paginationBundle;
                    final List<Bundleable> objs = new ArrayList<>(items.size());
                    for (Bundle b : items) {
                        try {
                            objs.add(OrpheusApi.transformBundle(b));
                        } catch (Exception e) {
                            //ignore for now
                        }
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (append) {
                                callback.onMoreDataFetched(objs);
                            } else {
                                callback.onDataFetched(objs);
                            }
                            isLoading = false;
                        }
                    });
                }

                @Override
                public void failure(int code, String reason) throws RemoteException {
                    Timber.v("failure(%d, %s)", code, reason);
                    switch (code) {
                        case OrpheusApi.Error.AUTH_FAILURE:
                            break;
                        case OrpheusApi.Error.NETWORK:
                            break;
                        case OrpheusApi.Error.RETRY:
                            break;
                        case OrpheusApi.Error.UNKNOWN:
                            break;
                    }
                    isLoading = false;
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connect() {
        connectCalled = true;
        context.startService(new Intent().setComponent(plugin.componentName));
        context.bindService(new Intent().setComponent(plugin.componentName), this, 0);
    }

    @Override
    public void disconnect() {
        connectCalled = false;
        context.unbindService(this);
    }

    private boolean ensureConnection(Callback<Bundleable> callback) {
        if (connected && libraryConnection != null) {
            return true;
        } else {
            connectionCallback = callback;
            if (!connectCalled) {
                Timber.w("connect() not called before loadAsync() or possibly disconnected");
                connect();
            }
            return false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        libraryConnection = RemoteLibrary.Stub.asInterface(service);
        connected = true;
        if (connectionCallback != null) {
            connectionCallback.onConnectionAvailable();
            connectionCallback = null;
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        libraryConnection = null;
        connectCalled = false;
        connected = false;
        isLoading = false;
    }
}
