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

import com.andrew.apollo.BuildConfig;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import org.opensilk.music.artwork.cache.CacheUtil;

import java.io.File;

/**
 *
 * Created by drew on 3/12/14.
 */
public class RequestQueueFactory {

    /** Default on-disk cache directory. */
    private static final String DEFAULT_API_CACHE_DIR = "0";
    private static final String DEFAULT_IMAGE_CACHE_DIR = "1";

    // We want to cache all api calls until they expire, to help avoid rate limiting
    private static final int DEFAULT_API_CACHE_SIZE = 5 * 1024 * 1024;
    // On the small side, image requests arent done with our api key so dont risk hitting rate limit
    private static final int DEFAULT_IMAGE_CACHE_SIZE = 8 * 1024 * 1024;

    private RequestQueueFactory() {
        /*never instantiate*/
    }

    static RequestQueue newApiQueue(Context context) {
        return newRequestQueue(context.getApplicationContext(), DEFAULT_API_CACHE_SIZE, DEFAULT_API_CACHE_DIR, 2);
    }

    static RequestQueue newImageQueue(Context context) {
        return newRequestQueue(context.getApplicationContext(), DEFAULT_IMAGE_CACHE_SIZE, DEFAULT_IMAGE_CACHE_DIR, 4);
    }

    /**
     * Stops the queue and clears the singleton
     */
    public static void destroy(RequestQueue queue) {
        queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true; //Everyone gets the ax
            }
        });
        queue.stop();
    }

    public static RequestQueue newRequestQueue(Context context, int cacheSize, String cacheSubDir, int poolSize) {
        File cacheDir = new File(CacheUtil.getCacheDir(context, "volley"), cacheSubDir);
        Network network = new BasicNetwork(new HurlStack());
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir, cacheSize), network, poolSize);
        queue.start();
        return queue;
    }
}
