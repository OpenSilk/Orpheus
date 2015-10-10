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

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.widget.TextView;

import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Preconditions;

/**
 * Created by drew on 10/9/15.
 */
public class TextViewTextColorTarget extends ViewTarget<TextView, Palette>
        implements Transition.ViewAdapter {

    private final PaletteSwatchType swatchType;
    private final PaletteSwatchType fallbackSwatch;
    private final boolean forTitle;

    private TextViewTextColorTarget(Builder builder) {
        super(Preconditions.checkNotNull(builder.textView));
        this.swatchType = Preconditions.checkNotNull(builder.swatchType);
        this.fallbackSwatch = builder.fallbackSwatch;
        this.forTitle = builder.forTitle;
    }

    @Override
    public void onResourceReady(Palette resource, Transition<? super Palette> transition) {
        if (transition == null || !transition.transition(resource, this)) {
            Palette.Swatch swatch = PalettableUtils.getSwatchFor(swatchType, resource);
            if (swatch == null && fallbackSwatch != null) {
                swatch = PalettableUtils.getSwatchFor(fallbackSwatch, resource);
            }
            if (swatch != null) {
                view.setTextColor(forTitle ? swatch.getTitleTextColor() : swatch.getBodyTextColor());
            }
        }
    }

    @Override
    public TextView getView() {
        return view;
    }

    @Nullable
    @Override
    public Drawable getCurrentDrawable() {
        return null; //unsupported
    }

    @Override
    public void setDrawable(Drawable drawable) {
        //unsuported
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        TextView textView;
        PaletteSwatchType swatchType;
        PaletteSwatchType fallbackSwatch;
        boolean forTitle;

        public Builder into(TextView textView) {
            this.textView = textView;
            return this;
        }

        public Builder forTitleText(PaletteSwatchType swatchType) {
            return forTitleText(swatchType, null);
        }

        public Builder forTitleText(PaletteSwatchType swatchType, PaletteSwatchType fallbackType) {
            this.swatchType = swatchType;
            this.fallbackSwatch = fallbackType;
            forTitle = true;
            return this;
        }

        public Builder forBodyText(PaletteSwatchType swatchType) {
            return forBodyText(swatchType, null);
        }

        public Builder forBodyText(PaletteSwatchType swatchType, PaletteSwatchType fallbackType) {
            this.swatchType = swatchType;
            this.fallbackSwatch = fallbackType;
            forTitle = false;
            return this;
        }

        public TextViewTextColorTarget build() {
            return new TextViewTextColorTarget(this);
        }

    }
}
