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

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.ColorInt;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.request.transition.Transition;

/**
 * A cross fade {@link Transition} for {@link Drawable}s that uses an
 * {@link TransitionDrawable} to transition from an existing drawable
 * already visible on the target to a new drawable. If no existing drawable exists, this class can
 * instead fall back to a default animation that doesn't rely on {@link
 * TransitionDrawable}.
 */
public class PalettizedBitmapTransition implements Transition<PalettizedBitmap> {
    private final Transition<PalettizedBitmap> defaultAnimation;
    private final int duration;

    /**
     * Constructor that takes a default animation and a duration in milliseconds that the cross fade
     * animation should last.
     *
     * @param duration The duration that the cross fade animation should run if there is something to
     *                 cross fade from when a new {@link Drawable} is put.
     */
    public PalettizedBitmapTransition(Transition<PalettizedBitmap> defaultAnimation, int duration) {
        this.defaultAnimation = defaultAnimation;
        this.duration = duration;
    }

    /**
     * Animates from the previous drawable to the current drawable in one of two ways.
     *
     * <ol> <li>Using the default animation provided in the constructor if the previous drawable is
     * null</li> <li>Using the cross fade animation with the duration provided in the constructor if
     * the previous drawable is non null</li> </ol>
     *
     * @param current {@inheritDoc}
     * @param adapter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean transition(PalettizedBitmap current, ViewAdapter adapter) {
        Drawable previous = adapter.getCurrentDrawable();
        if (previous != null) {
            TransitionDrawable transitionDrawable =
                    new TransitionDrawable(new Drawable[] { previous, current.getDrawable() });
            transitionDrawable.setCrossFadeEnabled(true);
            transitionDrawable.startTransition(duration);
            adapter.setDrawable(transitionDrawable);
            transitionColors(current, adapter);
            notifyCallbacks(current, adapter);
            return true;
        } else {
            defaultAnimation.transition(current, adapter);
            //TODO this will jank when the target gets its setResource method called
//            transitionColors(current, adapter);
            return false;
        }
    }

    private void notifyCallbacks(PalettizedBitmap current, ViewAdapter adapter) {
        if (current.getPalette() != null && adapter instanceof PalettizedBitmapTarget) {
            PalettizedBitmapTarget target = (PalettizedBitmapTarget) adapter;
            for (Palette.PaletteAsyncListener c :  target.getCallbacks()) {
                c.onGenerated(current.getPalette());
            }
        }
    }

    private void transitionColors(PalettizedBitmap current, ViewAdapter adapter) {
        Palette palette = current.getPalette();
        if (palette == null) return;
        if (adapter instanceof PalettizedBitmapTarget) {
            PalettizedBitmapTarget target = (PalettizedBitmapTarget) adapter;
            for (PalettizedBitmapTarget.ChildTarget childTarget : target.getPaletteTargets()) {
                Palette.Swatch swatch = PalettizedBitmapTarget.getSwatchFor(childTarget.getSwatchType(), palette);
                if (swatch == null && childTarget.getFallbackSwatchType() != null) {
                    swatch = PalettizedBitmapTarget.getSwatchFor(childTarget.getFallbackSwatchType(), palette);
                }
                final boolean hasSwatch = swatch != null;
                for (final View view : childTarget.getBackgrounds()) {
                    if (hasSwatch) {
                        Drawable backgroundDrawable = view.getBackground();
                        if (backgroundDrawable instanceof ColorDrawable) {
                            int color = ((ColorDrawable) backgroundDrawable).getColor();
                            animateColorChange(color, swatch.getRgb(), new Apply() {
                                @Override
                                public void call(@ColorInt int color) {
                                    view.setBackgroundColor(color);
                                }
                            });
                        } else {
                            //go straight to it
                            view.setBackgroundColor(swatch.getRgb());
                        }
                    } else if (childTarget.hasFallbackColor(view)) {
                        //go straight to it who cares
                        view.setBackgroundColor(childTarget.getFallbackColor(view));
                    }
                }
                for (final TextView view : childTarget.getTitles()) {
                    if (hasSwatch) {
                        int color = view.getCurrentTextColor();
                        animateColorChange(color, swatch.getTitleTextColor(), new Apply() {
                            @Override
                            public void call(@ColorInt int color) {
                                view.setTextColor(color);
                            }
                        });
                    } else if (childTarget.hasFallbackColor(view)) {
                        //go straight to it who cares
                        view.setTextColor(childTarget.getFallbackColor(view));
                    }
                }
                for (final TextView view : childTarget.getBodies()) {
                    if (hasSwatch) {
                        int color = view.getCurrentTextColor();
                        animateColorChange(color, swatch.getBodyTextColor(), new Apply() {
                            @Override
                            public void call(@ColorInt int color) {
                                view.setTextColor(color);
                            }
                        });
                    } else if (childTarget.hasFallbackColor(view)) {
                        //go straight to it who cares
                        view.setTextColor(childTarget.getFallbackColor(view));
                    }
                }
            }
        }
    }

    private void animateColorChange(@ColorInt int start, @ColorInt int end, final Apply apply) {
        //http://stackoverflow.com/a/24641977
        final float[] from = new float[3], to = new float[3];
        Color.colorToHSV(start, from);   // from white
        Color.colorToHSV(end, to);     // to red
        final ValueAnimator anim = ValueAnimator.ofFloat(0, 1);   // animate from 0 to 1
        anim.setDuration(duration);                              // for 300 ms
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            final float[] hsv  = new float[3];                  // transition color
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                // Transition along each axis of HSV (hue, saturation, value)
                final float animF = animation.getAnimatedFraction();
                hsv[0] = from[0] + (to[0] - from[0])*animF;
                hsv[1] = from[1] + (to[1] - from[1])*animF;
                hsv[2] = from[2] + (to[2] - from[2])*animF;
                apply.call(Color.HSVToColor(hsv));
            }
        });
        anim.start();
    }

    interface Apply {
        void call(@ColorInt int color);
    }
}
