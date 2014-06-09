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

package org.opensilk.music.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.bus.EventBus;
import org.opensilk.music.bus.events.RemoteLibraryEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by drew on 6/14/14.
 */
public class RemoteLibraryUtil {

    private static final Map<ComponentName, ServiceBinder> sMap;
    static {
        sMap = new HashMap<>();
    }

    public static boolean bindToService(final Context context, final ComponentName componentName) {
        final Context ctx = context.getApplicationContext();
        final ServiceBinder binder = new ServiceBinder(componentName);
        final Intent i = new Intent().setComponent(componentName);
        if (ctx.bindService(i, binder, Context.BIND_AUTO_CREATE)) {
            sMap.put(componentName, binder);
            return true;
        }
        return false;
    }

    public static void unbindFromService(final Context context, final ComponentName componentName) {
        final Context ctx = context.getApplicationContext();
        final ServiceBinder binder = sMap.remove(componentName);
        if (binder == null || binder.service == null) {
            return;
        }
        ctx.unbindService(binder);
    }

    public static void unBindAll(final Context context) {
        final Context ctx = context.getApplicationContext();
        for (Map.Entry<ComponentName, ServiceBinder> entry : sMap.entrySet()) {
            ctx.unbindService(entry.getValue());
        }
        sMap.clear();
    }

    public static boolean isBound(ComponentName componentName) {
        final ServiceBinder binder = sMap.get(componentName);
        return binder != null && binder.service != null;
    }

    public static RemoteLibrary getService(ComponentName componentName) throws RemoteException {
        if (isBound(componentName)) {
            return sMap.get(componentName).service;
        }
        throw new RemoteException("Service not bound");
    }

    public static final class ServiceBinder implements ServiceConnection {
        private final ComponentName mComponentName;
        private RemoteLibrary service;

        public ServiceBinder(final ComponentName componentName) {
            mComponentName = componentName;
        }

        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            this.service = RemoteLibrary.Stub.asInterface(service);
            EventBus.getInstance().post(new RemoteLibraryEvent.Bound(mComponentName));
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            EventBus.getInstance().post(new RemoteLibraryEvent.Unbound(mComponentName));
            this.service = null;
        }
    }

}
