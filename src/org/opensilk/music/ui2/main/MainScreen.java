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

import org.opensilk.music.AppModule;
import org.opensilk.music.ui2.core.FlowOwner;
import org.opensilk.music.ui2.gallery.GalleryScreen;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Parcer;
import mortar.Blueprint;
import timber.log.Timber;

/**
 * Created by drew on 10/5/14.
 */
public class MainScreen implements Blueprint {

    /**
     * Required for a race condition cause by Android when a new scope is created
     * before the old one is destroyed
     * <p/>
     * https://github.com/square/mortar/issues/87#issuecomment-43849264
     */
    final String scopename;

    public MainScreen(String scopename) {
        this.scopename = scopename;
    }

    @Override
    public String getMortarScopeName() {
        return getClass().getName() + scopename;
    }

    @Override
    public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module(
            addsTo = AppModule.class,
            injects = MainView.class,
            library = true
    )
    public static class Module {

        @Provides @Singleton
        public Flow provideFlow(Presenter presenter) {
            return presenter.getFlow();
        }

    }

    @Singleton
    public static class Presenter extends FlowOwner<Blueprint, MainView> {

        @Inject
        protected Presenter(Parcer<Object> parcer) {
            super(parcer);
            Timber.v("new MainScreen.Presenter()");
        }

        @Override
        protected Blueprint getFirstScreen() {
            return new GalleryScreen();
        }
    }

}
