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
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import com.andrew.apollo.ApolloApplication;
import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.model.Album;
import com.jakewharton.disklrucache.DiskLruCache;

import org.apache.commons.io.IOUtils;
import org.opensilk.music.artwork.cache.CacheUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by drew on 3/25/14.
 */
public class ArtworkProvider extends ContentProvider {

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

    private int mScreenWidth;
    private File mCacheDir;
    private Handler mHandler;

    @Override
    public boolean onCreate() {
        mScreenWidth = ApolloApplication.getMinDisplayWidth(getContext());
        mCacheDir = CacheUtil.getCacheDir(getContext(), "artworkprovidercache");
        mHandler = new Handler();
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
                break;
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
    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        switch (sUriMatcher.match(uri)) {
            case 1:
                FileOutputStream fos = null;
                DiskLruCache.Snapshot snapshot = null;
                File temp = null;
                try {
                    final String cacheKey = getCacheKey(Long.decode(uri.getLastPathSegment()));
                    snapshot = ArtworkManager.getInstance(getContext()).getDiskCache().get(cacheKey);
                    temp = File.createTempFile("img", null, mCacheDir);
                    fos = new FileOutputStream(temp);
                    IOUtils.copy(snapshot.getInputStream(0), fos);
                    return ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY,
                            mHandler, new ParcelFileDescriptor.OnCloseListener() {
                                @Override
                                public void onClose(IOException e) {

                                }
                            });
                } catch (NullPointerException|IOException e) {
                    if (temp != null && temp.exists()) {
                        temp.delete();
                    }
                    e.printStackTrace();
                    throw new FileNotFoundException();
                } finally {
                    IOUtils.closeQuietly(fos);
                    if (snapshot != null) {
                        snapshot.close();
                    }
                }
        }
        return null;
    }

    private final LruCache<Long, String> cacheKeyCache = new LruCache<>(100);

    public String getCacheKey(long albumId) {
        String cacheKey = cacheKeyCache.get(albumId);
        if (!TextUtils.isEmpty(cacheKey)) {
            return cacheKey;
        }
        Cursor c = getContext().getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {
                        /* 0 */
                        BaseColumns._ID,
                        /* 1 */
                        MediaStore.Audio.AlbumColumns.ALBUM,
                        /* 2 */
                        MediaStore.Audio.AlbumColumns.ARTIST,
                },
                BaseColumns._ID + "=?",
                new String[]{ String.valueOf(albumId) },
                MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
        if (c != null && c.moveToFirst()) {
            cacheKey = ArtworkLoader.getCacheKey(
                    c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ALBUM)),
                    c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ARTIST)),
                    mScreenWidth, 0);
        }
        if (c != null) {
            c.close();
        }
        if (!TextUtils.isEmpty(cacheKey)) {
            cacheKeyCache.put(albumId, cacheKey);
        }
        return cacheKey;
    }
}
