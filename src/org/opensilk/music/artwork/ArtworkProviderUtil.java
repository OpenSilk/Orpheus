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

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.andrew.apollo.BuildConfig;

import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.artwork.cache.BitmapLruCache;

import java.io.IOException;

import hugo.weaving.DebugLog;

/**
 * Helper class for MusicPlaybackService to request and manage
 * artwork from the ArtworkProvider
 *
 * Created by drew on 3/23/14.
 */
public class ArtworkProviderUtil {
    private static final String TAG = ArtworkProviderUtil.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    /**
     * Default memory cache size as a percent of device memory class
     */
    private static final float THUMB_MEM_CACHE_DIVIDER = 0.08f;

    /**
     * Context
     */
    private Context mContext;

    /**
     * Services private image cache
     */
    private BitmapLruCache mL1Cache;

    public ArtworkProviderUtil(Context context) {
        mContext = context;
        initCache();
    }

    private void initCache() {
        final ActivityManager activityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        final int memClass = activityManager.getMemoryClass();
        final int lruThumbCacheSize = Math.round(THUMB_MEM_CACHE_DIVIDER * memClass * 1024 * 1024);
        if (D) Log.d(TAG, "thumbcache=" + ((float) lruThumbCacheSize / 1024 / 1024) + "MB");
        mL1Cache = new BitmapLruCache(lruThumbCacheSize);
    }

    /**
     * Fetches artwork from the ArtworkProvider, attempts to get fullscreen
     * artwork first, on failure tries to get a thumbnail
     *
     * @param artistName
     * @param albumName
     * @return Bitmap if found else null
     */
    @DebugLog
    public Bitmap getArtwork(String artistName, String albumName) {
        final String cacheKey = makeCacheKey(artistName, albumName,"LARGE");
        final Uri artworkUri = ArtworkProvider.createArtworkUri(artistName, albumName);
        Bitmap bitmap = queryArtworkProvider(artworkUri, cacheKey);
        if (bitmap == null) {
            // Fullscreen not available try the thumbnail for a temp fix
            bitmap = getArtworkThumbnail(artistName, albumName);
        }
        return bitmap;
    }

    /**
     * Fetches thumbnail from the ArtworkProvider
     * @param artistName
     * @param albumName
     * @return
     */
    @DebugLog
    public Bitmap getArtworkThumbnail(String artistName, String albumName) {
        final String cacheKey = makeCacheKey(artistName, albumName, "THUMB");
        final Uri artworkUri = ArtworkProvider.createArtworkThumbnailUri(artistName, albumName);
        return queryArtworkProvider(artworkUri, cacheKey);
    }

    /**
     * Queries ArtworkProvider for given uri, first checking local cache
     * @param artworkUri
     * @param cacheKey
     * @return Decoded bitmap
     */
    public Bitmap queryArtworkProvider(Uri artworkUri, String cacheKey) {
        Bitmap bitmap = mL1Cache.getBitmap(cacheKey);
        if (bitmap == null) {
            ParcelFileDescriptor pfd = null;
            try {
                pfd = mContext.getContentResolver().openFileDescriptor(artworkUri, "r");
                if (pfd != null) {
                    //Synchronize on the disk cache lock to better prevent OOMs
                    synchronized (BitmapDiskLruCache.sDecodeLock) {
                        try {
                            bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                        } catch (OutOfMemoryError e) {
                            bitmap = null;
                        }
                    }
                    if (bitmap != null) {
                        mL1Cache.putBitmap(cacheKey, bitmap);
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "" + e.getClass().getName() + " " + e.getMessage());
                bitmap = null;
            } finally {
                if (pfd != null) {
                    try {
                        pfd.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return bitmap;
    }

    /**
     * Generates a cache key for local  L1Cache
     */
    private static String makeCacheKey(String artistName, String albumName, String size) {
        return new StringBuilder((artistName != null ? artistName.length() : 4)
                + (albumName != null ? albumName.length() : 4) + 1
                + (size != null ? size.length() : 4))
                .append(artistName)
                .append(albumName)
                .append(size)
                .toString();
    }
}
