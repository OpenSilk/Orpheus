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

package org.opensilk.music.ui2.library;

import android.os.Bundle;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.FlowBundler;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.AppFlowPresenter;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui2.ActivityBlueprint;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Backstack;
import flow.Flow;
import flow.Layout;
import flow.Parcer;
import mortar.MortarScope;
import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 11/2/14.
 */
@Layout(R.layout.library_switcher)
@WithModule(LibrarySwitcherScreen.Module.class)
@WithTransitions(
        single = R.anim.grow_fade_in,
        forward = { R.anim.slide_out_left, R.anim.slide_in_right },
        backward = { R.anim.slide_out_right, R.anim.slide_in_left },
        replace = { R.anim.shrink_fade_out, R.anim.slide_in_left }
)
public class LibrarySwitcherScreen extends Screen {

    public final PluginInfo plugin;

    public LibrarySwitcherScreen(PluginInfo plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return super.getName() + plugin.componentName;
    }

    @dagger.Module (
            addsTo = ActivityBlueprint.Module.class,
            injects = LibrarySwitcherView.class,
            library = true
    )
    public static class Module {
        final LibrarySwitcherScreen screen;

        public Module(LibrarySwitcherScreen screen) {
            this.screen = screen;
        }

        @Provides
        public PluginInfo providePluginInfo() {
            return screen.plugin;
        }

        @Provides @Singleton
        public LibraryConnection provideLibraryConnection(PluginConnectionManager m, PluginInfo p) {
            return new LibraryConnection(m, p);
        }

    }

    @Singleton
    public static class Presenter extends AppFlowPresenter<LibrarySwitcherView> {

        final PluginInfo pluginInfo;
        final PluginConnectionManager connectionManager;

        @Inject
        Presenter(Parcer<Object> flowParcer,
                  PluginInfo pluginInfo,
                  PluginConnectionManager connectionManager) {
            super(flowParcer);
            this.pluginInfo = pluginInfo;
            this.connectionManager = connectionManager;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            super.onEnterScope(scope);
            //eagerly initiate bind
            connectionManager.bind(pluginInfo.componentName).subscribe();
        }

        @Override
        public void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            getView().setup();
            getFlow().resetTo(getDefaultScreen());
        }

        @Override
        protected void showScreen(Screen screen, Flow.Direction flowDirection, Flow.Callback callback) {
            Timber.v("showScreen %s", screen.getName());
            super.showScreen(screen, flowDirection, callback);
        }

        @Override
        public Screen getDefaultScreen() {
            return new PluginScreen();
        }
    }

}
