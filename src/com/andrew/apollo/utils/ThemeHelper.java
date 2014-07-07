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

package com.andrew.apollo.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import com.andrew.apollo.R;

/**
 * Created by drew on 2/25/14.
 */
public class ThemeHelper {

    private static ThemeHelper sThemeHelper = null;

    private final Context mContext;
    private final PreferenceUtils mPreferences;
    private ThemeStyle mActiveTheme;

    public static ThemeHelper getInstance(Context context) {
        if (sThemeHelper == null) {
            sThemeHelper = new ThemeHelper(context);
        }
        return sThemeHelper;
    }

    private ThemeHelper(Context context) {
        mContext = context.getApplicationContext();
        mPreferences = PreferenceUtils.getInstance(mContext);
        mActiveTheme = mPreferences.getThemeStyle();
    }

    public void reloadTheme() {
        mActiveTheme = mPreferences.getThemeStyle();
    }

    /**
     * @return Theme style used for panel activity
     */
    public final int getPanelTheme() {
        //Set theme
        switch (mActiveTheme) {
            case ORPHEUS:
                return R.style.Theme_Orpheus_Panel;
            case BLUPHEUS:
                return R.style.Theme_Blupheus_Panel;
            case REPHEUS:
                return R.style.Theme_Repheus_Panel;
            case GREPHEUS:
                return R.style.Theme_Grepheus_Panel;
            case PURPHEUS:
                return R.style.Theme_Purpheus_Panel;
            case ORPHEUSDARK:
                return R.style.Theme_OrpheusDark_Panel;
            case BLUPHEUSDARK:
                return R.style.Theme_BlupheusDark_Panel;
            case REPHEUSDARK:
                return R.style.Theme_RepheusDark_Panel;
            case GREPHEUSDARK:
                return R.style.Theme_GrepheusDark_Panel;
            case PURPHEUSDARK:
                return R.style.Theme_PurpheusDark_Panel;
        }
        return -1;
    }

    /**
     * @return Theme style used for ordinary activities
     */
    public final int getTheme() {
        //Set theme
        switch (mActiveTheme) {
            case ORPHEUS:
                return R.style.Theme_Orpheus;
            case BLUPHEUS:
                return R.style.Theme_Blupheus;
            case REPHEUS:
                return R.style.Theme_Repheus;
            case GREPHEUS:
                return R.style.Theme_Grepheus;
            case PURPHEUS:
                return R.style.Theme_Purpheus;
            case ORPHEUSDARK:
                return R.style.Theme_OrpheusDark;
            case BLUPHEUSDARK:
                return R.style.Theme_BlupheusDark;
            case REPHEUSDARK:
                return R.style.Theme_RepheusDark;
            case GREPHEUSDARK:
                return R.style.Theme_GrepheusDark;
            case PURPHEUSDARK:
                return R.style.Theme_PurpheusDark;
        }
        return -1;
    }

    /**
     * @return current theme color
     */
    public final int getThemeColor() {
        return getThemeColor(mActiveTheme);
    }

    /**
     * @return theme color for style
     */
    public final int getThemeColor(ThemeStyle style) {
        switch (style) {
            case ORPHEUS:
            case ORPHEUSDARK:
                return mContext.getResources().getColor(R.color.app_color_orpheus);
            case BLUPHEUS:
            case BLUPHEUSDARK:
                return mContext.getResources().getColor(R.color.app_color_blupheus);
            case REPHEUS:
            case REPHEUSDARK:
                return mContext.getResources().getColor(R.color.app_color_repheus);
            case GREPHEUS:
            case GREPHEUSDARK:
                return mContext.getResources().getColor(R.color.app_color_grepheus);
            case PURPHEUS:
            case PURPHEUSDARK:
                return mContext.getResources().getColor(R.color.app_color_purpheus);
        }
        return -1;
    }

    /**
     * @return theme name
     */
    public final String getThemeName() {
        return mActiveTheme.name();
    }

    /**
     * @return drawable used for action bar background
     */
    public static Drawable getActionBarBackground(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimary, outValue, true);
        final int color = outValue.data;
        return new ColorDrawable(color);
    }

    public static int getPrimaryColor(Context context) {
        return resolveAttr(context, R.attr.colorPrimary).data;
    }

    public static int getAccentColor(Context context) {
        return resolveAttr(context, R.attr.colorAccent).data;
    }

    static TypedValue resolveAttr(Context context, int attr) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, outValue, true);
        return outValue;
    }

    public final Drawable getShuffleButtonDrawable() {
        return themeDrawable(R.drawable.ic_action_playback_shuffle_black);
    }

    public final Drawable getRepeatButtonDrawable() {
        return themeDrawable(R.drawable.ic_action_playback_repeat_black);
    }

    public final Drawable getRepeatOneButtonDrawable() {
        return themeDrawable(R.drawable.ic_action_playback_repeat_1_black);
    }

    public final Drawable getQueueButtonDrawable() {
        return themeDrawable(R.drawable.ic_action_queue_black);
    }

    /**
     * Themes drawable resource to current theme color
     *
     * @param resId
     * @return
     */
    public final Drawable themeDrawable(int resId) {
        return themeDrawable(mContext, resId, getThemeColor());
    }

    /**
     * Themes drawable resource to given color
     *
     * @param resId
     * @param newColor
     * @return
     */
    public static Drawable themeDrawable(Context context, int resId, int newColor) {
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
        maskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

        canvas.drawRect(0, 0, width, height, maskedPaint);

        return new BitmapDrawable(context.getResources(), outBitmap);
    }

    /**
     * Whether or not theme is light
     *
     * from AOSP see @MediaRouterThemeHelper.java
     * @param context
     * @return
     */
    public static boolean isLightTheme(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.isLightTheme, value, true) && value.data != 0;
    }

}
