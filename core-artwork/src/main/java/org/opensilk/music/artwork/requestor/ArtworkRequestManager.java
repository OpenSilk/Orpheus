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

package org.opensilk.music.artwork.requestor;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.widget.ImageView;

import com.bumptech.glide.request.target.Target;

import org.opensilk.common.glide.Paletteable;
import org.opensilk.common.glide.PalettizedBitmapDrawable;
import org.opensilk.music.model.ArtInfo;

/**
 * Created by drew on 10/22/14.
 */
public interface ArtworkRequestManager {

    Target<PalettizedBitmapDrawable> newRequest(ArtInfo artInfo, ImageView imageView, @Nullable Bundle extras);
    Target<PalettizedBitmapDrawable> newRequest(Uri uri, ImageView imageView, @Nullable Bundle extras);

    Target<PalettizedBitmapDrawable> newRequest(Uri uri, ImageView imageView, @Nullable Paletteable paletteable, @Nullable Bundle extras);
    Target<PalettizedBitmapDrawable> newRequest(ArtInfo artInfo, ImageView imageView, @Nullable Paletteable paletteable, @Nullable Bundle extras);

    Target<PalettizedBitmapDrawable> newRequest(ArtInfo artInfo, ImageView imageView, Palette.PaletteAsyncListener listener, @Nullable Bundle extras);
    Target<PalettizedBitmapDrawable> newRequest(Uri uri, ImageView imageView, Palette.PaletteAsyncListener listener, @Nullable Bundle extras);

    void cancelRequest(ImageView imageView, Target<?> target);
}
