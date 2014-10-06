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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.opensilk.music.AppModule;
import org.opensilk.music.ui2.GodActivity;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.util.GsonParcer;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Backstack;
import flow.Flow;
import flow.Parcer;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.Presenter;

/**
 * Created by drew on 10/5/14.
 */
public class God implements Blueprint {

    @Override
    public String getMortarScopeName() {
        return getClass().getName();
    }

    @Override
    public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module(
            addsTo = AppModule.class,
            injects = GodActivity.class,
            library = true
    )
    public static class Module {

        @Provides @Singleton
        public Flow provideFlow(Presenter presenter) {
            return presenter.getFlow();
        }

    }

    @Singleton
    public static class Presenter extends mortar.Presenter<GodActivity> implements Flow.Listener {
        static final String FLOW_KEY = "FLOW_KEY";

        final Parcer<Object> parcer;

        Flow flow;

        @Inject
        protected Presenter(Parcer<Object> parcer) {
            this.parcer = parcer;
        }

        @Override
        protected MortarScope extractScope(GodActivity view) {
            return null;// view.getScope();
        }

        @Override
        public void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);

            if (flow == null) {
                Backstack backstack;

                if (savedInstanceState != null) {
                    backstack = Backstack.from(savedInstanceState.getParcelable(FLOW_KEY), parcer);
                } else {
                    backstack = Backstack.fromUpChain(new GalleryScreen());
                }

                flow = new Flow(backstack, this);
            }

            showScreen((Blueprint) flow.getBackstack().current().getScreen(), null);
        }

        @Override
        public void onSave(Bundle outState) {
            super.onSave(outState);
            outState.putParcelable(FLOW_KEY, flow.getBackstack().getParcelable(parcer));
        }

        @Override
        public void go(Backstack backstack, Flow.Direction direction,
                                 Flow.Callback callback) {
            Blueprint newScreen = (Blueprint) backstack.current().getScreen();
            showScreen(newScreen, direction);
            callback.onComplete();
        }

        public boolean onRetreatSelected() {
            return getFlow().goBack();
        }

        public boolean onUpSelected() {
            return getFlow().goUp();
        }

        protected void showScreen(Blueprint newScreen, Flow.Direction flowDirection) {
            GodActivity view = getView();
            if (view == null) return;
//            view.showScreen(newScreen, flowDirection);
        }

        public final Flow getFlow() {
            return flow;
        }

    }

}
