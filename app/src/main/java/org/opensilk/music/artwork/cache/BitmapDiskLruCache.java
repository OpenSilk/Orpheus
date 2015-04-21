
package org.opensilk.music.artwork.cache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of DiskLruCache by Jake Wharton
 * modified from http://stackoverflow.com/questions/10185898/using-disklrucache-in-android-4-0-does-not-provide-for-opencache-method
 */
public class BitmapDiskLruCache implements BitmapDiskCache {

    private File mDiskCacheDir;
    private int mDiskCacheSize;
    private DiskLruCache mDiskCache;
    private Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.JPEG;
    private static int IO_BUFFER_SIZE = 8*1024;
    private int mCompressQuality = 70;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;

    public static final Object sDecodeLock = new Object();

    private BitmapDiskLruCache(File diskCacheDir, int diskCacheSize, Bitmap.CompressFormat compressFormat, int quality) throws IOException {
        mDiskCacheDir = diskCacheDir;
        mDiskCacheSize = diskCacheSize;
        mCompressFormat = compressFormat;
        mCompressQuality = quality;
        mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize);
    }

    public static BitmapDiskLruCache open(File diskCacheDir, int diskCacheSize, Bitmap.CompressFormat compressFormat, int quality) {
        try {
            return new BitmapDiskLruCache(diskCacheDir, diskCacheSize, compressFormat, quality);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Closes the diskcache
     */
    public void close() {
        if (!mDiskCache.isClosed()) {
            try {
                mDiskCache.close();
            } catch (IOException ignored) { }
        }
    }

    private boolean writeBitmapToFile(Bitmap bitmap, DiskLruCache.Editor editor) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public File getCacheFolder() {
        return mDiskCache.getDirectory();
    }

    /**
     * @param url
     * @return raw snapshot of given url
     */
    public DiskLruCache.Snapshot getSnapshot(String url) {
        try {
            return mDiskCache.get(CacheUtil.md5(url));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void putBitmap(String url, Bitmap data) {
        DiskLruCache.Editor editor = null;
        try {
            editor = mDiskCache.edit(CacheUtil.md5(url));
            if (editor == null) {
                return;
            }

            if(writeBitmapToFile(data, editor)) {
                mDiskCache.flush();
                editor.commit();
            } else {
                editor.abort();
            }
        } catch (IOException|IllegalStateException e) {
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public Bitmap getBitmap(String url) {
        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        try {

            snapshot = mDiskCache.get(CacheUtil.md5(url));
            if ( snapshot == null ) {
                return null;
            }
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                synchronized (sDecodeLock) {
                    try {
                        final BufferedInputStream buffIn =
                                new BufferedInputStream(in, IO_BUFFER_SIZE);
                        bitmap = BitmapFactory.decodeStream(buffIn);
                    } catch (OutOfMemoryError e) {
                        bitmap = null;
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }
        return bitmap;
    }

    public boolean containsKey(String key) {
        boolean contained = false;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mDiskCache.get(CacheUtil.md5(key));
            contained = snapshot != null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }
        return contained;
    }

    public boolean clearCache() {
        try {
            mDiskCache.delete();
            mDiskCache = DiskLruCache.open(mDiskCacheDir, APP_VERSION, VALUE_COUNT, mDiskCacheSize);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
