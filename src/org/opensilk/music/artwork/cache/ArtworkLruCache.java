/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.artwork.cache;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;

import org.opensilk.music.artwork.Artwork;

/**
 * Created by drew on 10/31/14.
 */
public class ArtworkLruCache extends LruCache<String, Artwork> {

    public ArtworkLruCache(int maxSize) {
        super(maxSize);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected int sizeOf(String key, Artwork value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return value.bitmap.getAllocationByteCount();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1){
            return value.bitmap.getByteCount();
        } else {
            return value.bitmap.getRowBytes() * value.bitmap.getHeight();
        }
    }

    public Artwork getArtwork(String url) {
        return get(CacheUtil.md5(url));
    }

    public void putArtwork(String url, Artwork artwork) {
        put(CacheUtil.md5(url), artwork);
    }

    public boolean containsKey(String url) {
        return get(CacheUtil.md5(url)) != null;
    }

}
