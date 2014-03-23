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

package org.opensilk.volley;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.andrew.apollo.BuildConfig;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;

import java.io.File;

/**
 * Holder for Volley RequestQueue instance
 *
 * Created by drew on 3/12/14.
 */
public class RequestQueueManager {

    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "volley";

    /** Defualt size for disk cache */
    private static final int DEFAULT_CACHE_SIZE = (BuildConfig.DEBUG ? 30 : 12) * 1024 * 1024;

    /** Queue instance */
    private static RequestQueue sRequestQueue = null;

    private RequestQueueManager() {
        /*never instantiate*/
    }

    /**
     * Call in application onCreate to init the requestQueue instance
     * @param context
     */
    public static synchronized void create(Context context) {
        sRequestQueue = newRequestQueue(context.getApplicationContext(), null, DEFAULT_CACHE_SIZE);
    }

    /**
     * @return The request queue
     */
    public static RequestQueue getQueue() {
        if (sRequestQueue != null) {
            return sRequestQueue;
        }
        throw new RuntimeException("Must call RequestQueueManager.create()");
    }

    /**
     * Returns the request queue creating it if needed;
     * @param context
     * @return
     */
    public static RequestQueue getQueue(Context context) {
        if (sRequestQueue == null) {
            create(context);
        }
        return sRequestQueue;
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
    public static RequestQueue newRequestQueue(Context context, HttpStack stack, int cacheSize) {
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);

        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }

        if (stack == null) {
            if (Build.VERSION.SDK_INT >= 9) {
                stack = new HurlStack();
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
            }
        }

        Network network = new BasicNetwork(stack);

        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir, cacheSize), network);
        queue.start();

        return queue;
    }
}
