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

package org.opensilk.music.ui2.library;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.silkdagger.qualifier.ForApplication;

import javax.inject.Inject;

/**
 * Created by drew on 10/5/14.
 */
public class LibraryConnection implements ServiceConnection {

    public interface Callback {
        public void onConnected();
    }

    final Context context;
    final PluginInfo plugin;

    RemoteLibrary libraryConnection;
    boolean isConnected;
    Callback connectionCallback;


    @Inject
    public LibraryConnection(@ForApplication Context context, PluginInfo plugin) {
        this.context = new ContextWrapper(context);
        this.plugin = plugin;
    }

    public boolean isConnected() {
        if (isConnected && libraryConnection != null) {
            return true;
        } else {
            return false;
        }
    }

    public void connect() {
        context.startService(new Intent().setComponent(plugin.componentName));
        context.bindService(new Intent().setComponent(plugin.componentName), this, 0);
    }

    public void disconnect() {
        context.unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        libraryConnection = RemoteLibrary.Stub.asInterface(service);
        isConnected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        libraryConnection = null;
        isConnected = false;

    }
}
