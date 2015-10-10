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

package org.opensilk.common.glide;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.util.Preconditions;

/**
 * Created by drew on 10/9/15.
 */
public class PalettizedBitmapDrawableTranscoder implements ResourceTranscoder<Bitmap, PalettizedBitmapDrawable> {
    private final Resources resources;
    private final BitmapPool bitmapPool;

    private PalettizedBitmapDrawableTranscoder(Resources resources, BitmapPool bitmapPool) {
        this.resources = Preconditions.checkNotNull(resources);
        this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
    }

    public static PalettizedBitmapDrawableTranscoder create(Context context) {
        return create(context.getResources(), Glide.get(context).getBitmapPool());
    }

    public static PalettizedBitmapDrawableTranscoder create(Resources resources, BitmapPool bitmapPool) {
        return new PalettizedBitmapDrawableTranscoder(resources, bitmapPool);
    }

    @Override
    public Resource<PalettizedBitmapDrawable> transcode(Resource<Bitmap> toTranscode) {
        final Bitmap bitmap = toTranscode.get();
        final Palette palette = PaletteResource.obtain(bitmap).get();
        return PalettizedBitmapDrawableResource.obtain(resources, bitmapPool, bitmap, palette);
    }

}
