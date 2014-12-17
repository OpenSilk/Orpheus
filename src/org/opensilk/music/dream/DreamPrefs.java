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
import android.text.TextUtils;

import org.opensilk.music.AppPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 4/13/14.
 */
@Singleton
public class DreamPrefs {

    /**
     * Available dream layouts
     */
    public interface DreamLayout {
        int ART_ONLY = 0;
        int ART_META = 1;
        int ART_CONTROLS = 2;
        int VISUALIZER_WAVE = 3;
    }

    final AppPreferences prefs;

    @Inject
    public DreamPrefs(AppPreferences settings) {
        this.prefs = settings;
    }

    /**
     * Stores dream layout into shared prefs
     * @param dreamLayout
     */
    public void saveDreamLayout(int dreamLayout) {
        prefs.putInt("daydream_layout_style", dreamLayout);
    }

    /**
     * Retrieves dream layout from shared prefs
     * @return
     */
    public int getDreamLayout() {
        return prefs.getInt("daydream_layout_style", DreamLayout.ART_CONTROLS);
    }

    /**
     * Stores alt dream component info in shared prefs
     * @param componentName
     */
    public void saveAltDreamComponent(ComponentName componentName) {
        prefs.putString("daydream_alt_dream_component", componentName.flattenToString());
    }

    /**
     * Gets alt dream compontent info from shared prefs
     * @return
     */
    public ComponentName getAltDreamComponent() {
        String altCmpnt = prefs.getString("daydream_alt_dream_component", null);
        if (!TextUtils.isEmpty(altCmpnt)) {
            return ComponentName.unflattenFromString(altCmpnt);
        }
        return null;
    }

    /**
     * Resets the alt dream shared pref
     */
    public void removeAltDreamComponent() {
        prefs.remove("daydream_alt_dream_component");
    }

    /**
     * Used to set screenBright()
     * @return true if user selected night mode
     */
    public boolean wantNightMode() {
        return prefs.getBoolean("daydream_nightmode", true);
    }

    /**
     * Used to set fullscreenMode()
     * @return true if user selected fullscreen mode
     */
    public boolean wantFullscreen() {
        return prefs.getBoolean("daydream_fullscreen", true);
    }

}
