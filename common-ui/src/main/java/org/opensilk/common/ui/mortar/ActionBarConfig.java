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

package org.opensilk.common.ui.mortar;

/**
 * Created by drew on 5/5/15.
 */
public class ActionBarConfig {
    public final boolean upButtonEnabled;
    public final int titleRes;
    public final CharSequence title;
    public final int subtitleRes;
    public final CharSequence subtitle;
    public final ActionBarMenuConfig menuConfig;
    public final boolean transparentBackground;

    private ActionBarConfig(
            boolean upButtonEnabled,
            int titleRes,
            CharSequence title,
            int subtitleRes,
            CharSequence subtitle,
            ActionBarMenuConfig menuConfig,
            boolean transparentBackground
    ) {
        this.upButtonEnabled = upButtonEnabled;
        this.titleRes = titleRes;
        this.title = title;
        this.subtitleRes = subtitleRes;
        this.subtitle = subtitle;
        this.menuConfig = menuConfig;
        this.transparentBackground = transparentBackground;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    public static class Builder {
        private boolean setUpButtonEnabled = false;
        private int titleRes = -1;
        private CharSequence title = null;
        private int subtitleRes = -1;
        private CharSequence subtitle = null;
        private ActionBarMenuConfig menuConfig = null;
        private boolean trasparentBackground = false;

        private Builder() {
        }

        private Builder(ActionBarConfig config) {
            setUpButtonEnabled = config.upButtonEnabled;
            titleRes = config.titleRes;
            title = config.title;
            subtitleRes = config.subtitleRes;
            subtitle = config.subtitle;
            menuConfig = config.menuConfig;
            trasparentBackground = config.transparentBackground;
        }

        public Builder setUpButtonEnabled(boolean enabled) {
            setUpButtonEnabled = enabled;
            return this;
        }

        public Builder setTitle(int resourceId) {
            titleRes = resourceId;
            return this;
        }

        public Builder setTitle(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder setSubtitle(int resourceId) {
            subtitleRes = resourceId;
            return this;
        }

        public Builder setSubtitle(CharSequence subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder setMenuConfig(ActionBarMenuConfig menuConfig) {
            this.menuConfig = menuConfig;
            return this;
        }

        public Builder setTransparentBackground(boolean yes) {
            this.trasparentBackground = yes;
            return this;
        }

        public ActionBarConfig build() {
            return new ActionBarConfig(setUpButtonEnabled, titleRes, title,
                    subtitleRes, subtitle, menuConfig, trasparentBackground);
        }
    }
}
