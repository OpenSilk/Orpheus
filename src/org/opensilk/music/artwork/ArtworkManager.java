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
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.android.volley.RequestQueue;

import org.apache.commons.io.FileUtils;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.artwork.cache.BitmapLruCache;
import org.opensilk.music.artwork.cache.CacheUtil;

import java.io.File;

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
    private static final int DISK_CACHE_SIZE = 40 * 1024 * 1024;

    /**
     * Uri for album thumbs
     */
    static final Uri sArtworkUri;
    static {
        sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    }

    final Context mContext;
    final ArtworkLoader mLoader;
    final BitmapLruCache mL1Cache;
    BitmapDiskLruCache mL2Cache;
    final RequestQueue mApiQueue;
    final RequestQueue mImageQueue;
    final PreferenceUtils mPreferences;
    final BackgroundRequestor mBackgroundRequestor;

    /**
     * Singleton instance
     */
    private static ArtworkManager sArtworkManager;

    /**
     * @return ArtworkManager instance
     */
    public static synchronized ArtworkManager getInstance() {
        return sArtworkManager;
    }

    /**
     * Our ArtworkService uses this to create the instance, it is started
     * by the ArtworkProvider very early so we can be relatively sure it
     * will be available when the ArtworkRequest calls getInstance();
     * @param context
     * @return
     */
    public static synchronized ArtworkManager getInstance(Context context) {
        if (sArtworkManager == null) {
            sArtworkManager = new ArtworkManager(context);
        }
        return sArtworkManager;
    }

    private ArtworkManager(Context context) {
        mContext = context.getApplicationContext();
        // Fire off l2 init early
        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
                mL2Cache = new BitmapDiskLruCache(CacheUtil.getCacheDir(mContext, DISK_CACHE_DIRECTORY),
                        DISK_CACHE_SIZE, Bitmap.CompressFormat.PNG, 100);
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                cleanupOldCache();
            }
        }).start();
        mPreferences = PreferenceUtils.getInstance(context);
        // Init mem cache
        final int lruThumbCacheSize = getL1CacheSize(context);
        if (D) Log.d(TAG, "L1Cache=" + ((float) lruThumbCacheSize / 1024 / 1024) + "MB");
        mL1Cache = new BitmapLruCache(lruThumbCacheSize);
        mLoader = new ArtworkLoader(mL1Cache);
        // init queues
        mApiQueue = RequestQueueFactory.newApiQueue(context);
        mImageQueue = RequestQueueFactory.newImageQueue(context);
        // init background requestor
        mBackgroundRequestor = new BackgroundRequestor();
    }

    private static int getL1CacheSize(Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final int memClass = context.getResources().getBoolean(R.bool.config_largeHeap) ?
                activityManager.getLargeMemoryClass() : activityManager.getMemoryClass();
        return Math.round(THUMB_MEM_CACHE_DIVIDER * memClass * 1024 * 1024);
    }

    /**
     * Cleans up caches, stops the volley queue, and clears the singleton
     */
    /*package*/ static synchronized void destroy() {
        if (sArtworkManager != null) {
            RequestQueueFactory.destroy(sArtworkManager.mApiQueue);
            RequestQueueFactory.destroy(sArtworkManager.mImageQueue);
            sArtworkManager.mBackgroundRequestor.mExecutor.shutdownNow();
            sArtworkManager.mL1Cache.evictAll();
            sArtworkManager.mL2Cache.close();
            sArtworkManager = null;
        }
    }

    /**
     * Initiates loading artist image into view
     */
    public static boolean loadArtistImage(final String artistName, final ArtworkImageView imageView) {
        if (sArtworkManager == null) {
            return false;
        }
        imageView.setImageInfo(artistName, null, sArtworkManager.mLoader);
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
        imageView.setImageInfo(artistName, albumName, sArtworkManager.mLoader);
        return true;
    }

    /**
     * Initiates loading of current album image into view;
     */
    public static boolean loadCurrentArtwork(final ArtworkImageView imageView) {
        return loadAlbumImage(MusicUtils.getAlbumArtistName(), MusicUtils.getAlbumName(),
                MusicUtils.getCurrentAlbumId(), imageView);
    }

    public static Bitmap getAlbumImageFromMediaStore(long albumId, boolean isThumbnail) {
        if (sArtworkManager == null) {
            return null;
        }
        return MediaStore.Images.Thumbnails.getThumbnail(sArtworkManager.mContext.getContentResolver(),
                albumId,
                isThumbnail ? MediaStore.Images.Thumbnails.MINI_KIND : MediaStore.Images.Thumbnails.FULL_SCREEN_KIND,
                null);
    }

    //TODO remove in 0.4
    private static final String OLD_DOWNLOAD_CACHE = "DownloadCache";
    private static final String OLD_IMAGE_CACHE = "ThumbnailCache";
    private void cleanupOldCache() {
        try {
            if (!mPreferences.isOldCacheDeleted()) {
                File oldCache = CacheUtil.getCacheDir(mContext, OLD_DOWNLOAD_CACHE);
                if (oldCache != null && oldCache.exists() && oldCache.isDirectory()) {
                    FileUtils.deleteDirectory(oldCache);
                }
                oldCache = CacheUtil.getCacheDir(mContext, OLD_IMAGE_CACHE);
                if (oldCache != null && oldCache.exists() && oldCache.isDirectory()) {
                    FileUtils.deleteDirectory(oldCache);
                }
                mPreferences.setOldCacheDeleted();
            }
        } catch (Exception ignored) {}
    }

}
