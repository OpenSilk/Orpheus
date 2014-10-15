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

import android.os.Bundle;

import org.opensilk.music.AppModule;
import org.opensilk.music.ui2.ActivityModule;
import org.opensilk.music.ui2.core.FlowOwner;
import org.opensilk.music.ui2.gallery.GalleryScreen;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Parcer;
import mortar.Blueprint;
import mortar.MortarScope;
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
        Timber.v("new MainScreen(%s)", scopename);
        this.scopename = scopename;
    }

    @Override
    public String getMortarScopeName() {
        return scopename;
    }

    @Override
    public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module(
            includes = ActivityModule.class,
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

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope(%s)", scope);
            super.onEnterScope(scope);
        }

        @Override
        protected void onExitScope() {
            Timber.v("onExitScope()");
            super.onExitScope();
        }

        @Override
        public void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad(%s)", savedInstanceState);
            super.onLoad(savedInstanceState);
        }

        @Override
        public void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
        }

        void openQueue() {
            Flow flow = getFlow();
            if (flow.getBackstack().current().getScreen() instanceof QueueScreen) return;
            flow.goTo(new QueueScreen());
        }

        void closeQueue() {
            Flow flow = getFlow();
            if (flow.getBackstack().current().getScreen() instanceof QueueScreen) flow.goBack();
        }
    }

}
