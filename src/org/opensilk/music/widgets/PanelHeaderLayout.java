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

package org.opensilk.music.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by drew on 3/16/14.
 */
public class PanelHeaderLayout extends FrameLayout {

    private static final int TRANSITION_DURATION = 250;

    private TransitionDrawable mBackground;
    private boolean isOpen;

    public PanelHeaderLayout(Context context) {
        this(context, null);
    }

    public PanelHeaderLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PanelHeaderLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        boolean isLightTheme = ThemeHelper.isLightTheme(getContext());
        if (isLightTheme) {
            mBackground = (TransitionDrawable) getResources().getDrawable(R.drawable.header_background_light);
        } else {
            mBackground = (TransitionDrawable) getResources().getDrawable(R.drawable.header_background_dark);
        }
        mBackground.setCrossFadeEnabled(true);
        setBackground(mBackground);
    }

    private View mButtonBarClosed;
    private View mButtonBarOpen;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mButtonBarClosed = findViewById(R.id.header_closed_button_bar);
        mButtonBarOpen = findViewById(R.id.header_open_button_bar);
        mButtonBarOpen.setVisibility(GONE);
        isOpen = false;
    }

    public void transitionToClosed() {
        if (isOpen) {
            mBackground.reverseTransition(TRANSITION_DURATION*2);
            flipit();
            isOpen = false;
        }
    }

    public void transitionToOpen() {
        if (!isOpen) {
            mBackground.startTransition(TRANSITION_DURATION*2);
            flipit();
            isOpen = true;
        }
    }

    public void makeClosed() {
        mBackground.resetTransition();
        mButtonBarOpen.setVisibility(GONE);
        mButtonBarClosed.setVisibility(VISIBLE);
        isOpen = false;
    }

    public void makeOpen() {
        mBackground.resetTransition();
        mBackground.startTransition(0);
        mButtonBarOpen.setVisibility(VISIBLE);
        mButtonBarClosed.setVisibility(GONE);
        isOpen = true;
    }

    private Interpolator accelerator = new AccelerateInterpolator();
    private Interpolator decelerator = new DecelerateInterpolator();
    private void flipit() {
        final View visibleLayout;
        final View invisibleLayout;
        if (mButtonBarClosed.getVisibility() == View.GONE) {
            visibleLayout = mButtonBarOpen;
            invisibleLayout = mButtonBarClosed;
        } else {
            visibleLayout = mButtonBarClosed;
            invisibleLayout = mButtonBarOpen;
        }
        ObjectAnimator visToInvis = ObjectAnimator.ofFloat(visibleLayout, "rotationX", 0f, 90f);
        visToInvis.setDuration(TRANSITION_DURATION);
        visToInvis.setInterpolator(accelerator);
        final ObjectAnimator invisToVis = ObjectAnimator.ofFloat(invisibleLayout, "rotationX",
                -90f, 0f);
        invisToVis.setDuration(TRANSITION_DURATION);
        invisToVis.setInterpolator(decelerator);
        visToInvis.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                visibleLayout.setVisibility(View.GONE);
                invisToVis.start();
                invisibleLayout.setVisibility(View.VISIBLE);
            }
        });
        visToInvis.start();
    }

}
