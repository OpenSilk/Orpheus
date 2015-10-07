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

import android.graphics.drawable.BitmapDrawable;
import android.support.v7.graphics.Palette;

import com.bumptech.glide.load.engine.Resource;

/**
 * Created by drew on 10/6/15.
 */
public class PalettizedBitmapResource implements Resource<PalettizedBitmap> {
    private final Resource<BitmapDrawable> wrapped;
    private final BitmapDrawable drawable;
    private final Palette palette;

    private PalettizedBitmapResource(Resource<BitmapDrawable> wrapped, BitmapDrawable drawable, Palette palette) {
        this.wrapped = wrapped;
        this.drawable = drawable;
        this.palette = palette;
    }

    public static PalettizedBitmapResource create(Resource<BitmapDrawable> wrapped) {
        final BitmapDrawable drawable = wrapped.get();
        final Palette palette = Palette.from(drawable.getBitmap())
                .maximumColorCount(24)
                .generate();
        return new PalettizedBitmapResource(wrapped, drawable, palette);
    }

    @Override
    public Class<PalettizedBitmap> getResourceClass() {
        return PalettizedBitmap.class;
    }

    @Override
    public PalettizedBitmap get() {
        return new PalettizedBitmap(drawable, palette);
    }

    @Override
    public int getSize() {
        return wrapped.getSize();
    }

    @Override
    public void recycle() {
        wrapped.recycle();
    }
}
