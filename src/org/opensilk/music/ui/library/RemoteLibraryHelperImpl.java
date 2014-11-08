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

package org.opensilk.music.ui.library;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.common.dagger.qualifier.ForActivity;
import org.opensilk.common.dagger.qualifier.ForApplication;

import javax.inject.Inject;

/**
 * Created by drew on 7/2/14.
 */
public class RemoteLibraryHelperImpl implements RemoteLibraryHelper, ServiceConnection {

    private final Context context;

    private RemoteLibrary service;
    private ConnectionListener callback;

    @Inject
    public RemoteLibraryHelperImpl(@ForActivity Context context) {
        this.context = new ContextWrapper(context);
    }

    public void acquireService(ComponentName component, ConnectionListener listener) {
        final Intent i = new Intent().setComponent(component);
        context.startService(i);
        context.bindService(i, this, 0);
        callback = listener;
    }

    public void releaseService() {
        context.unbindService(this);
        callback = null;
    }

    public RemoteLibrary getService() {
        return service;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.service = RemoteLibrary.Stub.asInterface(service);
        if (callback != null) {
            callback.onConnected();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        this.service = null;
        if (callback != null) {
            callback.onConnectionBroke();
        }
    }
}
