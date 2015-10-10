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

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.view.View;

import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Preconditions;

/**
 * Created by drew on 10/9/15.
 */
public class ViewBackgroundDrawableTarget extends ViewTarget<View, Palette>
        implements Transition.ViewAdapter, PaletteableTarget {

    private final PaletteSwatchType swatchType;
    private final PaletteSwatchType fallbackSwatch;

    private ViewBackgroundDrawableTarget(Builder builder) {
        super(Preconditions.checkNotNull(builder.view));
        this.swatchType = Preconditions.checkNotNull(builder.swatchType);
        this.fallbackSwatch = builder.fallbackSwatch;
    }

    @Override
    public void onResourceReady(Palette resource, Transition<? super Palette> transition) {
        if (transition == null || !transition.transition(resource, this)) {
            Palette.Swatch swatch = PalettableUtils.getSwatchFor(swatchType, resource);
            if (swatch == null && fallbackSwatch != null) {
                swatch = PalettableUtils.getSwatchFor(fallbackSwatch, resource);
            }
            if (swatch != null) {
                setDrawable(new ColorDrawable(swatch.getRgb()));
            }
        }
    }

    @Nullable @Override
    public Drawable getCurrentDrawable() {
        return view.getBackground();
    }

    @Override
    public void setDrawable(Drawable drawable) {
        //noinspection deprecation
        view.setBackgroundDrawable(drawable);
    }

    @Override @NonNull
    public PaletteSwatchType getSwatchType() {
        return swatchType;
    }

    @Override @Nullable
    public PaletteSwatchType getFallbackSwatch() {
        return fallbackSwatch;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        View view;
        PaletteSwatchType swatchType;
        PaletteSwatchType fallbackSwatch;

        public Builder into(View view) {
            this.view = view;
            return this;
        }

        public Builder using(PaletteSwatchType swatchType) {
            return using(swatchType, null);
        }

        public Builder using(PaletteSwatchType swatchType, PaletteSwatchType fallbackSwatch) {
            this.swatchType = swatchType;
            this.fallbackSwatch = fallbackSwatch;
            return this;
        }

        public ViewBackgroundDrawableTarget build() {
            return new ViewBackgroundDrawableTarget(this);
        }

    }

}
