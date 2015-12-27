/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.artwork.glide;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;

import org.opensilk.music.artwork.BuildConfig;
import org.opensilk.music.artwork.cache.CacheUtil;

import java.io.File;
import java.io.InputStream;

/**
 * Created by drew on 10/6/15.
 */
public class ArtworkGlideModule implements com.bumptech.glide.module.GlideModule {

    public ArtworkGlideModule() {}

    @Override
    public void applyOptions(final Context context, GlideBuilder builder) {
        builder.setDiskCache(new DiskCache.Factory() {
            @Override
            public DiskCache build() {
                File dir = CacheUtil.getCacheDir(context, "glide/1");
                return DiskLruCacheWrapper.get(dir, 50 * 1024 * 1024);
            }
        });
        //blows up
//        RequestOptions options = new RequestOptions()
//                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
//                .centerCrop(context)
//                .placeholder(R.drawable.default_artwork)
//                ;
//        builder.setDefaultRequestOptions(options);
        if (BuildConfig.LOGVV) {
            builder.setLogLevel(Log.DEBUG);
        }
    }

    @Override
    public void registerComponents(Context context, Registry registry) {
        registry.append(ArtInfoRequest.class, InputStream.class, new ArtInfoRequestStreamLoaderFactory());
    }
}
