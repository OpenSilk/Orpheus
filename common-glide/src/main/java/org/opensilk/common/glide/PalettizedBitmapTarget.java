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

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.ImageViewTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by drew on 10/6/15.
 */
public class PalettizedBitmapTarget extends ImageViewTarget<PalettizedBitmap> {

    private final LinkedList<ChildTarget> childTargets;
    private final ArrayList<Palette.PaletteAsyncListener> callbacks;

    protected PalettizedBitmapTarget(Builder builder) {
        super(builder.imageView);
        this.childTargets = builder.childTargets;
        this.callbacks = builder.callbacks;
    }

    @Override
    protected void setResource(@Nullable PalettizedBitmap resource) {
        if (resource != null) {
            view.setImageDrawable(resource.getDrawable());
            applyPalette(resource.getPalette());
        } else {
            view.setImageDrawable(null);
            applyPalette(null);
        }
    }

    protected void applyPalette(@Nullable Palette palette) {
        if (palette != null) {
            for (Palette.PaletteAsyncListener c : callbacks) {
                c.onGenerated(palette);
            }
        }
        for (ChildTarget childTarget : childTargets) {
            Palette.Swatch swatch = getSwatchFor(childTarget.type, palette);
            if (swatch == null && childTarget.fallbackType != null) {
                swatch = getSwatchFor(childTarget.fallbackType, palette);
            }
            final boolean hasSwatch = swatch != null;
            for (View view : childTarget.backgrounds) {
                if (hasSwatch) {
                    view.setBackgroundColor(swatch.getRgb());
                } else if (childTarget.fallbackColors.containsKey(view)) {
                    view.setBackgroundColor(childTarget.fallbackColors.get(view));
                }
            }
            for (TextView view : childTarget.titles) {
                if (hasSwatch) {
                    view.setTextColor(swatch.getTitleTextColor());
                } else if (childTarget.fallbackColors.containsKey(view)) {
                    view.setTextColor(childTarget.fallbackColors.get(view));
                }
            }
            for (TextView view : childTarget.bodies) {
                if (hasSwatch) {
                    view.setTextColor(swatch.getBodyTextColor());
                } else if (childTarget.fallbackColors.containsKey(view)) {
                    view.setTextColor(childTarget.fallbackColors.get(view));
                }
            }
        }
    }

    static Palette.Swatch getSwatchFor(PaletteSwatchType type, @Nullable Palette palette) {
        if (palette == null) {
            return null;
        }
        switch (type) {
            case VIBRANT:
                return palette.getVibrantSwatch();
            case VIBRANT_DARK:
                return palette.getDarkVibrantSwatch();
            case VIBRANT_LIGHT:
                return palette.getLightVibrantSwatch();
            case MUTED:
                return palette.getMutedSwatch();
            case MUTED_DARK:
                return palette.getDarkMutedSwatch();
            case MUTED_LIGHT:
                return palette.getLightMutedSwatch();
            default:
                return null;
        }
    }

    public List<ChildTarget> getPaletteTargets() {
        return Collections.unmodifiableList(childTargets);
    }

    public List<Palette.PaletteAsyncListener> getCallbacks() {
        return Collections.unmodifiableList(callbacks);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ImageView imageView;
        private LinkedList<ChildTarget> childTargets = new LinkedList<>();
        private ArrayList<Palette.PaletteAsyncListener> callbacks = new ArrayList<>();

        private Builder() {}

        public Builder from(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        public Builder using(PaletteSwatchType swatchType) {
            return using(swatchType, null);
        }

        public Builder using(PaletteSwatchType swatchType, @Nullable PaletteSwatchType fallbackSwatchType) {
            this.childTargets.add(new ChildTarget(swatchType, fallbackSwatchType));
            return this;
        }

        public Builder intoPalettable(@NonNull Paletteable paletteable) {
            List<? extends View> backgrounds = paletteable.getBackgroundViews();
            if (backgrounds != null) {
                for (View v : backgrounds) {
                    intoBackground(v);
                }
            }
            List<TextView> titles = paletteable.getTitleViews();
            if (titles != null) {
                for (TextView textView : titles) {
                    intoTitleText(textView);
                }
            }
            List<TextView> bodies = paletteable.getBodyViews();
            if (bodies != null) {
                for (TextView textView : bodies) {
                    intoBodyText(textView);
                }
            }
            return this;
        }

        public Builder intoBackground(View view) {
            throwIfEmpty(childTargets);
            childTargets.getLast().backgrounds.add(view);
            return this;
        }

        public Builder intoBackground(@NonNull View view, @ColorInt int fallbackColor) {
            intoBackground(view);
            insertFallback(view, fallbackColor);
            return this;
        }

        public Builder intoTitleText(@NonNull TextView textView) {
            throwIfEmpty(childTargets);
            childTargets.getLast().titles.add(textView);
            return this;
        }

        public Builder intoTitleText(@NonNull TextView textView, @ColorInt int fallbackColor) {
            intoTitleText(textView);
            insertFallback(textView, fallbackColor);
            return this;
        }

        public Builder intoBodyText(@NonNull TextView textView) {
            throwIfEmpty(childTargets);
            childTargets.getLast().bodies.add(textView);
            return this;
        }

        public Builder intoBodyText(@NonNull TextView textView, @ColorInt int fallbackColor) {
            intoBodyText(textView);
            insertFallback(textView, fallbackColor);
            return this;
        }

        public Builder intoCallBack(@NonNull Palette.PaletteAsyncListener callBack) {
            callbacks.add(callBack);
            return this;
        }

        public PalettizedBitmapTarget build() {
            return new PalettizedBitmapTarget(this);
        }

        private void insertFallback(View view, @ColorInt int fallbackColor) {
            childTargets.getLast().fallbackColors.put(view, fallbackColor);
        }

        private static void throwIfEmpty(List<?> list) {
            if (list.isEmpty()) throw new IllegalArgumentException("Must set a SwatchType first");
        }

    }

    public static final class ChildTarget {
        final HashSet<View> backgrounds = new HashSet<>();
        final HashSet<TextView> titles = new HashSet<>();
        final HashSet<TextView> bodies = new HashSet<>();
        final HashMap<View, Integer> fallbackColors = new HashMap<>();
        final PaletteSwatchType type;
        final PaletteSwatchType fallbackType;
        public ChildTarget(PaletteSwatchType type, PaletteSwatchType fallbackType) {
            this.type = type;
            this.fallbackType = fallbackType;
        }
        public PaletteSwatchType getSwatchType() {
            return type;
        }
        public PaletteSwatchType getFallbackSwatchType() {
            return fallbackType;
        }
        public Set<View> getBackgrounds() {
            return Collections.unmodifiableSet(backgrounds);
        }
        public Set<TextView> getTitles() {
            return Collections.unmodifiableSet(titles);
        }
        public Set<TextView> getBodies() {
            return Collections.unmodifiableSet(bodies);
        }
        public boolean hasFallbackColor(View view) {
            return fallbackColors.containsKey(view);
        }
        public int getFallbackColor(View view) {
            if (hasFallbackColor(view)) {
                return fallbackColors.get(view);
            }
            //todo are there any ints that arent colors?
            throw new IllegalArgumentException("View not found in fallbackColors");
        }
    }

}
