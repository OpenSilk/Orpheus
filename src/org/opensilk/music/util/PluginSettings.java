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

package org.opensilk.music.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by drew on 7/19/14.
 */
public class PluginSettings {

    public static final String PREF_DEFAULT_SOURCE = "def_source";

    private final SharedPreferences prefs;

    public PluginSettings(Context context, ComponentName componentName) {
        prefs = context.getSharedPreferences("plugin_"+componentName.flattenToString().replaceAll("/", "_"), Context.MODE_PRIVATE);
    }


    public void setDefaultSource(String identity) {
        prefs.edit().putString(PREF_DEFAULT_SOURCE, identity).apply();
    }

    public String getDefaultSource() {
        return prefs.getString(PREF_DEFAULT_SOURCE, null);
    }

    public void clearDefaultSource() {
        prefs.edit().remove(PREF_DEFAULT_SOURCE).apply();
    }


}
