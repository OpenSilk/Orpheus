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
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/**
 * Created by drew on 10/9/15.
 */
public class PalettableUtils {

    public static final int DEFAULT_DURATION_MS = 300;

    private PalettableUtils() {}

    public interface Apply {
        void call(@ColorInt int color);
    }

    public static void animateColorChange(@ColorInt int start, @ColorInt int end, final Apply apply) {
        animateColorChange(start, end, DEFAULT_DURATION_MS, apply);
    }

    public static void animateColorChange(@ColorInt int start, @ColorInt int end, int duration, final Apply apply) {
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

    public static Palette.Swatch getSwatchFor(PaletteSwatchType type, @Nullable Palette palette) {
        if (palette != null) {
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
        return null;
    }

    public static Animation fadeInAnimation() {
        Animation animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(DEFAULT_DURATION_MS);
        return animation;
    }

}
