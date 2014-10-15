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

import com.andrew.apollo.R;

import javax.inject.Inject;
import javax.inject.Singleton;

import flow.Layout;
import mortar.Blueprint;
import mortar.ViewPresenter;

/**
 * Created by drew on 10/15/14.
 */
@Layout(R.layout.queue)
public class QueueScreen implements Blueprint {
    @Override
    public String getMortarScopeName() {
        return getClass().getName();
    }

    @Override
    public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module(
            injects = QueueView.class,
            addsTo = MainScreen.Module.class
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<QueueView> {

        @Inject
        public Presenter() {

        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            getView().setup();
        }
    }
}
