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

import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;

import com.bumptech.glide.load.engine.Resource;

/**
 * Created by drew on 10/9/15.
 */
public class PaletteResource implements Resource<Palette> {

    private final Palette palette;

    private PaletteResource(Palette palette) {
        this.palette = palette;
    }

    public static PaletteResource obtain(Bitmap bitmap) {
        final Palette palette = Palette.from(bitmap)
                .maximumColorCount(32) //TODO allow configure
                .generate(); //Compute heavy, must be on background thread
        return new PaletteResource(palette);
    }

    @Override
    public Class<Palette> getResourceClass() {
        return Palette.class;
    }

    @Override
    public Palette get() {
        return palette;
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public void recycle() {
        //noop
    }
}
