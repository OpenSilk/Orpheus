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

package org.opensilk.music.artwork.cache;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.util.Locale;

import timber.log.Timber;

/**
 * Created by drew on 3/11/14.
 */
public class BitmapLruCache extends LruCache<String, Bitmap> implements BitmapCache {

    public BitmapLruCache(int maxSize) {
        super(maxSize);
        Log.i("BitmapLruCache", String.format(Locale.US, "BitmapLruCache size=%.02fM", ((float) maxSize / 1024 / 1024)));
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected int sizeOf(String key, Bitmap value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return value.getAllocationByteCount();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1){
            return value.getByteCount();
        } else {
            return value.getRowBytes() * value.getHeight();
        }
    }

    public Bitmap getBitmap(String url) {
        return get(CacheUtil.md5(url));
    }

    public void putBitmap(String url, Bitmap bitmap) {
        put(CacheUtil.md5(url), bitmap);
    }

    public boolean containsKey(String url) {
        return get(CacheUtil.md5(url)) != null;
    }

    @Override
    public boolean clearCache() {
        evictAll();
        return true;
    }
}
