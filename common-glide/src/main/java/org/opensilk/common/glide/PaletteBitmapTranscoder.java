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
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

/**
 * Created by drew on 10/9/15.
 */
public class PaletteBitmapTranscoder implements ResourceTranscoder<Bitmap, Palette> {

    private PaletteBitmapTranscoder() { }

    public static PaletteBitmapTranscoder create() {
        return new PaletteBitmapTranscoder();
    }

    @Override
    public Resource<Palette> transcode(Resource<Bitmap> toTranscode) {
        Bitmap bitmap = toTranscode.get();
        PaletteResource resource = PaletteResource.obtain(bitmap);
        toTranscode.recycle();
        return resource;
    }
}
