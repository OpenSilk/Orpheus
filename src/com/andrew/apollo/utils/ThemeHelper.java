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
import android.graphics.drawable.Drawable;

import com.andrew.apollo.R;

/**
 * Created by drew on 2/25/14.
 */
public class ThemeHelper {

    private static ThemeHelper sThemeHelper = null;

    private final Context mContext;
    private final PreferenceUtils mPreferences;
    private final ThemeStyle mActiveTheme;

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
                return R.style.Theme_Repheus;
            case PURPHEUS:
                return R.style.Theme_Purpheus;
        }
        return -1;
    }

    /**
     * @return theme color
     */
    public final int getThemeColor() {
        switch (mActiveTheme) {
            case ORPHEUS:
                return mContext.getResources().getColor(R.color.app_color_orpheus);
            case BLUPHEUS:
                return mContext.getResources().getColor(R.color.app_color_blupheus);
            case REPHEUS:
                return mContext.getResources().getColor(R.color.app_color_repheus);
            case GREPHEUS:
                return mContext.getResources().getColor(R.color.app_color_grepheus);
            case PURPHEUS:
                return mContext.getResources().getColor(R.color.app_color_purpheus);
        }
        return -1;
    }

    /**
     * @return drawable used for action bar background
     */
    public final Drawable getActionBarBackground() {
        switch (mActiveTheme) {
            case ORPHEUS:
                return mContext.getResources().getDrawable(R.drawable.ab_solid_orpheus);
            case BLUPHEUS:
                return mContext.getResources().getDrawable(R.drawable.ab_solid_blupheus);
            case REPHEUS:
                return mContext.getResources().getDrawable(R.drawable.ab_solid_repheus);
            case GREPHEUS:
                return mContext.getResources().getDrawable(R.drawable.ab_solid_grepheus);
            case PURPHEUS:
                return mContext.getResources().getDrawable(R.drawable.ab_solid_purpheus);
        }
        return null;
    }

}
