/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.api;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.annotation.NonNull;

/**
 * Created by drew on 11/11/14.
 */
public class PluginConfig {

    /*
     * Capabilities the service exposes
     */

    public static final int SEARCHABLE = 1 << 0;
    public static final int SETTINGS = 1 << 1;
    public static final int DOWNLOAD = 1 << 2;
    public static final int DELETE = 1 << 3;
    public static final int RENAME = 1 << 4;

    /**
     * Api version of plugin. Set automatically by {@code Builder}
     */
    public final int apiVersion;
    /**
     * Bitmask of abilities listed above
     */
    public final int capabilities;
    /**
     * Component for activity to allow user to choose from available libraries.
     * The activity should be of Dialog Style, and should take care of everything needed
     * to allow user to access the library, including selecting from an available library (or
     * account) and any auth/sign in required. The activity must return {@link android.app.Activity#RESULT_OK}
     * with the extra {@link OrpheusApi#EXTRA_LIBRARY_ID} in the Intent containing the identity Orpheus will pass
     * to all subsequent calls. Or pass a {@link org.opensilk.music.api.meta.LibraryInfo} as the extra
     * {@link OrpheusApi#EXTRA_LIBRARY_INFO} with the {@link org.opensilk.music.api.meta.LibraryInfo#libraryId}
     * and {@link org.opensilk.music.api.meta.LibraryInfo#libraryName} populated.
     * <p>
     * Although not required, it is preferable the activity utilizes
     * {@link OrpheusApi#EXTRA_WANT_LIGHT_THEME} to style the activity to match the
     * current Orpheus theme.
     */
    public final ComponentName pickerComponent;
    /**
     * Contains optional config information
     */
    public final Bundle meta;

    public static final String META_MENU_NAME_PICKER = "menu_picker";
    public static final String META_SETTINGS_COMPONENT = "settingsComponent";
    public static final String META_MENU_NAME_SETTINGS = "menu_settings";

    protected PluginConfig(int apiVersion,
                           int capabilities,
                           @NonNull ComponentName pickerComponent,
                           @NonNull Bundle meta) {
        this.apiVersion = apiVersion;
        this.capabilities = capabilities;
        this.pickerComponent = pickerComponent;
        this.meta = meta;
    }

    public boolean hasAbility(int ability) {
        return (capabilities & ability) != 0;
    }

    public <T> T getMeta(String key) {
        return (T) meta.get(key);
    }

    public Bundle dematerialize() {
        Bundle b = new Bundle(4);
        b.putInt("_1", apiVersion);
        b.putInt("_2", capabilities);
        b.putParcelable("_3", pickerComponent);
        b.putBundle("_4", meta);
        return b;
    }

    public static PluginConfig materialize(Bundle b) {
        return new PluginConfig(
                b.getInt("_1"),
                b.getInt("_2"),
                b.<ComponentName>getParcelable("_3"),
                b.getBundle("_4")
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int apiVersion = OrpheusApi.API_VERSION;
        private int capabilities;
        private ComponentName pickerComponent;
        private Bundle meta = new Bundle();

        public Builder setCapabilities(int capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder addAbility(int ability) {
            capabilities |= ability;
            return this;
        }

        public Builder setPickerComponent(ComponentName pickerComponent, String menuName) {
            this.pickerComponent = pickerComponent;
            meta.putString(META_MENU_NAME_PICKER, menuName);
            return this;
        }

        /**
         * Component for settings activity. The settings activity must process the
         * {@link OrpheusApi#EXTRA_LIBRARY_ID} and only manipulate preferences concerning the
         * given identity.
         * <p>
         * Although not required, it is preferable the activity utilizes
         * {@link OrpheusApi#EXTRA_WANT_LIGHT_THEME} to style the activity to match the
         * current Orpheus theme.
         */
        public Builder setSettingsComponent(ComponentName settingsComponent, String menuName) {
            addAbility(SETTINGS);
            meta.putParcelable(META_SETTINGS_COMPONENT, settingsComponent);
            meta.putString(META_MENU_NAME_SETTINGS, menuName);
            return this;
        }

        public PluginConfig build() {
            if (pickerComponent == null) {
                throw new IllegalArgumentException("pickerComponent must not be null");
            }
            if ((capabilities & SETTINGS) != 0 && meta.getParcelable(META_SETTINGS_COMPONENT) == null) {
                throw new IllegalArgumentException("You defined SETTINGS but left settingsComponent null");
            }
            return new PluginConfig(apiVersion, capabilities, pickerComponent, meta);
        }

    }
}
