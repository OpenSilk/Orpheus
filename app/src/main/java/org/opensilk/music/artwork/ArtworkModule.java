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

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.MusicApp;
import org.opensilk.music.R;
import org.opensilk.music.artwork.cache.ArtworkCache;
import org.opensilk.music.artwork.cache.ArtworkLruCache;
import org.opensilk.music.artwork.cache.BitmapCache;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.artwork.cache.CacheUtil;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

/**
 * Created by drew on 6/21/14.
 */
@Module (
        injects = {
                ArtworkProvider.class,
        },
        complete = false,
        library = true
)
public class ArtworkModule {

    private static final int VOLLEY_CACHE_SIZE = 16 * 1024 * 1024;
    private static final String VOLLEY_CACHE_DIR = "volley/1";
    private static final int VOLLEY_POOL_SIZE = 4;
    private static final int VOLLEY_POOL_SIZE_SMALL = 2;

    static final float THUMB_MEM_CACHE_DIVIDER = 0.15f;
    public static final String DISK_CACHE_DIRECTORY = "artworkcache";

    @Provides @Singleton
    public ArtworkRequestManager provideArtworkRequestManager(ArtworkRequestManagerImpl impl) {
        return impl;
    }

    @Provides @Singleton
    public RequestQueue provideRequestQueue(@ForApplication Context context) {
        final int poolSize = MusicApp.isLowEndHardware(context) ? VOLLEY_POOL_SIZE_SMALL : VOLLEY_POOL_SIZE;
        RequestQueue queue = new RequestQueue(
                new DiskBasedCache(CacheUtil.getCacheDir(context, VOLLEY_CACHE_DIR), VOLLEY_CACHE_SIZE),
                new BasicNetwork(new HurlStack()),
                poolSize
        );
        queue.start();
        return queue;
    }

    @Provides @Singleton @Named("L1Cache")
    public ArtworkCache provideArtworkLruCache(@ForApplication Context context) {
        return new ArtworkLruCache(calculateL1CacheSize(context, false));
    }

    @Provides @Singleton @Named("L2Cache") //TODO when/how to close this?
    public BitmapDiskCache provideBitmapDiskLruCache(@ForApplication Context context, AppPreferences preferences) {
        final int size = Integer.decode(preferences.getString(AppPreferences.IMAGE_DISK_CACHE_SIZE, "60")) * 1024 * 1024;
        return BitmapDiskLruCache.open(
                CacheUtil.getCacheDir(context, DISK_CACHE_DIRECTORY),
                size, Bitmap.CompressFormat.PNG, 100
        );
    }

    public static int calculateL1CacheSize(Context context, boolean forceLarge) {
        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final int memClass = (forceLarge || !MusicApp.isLowEndHardware(context)) ? am.getLargeMemoryClass() : am.getMemoryClass();
        return Math.round(THUMB_MEM_CACHE_DIVIDER * memClass * 1024 * 1024);
    }
}
