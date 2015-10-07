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

/**
 * Created by drew on 10/6/15.
 */
public class PalettizedBitmap {
    private final BitmapDrawable drawable;
    private final Palette palette;

    public PalettizedBitmap(BitmapDrawable drawable, Palette palette) {
        this.drawable = drawable;
        this.palette = palette;
    }

    public BitmapDrawable getDrawable() {
        return drawable;
    }

    public Palette getPalette() {
        return palette;
    }
}
