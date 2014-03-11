/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.cache;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentCallbacks2;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.WindowManager;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.umass.util.StringUtilities;

/**
 * This class holds the memory and disk bitmap caches.
 */
public final class ImageCache {

    private static final String TAG = ImageCache.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    /**
     * The {@link Uri} used to retrieve album art
     */
    public static final Uri mArtworkUri;

    /**
     * Default memory cache size as a percent of device memory class
     */
    private static final float THUMB_MEM_CACHE_DIVIDER = 0.25f;

    /**
     * Default memory cache size as a percent of device memory class
     */
    private static final float LA_MEM_CACHE_DIVIDER = 0.08f;

    /**
     * Default disk cache size 10MB
     */
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 30;

    /**
     * Compression settings when writing images to disk cache
     */
    private static final CompressFormat COMPRESS_FORMAT = CompressFormat.JPEG;

    /**
     * Disk cache index to read from
     */
    private static final int DISK_CACHE_INDEX = 0;

    /**
     * Image compression quality
     */
    private static final int COMPRESS_QUALITY = 98;

    /**
     * Maximum sizes for artwork
     */
    public final int mDefaultMaxImageHeight;
    public final int mDefaultMaxImageWidth;

    /**
     * Largest size a thumbnail will be
     */
    public  final int DEFAULT_THUMBNAIL_SIZE_DP = 200; // Largest size of any thumbnail displayed
    public final int mDefaultThumbnailSizePx;

    /** Directory diskcache is stored in */
    public static final String THUMBNAIL_CACHE_DIR = "ThumbnailCache";
    /** Directory downloaded art is stored in */
    public static final String DOWNLOAD_CACHE_DIR = "DownloadCache";

    /**
     * Context
     */
    private final Context mContext;

    /**
     * LRU cache for thumbnails
     */
    private MemoryCache mThumbnailMemCache;

    /**
     * LRU cache for fullscreen art
     */
    private MemoryCache mLargeArtMemCache;

    /**
     * Disk LRU cache
     */
    private DiskLruCache mThumbnailDiskCache;

    private static ImageCache sInstance;

    /**
     * Used to temporarily pause the disk cache while scrolling
     */
    public boolean mPauseDiskAccess = false;
    private Object mPauseLock = new Object();

    static {
        mArtworkUri = Uri.parse("content://media/external/audio/albumart");
    }

    /**
     * Constructor of <code>ImageCache</code>
     *
     * @param context The {@link Context} to use
     */
    public ImageCache(final Context context) {
        mContext = context;
        mDefaultMaxImageHeight = mDefaultMaxImageWidth = getMaxDisplaySize(context);
        mDefaultThumbnailSizePx = convertDpToPx(context, DEFAULT_THUMBNAIL_SIZE_DP);
        if (D) Log.d(TAG, "mx=" + mDefaultMaxImageWidth + " my=" + mDefaultMaxImageHeight + " mt=" + mDefaultThumbnailSizePx);
        init(context);
    }

    /**
     * Used to create a singleton of {@link ImageCache}
     *
     * @param context The {@link Context} to use
     * @return A new instance of this class.
     */
    public final static ImageCache getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new ImageCache(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Initialize the cache, providing all parameters.
     *
     * @param context The {@link Context} to use
     * @param cacheParams The cache parameters to initialize the cache
     */
    private void init(final Context context) {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... unused) {
                // Initialize the disk cahe in a background thread
                initDiskCache(context);
                return null;
            }
        }, (Void[])null);
        // Set up the memory cache
        initLruCache(context);
    }

    /**
     * Initializes the disk cache. Note that this includes disk access so this
     * should not be executed on the main/UI thread. By default an ImageCache
     * does not initialize the disk cache when it is created, instead you should
     * call initDiskCache() to initialize it on a background thread.
     *
     * @param context The {@link Context} to use
     */
    private synchronized void initDiskCache(final Context context) {
        // Set up disk cache
        if (mThumbnailDiskCache == null || mThumbnailDiskCache.isClosed()) {
            File diskCacheDir = getDiskCacheDir(context, THUMBNAIL_CACHE_DIR);
            if (diskCacheDir != null) {
                if (!diskCacheDir.exists()) {
                    diskCacheDir.mkdirs();
                }
                if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
                    try {
                        mThumbnailDiskCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                    } catch (final IOException e) {
                        diskCacheDir = null;
                    }
                }
            }
        }
    }

    /**
     * Sets up the Lru cache
     *
     * @param context The {@link Context} to use
     */
    @SuppressLint("NewApi")
    public void initLruCache(final Context context) {
        final ActivityManager activityManager = (ActivityManager)context
                .getSystemService(Context.ACTIVITY_SERVICE);
        int memClass = context.getResources().getBoolean(R.bool.config_largeHeap) ?
                activityManager.getLargeMemoryClass() : activityManager.getMemoryClass();
        final int lruThumbCacheSize = Math.round(THUMB_MEM_CACHE_DIVIDER * memClass * 1024 * 1024);
        // Large art use max of 4mb since we dont use it for very many things
        final int lruLACacheSize = Math.min(Math.round(LA_MEM_CACHE_DIVIDER * memClass * 1024 * 1024), (6*1024*1024));
        if (D) Log.d(TAG, "thumbcache=" + ((float)lruThumbCacheSize/1024/1024) + "MB largeartcache=" + ((float)lruLACacheSize/1024/1024) + "MB");
        mThumbnailMemCache = new MemoryCache(lruThumbCacheSize);
        mLargeArtMemCache = new MemoryCache(lruLACacheSize);
        // Release some memory as needed
        context.registerComponentCallbacks(new ComponentCallbacks2() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void onTrimMemory(final int level) {
                if (level >= TRIM_MEMORY_MODERATE) {
                    evictAll();
                } else if (level >= TRIM_MEMORY_BACKGROUND) {
                    mThumbnailMemCache.trimToSize(mThumbnailMemCache.size() / 2);
                    mLargeArtMemCache.trimToSize(-1); //Nuke it
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void onLowMemory() {
                // Nothing to do
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void onConfigurationChanged(final Configuration newConfig) {
                // Nothing to do
            }
        });
    }

    /**
     * Find and return an existing ImageCache stored in a {@link RetainFragment}
     * , if not found a new one is created using the supplied params and saved
     * to a {@link RetainFragment}
     *
     * @param activity The calling {@link FragmentActivity}
     * @return An existing retained ImageCache object or a new one if one did
     *         not exist
     */
    public static final ImageCache findOrCreateCache(final Activity activity) {

        // Search for, or create an instance of the non-UI RetainFragment
        final RetainFragment retainFragment = findOrCreateRetainFragment(
                activity.getFragmentManager());

        // See if we already have an ImageCache stored in RetainFragment
        ImageCache cache = (ImageCache)retainFragment.getObject();

        // No existing ImageCache, create one and store it in RetainFragment
        if (cache == null) {
            cache = getInstance(activity);
            retainFragment.setObject(cache);
        }
        return cache;
    }

    /**
     * Locate an existing instance of this {@link Fragment} or if not found,
     * create and add it using {@link FragmentManager}
     *
     * @param fm The {@link FragmentManager} to use
     * @return The existing instance of the {@link Fragment} or the new instance
     *         if just created
     */
    public static final RetainFragment findOrCreateRetainFragment(final FragmentManager fm) {
        // Check to see if we have retained the worker fragment
        RetainFragment retainFragment = (RetainFragment)fm.findFragmentByTag(TAG);

        // If not retained, we need to create and add it
        if (retainFragment == null) {
            retainFragment = new RetainFragment();
            fm.beginTransaction().add(retainFragment, TAG).commit();
        }
        return retainFragment;
    }

    @Deprecated
    public void addBitmapToCache(final String data, final Bitmap bitmap) {
        addBitmapToCache(data, bitmap, true);
    }

    /**
     * Adds a new image to the memory and disk caches
     *
     * @param data The key used to store the image
     * @param bitmap The {@link Bitmap} to cache
     */
    public void addBitmapToCache(final String data, final Bitmap bitmap, final boolean isThumbnail) {
        if (data == null || bitmap == null) {
            return;
        }

        // Add to memory cache
        addBitmapToMemCache(data, bitmap, isThumbnail);

        if (!isThumbnail) {
            return; //Large art has no diskcache
        }

        // Add to disk cache
        if (mThumbnailDiskCache != null && !mThumbnailDiskCache.isClosed()) {
            final String key = hashKeyForDisk(data);
            OutputStream out = null;
            try {
                final DiskLruCache.Snapshot snapshot = mThumbnailDiskCache.get(key);
                if (snapshot == null) {
                    final DiskLruCache.Editor editor = mThumbnailDiskCache.edit(key);
                    if (editor != null) {
                        out = editor.newOutputStream(DISK_CACHE_INDEX);
                        bitmap.compress(COMPRESS_FORMAT, COMPRESS_QUALITY, out);
                        editor.commit();
                        out.close();
                        flush();
                    }
                } else {
                    snapshot.getInputStream(DISK_CACHE_INDEX).close();
                }
            } catch (final IOException e) {
                Log.e(TAG, "addBitmapToCache - " + e);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                        out = null;
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "addBitmapToCache - " + e);
                } catch (final IllegalStateException e) {
                    Log.e(TAG, "addBitmapToCache - " + e);
                }
            }
        }
    }

    @Deprecated
    public void addBitmapToMemCache(final String data, final Bitmap bitmap) {
        addBitmapToMemCache(data, bitmap, true);
    }

    /**
     * Called to add a new image to the memory cache
     *
     * @param data The key identifier
     * @param bitmap The {@link Bitmap} to cache
     */
    public void addBitmapToMemCache(final String data, final Bitmap bitmap, boolean isThumbnail) {
        if (data == null || bitmap == null) {
            return;
        }
        // Add to memory cache
        if (getBitmapFromMemCache(data, isThumbnail) == null) {
            if (isThumbnail) {
                mThumbnailMemCache.put(data, bitmap);
                if (D) Log.d(TAG, "ThumbCache: " + mThumbnailMemCache.toString());
            } else {
                mLargeArtMemCache.put(data, bitmap);
                if (D) Log.e(TAG, "LargeArtCache: " + mLargeArtMemCache.toString());
            }
        }
    }

    @Deprecated
    public final Bitmap getBitmapFromMemCache(final String data) {
        return getBitmapFromMemCache(data, true);
    }

    /**
     * Fetches a cached image from the memory cache
     *
     * @param data Unique identifier for which item to get
     * @return The {@link Bitmap} if found in cache, null otherwise
     */
    public final Bitmap getBitmapFromMemCache(final String data, final boolean isThumbnail) {
        if (data == null) {
            return null;
        }
        if (isThumbnail) {
            if (mThumbnailMemCache != null) {
                final Bitmap lruBitmap = mThumbnailMemCache.get(data);
                if (lruBitmap != null) {
                    return lruBitmap;
                }
            }
        } else {
            if (mLargeArtMemCache != null) {
                final Bitmap lruBitmap = mLargeArtMemCache.get(data);
                if (lruBitmap != null) {
                    return lruBitmap;
                }
            }
        }

        return null;
    }

    /**
     * Fetches a cached image from the disk cache
     *
     * @param data Unique identifier for which item to get
     * @return The {@link Bitmap} if found in cache, null otherwise
     */
    public final Bitmap getBitmapFromDiskCache(final String data) {
        if (data == null) {
            return null;
        }

        // Check in the memory cache here to avoid going to the disk cache less
        // often
        if (getBitmapFromMemCache(data) != null) {
            return getBitmapFromMemCache(data);
        }

        waitUntilUnpaused();
        final String key = hashKeyForDisk(data);
        if (mThumbnailDiskCache != null && !mThumbnailDiskCache.isClosed()) {
            InputStream inputStream = null;
            try {
                final DiskLruCache.Snapshot snapshot = mThumbnailDiskCache.get(key);
                if (snapshot != null) {
                    inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                    if (inputStream != null) {
                        final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap != null) {
                            return bitmap;
                        }
                    }
                }
            } catch (final IOException e) {
                Log.e(TAG, "getBitmapFromDiskCache - " + e);
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (final IOException e) {
                }
            }
        }
        return null;
    }

    @Deprecated
    public final Bitmap getCachedBitmap(final String data) {
        return getCachedBitmap(data, true);
    }

    /**
     * Tries to return a cached image from memory cache before fetching from the
     * disk cache
     *
     * @param data Unique identifier for which item to get
     * @return The {@link Bitmap} if found in cache, null otherwise
     */
    public final Bitmap getCachedBitmap(final String data, boolean isThumbnail) {
        if (data == null) {
            return null;
        }
        Bitmap cachedImage = getBitmapFromMemCache(data, isThumbnail);
        if (cachedImage == null) {
            if (isThumbnail) {
                cachedImage = getBitmapFromDiskCache(data);
            } else {
                cachedImage = getLargeArtworkFromDownloadCache(data);
            }
        }
        if (cachedImage != null) {
            addBitmapToMemCache(data, cachedImage, isThumbnail);
            return cachedImage;
        }
        return null;
    }

    @Deprecated
    public final Bitmap getCachedArtwork(final Context context, final String key, final long id) {
        return getCachedArtwork(context, key, id, true);
    }

    /**
     * Tries to return the album art from memory cache and disk cache, before
     * calling {@code #getArtworkFromMediaStore(Context, String)} again
     *
     * @param context The {@link Context} to use
     * @param key The name of the album art
     * @param id The ID of the album to find artwork for
     * @return The artwork for an album
     */
    public final Bitmap getCachedArtwork(final Context context, final String key, final long id, final boolean isThubmnail) {
        if (context == null || key == null) {
            return null;
        }
        Bitmap cachedImage = getCachedBitmap(key, isThubmnail);
        if (cachedImage != null) {
            return cachedImage;
        }
        if (id >= 0) {
            cachedImage = getArtworkFromMediaStore(context, id);
        }
        if (cachedImage == null) {
            if (isThubmnail) {
                cachedImage = getArtworkFromDownloadCache(key);
            } else {
                cachedImage = getLargeArtworkFromDownloadCache(key);
            }
        }
        if (cachedImage != null) {
            addBitmapToMemCache(key, cachedImage, isThubmnail);
            return cachedImage;
        }
        return null;
    }

    /**
     * Used to fetch the artwork for an album locally from the user's device
     *
     * @param context The {@link Context} to use
     * @param albumID The ID of the album to find artwork for
     * @return The artwork for an album
     */
    public final Bitmap getArtworkFromMediaStore(final Context context, final long albumId) {
        if (albumId < 0) {
            return null;
        }
        Bitmap artwork = null;
        waitUntilUnpaused();
        try {
            final Uri uri = ContentUris.withAppendedId(mArtworkUri, albumId);
            final ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver()
                    .openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                artwork = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
            }
        } catch (final IllegalStateException e) {
            // Log.e(TAG, "IllegalStateExcetpion - getArtworkFromMediaStore - ", e);
        } catch (final FileNotFoundException e) {
            // Log.e(TAG, "FileNotFoundException - getArtworkFromMediaStore - ", e);
        } catch (final OutOfMemoryError evict) {
            // Log.e(TAG, "OutOfMemoryError - getArtworkFromMediaStore - ", evict);
            evictAll();
        } catch (IOException e) {
            // pass
        }
        return artwork;
    }

    /**
     * Returns bitmap from downloadcache suitable for thumbnail use
     * @param key
     * @return
     */
    public final Bitmap getArtworkFromDownloadCache(String key) {
        File f = new File(getDiskCacheDir(mContext, DOWNLOAD_CACHE_DIR), hashKeyForDisk(key));
        if (f.exists()) {
            return decodeSampledBitmapFromFile(f.toString(), mDefaultThumbnailSizePx, mDefaultThumbnailSizePx);
        }
        return null;
    }

    /**
     * Returns bitmap from downloadcache sampled down if neccessary for full screen display
     * @param key
     * @return
     */
    public final Bitmap getLargeArtworkFromDownloadCache(String key) {
        File f = new File(getDiskCacheDir(mContext, DOWNLOAD_CACHE_DIR), hashKeyForDisk(key));
        if (f.exists()) {
            return decodeSampledBitmapFromFile(f.toString(), mDefaultMaxImageWidth, mDefaultMaxImageHeight);
        }
        return null;
    }

    /**
     * Decode and sample down a {@link Bitmap} from a file to the requested
     * width and height.
     *
     * @param filename The full path of the file to decode
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A {@link Bitmap} sampled down from the original with the same
     *         aspect ratio and dimensions that are equal to or greater than the
     *         requested width and height
     */
    public static Bitmap decodeSampledBitmapFromFile(final String filename, int maxWidth, int maxHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename, options);
    }

    /**
     * Calculate an inSampleSize for use in a
     * {@link android.graphics.BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This
     * implementation calculates the closest inSampleSize that will result in
     * the final decoded bitmap having a width and height equal to or larger
     * than the requested width and height. This implementation does not ensure
     * a power of 2 is returned for inSampleSize which can be faster when
     * decoding but results in a larger bitmap which isn't as useful for caching
     * purposes.
     *
     * @param options An options object with out* params already populated (run
     *            through a decode* method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(final BitmapFactory.Options options,
                                                  final int reqWidth, final int reqHeight) {
        /* Raw height and width of image */
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float)height / (float)reqHeight);
            } else {
                inSampleSize = Math.round((float)width / (float)reqWidth);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            /* More than 2x the requested pixels we'll sample down further */
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    /**
     * flush() is called to synchronize up other methods that are accessing the
     * cache first
     */
    public void flush() {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... unused) {
                if (mThumbnailDiskCache != null) {
                    try {
                        if (!mThumbnailDiskCache.isClosed()) {
                            mThumbnailDiskCache.flush();
                        }
                    } catch (final IOException e) {
                        Log.e(TAG, "flush - " + e);
                    }
                }
                return null;
            }
        }, (Void[])null);
    }

    /**
     * Clears the disk and memory caches
     */
    public void clearCaches() {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... unused) {
                // Clear download cache
                File downloadCache = getDiskCacheDir(mContext, DOWNLOAD_CACHE_DIR);
                if (downloadCache != null && downloadCache.exists() && downloadCache.isDirectory()) {
                    for (File f: downloadCache.listFiles()) {
                        f.delete();
                    }
                }
                // Clear the disk cache
                try {
                    if (mThumbnailDiskCache != null) {
                        mThumbnailDiskCache.delete();
                        mThumbnailDiskCache = null;
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "clearCaches - " + e);
                } finally {
                    initDiskCache(mContext);
                }
                // Clear the memory cache
                evictAll();
                return null;
            }
        }, (Void[])null);
    }

    /**
     * Closes the disk cache associated with this ImageCache object. Note that
     * this includes disk access so this should not be executed on the main/UI
     * thread.
     */
    public void close() {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... unused) {
                if (mThumbnailDiskCache != null) {
                    try {
                        if (!mThumbnailDiskCache.isClosed()) {
                            mThumbnailDiskCache.close();
                            mThumbnailDiskCache = null;
                        }
                    } catch (final IOException e) {
                        Log.e(TAG, "close - " + e);
                    }
                }
                return null;
            }
        }, (Void[])null);
    }

    /**
     * Evicts all of the items from the memory cache and lets the system know
     * now would be a good time to garbage collect
     */
    public void evictAll() {
        if (mThumbnailMemCache != null) {
            mThumbnailMemCache.evictAll();
        }
        if (mLargeArtMemCache != null) {
            mLargeArtMemCache.evictAll();
        }
        System.gc();
    }

    /**
     * @param key The key used to identify which cache entries to delete.
     */
    public void removeFromCache(final String key) {
        if (key == null) {
            return;
        }
        // Remove the Lru entry
        if (mThumbnailMemCache != null) {
            mThumbnailMemCache.remove(key);
        }

        try {
            // Remove the disk entry
            if (mThumbnailDiskCache != null) {
                mThumbnailDiskCache.remove(hashKeyForDisk(key));
            }
        } catch (final IOException e) {
            Log.e(TAG, "remove - " + e);
        }
        flush();
    }

    /**
     * Used to temporarily pause the disk cache while the user is scrolling to
     * improve scrolling.
     *
     * @param pause True to temporarily pause the disk cache, false otherwise.
     */
    public void setPauseDiskCache(final boolean pause) {
        synchronized (mPauseLock) {
            if (mPauseDiskAccess != pause) {
                mPauseDiskAccess = pause;
                if (!pause) {
                    mPauseLock.notify();
                }
            }
        }
    }

    private void waitUntilUnpaused() {
        synchronized (mPauseLock) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                while (mPauseDiskAccess) {
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                        // ignored, we'll start waiting again
                    }
                }
            }
        }
    }

    /**
     * @return True if the user is scrolling, false otherwise.
     */
    public boolean isDiskCachePaused() {
        return mPauseDiskAccess;
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise)
     *
     * @param context The {@link Context} to use
     * @param uniqueName A unique directory name to append to the cache
     *            directory
     * @return The cache directory
     */
    public static final File getDiskCacheDir(final Context context, final String uniqueName) {
        // getExternalCacheDir(context) returns null if external storage is not ready
        final String cachePath = getExternalCacheDir(context) != null
                                    ? getExternalCacheDir(context).getPath()
                                    : context.getCacheDir().getPath();
        return new File(cachePath, uniqueName);
    }

    /**
     * Check if external storage is built-in or removable
     *
     * @return True if external storage is removable (like an SD card), false
     *         otherwise
     */
    public static final boolean isExternalStorageRemovable() {
        return Environment.isExternalStorageRemovable();
    }

    /**
     * Get the external app cache directory
     *
     * @param context The {@link Context} to use
     * @return The external cache directory
     */
    public static final File getExternalCacheDir(final Context context) {
        return context.getExternalCacheDir();
    }

    /**
     * Check how much usable space is available at a given path.
     *
     * @param path The path to check
     * @return The space available in bytes
     */
    public static final long getUsableSpace(final File path) {
        return path.getUsableSpace();
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable
     * for using as a disk filename.
     *
     * @param key The key used to store the file
     */
    public static final String hashKeyForDisk(final String key) {
        return StringUtilities.md5(key);
    }

    /**
     * Converts given dp value to density specific pixel value
     * @param context
     * @param dp
     * @return
     */
    public static int convertDpToPx(Context context, float dp) {
        return Math.round(dp * (context.getResources().getDisplayMetrics().densityDpi / 160f));
    }

    /**
     * Returns largest screen dimension
     * @param context
     * @return
     */
    public static int getMaxDisplaySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        return Math.max(size.x, size.y);
    }

    /**
     * A simple non-UI Fragment that stores a single Object and is retained over
     * configuration changes. In this sample it will be used to retain an
     * {@link ImageCache} object.
     */
    public static final class RetainFragment extends Fragment {

        /**
         * The object to be stored
         */
        private Object mObject;

        /**
         * Empty constructor as per the {@link Fragment} documentation
         */
        public RetainFragment() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Make sure this Fragment is retained over a configuration change
            setRetainInstance(true);
        }

        /**
         * Store a single object in this {@link Fragment}
         *
         * @param object The object to store
         */
        public void setObject(final Object object) {
            mObject = object;
        }

        /**
         * Get the stored object
         *
         * @return The stored object
         */
        public Object getObject() {
            return mObject;
        }
    }

    /**
     * Used to cache images via {@link LruCache}.
     */
    public static final class MemoryCache extends LruCache<String, Bitmap> {

        /**
         * Constructor of <code>MemoryCache</code>
         *
         * @param maxSize The allowed size of the {@link LruCache}
         */
        public MemoryCache(final int maxSize) {
            super(maxSize);
        }

        /**
         * Get the size in bytes of a bitmap.
         */
        public static final int getBitmapSize(final Bitmap bitmap) {
            return bitmap.getByteCount();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected int sizeOf(final String paramString, final Bitmap paramBitmap) {
            return getBitmapSize(paramBitmap);
        }

    }

}
