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

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.android.volley.RequestQueue;

import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.artwork.cache.BitmapLruCache;
import org.opensilk.music.artwork.cache.CacheUtil;
import org.opensilk.volley.RequestQueueManager;

import java.io.File;
import java.io.IOException;

/**
 * Singleton class used to manager everything related to loading/fetching
 * artwork. This class contains our volley RequestQueue and Cache Instances
 *
 * This class must be created in the Application! The prefered method of use
 * is through static methods, though you can call getInstance() without providing
 * a context so this class must be created before anyone uses it!
 *
 * Created by drew on 3/23/14.
 */
public class ArtworkManager {
    private static final String TAG = ArtworkManager.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    /**
     * Default memory cache size as a percent of device memory class
     */
    private static final float THUMB_MEM_CACHE_DIVIDER = 0.20f;

    private static final String DISK_CACHE_DIRECTORY = "artworkcache";
    private static final int DISK_CACHE_SIZE = 35 * 1024 * 1024;

    private final Context mContext;
    private final RequestQueue mRequestQueue;
    private ArtworkLoader mLoader;
    private BitmapLruCache mL1Cache;
    private BitmapDiskLruCache mL2Cache;
    private final PreferenceUtils mPreferences;

    private static ArtworkManager sArtworkManager;

    /**
     * Call in Application.OnCreate()
     * @param context ApplicationContext
     */
    public static synchronized void create(Context context) {
        if (sArtworkManager == null) {
            sArtworkManager = new ArtworkManager(context);
        }
    }

    /**
     * @return ArtworkManager instance
     */
    public static ArtworkManager getInstance() {
        return sArtworkManager;
    }

    /**
     * Returns ArtworkManager instance creating it if needed,
     * used my ArtworkService to insure we dont get a null reference
     * @param context
     * @return
     */
    public static ArtworkManager getInstance(Context context) {
        if (sArtworkManager == null) {
            create(context);
        }
        return sArtworkManager;
    }

    private ArtworkManager(Context context) {
        mContext = context.getApplicationContext();
        mRequestQueue = RequestQueueManager.getQueue(context);
        mPreferences = PreferenceUtils.getInstance(context);
        // Don't block create
        new Thread(new Runnable() {
            @Override
            public void run() {
                initCaches();
            }
        }).start();
    }

    /**
     * Setups L1 and L2 caches
     */
    private void initCaches() {
        final ActivityManager activityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        int memClass = mContext.getResources().getBoolean(R.bool.config_largeHeap) ?
                activityManager.getLargeMemoryClass() : activityManager.getMemoryClass();
        final int lruThumbCacheSize = Math.round(THUMB_MEM_CACHE_DIVIDER * memClass * 1024 * 1024);
        if (D) Log.d(TAG, "thumbcache=" + ((float) lruThumbCacheSize / 1024 / 1024) + "MB");
        mL1Cache = new BitmapLruCache(lruThumbCacheSize);
        mLoader = new ArtworkLoader(mL1Cache);
        mL2Cache = new BitmapDiskLruCache(CacheUtil.getCacheDir(mContext, DISK_CACHE_DIRECTORY),
                DISK_CACHE_SIZE, Bitmap.CompressFormat.PNG, 100);
        cleanupOldCache();
    }

    /**
     * Initiates loading artist image into view
     */
    public static boolean loadArtistImage(final String artistName, final ArtworkImageView imageView) {
        if (sArtworkManager == null) {
            return false;
        }
        imageView.setImageInfo(artistName, null, sArtworkManager.getLoader());
        return true;
    }

    /**
     * Initiates loading of album image into view
     */
    public static boolean loadAlbumImage(final String artistName, final String albumName,
                                         final long albumId, final ArtworkImageView imageView) {
        if (sArtworkManager == null) {
            return false;
        }
        imageView.setImageInfo(artistName, albumName, sArtworkManager.getLoader());
        return true;
    }

    /**
     * Initiates loading of current album image into view;
     */
    public static boolean loadCurrentArtwork(final ArtworkImageView imageView) {
        return loadAlbumImage(MusicUtils.getArtistName(), MusicUtils.getAlbumName(),
                MusicUtils.getCurrentAlbumId(), imageView);
    }

    /**
     * @return Memory cache interface
     */
    public ArtworkLoader.ImageCache getL1Cache() {
        return mL1Cache;
    }

    /**
     * @return Diskcache interface
     */
    public ArtworkLoader.ImageCache getL2Cache() {
        return mL2Cache;
    }

    /**
     * @return The BitmapDiskLruCache instance
     */
    public BitmapDiskLruCache getDiskCache() {
        return mL2Cache;
    }

    public PreferenceUtils getPreferences() {
        return mPreferences;
    }

    public RequestQueue getQueue() {
        return mRequestQueue;
    }

    public ArtworkLoader getLoader() {
        return mLoader;
    }

    //TODO remove in 0.4
    private static final String OLD_DOWNLOAD_CACHE = "DownloadCache";
    private static final String OLD_IMAGE_CACHE = "ThumbnailCache";
    private void cleanupOldCache() {
        try {
//            if (!mPreferences.isOldCacheDeleted()) {
                File oldCache = CacheUtil.getCacheDir(mContext, OLD_DOWNLOAD_CACHE);
                if (oldCache != null && oldCache.exists() && oldCache.isDirectory()) {
                    deleteContents(oldCache);
                }
                oldCache = CacheUtil.getCacheDir(mContext, OLD_IMAGE_CACHE);
                if (oldCache != null && oldCache.exists() && oldCache.isDirectory()) {
                    deleteContents(oldCache);
                }
                mPreferences.setOldCacheDeleted();
//            }
        } catch (Exception ignored) {}
    }

    /**
     * Deletes the contents of {@code dir}. Throws an IOException if any file
     * could not be deleted, or if {@code dir} is not a readable directory.
     */
    private static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("not a readable directory: " + dir);
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (!file.delete()) {
                throw new IOException("failed to delete file: " + file);
            }
        }
    }

}
