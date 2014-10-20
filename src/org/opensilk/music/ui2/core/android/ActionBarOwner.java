/*
 * Copyright 2013 Square Inc.
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

import android.content.Context;
import android.os.Bundle;

import org.opensilk.common.mortar.HasScope;

import mortar.Mortar;
import mortar.MortarScope;
import mortar.Presenter;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/** Allows shared configuration of the Android ActionBar. */
public class ActionBarOwner extends Presenter<ActionBarOwner.View> {
    public interface View extends HasScope {
        void setShowHomeEnabled(boolean enabled);

        void setUpButtonEnabled(boolean enabled);

        void setTitle(int titleRes);

        void setMenu(MenuConfig menuConfig);
    }

    public static class Config {
        public final boolean showHomeEnabled;
        public final boolean upButtonEnabled;
        public final int titleRes;
        public final MenuConfig menuConfig;

        public Config(boolean showHomeEnabled, boolean upButtonEnabled,
                      int titleRes, MenuConfig menuConfig) {
            this.showHomeEnabled = showHomeEnabled;
            this.upButtonEnabled = upButtonEnabled;
            this.titleRes = titleRes;
            this.menuConfig = menuConfig;
        }

        public Config withActions(Func1<Integer, Boolean> actionHandler, int... menus) {
            return new Config(showHomeEnabled, upButtonEnabled, titleRes, new MenuConfig(actionHandler, menus));
        }
    }

    public static class MenuConfig {
        public final Func1<Integer, Boolean> actionHandler;
        public final int[] menus;

        public MenuConfig(Func1<Integer, Boolean> actionHandler, int... menus) {
            this.actionHandler = actionHandler;
            this.menus = menus;
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

    @Override protected MortarScope extractScope(View view) {
        return view.getScope();
    }

    private void update() {
        View view = getView();
        if (view == null) return;

        view.setShowHomeEnabled(config.showHomeEnabled);
        view.setUpButtonEnabled(config.upButtonEnabled);
        view.setTitle(config.titleRes);
        view.setMenu(config.menuConfig);
    }
}
