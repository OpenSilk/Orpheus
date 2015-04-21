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

package org.opensilk.common.mortarflow;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.opensilk.common.R;
import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.HandlesBack;
import org.opensilk.common.flow.HandlesUp;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.flow.ScreenSwitcherView;
import org.opensilk.common.flow.UpAndBackHandler;

import flow.Flow;

/** A FrameLayout that can show screens for an {@link AppFlow}. */
public class FrameScreenSwitcherView extends FrameLayout
        implements HandlesBack, HandlesUp, ScreenSwitcherView {
    private final ScreenSwitcher container;
    private final UpAndBackHandler upAndBackHandler;

    private boolean disabled;

    @SuppressWarnings("UnusedDeclaration") // Used by layout inflation, of course!
    public FrameScreenSwitcherView(Context context, AttributeSet attrs) {
        this(context, attrs, new TransitionScreenSwitcher.Factory(R.id.screen_switcher_tag,
                new MortarContextFactory()));
    }

    /**
     * Allows subclasses to use custom {@link ScreenSwitcher} implementations. Allows the use
     * of more sophisticated transition schemes, and customized context wrappers.
     */
    protected FrameScreenSwitcherView(Context context, AttributeSet attrs,
                                      ScreenSwitcher.Factory switcherFactory) {
        super(context, attrs);
        container = switcherFactory.createScreenSwitcher(this);
        upAndBackHandler = new UpAndBackHandler(AppFlow.get(context));
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        return !disabled && super.dispatchTouchEvent(ev);
    }

    @Override public ViewGroup getContainerView() {
        return this;
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
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
        int childCount = getContainerView().getChildCount();
        if (childCount > 0) {
            return (ViewGroup) getChildAt(childCount-1);
        }
        return (ViewGroup) getContainerView().getChildAt(0);
    }
}
