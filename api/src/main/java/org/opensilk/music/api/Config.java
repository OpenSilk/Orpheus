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

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by drew on 11/11/14.
 */
public class Config implements Parcelable {


    public static final int SEARCHABLE = 1 << 0;
    public static final int SETTINGS = 1 << 1;
    public static final int DOWNLOAD = 1 << 2;
    public static final int DELETE = 1 << 3;
    public static final int RENAME = 1 << 4;

    /**
     * Api version of plugin. Set automatically by {@link Builder}
     */
    public final int apiVersion;
    /**
     * Bitmask of abilities listed above
     */
    public final int capabilities;
    /**
     * Intent for activity to allow user to choose from available libraries.
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
    public final Intent pickerIntent;
    /**
     * Intent for settings activity. The settings activity must process the
     * {@link OrpheusApi#EXTRA_LIBRARY_ID} and only manipulate preferences concerning the
     * given identity.
     * <p>
     * Although not required, it is preferable the activity utilizes
     * {@link OrpheusApi#EXTRA_WANT_LIGHT_THEME} to style the activity to match the
     * current Orpheus theme.
     */
    public final Intent settingsIntent;

    protected Config(int apiVersion, int capabilities, @NonNull Intent pickerIntent, @Nullable Intent settingsIntent) {
        this.apiVersion = apiVersion;
        this.capabilities = capabilities;
        this.pickerIntent = pickerIntent;
        this.settingsIntent = settingsIntent;
    }

    public boolean hasAbility(int ability) {
        return (capabilities & ability) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Config)) return false;
        Config config = (Config) o;
        if (apiVersion != config.apiVersion) return false;
        if (capabilities != config.capabilities) return false;
        if (!pickerIntent.equals(config.pickerIntent)) return false;
        if (settingsIntent != null ? !settingsIntent.equals(config.settingsIntent) : config.settingsIntent != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = apiVersion;
        result = 31 * result + capabilities;
        result = 31 * result + pickerIntent.hashCode();
        result = 31 * result + (settingsIntent != null ? settingsIntent.hashCode() : 0);
        return result;
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel out, int flags) {
        out.writeInt(apiVersion);
        out.writeInt(capabilities);
        pickerIntent.writeToParcel(out, flags);
        if (settingsIntent != null) {
            out.writeInt(1);
            settingsIntent.writeToParcel(out, flags);
        } else {
            out.writeInt(0);
        }
    }

    private static Config readParcel(Parcel in) {
        final int ver = in.readInt();
        final int caps = in.readInt();
        final Intent picker = Intent.CREATOR.createFromParcel(in);
        final Intent settings =
            (in.readInt() == 1) ? Intent.CREATOR.createFromParcel(in) : null;
        return new Config(ver, caps, picker, settings);
    }

    public static final Creator<Config> CREATOR = new Creator<Config>() {
        @Override public Config createFromParcel(Parcel source) {
            return readParcel(source);
        }
        @Override public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public static class Builder {
        private int apiVersion = OrpheusApi.API_VERSION;
        private int capabilities;
        private Intent pickerIntent;
        private Intent settingsIntent;

        public Builder setCapabilities(int capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder addAbility(int ability) {
            capabilities |= ability;
            return this;
        }

        public Builder setPickerIntent(Intent intent) {
            this.pickerIntent = intent;
            return this;
        }

        public Builder setSettingsIntent(Intent intent) {
            this.settingsIntent = intent;
            return this;
        }

        public Config build() {
            if (pickerIntent == null) {
                throw new IllegalArgumentException("pickerIntent must not be null");
            }
            if ((capabilities & SETTINGS) != 0 && settingsIntent == null) {
                throw new IllegalArgumentException("You defined SETTINGS but left settingsIntent null");
            }
            return new Config(apiVersion, capabilities, pickerIntent, settingsIntent);
        }

    }
}
