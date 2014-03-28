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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.andrew.apollo.BuildConfig;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;

import org.opensilk.music.artwork.cache.CacheUtil;

import java.io.File;

/**
 * Holder for Volley RequestQueue instance
 *
 * Created by drew on 3/12/14.
 */
public class RequestQueueFactory {

    /** Default on-disk cache directory. */
    private static final String DEFAULT_API_CACHE_DIR = "volley_api_cache";
    private static final String DEFAULT_IMAGE_CACHE_DIR = "volley_image_cache";

    // Way to big, make sure to cache everything, to help avoid rate limiting
    private static final int DEFAULT_API_CACHE_SIZE = 20 * 1024 * 1024;
    // On the small side, image requests arent done with our api key so dont risk hitting rate limit
    private static final int DEFAULT_IMAGE_CACHE_SIZE = 8 * 1024 * 1024;

    /** Queue instance */
    private static RequestQueue sApiQueue = null;
    private static RequestQueue sImageQueue = null;
    private static RequestQueue sBackgroundQueue = null;

    private RequestQueueFactory() {
        /*never instantiate*/
    }

    /**
     * Call in application onCreate to init the requestQueue instance
     * @param context
     */
    public static synchronized void create(Context context) {
        sApiQueue = newRequestQueue(context.getApplicationContext(), DEFAULT_API_CACHE_SIZE, DEFAULT_API_CACHE_DIR, 2);
        sImageQueue = newRequestQueue(context.getApplicationContext(), DEFAULT_IMAGE_CACHE_SIZE, DEFAULT_API_CACHE_DIR, 4);
    }

    /**
     * @return The request queue
     */
    public static RequestQueue getQueue() {
        if (sApiQueue != null) {
            return sApiQueue;
        }
        throw new RuntimeException("Must call RequestQueueManager.create()");
    }

    /**
     * Returns the request queue creating it if needed;
     * @param context
     * @return
     */
    public static RequestQueue getQueue(Context context) {
        if (sApiQueue == null) {
            create(context);
        }
        return sApiQueue;
    }

    /**
     * Stops the queue and clears the singleton
     */
    public static void destroy() {
        if (sApiQueue != null) {
            sApiQueue.cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(Request<?> request) {
                    return true; //Everyone gets the ax
                }
            });
            sApiQueue.stop();
            sApiQueue = null;
        }
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * Same as Volley.newRequestQueue with support for specifying cache size
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack An {@link com.android.volley.toolbox.HttpStack} to use for the network, or null for default.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, int cacheSize, String cacheSubDir, int poolSize) {
        File cacheDir = CacheUtil.getCacheDir(context, cacheSubDir);
        Network network = new BasicNetwork(new HurlStack());
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir, cacheSize), network, poolSize);
        queue.start();
        return queue;
    }
}
