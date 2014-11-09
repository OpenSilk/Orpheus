/*
 * Copyright 2013 Square Inc.
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.common.mortarflow;

import android.os.Bundle;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.CanShowScreen;
import org.opensilk.common.flow.FlowBundler;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.HasScope;

import flow.Backstack;
import flow.Flow;
import flow.Parcer;
import mortar.MortarScope;
import mortar.Presenter;

public class AppFlowPresenter<A extends AppFlowPresenter.Activity> extends Presenter<A> implements Flow.Listener {

    public interface Activity extends CanShowScreen, HasScope {
        Screen getDefaultScreen();
    }

    private final Parcer<Object> flowParcer;
    /** Persists the {@link Flow} in the bundle. Initialized with the home screen */
    private FlowBundler flowBundler;
    private AppFlow appFlow;

    public AppFlowPresenter(Parcer<Object> flowParcer) {
        this.flowParcer = flowParcer;
    }

    @Override protected MortarScope extractScope(A activity) {
        return activity.getScope();
    }

    @Override public void onLoad(Bundle savedInstanceState) {
        if (appFlow == null) {
            flowBundler = new FlowBundler(getView().getDefaultScreen(), this, flowParcer);
            appFlow = new AppFlow(flowBundler.onCreate(savedInstanceState).getFlow());
        }
    }

    @Override public void onSave(Bundle outState) {
        if (flowBundler != null) flowBundler.onSaveInstanceState(outState);
    }

    @Override public void go(Backstack nextBackstack, Flow.Direction flowDirection,
                             Flow.Callback callback) {
        A view = getView();
        if (view == null) {
            callback.onComplete();
        } else {
            Screen screen = (Screen) nextBackstack.current().getScreen();
            showScreen(screen, flowDirection, callback);
        }
    }

    /**
     * Called from {@link #go} only when view is not null. Calls through to
     * {@link A#showScreen}, exposed as a hook for subclasses.
     */
    protected void showScreen(Screen screen, Flow.Direction flowDirection, Flow.Callback callback) {
        getView().showScreen(screen, flowDirection, callback);
    }

    /**
     * Gives access to the {@link AppFlow}, to allow it to be provided as a system service as
     * required by {@link AppFlow#get}.
     */
    public AppFlow getAppFlow() {
        return appFlow;
    }

    public Flow getFlow() {
        return flowBundler.getFlow();
    }

}
