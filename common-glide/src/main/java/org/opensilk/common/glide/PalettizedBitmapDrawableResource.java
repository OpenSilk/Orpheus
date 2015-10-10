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
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

/**
 * Created by drew on 10/9/15.
 */
public class PalettizedBitmapDrawableResource implements Resource<PalettizedBitmapDrawable> {
    private final Bitmap bitmap;
    private final Palette palette;
    private final Resources resources;
    private final BitmapPool bitmapPool;

    private PalettizedBitmapDrawableResource(Resources resources, BitmapPool bitmapPool, Bitmap bitmap, Palette palette) {
        this.resources = Preconditions.checkNotNull(resources);
        this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
        this.bitmap = Preconditions.checkNotNull(bitmap);
        this.palette = Preconditions.checkNotNull(palette);
    }

    public static PalettizedBitmapDrawableResource obtain(Context context, Bitmap bitmap, Palette palette) {
        return obtain(context.getResources(), Glide.get(context).getBitmapPool(), bitmap, palette);
    }

    public static PalettizedBitmapDrawableResource obtain(Resources resources, BitmapPool bitmapPool,
                                                          Bitmap bitmap, Palette palette) {
        return new PalettizedBitmapDrawableResource(resources, bitmapPool, bitmap, palette);
    }

    @Override
    public Class<PalettizedBitmapDrawable> getResourceClass() {
        return PalettizedBitmapDrawable.class;
    }

    @Override
    public PalettizedBitmapDrawable get() {
        //lazy create
        return new PalettizedBitmapDrawable(resources, bitmap, palette);
    }

    @Override
    public int getSize() {
        return Util.getBitmapByteSize(bitmap);
    }

    @Override
    public void recycle() {
        bitmapPool.put(bitmap);
    }
}
