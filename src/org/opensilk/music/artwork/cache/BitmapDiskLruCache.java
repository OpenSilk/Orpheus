
package org.opensilk.music.artwork.cache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;

import org.opensilk.music.artwork.ArtworkLoader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import hugo.weaving.DebugLog;

/**
 * Implementation of DiskLruCache by Jake Wharton
 * modified from http://stackoverflow.com/questions/10185898/using-disklrucache-in-android-4-0-does-not-provide-for-opencache-method
 */
public class BitmapDiskLruCache implements ArtworkLoader.ImageCache {

    private DiskLruCache mDiskCache;
    private Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.JPEG;
    private static int IO_BUFFER_SIZE = 8*1024;
    private int mCompressQuality = 70;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;

    public BitmapDiskLruCache(File diskCacheDir, int diskCacheSize, Bitmap.CompressFormat compressFormat, int quality) {
        try {
            mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize);
            mCompressFormat = compressFormat;
            mCompressQuality = quality;
        } catch (IOException e) {
            e.printStackTrace();
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

    public boolean containsKey(String key) {
        boolean contained = false;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mDiskCache.get(key);
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

    public void clearCache() {
        try {
            mDiskCache.delete();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public File getCacheFolder() {
        return mDiskCache.getDirectory();
    }

    /**
     * @param url
     * @return raw snapshot of given url
     */
    public DiskLruCache.Snapshot get(String url) {
        try {
            return mDiskCache.get(CacheUtil.md5(url));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a ParcelFileDescriptor for the file backing the entry
     * specified by url, This is probably a very stupid idea but
     * saves a bunch of copying to temp files
     * @param url
     * @return
     */
    @DebugLog
    public synchronized ParcelFileDescriptor getParcelFileDescriptor(String url) {
        DiskLruCache.Snapshot snapshot = null;
        DiskLruCache.Editor editor = null;
        try {
            snapshot = mDiskCache.get(CacheUtil.md5(url));
            if (snapshot != null) {
                editor = snapshot.edit();
                if (editor != null) {
                    File f = editor.getFile(0);
                    if (f != null && f.exists()) {
                        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (editor != null) {
                try {
                    // must recommit to mark the entry as clean
                    editor.commit();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                editor.abortUnlessCommitted();
            }
            if (snapshot != null) {
                snapshot.close();
            }
        }
        return null;
    }

    /*
     * Implement ImageCache interface
     */

    @Override
    public synchronized void putBitmap(String url, Bitmap data) {
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
        } catch (IOException e) {
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public synchronized Bitmap getBitmap(String url) {
        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        try {

            snapshot = mDiskCache.get(CacheUtil.md5(url));
            if ( snapshot == null ) {
                return null;
            }
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final BufferedInputStream buffIn =
                        new BufferedInputStream(in, IO_BUFFER_SIZE);
                bitmap = BitmapFactory.decodeStream(buffIn);
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

}
