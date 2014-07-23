
package org.opensilk.music.artwork.cache;

import android.content.Context;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by drew on 3/11/14.
 */
public class CacheUtil {

    private static MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // better never happens
        }
    }

    private CacheUtil() {
        /*static*/
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise)
     *
     * @param context The {@link android.content.Context} to use
     * @param uniqueName A unique directory name to append to the cache
     *            directory
     * @return The cache directory
     */
    public static File getCacheDir(final Context context, final String uniqueName) {
        File cachePath = context.getExternalCacheDir();
        if (cachePath == null || !cachePath.canWrite()) {
            cachePath = context.getCacheDir();
        }
        return new File(cachePath, uniqueName);
    }

    /**
     * Returns a 32 chararacter hexadecimal representation of an MD5 hash of the given String.
     *
     * @param s the String to hash
     * @return the md5 hash
     */
    public static String md5(String s) {
        try {
            byte[] bytes;
            synchronized (CacheUtil.class) {
                bytes = digest.digest(s.getBytes("UTF-8"));
            }
            StringBuilder b = new StringBuilder(32);
            for (byte aByte : bytes) {
                String hex = Integer.toHexString((int) aByte & 0xFF);
                if (hex.length() == 1)
                    b.append('0');
                b.append(hex);
            }
            return b.toString();
        } catch (UnsupportedEncodingException e) {
            // utf-8 always available
        }
        return null;
    }

}
