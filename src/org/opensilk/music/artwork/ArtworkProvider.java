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

package org.opensilk.music.artwork;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.andrew.apollo.BuildConfig;

import java.io.FileNotFoundException;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 3/25/14.
 */
public class ArtworkProvider extends ContentProvider implements ServiceConnection {
    private static final String TAG = ArtworkProvider.class.getSimpleName();

    private static final String AUTHORITY = BuildConfig.ARTWORK_AUTHORITY;
    private static final UriMatcher sUriMatcher;

    public static final Uri ARTWORK_URI;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        ARTWORK_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("artwork").build();
        sUriMatcher.addURI(AUTHORITY, "artwork/#", 1);
    }

    /**
     * Creates an artwork uri to retrieve album art for specified albumId
     * @param albumId
     * @return
     */
    public static Uri createArtworkUri(final long albumId) {
        return ARTWORK_URI.buildUpon().appendPath(String.valueOf(albumId)).build();
    }

    /**
     * Artwork service connection, we proxy requests to the image cache through here
     */
    private IArtworkServiceImpl mArtworkService;

    @Override
    public boolean onCreate() {
        doBindService();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case 1:
                return "image/*";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Provider is read only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Provider is read only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Provider is read only");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return openFile(uri, mode, null);
    }

    @Override
    @DebugLog
    public synchronized ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new IllegalArgumentException("Provider is read only");
        }
        switch (sUriMatcher.match(uri)) {
            case 1:
                try {
                    final long id = Long.decode(uri.getLastPathSegment());
                    if (mArtworkService == null) {
                        long start = System.currentTimeMillis();
                        // We were called too soon after onCreate, give the service some time
                        // to spin up, This is run in a binder thread so it shouldn't be a big deal
                        // to block it.
                        wait(500);
                        Log.i(TAG, "Waited for " + (System.currentTimeMillis() - start) + "ms");
                    }
                    if (mArtworkService == null) {
                        throw new FileNotFoundException("Service not bound");
                    }
                    final ParcelFileDescriptor pfd = mArtworkService.getArtwork(id);
                    if (pfd == null) {
                        throw new FileNotFoundException("Parcel was null");
                    }
                    return pfd;
                } catch (InterruptedException|RemoteException e) {
                    throw new FileNotFoundException("" + e.getClass().getName() + " " + e.getMessage());
                }
        }
        return null;
    }

    /**
     * Tries to bind to the artwork service
     */
    private void doBindService() {
        final Context ctx = getContext();
        if (ctx != null) {
            ctx.bindService(new Intent(ctx, ArtworkService.class), this, Context.BIND_AUTO_CREATE);
        }
    }

    /*
     * Implement ServiceConnection interface
     */

    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder service) {
        mArtworkService = (IArtworkServiceImpl) IArtworkServiceImpl.asInterface(service);
        notifyAll();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mArtworkService = null;
        doBindService();
    }

}
