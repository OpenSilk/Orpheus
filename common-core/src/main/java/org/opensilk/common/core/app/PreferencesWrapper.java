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

package org.opensilk.common.core.app;

import android.content.SharedPreferences;

/**
 * Created by drew on 4/30/15.
 */
public abstract class PreferencesWrapper {

    protected abstract SharedPreferences getPrefs();

    public void putBoolean(String key, boolean value) {
        getPrefs().edit().putBoolean(key, value).apply();
    }

    public void putLong(String key, long value) {
        getPrefs().edit().putLong(key, value).apply();
    }

    public void putInt(String key, int value) {
        getPrefs().edit().putInt(key, value).apply();
    }

    public void putString(String key, String value) {
        getPrefs().edit().putString(key, value).apply();
    }

    public boolean getBoolean(String key, boolean def) {
        return getPrefs().getBoolean(key, def);
    }

    public long getLong(String key, long def) {
        return getPrefs().getLong(key, def);
    }

    public int getInt(String key, int def) {
        return getPrefs().getInt(key, def);
    }

    public String getString(String key, String def) {
        return getPrefs().getString(key, def);
    }

    public void remove(String key) {
        getPrefs().edit().remove(key).apply();
    }

}
