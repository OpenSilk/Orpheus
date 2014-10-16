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
import android.view.View;

import org.opensilk.music.ui2.ActivityModule;
import org.opensilk.music.ui2.core.FlowOwner;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui3.theme.Themer;
import org.opensilk.music.widgets.FloatingActionButton;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Parcer;
import mortar.Blueprint;
import mortar.MortarScope;
import rx.Observable;
import rx.functions.Action1;
import rx.operators.OperatorViewClick;
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
            injects = {
                    MainView.class,
                    FooterView.class,
            },
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

        final MusicServiceConnection musicService;

        @Inject
        protected Presenter(Parcer<Object> parcer, MusicServiceConnection musicService) {
            super(parcer);
            Timber.v("new MainScreen.Presenter()");
            this.musicService = musicService;
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
            initFabButtons();
        }

        @Override
        public void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
        }

        void initFabButtons() {
            MainView v = getView();
            if (v == null) return;
            //TODO use broadcast receiver to update instead
            musicService.isPlaying().subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    setFabPlayIcon(aBoolean);
                }
            });
            v.fabPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    musicService.playOrPause().subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            setFabPlayIcon(aBoolean);
                        }
                    });
                }
            });
        }

        void setFabPlayIcon(boolean playing) {
            MainView v = getView();
            if (v == null) return;
            v.fabPlay.setIcon(playing ? Themer.getPauseIcon(v.getContext(), true)
                    : Themer.getPlayIcon(v.getContext(), true));
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

        OperatorViewClick<FloatingActionButton> fabPlayOperator;
    }

}
