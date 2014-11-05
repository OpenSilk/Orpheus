/*
 * Copyright 2014 Square Inc.
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

package org.opensilk.music.ui2.library;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.HandlesBack;
import org.opensilk.common.flow.HandlesUp;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.flow.ScreenSwitcherView;
import org.opensilk.common.flow.UpAndBackHandler;
import org.opensilk.common.mortarflow.AppFlowPresenter;
import org.opensilk.common.mortarflow.MortarAppFlowContextFactory;
import org.opensilk.common.mortarflow.ScreenSwitcher;
import org.opensilk.common.mortarflow.TransitionScreenSwitcher;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import flow.Flow;
import mortar.Mortar;
import mortar.MortarScope;

/** A FrameLayout that can show screens for an {@link AppFlow}. */
public class LibrarySwitcherView extends FrameLayout
        implements HandlesBack, HandlesUp, ScreenSwitcherView, AppFlowPresenter.Activity {

    @Inject LibrarySwitcherScreen.Presenter presenter;

    private ScreenSwitcher container;
    private UpAndBackHandler upAndBackHandler;
    private boolean disabled;

    public LibrarySwitcherView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(context, this);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        return !disabled && super.dispatchTouchEvent(ev);
    }

    @Override public ViewGroup getContainerView() {
        return this;
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    /**
     * Call from presenter after initializing flow
     */
    public void setup() {
        container = new TransitionScreenSwitcher(this, R.id.screen_switcher_tag,
                new MortarAppFlowContextFactory(presenter.getAppFlow()));
        upAndBackHandler = new UpAndBackHandler(presenter.getFlow());
    }

    @Override public void showScreen(Screen screen, Flow.Direction direction,
                                     final Flow.Callback callback) {
        disabled = true;
        container.showScreen(screen, direction, new Flow.Callback() {
            @Override public void onComplete() {
                callback.onComplete();
                disabled = false;
            }
        });
    }

    @Override public boolean onUpPressed() {
        return upAndBackHandler.onUpPressed(getCurrentChild());
    }

    @Override public boolean onBackPressed() {
        return upAndBackHandler.onBackPressed(getCurrentChild());
    }

    @Override public ViewGroup getCurrentChild() {
        return (ViewGroup) getContainerView().getChildAt(0);
    }

    @Override
    public MortarScope getScope() {
        return Mortar.getScope(getContext());
    }

}
