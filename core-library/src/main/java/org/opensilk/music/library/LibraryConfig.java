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

package org.opensilk.music.library;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by drew on 11/11/14.
 */
public class LibraryConfig {

    /**
     * Library has a settings activity
     * Scope: Global
     */
    public static final long FLAG_HAS_SETTINGS   = 1 << 1;
    /**
     * Library requires authentication
     * Scope: Global
     */
    public static final long FLAG_REQUIRES_AUTH = 1 << 2;
    /**
     * Object supports deletion
     * Scope: Container, Item
     */
    public static final long FLAG_SUPPORTS_DELETE = 1 << 11;

    private final int apiVersion;
    private final long flags;
    private final Bundle meta;
    private final String authority;
    private final String label;

    public static final String META_LOGIN_COMPONENT = "loginComponent";
    public static final String META_SETTINGS_COMPONENT = "settingsComponent";
    public static final String META_MENU_NAME_SETTINGS = "menu_settings_name";

    protected LibraryConfig(
            int apiVersion,
            long flags,
            @NonNull Bundle meta,
            @NonNull String authority,
            @NonNull String label
    ) {
        this.apiVersion = apiVersion;
        this.flags = flags;
        this.meta = meta;
        this.authority = authority;
        this.label = label;
    }

    /**
     * @return Api version of plugin. Set automatically by {@code Builder}
     */
    public int getApiVersion() {
        return apiVersion;
    }

    /**
     * Checks if an {@link LibraryCapability} is declared
     */
    public boolean hasFlag(long flag) {
        return (flags & flag) != 0;
    }

    /**
     * @return Bitmask of {@link LibraryCapability}s
     */
    public long getFlags() {
        return flags;
    }

    /**
     * @return optional config bundle
     */
    public Bundle getMetaBundle() {
        return meta;
    }

    /**
     * @return Object at key or null (uses unsafe casting)
     */
    public <T> T getMeta(String key) {
        return getMeta(key, null);
    }

    /**
     * @return Object at key or defvalue (uses unsafe casting)
     */
    public <T> T getMeta(String key, T defvalue) {
        if (meta.containsKey(key)) {
            return (T) meta.get(key);
        } else {
            return defvalue;
        }
    }

    /**
     * @return Library's authority
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * @return Label declared in manifest
     */
    public String getLabel() {
        return label;
    }

    public boolean requiresAuth() {
        return (flags & FLAG_REQUIRES_AUTH) != 0;
    }

    /**
     * Component for activity to allow user to choose from available libraries.
     * The activity should be of Dialog Style, and should take care of everything needed
     * to allow user to access the library, including selecting from an available library (or
     * account) and any auth/sign in required. The activity must return {@link android.app.Activity#RESULT_OK}
     * with the extra {@link LibraryConstants#EXTRA_LIBRARY_ID} in the Intent containing the identity Orpheus will pass
     * to all subsequent calls. Or pass a {@link org.opensilk.music.library.LibraryInfo} as the extra
     * {@link LibraryConstants#EXTRA_LIBRARY_INFO} with the {@link org.opensilk.music.library.LibraryInfo#libraryId}
     * and {@link org.opensilk.music.library.LibraryInfo#libraryName} populated.
     * <p>
     * Although not required, it is preferable the activity utilizes
     * {@link LibraryConstants#EXTRA_WANT_LIGHT_THEME} to style the activity to match the
     * current Orpheus theme.
     */
    public ComponentName getLoginComponent() {
        return meta.<ComponentName>getParcelable(META_LOGIN_COMPONENT);
    }

    public Bundle dematerialize() {
        Bundle b = new Bundle(4);
        b.putInt("_1", apiVersion);
        b.putLong("_2", flags);
        b.putBundle("_4", meta);
        b.putString("_5", authority);
        b.putString("_6", label);
        return b;
    }

    public static LibraryConfig materialize(Bundle b) {
        return new LibraryConfig(
                b.getInt("_1"),
                b.getLong("_2"),
                b.getBundle("_4"),
                b.getString("_5"),
                b.getString("_6")
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int apiVersion = 1; //TODO
        private long flags;
        private Bundle meta = new Bundle();
        private String authority;
        private String label;

        private Builder() {

        }

        public Builder setFlags(long flags) {
            this.flags = flags;
            return this;
        }

        public Builder setFlag(long flag) {
            flags |= flag;
            return this;
        }

        public Builder setLoginComponent(ComponentName component) {
            setFlag(FLAG_REQUIRES_AUTH);
            meta.putParcelable(META_LOGIN_COMPONENT, component);
            return this;
        }

        /**
         * Component for settings activity. The settings activity must process the
         * {@link LibraryConstants#EXTRA_LIBRARY_ID} and only manipulate preferences concerning the
         * given identity.
         * <p>
         * Although not required, it is preferable the activity utilizes
         * {@link LibraryConstants#EXTRA_WANT_LIGHT_THEME} to style the activity to match the
         * current Orpheus theme.
         */
        public Builder setSettingsComponent(ComponentName settingsComponent, String menuName) {
            setFlag(FLAG_HAS_SETTINGS);
            meta.putParcelable(META_SETTINGS_COMPONENT, settingsComponent);
            meta.putString(META_MENU_NAME_SETTINGS, menuName);
            return this;
        }

        public Builder setAuthority(String authority) {
            this.authority = authority;
            return this;
        }

        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }

        public LibraryConfig build() {
            if ((flags & FLAG_REQUIRES_AUTH) != 0 && meta.getParcelable(META_LOGIN_COMPONENT) == null) {
                throw new IllegalArgumentException("You defined REQUIRES_AUTH but left loginComponent null");
            }
            if ((flags & FLAG_HAS_SETTINGS) != 0 && meta.getParcelable(META_SETTINGS_COMPONENT) == null) {
                throw new IllegalArgumentException("You defined SETTINGS but left settingsComponent null");
            }
            if ((StringUtils.isEmpty(authority))) {
                throw new IllegalArgumentException("Authority may not be null");
            }
            if (StringUtils.isEmpty(label)) {
                throw new IllegalArgumentException("Label may not be null");
            }
            return new LibraryConfig(apiVersion, flags, meta, authority, label);
        }

    }
}
