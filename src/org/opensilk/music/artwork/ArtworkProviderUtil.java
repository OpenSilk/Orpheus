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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import org.opensilk.music.R;
import org.opensilk.music.artwork.cache.BitmapLruCache;

import java.io.IOException;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Helper class for MusicPlaybackService to request and manage
 * artwork from the ArtworkProvider
 *
 * Created by drew on 3/23/14.
 */
public class ArtworkProviderUtil {
    private static final Object sDecodeLock = new Object();

    private final Context mContext;
    private final BitmapLruCache mL1Cache;

    public ArtworkProviderUtil(Context context) {
        mContext = context;
        mL1Cache = new BitmapLruCache(ArtworkModule.calculateL1CacheSize(context, true));
    }

    private Bitmap getDefaultArt() {
        return ((BitmapDrawable) mContext.getResources().getDrawable(R.drawable.default_artwork)).getBitmap();
    }

    /**
     * Fetches artwork from the ArtworkProvider, attempts to get fullscreen
     * artwork first, on failure tries to get a thumbnail
     * @return Bitmap if found else null
     */
    //@DebugLog
    public Bitmap getArtwork(String artistName, String albumName) {
        if (artistName == null || albumName == null) {
            return getDefaultArt();
        }
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
     * @return Bitmap if found else default artwork
     */
    //@DebugLog
    public Bitmap getArtworkThumbnail(String artistName, String albumName) {
        Timber.d("getArtworkThumbnail("+artistName+", "+albumName+")");
        if (artistName == null || albumName == null) {
            return getDefaultArt();
        }
        final String cacheKey = makeCacheKey(artistName, albumName, "THUMB");
        final Uri artworkUri = ArtworkProvider.createArtworkThumbnailUri(artistName, albumName);
        Bitmap bitmap = queryArtworkProvider(artworkUri, cacheKey);
        if (bitmap == null) {
            bitmap = getDefaultArt();
        }
        return bitmap;
    }

    /**
     * Queries ArtworkProvider for given uri, first checking local cache
     * @return Decoded bitmap
     */
    public Bitmap queryArtworkProvider(Uri artworkUri, String cacheKey) {
        Bitmap bitmap = mL1Cache.getBitmap(cacheKey);
        if (bitmap == null) {
            ParcelFileDescriptor pfd = null;
            try {
                pfd = mContext.getContentResolver().openFileDescriptor(artworkUri, "r");
                if (pfd != null) {
                    synchronized (sDecodeLock) {
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
                Timber.w(e, "queryArtworkProvider()");
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

    @DebugLog
    public void evict() {
        mL1Cache.evictAll();
        Runtime.getRuntime().gc();
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
