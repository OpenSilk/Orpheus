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

import android.graphics.Bitmap;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.android.volley.Request;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Single thread executor service to process canceled/missing requests
 * without clogging up the RequestQueue
 *
 * Created by drew on 3/26/14.
 */
public class BackgroundRequestor {
    private static final String TAG = "BGR";
    private static final boolean D = BuildConfig.DEBUG;

    static ThreadPoolExecutor EXECUTOR;

    static {
        initExecutor();
    }

    static void initExecutor() {
        EXECUTOR = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public static void add(String artist, String album, long albumId, ArtworkType imageType) {
        final String cacheKey = ArtworkLoader.getCacheKey(artist, album, imageType);
        ArtworkRequest request = new ArtworkRequest(artist, album, albumId, cacheKey, null, imageType, null);
        request.setPriority(Request.Priority.LOW);
        request.start();
        // Should be ok to discard our reference to request since its inner classes will hold their own
    }

    /**
     * Checks cache for existence of item, if its
     * not in the cache a background request is queued
     */
    static class CheckCacheRunnable implements Runnable {
        final ArtworkLoader.ImageCache cache;
        final String artist;
        final String album;
        final long albumId;
        final ArtworkType artworkType;

        CheckCacheRunnable(ArtworkLoader.ImageCache cache,
                              String artist, String album, long albumId,
                              ArtworkType artworkType) {
            this.artist = artist;
            this.album = album;
            this.albumId = albumId;
            this.cache = cache;
            this.artworkType = artworkType;
        }

        @Override
        public void run() {
            final String cacheKey = ArtworkLoader.getCacheKey(artist, album, artworkType);
            if (cache != null && !cache.containsKey(cacheKey)) {
                if (D) Log.d(TAG, "MediaStoreAltRunnable: run() queuing bgrequest for " + cacheKey);
                BackgroundRequestor.add(artist, album, albumId, artworkType);
            }
        }
    }

    /**
     * Adds bitmap to cache if its not in there yet
     */
    static class AddToCacheRunnable implements Runnable {
        final ArtworkLoader.ImageCache cache;
        final String key;
        final Bitmap bitmap;

        AddToCacheRunnable(ArtworkLoader.ImageCache cache, String key, Bitmap bitmap) {
            this.cache = cache;
            this.key = key;
            this.bitmap = bitmap;
        }

        @Override
        public void run() {
            if (cache != null && !cache.containsKey(key)) {
                if (D) Log.d(TAG, "AddToCacheRunnable: run() adding to cache " + key);
                cache.putBitmap(key, bitmap);
            }
        }
    }

}
