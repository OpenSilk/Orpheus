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
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Iterator;

/**
 * Created by drew on 11/11/14.
 */
public class Config {

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
     * Component for settings activity. The settings activity must process the
     * {@link OrpheusApi#EXTRA_LIBRARY_ID} and only manipulate preferences concerning the
     * given identity.
     * <p>
     * Although not required, it is preferable the activity utilizes
     * {@link OrpheusApi#EXTRA_WANT_LIGHT_THEME} to style the activity to match the
     * current Orpheus theme.
     */
    public final ComponentName settingsComponent;

    protected Config(int apiVersion,
                     int capabilities,
                     @NonNull ComponentName pickerComponent,
                     @Nullable ComponentName settingsComponent) {
        this.apiVersion = apiVersion;
        this.capabilities = capabilities;
        this.pickerComponent = pickerComponent;
        this.settingsComponent = settingsComponent;
    }

    public boolean hasAbility(int ability) {
        return (capabilities & ability) != 0;
    }

    public Bundle dematerialize() {
        Bundle b = new Bundle(4);
        b.putInt("_1", apiVersion);
        b.putInt("_2", capabilities);
        b.putParcelable("_3", pickerComponent);
        b.putParcelable("_4", settingsComponent);
        return b;
    }

    public static Config materialize(Bundle b) {
        return new Config(
                b.getInt("_1"),
                b.getInt("_2"),
                b.<ComponentName>getParcelable("_3"),
                b.<ComponentName>getParcelable("_4")
        );
    }

    public static final class Builder {
        private int apiVersion = OrpheusApi.API_VERSION;
        private int capabilities;
        private ComponentName pickerComponent;
        private ComponentName settingsComponent;

        public Builder setCapabilities(int capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder addAbility(int ability) {
            capabilities |= ability;
            return this;
        }

        public Builder setPickerComponent(ComponentName pickerComponent) {
            this.pickerComponent = pickerComponent;
            return this;
        }

        public Builder setSettingsComponent(ComponentName settingsComponent) {
            this.settingsComponent = settingsComponent;
            return this;
        }

        public Config build() {
            if (pickerComponent == null) {
                throw new IllegalArgumentException("pickerComponent must not be null");
            }
            if ((capabilities & SETTINGS) != 0 && settingsComponent == null) {
                throw new IllegalArgumentException("You defined SETTINGS but left settingsComponent null");
            }
            return new Config(apiVersion, capabilities, pickerComponent, settingsComponent);
        }

    }
}
