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

import org.opensilk.common.flow.Screen;
import org.opensilk.common.flow.WithTransition;
import org.opensilk.common.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.mortar.PausesAndResumes;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.R;
import org.opensilk.music.ui2.ActivityBlueprint;

import javax.inject.Inject;
import javax.inject.Singleton;

import flow.Layout;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;

/**
 * Created by drew on 10/15/14.
 */
@Layout(R.layout.queue)
@WithModule(QueueScreen.Module.class)
@WithTransition(in = R.animator.slide_in_bottom, out = R.animator.slide_out_bottom)
public class QueueScreen extends Screen {

    @dagger.Module(
            addsTo = ActivityBlueprint.Module.class,
            injects = QueueView.class
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<QueueView> implements PausesAndResumes {

        final PauseAndResumeRegistrar pauseAndResumeRegistrar;

        @Inject
        public Presenter(PauseAndResumeRegistrar pauseAndResumeRegistrar) {
            this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            super.onEnterScope(scope);
            pauseAndResumeRegistrar.register(scope, this);
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            getView().setup();
        }

        @Override
        public void onResume() {

        }

        @Override
        public void onPause() {

        }
    }
}
