
package org.opensilk.music.artwork.cache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.Pools;

import com.jakewharton.disklrucache.DiskLruCache;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

import timber.log.Timber;

/**
 * Implementation of DiskLruCache by Jake Wharton
 * modified from http://stackoverflow.com/questions/10185898/using-disklrucache-in-android-4-0-does-not-provide-for-opencache-method
 */
public class BitmapDiskLruCache implements BitmapDiskCache {

    private DiskLruCache mDiskCache;

    private final File mDiskCacheDir;
    private final int mDiskCacheSize;
    private final ByteArrayPool mBytePool;
    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
    private static final int COMPRESS_QUALITY = 100;
    private static final int APP_VERSION = 2;
    private static final int VALUE_COUNT = 1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 8;
    private static final int OUTPUT_BUFFER_SIZE = 1024 * 64;
    private final Pools.Pool<ByteArrayOutputStream> mOutputPool = new Pools.SynchronizedPool<>(5);
    private final Semaphore mOutputSemaphore = new Semaphore(5, true);

    public static final Object sDecodeLock = new Object();

    private BitmapDiskLruCache(File diskCacheDir, int diskCacheSize, ByteArrayPool bytePool) {
        Timber.d("new BitmapDiskLruCache path=%s size=%d", diskCacheDir.getAbsolutePath(), diskCacheSize);
        mDiskCacheDir = diskCacheDir;
        mDiskCacheSize = diskCacheSize;
        mBytePool = bytePool;
    }

    public static BitmapDiskLruCache open(File diskCacheDir, int diskCacheSize, ByteArrayPool bytePool) {
        return new BitmapDiskLruCache(diskCacheDir, diskCacheSize, bytePool);
    }

    //lazily created to avoid blocking on main thread during app startup
    private synchronized DiskLruCache getDiskCache() {
        if (mDiskCache == null) {
            try {
                mDiskCache = DiskLruCache.open(mDiskCacheDir, APP_VERSION, VALUE_COUNT, mDiskCacheSize);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return mDiskCache;
    }

    public synchronized void close() {
        IOUtils.closeQuietly(mDiskCache);
        mDiskCache = null;
    }

    private ByteArrayOutputStream getOutput() {
        mOutputSemaphore.acquireUninterruptibly();
        ByteArrayOutputStream out = mOutputPool.acquire();
        if (out == null) {
            out = new PoolingByteArrayOutputStream(mBytePool, OUTPUT_BUFFER_SIZE);
        }
        return out;
    }

    private void returnOutput(ByteArrayOutputStream out) {
        if (out != null) {
            out.reset();
            if (!mOutputPool.release(out)){
                IOUtils.closeQuietly(out);
            }
        }
        mOutputSemaphore.release();
    }

    @Override
    public synchronized void onTrimMemory() {
        ByteArrayOutputStream out = null;
        while ((out = mOutputPool.acquire()) != null) {
            IOUtils.closeQuietly(out);
        }
    }

    public byte[] getBytes(String url) {
        DiskLruCache.Snapshot snapshot = null;
        final ByteArrayOutputStream out = getOutput();
        final byte[] buff = mBytePool.getBuf(DEFAULT_BUFFER_SIZE);
        try {
            snapshot = getDiskCache().get(CacheUtil.md5(url));
            if (snapshot != null) {
                long copied = IOUtils.copyLarge(snapshot.getInputStream(0), out, buff);
                if (copied > 0) {
                    return out.toByteArray();
                }
            }
        } catch (IOException e) {
            Timber.e(e, "getBytes(%s)", url);
        } finally {
            IOUtils.closeQuietly(snapshot);
            returnOutput(out);
            mBytePool.returnBuf(buff);
        }
        return null;
    }

    @Override
    public byte[] bitmapToBytes(Bitmap bitmap) {
        final ByteArrayOutputStream out = getOutput();
        try {
            if (bitmap.compress(COMPRESS_FORMAT, COMPRESS_QUALITY, out)) {
                return out.toByteArray();
            } else {
                return null;
            }
        } finally {
            returnOutput(out);
        }
    }

    public Bitmap getBitmap(String url) {
        byte[] bytes = getBytes(url);
        if (bytes != null) {
            synchronized (sDecodeLock) {
                try {
                    BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
                    decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, decodeOptions);
                } catch (OutOfMemoryError e) {
                    Timber.w(e, "getBitmap(%s)", url);
                } finally {
                    //dont return huge buffers to the pool
                    if (bytes.length <= OUTPUT_BUFFER_SIZE) {
                        mBytePool.returnBuf(bytes);
                    }
                }
            }
        }
        return null;
    }

    public void putBitmap(String url, Bitmap data) {
        final DiskLruCache cache = getDiskCache();
        DiskLruCache.Editor editor = null;
        try {
            editor = cache.edit(CacheUtil.md5(url));
            if (editor != null) {
                byte[] bytes = bitmapToBytes(data);
                if (bytes != null) {
                    OutputStream out = null;
                    try {
                        out = editor.newOutputStream(0);
                        IOUtils.write(bytes, out);
                    } finally {
                        //dont return huge buffers to the pool
                        if (bytes.length <= OUTPUT_BUFFER_SIZE) {
                            mBytePool.returnBuf(bytes);
                        }
                        IOUtils.closeQuietly(out);
                    }
                    editor.commit();
                } else {
                    editor.abort();
                }
            }
        } catch (IOException|IllegalStateException e) {
            try {
                if (editor != null) editor.abort();
            } catch (IOException ignored) { }
        } finally {
            try {
                cache.flush();
            } catch (IOException ignored) {}
        }
    }

    public boolean containsKey(String url) {
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = getDiskCache().get(CacheUtil.md5(url));
            return snapshot != null;
        } catch (IOException e) {
            Timber.w(e, "containsKey(%s)", url);
        } finally {
            IOUtils.closeQuietly(snapshot);
        }
        return false;
    }

    public synchronized boolean clearCache() {
        try {
            getDiskCache().delete();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            close();
        }
    }

}
