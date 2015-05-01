/*
 * Copyright 2013 Square Inc.
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.ui.mortar;

import android.os.Bundle;
import android.view.Menu;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.common.core.util.Preconditions;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;
import rx.functions.Func1;

/** Allows shared configuration of the Android ActionBar. */
@ActivityScope
public class ActionBarOwner extends Presenter<ActionBarOwnerActivity> {

    public static class Config {
        public final boolean upButtonEnabled;
        public final int titleRes;
        public final CharSequence title;
        public final int subtitleRes;
        public final CharSequence subtitle;
        public final MenuConfig menuConfig;
        public final boolean transparentBackground;

        private Config(boolean upButtonEnabled,
                       int titleRes,
                       CharSequence title,
                       int subtitleRes,
                       CharSequence subtitle,
                       MenuConfig menuConfig,
                       boolean transparentBackground) {
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
            private MenuConfig menuConfig = null;
            private boolean trasparentBackground = false;

            public Builder() {
            }

            public Builder(Config config) {
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
            public Builder setMenuConfig(MenuConfig menuConfig) {
                this.menuConfig = menuConfig;
                return this;
            }
            public Builder setTransparentBackground(boolean yes) {
                this.trasparentBackground = yes;
                return this;
            }
            public Config build() {
                return new Config(setUpButtonEnabled,titleRes, title,
                        subtitleRes, subtitle, menuConfig, trasparentBackground);
            }
        }
    }

    public static class MenuConfig {
        public final Func1<Integer, Boolean> actionHandler;
        public final int[] menus;
        public final CustomMenuItem[] customMenus;

        private MenuConfig(Func1<Integer, Boolean> actionHandler,
                           int[] menus, CustomMenuItem[] customMenus) {
            this.actionHandler = actionHandler;
            this.menus = menus;
            this.customMenus = customMenus;
        }

        public static class Builder {
            public Func1<Integer, Boolean> actionHandler;
            public int[] menus = new int[0];
            public CustomMenuItem[] customMenus = new CustomMenuItem[0];
            public Builder() {}
            public Builder setActionHandler(Func1<Integer, Boolean> actionHandler) {
                this.actionHandler = actionHandler;
                return this;
            }
            public Builder withMenus(int... menus) {
                this.menus = concatArrays(this.menus, menus);
                return this;
            }
            public Builder withMenus(CustomMenuItem... customMenus) {
                this.customMenus = concatArrays(this.customMenus, customMenus);
                return this;
            }
            public MenuConfig build() {
                Preconditions.checkNotNull(actionHandler, "Must set actionHandler");
                return new MenuConfig(actionHandler, menus, customMenus);
            }
        }
    }

    public static class CustomMenuItem {
        public final int groupId;
        public final int itemId;
        public final int order;
        public final CharSequence title;
        public final int iconRes;

        public CustomMenuItem(int itemId, CharSequence title) {
            this.groupId = Menu.NONE;
            this.itemId = itemId;
            this.order = Menu.NONE;
            this.title = title;
            this.iconRes = -1;
        }

        public CustomMenuItem(int groupId, int itemId, int order, CharSequence title, int iconRes) {
            this.groupId = groupId;
            this.itemId = itemId;
            this.order = order;
            this.title = title;
            this.iconRes = iconRes;
        }
    }

    private Config config;

    @Inject
    public ActionBarOwner() {
        super();
    }

    @Override
    protected BundleService extractBundleService(ActionBarOwnerActivity view) {
        return BundleService.getBundleService(view.getScope());
    }

    @Override public void onLoad(Bundle savedInstanceState) {
        if (config != null) update();
    }

    public void setConfig(Config config) {
        this.config = config;
        update();
    }

    public Config getConfig() {
        return config;
    }

    private void update() {
        ActionBarOwnerActivity view = getView();
        if (view == null) return;
        view.setUpButtonEnabled(config.upButtonEnabled);
        if (config.titleRes >= 0) {
            view.setTitle(config.titleRes);
        } else {
            view.setTitle(config.title);
        }
        if (config.subtitleRes >= 0) {
            view.setSubtitle(config.subtitleRes);
        } else {
            view.setSubtitle(config.subtitle);
        }
        view.setMenu(config.menuConfig);
        view.setTransparentActionbar(config.transparentBackground);
    }

    protected static int[] concatArrays(int[] a1, int[] a2) {
        if (a1.length == 0) return a2;
        if (a2.length == 0) return a1;
        int a3[] = new int[a1.length + a2.length];
        System.arraycopy(a1, 0, a3, 0, a1.length);
        System.arraycopy(a2, 0, a3, a1.length, a2.length);
        return a3;
    }

    protected static CustomMenuItem[] concatArrays(CustomMenuItem[] a1, CustomMenuItem[] a2) {
        if (a1.length == 0) return a2;
        if (a2.length == 0) return a1;
        CustomMenuItem a3[] = new CustomMenuItem[a1.length + a2.length];
        System.arraycopy(a1, 0, a3, 0, a1.length);
        System.arraycopy(a2, 0, a3, a1.length, a2.length);
        return a3;
    }
}
