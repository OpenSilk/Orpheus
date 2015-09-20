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

package org.opensilk.music.index.client;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.index.provider.Methods;
import org.opensilk.music.library.provider.LibraryExtras;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Created by drew on 9/17/15.
 */
@Singleton
public class IndexClientImpl implements IndexClient {

    final Context appContext;
    final Uri callUri;

    @Inject
    public IndexClientImpl(
            @ForApplication Context appContext,
            @Named("IndexProviderAuthority") String authority
    ) {
        this.appContext = appContext;
        callUri = IndexUris.call(authority);
    }

    @Override
    public boolean isIndexed(Uri uri) {
        Bundle args = LibraryExtras.b().putUri(uri).get();
        return makeCall(Methods.IS_INDEXED, args);
    }

    @Override
    public boolean add(Uri uri) {
        Bundle args = LibraryExtras.b().putUri(uri).get();
        return makeCall(Methods.ADD, args);
    }

    @Override
    public boolean remove(Uri uri) {
        Bundle args = LibraryExtras.b().putUri(uri).get();
        return makeCall(Methods.REMOVE, args);
    }

    private boolean makeCall(String method, Bundle args) {
        Bundle result = appContext.getContentResolver().call(callUri, method, null, args);
        if (result == null) {
            Timber.e("Got null reply from index provider method=%s", method);
            return false;
        }
        return LibraryExtras.getOk(result);
    }

}
