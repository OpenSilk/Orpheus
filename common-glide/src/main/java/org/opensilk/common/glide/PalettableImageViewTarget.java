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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 10/6/15.
 */
public class PalettableImageViewTarget extends ViewTarget<ImageView, PalettizedBitmapDrawable>
                implements Transition.ViewAdapter {

    private final ArrayList<Palette.PaletteAsyncListener> callbacks;
    private final ArrayList<AddonTarget> childTargets;

    protected PalettableImageViewTarget(Builder builder) {
        super(builder.imageView);
        this.callbacks = builder.callbacks;
        this.childTargets = builder.childTargets;
    }

    /**
     * Returns the current {@link android.graphics.drawable.Drawable} being displayed in the view
     * using {@link android.widget.ImageView#getDrawable()}.
     */
    @Override
    @Nullable
    public Drawable getCurrentDrawable() {
        return view.getDrawable();
    }

    /**
     * Sets the given {@link android.graphics.drawable.Drawable} on the view using {@link
     * android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}.
     *
     * @param drawable {@inheritDoc}
     */
    @Override
    public void setDrawable(Drawable drawable) {
        view.setImageDrawable(drawable);
    }

    /**
     * Sets the given {@link android.graphics.drawable.Drawable} on the view using {@link
     * android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}.
     *
     * @param placeholder {@inheritDoc}
     */
    @Override
    public void onLoadStarted(@Nullable Drawable placeholder) {
        super.onLoadStarted(placeholder);
        setResource(null);
        setDrawable(placeholder);
    }

    /**
     * Sets the given {@link android.graphics.drawable.Drawable} on the view using {@link
     * android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}.
     *
     * @param errorDrawable {@inheritDoc}
     */
    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
        super.onLoadFailed(errorDrawable);
        setResource(null);
        setDrawable(errorDrawable);
    }

    /**
     * Sets the given {@link android.graphics.drawable.Drawable} on the view using {@link
     * android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}.
     *
     * @param placeholder {@inheritDoc}
     */
    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
        super.onLoadCleared(placeholder);
        setResource(null);
        setDrawable(placeholder);
    }

    @Override
    public void onResourceReady(PalettizedBitmapDrawable resource,
                                @Nullable Transition<? super PalettizedBitmapDrawable> transition) {
        if (transition == null || !(transition.transition(resource, this))) {
            setResource(resource);
        }
        applyPalette(resource.getPalette());
    }

    protected void setResource(@Nullable PalettizedBitmapDrawable resource) {
        view.setImageDrawable(resource);
    }

    protected void applyPalette(@Nullable Palette palette) {
        if (palette != null) {
            for (Palette.PaletteAsyncListener c : callbacks) {
                c.onGenerated(palette);
            }
            for (final AddonTarget childTarget : childTargets) {
                childTarget.target.onResourceReady(palette, childTarget.transition);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ImageView imageView;
        private final ArrayList<Palette.PaletteAsyncListener> callbacks = new ArrayList<>();
        private final ArrayList<AddonTarget> childTargets = new ArrayList<>();
        private final PaletteableDrawableCrossFadeTransition defaultBackgroundTransiton =
                new PaletteableDrawableCrossFadeTransition(PalettableUtils.DEFAULT_DURATION_MS);

        private Builder() {}

        public Builder into(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        /**
         * Notify the given listener when palette is available
         */
        public Builder withCallback(@NonNull Palette.PaletteAsyncListener callback) {
            callbacks.add(Preconditions.checkNotNull(callback));
            return this;
        }

        /**
         * Apply to a Background target with default transition (CrossFade)
         */
        public Builder intoBackground(@NonNull ViewBackgroundDrawableTarget target) {
            return intoBackground(target, defaultBackgroundTransiton);
        }

        /**
         * Apply to a Background target with given transition
         * @param target
         * @param transition May be null for no transition
         * @return this
         */
        public Builder intoBackground(@NonNull ViewBackgroundDrawableTarget target,
                                      @Nullable Transition<Palette> transition) {
            childTargets.add(new AddonTarget(target, transition));
            return this;
        }

        /**
         * Apply to a TextView target with default transition (No Transition)
         */
        public Builder intoTextView(TextViewTextColorTarget target) {
            return intoTextView(target, null);
        }

        /**
         * Apply to TextView target with given transition
         * @param target
         * @param transition May be null for no transition
         * @return this
         */
        public Builder intoTextView(@NonNull TextViewTextColorTarget target,
                                    @Nullable Transition<Palette> transition) {
            childTargets.add(new AddonTarget(target, transition));
            return this;
        }

        /**
         * Convenience method for adding all views in the passed {@link Paletteable}
         * @param swatchType
         * @param paletteable
         * @return this
         */
        public Builder intoPalettable(@NonNull PaletteSwatchType swatchType,
                                      @Nullable PaletteSwatchType fallbackSwatch,
                                      @NonNull Paletteable paletteable) {
            Preconditions.checkNotNull(paletteable);
            List<? extends View> backgrounds = paletteable.getBackgroundViews();
            if (backgrounds != null) {
                for (View v : backgrounds) {
                    intoBackground(ViewBackgroundDrawableTarget.builder()
                            .into(v).using(swatchType, fallbackSwatch).build());
                }
            }
            List<TextView> titles = paletteable.getTitleViews();
            if (titles != null) {
                for (TextView textView : titles) {
                    intoTextView(TextViewTextColorTarget.builder()
                            .into(textView).forTitleText(swatchType, fallbackSwatch).build());
                }
            }
            List<TextView> bodies = paletteable.getBodyViews();
            if (bodies != null) {
                for (TextView textView : bodies) {
                    intoTextView(TextViewTextColorTarget.builder()
                            .into(textView).forBodyText(swatchType, fallbackSwatch).build());
                }
            }
            return this;
        }

        public PalettableImageViewTarget build() {
            return new PalettableImageViewTarget(this);
        }

    }

    private static class AddonTarget {
        final Target<Palette> target;
        final Transition<Palette> transition;
        public AddonTarget(Target<Palette> target, Transition<Palette> transition) {
            this.target = Preconditions.checkNotNull(target);
            this.transition = transition;
        }
    }

}
