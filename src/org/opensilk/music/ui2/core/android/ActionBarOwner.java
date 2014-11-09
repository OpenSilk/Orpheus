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

package org.opensilk.music.ui2.core.android;

import android.app.Notification;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import org.opensilk.common.mortar.HasScope;

import java.util.HashMap;

import javax.inject.Singleton;

import dagger.Provides;
import mortar.MortarScope;
import mortar.Presenter;
import rx.functions.Func1;

/** Allows shared configuration of the Android ActionBar. */
public class ActionBarOwner extends Presenter<ActionBarOwner.Activity> {

    @dagger.Module(
        library = true
    )
    public static class Module {
        @Provides @Singleton ActionBarOwner provideActionBarOwner() { return new ActionBarOwner(); }
    }

    public interface Activity extends HasScope {
//        void setShowHomeEnabled(boolean enabled);

        void setUpButtonEnabled(boolean enabled);

        void setTitle(int titleRes);
        void setTitle(CharSequence title);

        void setSubtitle(int subTitleRes);
        void setSubtitle(CharSequence title);

        void setMenu(MenuConfig menuConfig);
    }

    public static class Config {
        public final boolean showHomeEnabled;
        public final boolean upButtonEnabled;
        public final int titleRes;
        public final CharSequence title;
        public final int subtitleRes;
        public final CharSequence subtitle;
        public final MenuConfig menuConfig;

        @Deprecated
        public Config(boolean showHomeEnabled, boolean upButtonEnabled,
                      int titleRes, MenuConfig menuConfig) {
            this.showHomeEnabled = showHomeEnabled;
            this.upButtonEnabled = upButtonEnabled;
            this.titleRes = titleRes;
            this.menuConfig = menuConfig;
            this.title = null;
            this.subtitleRes = -1;
            this.subtitle = null;
        }

        @Deprecated
        public Config(boolean showHomeEnabled, boolean upButtonEnabled,
                      CharSequence title, MenuConfig menuConfig) {
            this.showHomeEnabled = showHomeEnabled;
            this.upButtonEnabled = upButtonEnabled;
            this.title = title;
            this.menuConfig = menuConfig;
            this.titleRes = -1;
            this.subtitleRes = -1;
            this.subtitle = null;
        }

        private Config(boolean showHomeEnabled, boolean upButtonEnabled,
                      int titleRes, CharSequence title, int subtitleRes,
                      CharSequence subtitle, MenuConfig menuConfig) {
            this.showHomeEnabled = showHomeEnabled;
            this.upButtonEnabled = upButtonEnabled;
            this.titleRes = titleRes;
            this.title = title;
            this.subtitleRes = subtitleRes;
            this.subtitle = subtitle;
            this.menuConfig = menuConfig;
        }

        public Builder buildUpon() {
            return new Builder(this);
        }

        public static class Builder {
            public boolean setHomeEnabled = true;
            public boolean setUpButtonEnabled = false;
            public int titleRes = -1;
            public CharSequence title = null;
            public int subtitleRes = -1;
            public CharSequence subtitle = null;
            public MenuConfig menuConfig = null;
            public Builder() {}
            public Builder(Config config) {
                setHomeEnabled = config.showHomeEnabled;
                setUpButtonEnabled = config.upButtonEnabled;
                titleRes = config.titleRes;
                title = config.title;
                subtitleRes = config.subtitleRes;
                subtitle = config.subtitle;
                menuConfig = config.menuConfig;
            }
            public Builder showHomeEnabled(boolean enabled) {
                setHomeEnabled = enabled;
                return this;
            }
            public Builder upButtonEnabled(boolean enabled) {
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
            public Builder withMenuConfig(MenuConfig menuConfig) {
                this.menuConfig = menuConfig;
                return this;
            }
            public Config build() {
                return new Config(setHomeEnabled, setUpButtonEnabled,
                        titleRes, title, subtitleRes, subtitle,
                        menuConfig);
            }
        }
    }

    public static class MenuConfig {
        public final Func1<Integer, Boolean> actionHandler;
        public final int[] menus;
        public final CustomMenuItem[] customMenus;

        public MenuConfig(Func1<Integer, Boolean> actionHandler, int... menus) {
            this.actionHandler = actionHandler;
            this.menus = menus;
            customMenus = new CustomMenuItem[0];
        }

        public MenuConfig(Func1<Integer, Boolean> actionHandler,
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
                this.menus = menus;
                return this;
            }
            public Builder withCustomMenus(CustomMenuItem... customMenus) {
                this.customMenus = customMenus;
                return this;
            }
            public MenuConfig build() {
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

    ActionBarOwner() {
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

    @Override protected MortarScope extractScope(Activity view) {
        return view.getScope();
    }

    private void update() {
        Activity view = getView();
        if (view == null) return;
//        view.setShowHomeEnabled(config.showHomeEnabled);
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
    }
}
