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

package org.opensilk.music.ui2;


import com.google.gson.Gson;

import org.opensilk.common.flow.GsonParcer;
import org.opensilk.common.flow.Screen;
import org.opensilk.music.AppModule;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui2.loader.LoaderModule;
import org.opensilk.music.ui2.main.FooterView;
import org.opensilk.music.ui2.main.MainView;
import org.opensilk.music.ui2.main.NavView;
import org.opensilk.music.ui2.main.QueueView;
import org.opensilk.common.mortarflow.AppFlowPresenter;

import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Parcer;
import mortar.Blueprint;

/**
 * Created by drew on 10/23/14.
 */
public class BaseSwitcherActivityBlueprint implements Blueprint {

    /**
     * Required for a race condition cause by Android when a new scope is created
     * before the old one is destroyed
     * <p/>
     * https://github.com/square/mortar/issues/87#issuecomment-43849264
     */
    private final String scopeName;

    public BaseSwitcherActivityBlueprint(String scopeName) {
        this.scopeName = scopeName;
    }

    @Override public String getMortarScopeName() {
        return scopeName;
    }

    @Override public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module( //
            addsTo = AppModule.class,
            includes = {
                    ActivityModule.class,
                    LoaderModule.class,
            },
            injects = {
                    LauncherActivity.class,
            },
            library = true //
    )
    public static class Module {

        @Provides @Singleton
        public Parcer<Object> provideParcer(Gson gson) {
            return new GsonParcer<>(gson);
        }

        @Provides @Singleton
        public AppFlowPresenter<BaseSwitcherActivity> providePresenter(Parcer<Object> floParcer) {
            return new Presenter(floParcer);
        }

    }

    static class Presenter extends AppFlowPresenter<BaseSwitcherActivity> {

        Presenter(Parcer<Object> floParcer) {
            super(floParcer);
        }

        @Override
        public Screen getDefaultScreen() {
            return new GalleryScreen();
        }

        @Override public void showScreen(Screen newScreen, Flow.Direction direction, Flow.Callback callback) {
            super.showScreen(newScreen, direction, callback);
        }
    }
}
