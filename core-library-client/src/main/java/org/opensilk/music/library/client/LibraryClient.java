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

import android.annotation.TargetApi;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.util.ArrayMap;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.Preconditions;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.library.provider.LibraryUris;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by drew on 10/22/15.
 */
public class LibraryClient {

    final Context appContext;
    String authority;
    ClientCompat client;

    @Inject
    public LibraryClient(@ForApplication Context context) {
        this.appContext = context;
    }

    public static LibraryClient create(Context context, Uri uri) {
        return new LibraryClient(context.getApplicationContext()).setAuthority(uri.getAuthority());
    }

    public LibraryClient setAuthority(String authority) {
        this.authority = authority;
        return this;
    }

    public Bundle makeCall(String method, Bundle args) {
        ClientCompat c = acquireClient();
        try {
            return c.call(method, args);
        } catch (RemoteException e) {
            release();
            return makeCall(method, args);
        }
    }

    public void release() {
        if (client != null) {
            client.release();
            client = null;
        }
    }

    private ClientCompat acquireClient() {
        if (client == null) {
            Preconditions.checkNotNull(authority, "Must set authority");
            if (VersionUtils.hasJellyBeanMR1()) {
                client = new ClientCompatJBMR1(appContext, LibraryUris.call(authority));
            } else {
                client = new ClientCompatBase(appContext, LibraryUris.call(authority));
            }
        }
        return client;
    }

    interface ClientCompat {
        Bundle call(String method, Bundle args) throws RemoteException;
        void release();
    }

    static class ClientCompatBase implements ClientCompat {
        final ContentResolver contentResolver;
        final Uri callUri;

        public ClientCompatBase(Context context, Uri callUri) {
            this.contentResolver = context.getContentResolver();
            this.callUri = callUri;
        }

        @Override
        public Bundle call(String method, Bundle args) throws RemoteException {
            return contentResolver.call(callUri, method, null, args);
        }

        @Override
        public void release() {
            //noop
        }
    }

    @TargetApi(17)
    static class ClientCompatJBMR1 implements ClientCompat {
        final ContentProviderClient client;

        public ClientCompatJBMR1(Context context, Uri callUri) {
            this.client = context.getContentResolver()
                    .acquireContentProviderClient(callUri);
        }

        @Override
        public Bundle call(String method, Bundle args) throws RemoteException {
            return client.call(method, null, args);
        }

        @Override
        public void release() {
            client.release();
        }
    }
}
