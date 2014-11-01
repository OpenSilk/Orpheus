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

package org.opensilk.music.ui2.main2;

import android.os.Bundle;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.CanShowScreen;
import org.opensilk.common.flow.FlowBundler;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.HasScope;
import org.opensilk.music.ui2.gallery.GalleryScreen;

import flow.Backstack;
import flow.Flow;
import flow.Parcer;
import mortar.MortarScope;
import mortar.Presenter;

/**
 * Created by drew on 10/23/14.
 */
public abstract class AppFlowPresenter<A extends AppFlowPresenter.Activity> extends Presenter<A> implements Flow.Listener {

    public interface Activity extends CanShowScreen, HasScope {

    }

    /**
     * Persists the {@link Flow} in the bundle. Initialized with the home screen,
     */
    private final FlowBundler flowBundler;

    private AppFlow appFlow;

    public AppFlowPresenter(Parcer<Object> flowParcer) {
        flowBundler = new FlowBundler(getDefaultScreen(), this, flowParcer);
    }

    public abstract Screen getDefaultScreen();

    @Override protected MortarScope extractScope(A activity) {
        return activity.getScope();
    }

    @Override public void onLoad(Bundle savedInstanceState) {
        if (appFlow == null) appFlow = flowBundler.onCreate(savedInstanceState);
    }

    @Override public void onSave(Bundle outState) {
        flowBundler.onSaveInstanceState(outState);
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
