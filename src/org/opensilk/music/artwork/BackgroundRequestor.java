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
                cache.putBitmap(key, bitmap);
            }
        }
    }

}
