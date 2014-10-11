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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import mortar.Mortar;
import mortar.MortarScope;
import mortar.Presenter;
import rx.functions.Action0;

/**
 * Created by drew on 10/5/14.
 */
@Singleton
public class ActionBarPresenter extends Presenter<ActionBarPresenter.View> {

    public interface View extends HasScope {
        void setShowHomeEnabled(boolean enabled);
        void setUpButtonEnabled(boolean enabled);
        void setTitle(int title);
        void setMenu(List<MenuAction> actions);
    }

    public static class Config {
        public final boolean showHomeEnabled;
        public final boolean upButtonEnabled;
        public final int title;
        public final List<MenuAction> actions;

        private Config(boolean showHomeEnabled, boolean upButtonEnabled,
                      int title, List<MenuAction> actions) {
            this.showHomeEnabled = showHomeEnabled;
            this.upButtonEnabled = upButtonEnabled;
            this.title = title;
            this.actions = actions;
        }

        public static class Builder {
            private boolean showHomeEnabled;
            private boolean upButtonEnabled;
            private int title;
            private final List<MenuAction> actions;

            public Builder() {
                actions = new ArrayList<>();
            }

            public void setShowHomeEnabled(boolean showHomeEnabled) {
                this.showHomeEnabled = showHomeEnabled;
            }

            public void setUpButtonEnabled(boolean upButtonEnabled) {
                this.upButtonEnabled = upButtonEnabled;
            }

            public void setTitle(int title) {
                this.title = title;
            }

            public void addAction(MenuAction action) {
                actions.add(action);
            }

            public Config build() {
                return new Config(showHomeEnabled, upButtonEnabled, title, actions);
            }

            public static Builder from(Config config) {
                Builder b = new Builder();
                if (config == null) return b;
                b.setShowHomeEnabled(config.showHomeEnabled);
                b.setUpButtonEnabled(config.upButtonEnabled);
                b.setTitle(config.title);
                for (MenuAction a : config.actions) {
                    b.addAction(a);
                }
                return b;
            }
        }
    }

    public static class MenuAction {
        public final int title;
        public final Action0 action;

        public MenuAction(int title, Action0 action) {
            this.title = title;
            this.action = action;
        }
    }

    private Config config;
    private Config previousConfig;

    @Override
    protected MortarScope extractScope(View view) {
        return view.getScope();
    }

    @Override
    public void onLoad(Bundle savedInstanceState) {
        if (config != null) update();
    }

    public void setConfig(Config config) {
        this.previousConfig = this.config;
        this.config = config;
        update();
    }

    public Config getConfig() {
        return config;
    }

    public boolean restorePreviousConfig() {
        if (previousConfig == null) return false;
        Config c = this.previousConfig;
        this.previousConfig = this.config;
        this.config = c;
        update();
        return true;
    }

    private void update() {
        View view = getView();
        if (view == null) return;

        view.setShowHomeEnabled(config.showHomeEnabled);
        view.setUpButtonEnabled(config.upButtonEnabled);
        view.setTitle(config.title);
        view.setMenu(config.actions);
    }
}
