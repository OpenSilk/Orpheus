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
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.artwork.cache.BitmapLruCache;
import org.opensilk.music.artwork.cache.CacheUtil;
import org.opensilk.silkdagger.qualifier.ForApplication;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 6/21/14.
 */
@Module (
        injects = {
                ArtworkServiceImpl.class,
                ArtworkProvider.class,
                ArtworkBroadcastReceiver.class,
        },
        complete = false,
        library = true
)
public class ArtworkModule {

    @Provides @Singleton
    public ArtworkService provideArtworkService(ArtworkServiceImpl impl) {
        return impl;
    }

    @Provides @Singleton
    public ArtworkManager provideArtworkManager(@ForApplication Context context) {
        return ArtworkManager.getInstance(context);
    }

    @Provides @Singleton
    public RequestQueue provideRequestQueue(@ForApplication Context context) {
        RequestQueue q = Volley.newRequestQueue(context);
        q.start();
        return q;
    }

    @Provides @Singleton
    public BitmapLruCache provideBitmapLruCache(@ForApplication Context context) {
        return new BitmapLruCache(getL1CacheSize(context));
    }

    @Provides @Singleton
    public BitmapDiskLruCache provideBitmapDiskLruCache(@ForApplication Context context, AppPreferences preferences) {
        final int size = Integer.decode(preferences.getString(AppPreferences.IMAGE_DISK_CACHE_SIZE, "60")) * 1024 * 1024;
        return BitmapDiskLruCache.open(CacheUtil.getCacheDir(context, DISK_CACHE_DIRECTORY),
                size, Bitmap.CompressFormat.PNG, 100);
    }

    private static final float THUMB_MEM_CACHE_DIVIDER = 0.20f;
    public static final String DISK_CACHE_DIRECTORY = "artworkcache";

    private static int getL1CacheSize(Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final int memClass = context.getResources().getBoolean(R.bool.config_largeHeap) ?
                activityManager.getLargeMemoryClass() : activityManager.getMemoryClass();
        return Math.round(THUMB_MEM_CACHE_DIVIDER * memClass * 1024 * 1024);
    }
}
