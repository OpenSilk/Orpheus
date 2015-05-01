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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import org.opensilk.music.BuildConfig;
import org.opensilk.music.GraphHolder;

import java.io.FileNotFoundException;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by drew on 3/25/14.
 */
public class ArtworkProvider extends ContentProvider {
    private static final String TAG = ArtworkProvider.class.getSimpleName();

    private static final String AUTHORITY = BuildConfig.ARTWORK_AUTHORITY;
    private static final UriMatcher sUriMatcher;

    public static final Uri ARTWORK_URI;
    public static final Uri ARTWORK_THUMB_URI;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        ARTWORK_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("artwork").build();
        sUriMatcher.addURI(AUTHORITY, "artwork/*/*", 1);

        ARTWORK_THUMB_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("thumbnail").build();
        sUriMatcher.addURI(AUTHORITY, "thumbnail/*/*", 2);
    }

    /**
     * @return Uri to retrieve large (fullscreen) artwork for specified albumId
     */
    public static Uri createArtworkUri(String artistName, String albumName) {
        return ARTWORK_URI.buildUpon().appendPath(artistName).appendPath(albumName).build();
    }

    /**
     * @return Uri to retrieve thumbnail for specified albumId
     */
    public static Uri createArtworkThumbnailUri(String artistName, String albumName) {
        return ARTWORK_THUMB_URI.buildUpon().appendPath(artistName).appendPath(albumName).build();
    }

    @Inject ArtworkRequestManager mArtworkRequestor;

    @Override
    //@DebugLog
    public boolean onCreate() {
        GraphHolder.get(getContext()).inject(this);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("query not implemented");
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
    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        return openFile(uri, mode);
    }

    @Override
    //@DebugLog
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new IllegalArgumentException("Provider is read only");
        }
        final List<String> seg;
        final ParcelFileDescriptor pfd;
        switch (sUriMatcher.match(uri)) {
            case 1: //Fullscreen
                seg = uri.getPathSegments();
                if (seg == null || seg.size() < 2) {
                    break;
                }
                pfd = mArtworkRequestor.getArtwork(seg.get(seg.size()-2), seg.get(seg.size()-1));
                if (pfd != null) {
                    return pfd;
                }
                break;
            case 2: //Thumbnail
                seg = uri.getPathSegments();
                if (seg == null || seg.size() < 2) {
                    break;
                }
                pfd = mArtworkRequestor.getArtworkThumbnail(seg.get(seg.size()-2), seg.get(seg.size()-1));
                if (pfd != null) {
                    return pfd;
                }
                break;
        }
        throw new FileNotFoundException("Could not obtain image from cache");
    }

}
