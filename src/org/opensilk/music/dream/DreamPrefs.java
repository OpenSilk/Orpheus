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

package org.opensilk.music.dream;

import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.GraphHolder;

/**
 * Created by drew on 4/13/14.
 */
public class DreamPrefs {

    /**
     * Available dream layouts
     */
    public class DreamLayout {
        public static final int ART_ONLY = 0;
        public static final int ART_META = 1;
        public static final int ART_CONTROLS = 2;
    }

    private static AppPreferences getPrefs(Context context) {
        return GraphHolder.get(context).getObjectGraph().get(AppPreferences.class);
    }

    /**
     * Stores dream layout into shared prefs
     * @param context
     * @param dreamLayout
     */
    public static void saveDreamLayout(Context context, int dreamLayout) {
        AppPreferences prefs = getPrefs(context);
        prefs.putInt("daydream_layout_style", dreamLayout);
    }

    /**
     * Retrieves dream layout from shared prefs
     * @param context
     * @return
     */
    public static int getDreamLayout(Context context) {
        AppPreferences prefs = getPrefs(context);
        return prefs.getInt("daydream_layout_style", DreamLayout.ART_CONTROLS);
    }

    /**
     * Stores alt dream component info in shared prefs
     * @param context
     * @param componentName
     */
    public static void saveAltDreamComponent(Context context, ComponentName componentName) {
        AppPreferences prefs = getPrefs(context);
        prefs.putString("daydream_alt_dream_component", componentName.flattenToString());
    }

    /**
     * Gets alt dream compontent info from shared prefs
     * @param context
     * @return
     */
    public static ComponentName getAltDreamComponent(Context context) {
        AppPreferences prefs = getPrefs(context);
        String altCmpnt = prefs.getString("daydream_alt_dream_component", null);
        if (!TextUtils.isEmpty(altCmpnt)) {
            return ComponentName.unflattenFromString(altCmpnt);
        }
        return null;
    }

    /**
     * Resets the alt dream shared pref
     * @param context
     */
    public static void removeAltDreamComponent(Context context) {
        AppPreferences prefs = getPrefs(context);
        prefs.remove("daydream_alt_dream_component");
    }

    /**
     * Used to set screenBright()
     * @param context
     * @return true if user selected night mode
     */
    public static boolean wantNightMode(Context context) {
        AppPreferences prefs = getPrefs(context);
        return prefs.getBoolean("daydream_nightmode", true);
    }

    /**
     * Used to set fullscreenMode()
     * @param context
     * @return true if user selected fullscreen mode
     */
    public static boolean wantFullscreen(Context context) {
        AppPreferences prefs = getPrefs(context);
        return prefs.getBoolean("daydream_fullscreen", true);
    }

}
