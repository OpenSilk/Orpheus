/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.ui.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.common.ui.R;
import org.opensilk.common.ui.widget.CompatSeekBar;

/**
 * Created by drew on 10/12/14.
 */
public class ThemeUtils {

    private static final TypedValue sTypedValue = new TypedValue();

    public static TypedValue resolveAttr(Context context, int attr) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, outValue, true);
        return outValue;
    }

    public static int getColorPrimary(Context context) {
        return getThemeAttrColor(context, R.attr.colorPrimary);
    }

    public static int getColorAccent(Context context) {
        return getThemeAttrColor(context, R.attr.colorAccent);
    }

    public static int getColorPrimaryDark(Context context) {
        return getThemeAttrColor(context, R.attr.colorPrimaryDark);
    }

    public static int getColorControlNormal(Context context) {
        return getThemeAttrColor(context, R.attr.colorControlNormal);
    }

    public static int getColorControlActivited(Context context) {
        return getThemeAttrColor(context, R.attr.colorControlActivated);
    }

    public static int getColorControlHighlight(Context context) {
        return getThemeAttrColor(context, R.attr.colorControlHighlight);
    }

    public static int getColorButtonNormal(Context context) {
        return getThemeAttrColor(context, R.attr.colorButtonNormal);
    }

    public static int getColorSwitchThumbNormal(Context context) {
        return getThemeAttrColor(context, R.attr.colorSwitchThumbNormal);
    }

    public static int setColorAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * MediaRouterThemeHelper (support-v7)
     */
    public static boolean isLightTheme(Context context) {
        synchronized (sTypedValue) {
            return context.getTheme().resolveAttribute(R.attr.isLightTheme, sTypedValue, true) && sTypedValue.data != 0;
        }
    }

    /*
     * TintManager (support-v7:21)
     */

    public static int getThemeAttrColor(Context context, int attr) {
        synchronized (sTypedValue) {
            if (context.getTheme().resolveAttribute(attr, sTypedValue, true)) {
                if (sTypedValue.type >= TypedValue.TYPE_FIRST_INT
                        && sTypedValue.type <= TypedValue.TYPE_LAST_INT) {
                    return sTypedValue.data;
                } else if (sTypedValue.type == TypedValue.TYPE_STRING) {
                    return ContextCompat.getColor(context, sTypedValue.resourceId);
                }
            }
        }
        return 0;
    }

    public static int getThemeAttrColor(Context context, int attr, float alpha) {
        final int color = getThemeAttrColor(context, attr);
        final int originalAlpha = Color.alpha(color);

        // Return the color, multiplying the original alpha by the disabled value
        return (color & 0x00ffffff) | (Math.round(originalAlpha * alpha) << 24);
    }

    public static int getDisabledThemeAttrColor(Context context, int attr) {
        // Now retrieve the disabledAlpha value from the theme
        final float disabledAlpha;
        synchronized (sTypedValue) {
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, sTypedValue, true);
            disabledAlpha = sTypedValue.getFloat();
        }
        return getThemeAttrColor(context, attr, disabledAlpha);
    }

    public static ColorStateList getDefaultColorStateList(Context context) {
            /**
             * Generate the default color state list which uses the colorControl attributes.
             * Order is important here. The default enabled state needs to go at the bottom.
             */

            final int colorControlNormal = getThemeAttrColor(context, R.attr.colorControlNormal);
            final int colorControlActivated = getThemeAttrColor(context, R.attr.colorControlActivated);

            final int[][] states = new int[7][];
            final int[] colors = new int[7];
            int i = 0;

            // Disabled state
            states[i] = new int[] { -android.R.attr.state_enabled };
            colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
            i++;

            states[i] = new int[] { android.R.attr.state_focused };
            colors[i] = colorControlActivated;
            i++;

            states[i] = new int[] { android.R.attr.state_activated };
            colors[i] = colorControlActivated;
            i++;

            states[i] = new int[] { android.R.attr.state_pressed };
            colors[i] = colorControlActivated;
            i++;

            states[i] = new int[] { android.R.attr.state_checked };
            colors[i] = colorControlActivated;
            i++;

            states[i] = new int[] { android.R.attr.state_selected };
            colors[i] = colorControlActivated;
            i++;

            // Default enabled state
            states[i] = new int[0];
            colors[i] = colorControlNormal;
            i++;
        return new ColorStateList(states, colors);
    }


    public static void themeToolbar(Toolbar toolbar) {
        toolbar.setBackgroundColor(getColorPrimary(toolbar.getContext()));
    }

    public static void themeSeekBar(SeekBar seekBar, int colorAttr) {
        themeSeekBar2(seekBar, getThemeAttrColor(seekBar.getContext(), colorAttr));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void themeSeekBar2(SeekBar seekBar, int color) {
        if (VersionUtils.hasLollipop()) {
            seekBar.getProgressDrawable().setTint(color);
            seekBar.getThumb().setTint(color);
        } else {
            seekBar.getProgressDrawable().mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            if (VersionUtils.hasJellyBean()) {
                seekBar.getThumb().mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            } else if (seekBar instanceof CompatSeekBar) {
                Drawable thumb = ((CompatSeekBar) seekBar).getThumb();
                if (thumb != null) {
                    thumb.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }
            }
        }
    }

    public static void themeProgressBar(ProgressBar progressBar, int colorAttr) {
        themeProgressBar2(progressBar, getThemeAttrColor(progressBar.getContext(), colorAttr));
    }

    @TargetApi(21)
    public static void themeProgressBar2(ProgressBar progressBar, int color) {
        if (VersionUtils.hasLollipop()) {
            progressBar.getProgressDrawable().setTint(color);
            progressBar.getIndeterminateDrawable().setTint(color);
        } else {
            progressBar.getProgressDrawable().mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            progressBar.getIndeterminateDrawable().mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    public static Drawable colorizeBitmapDrawableCopy(Context context, int resId, int newColor) {
        final Drawable maskDrawable = context.getResources().getDrawable(resId);
        if (!(maskDrawable instanceof BitmapDrawable)) {
            return null;
        }

        final Bitmap maskBitmap = ((BitmapDrawable) maskDrawable).getBitmap();
        final int width = maskBitmap.getWidth();
        final int height = maskBitmap.getHeight();

        final Bitmap outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(outBitmap);
        canvas.drawBitmap(maskBitmap, 0, 0, null);

        final Paint maskedPaint = new Paint();
        maskedPaint.setColor(newColor);
        maskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        canvas.drawRect(0, 0, width, height, maskedPaint);

        return new BitmapDrawable(context.getResources(), outBitmap);
    }

}
