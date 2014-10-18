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
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import org.opensilk.music.R;
import com.andrew.apollo.utils.ThemeHelper;

import butterknife.ButterKnife;

/**
 * Created by drew on 3/16/14.
 */
public class PanelHeaderLayout extends FrameLayout {

    private static final int TRANSITION_DURATION = 200;
    private static final int LIGHT_BG_ALHPA = 0xcc;
    private static final int DARK_BG_ALHPA = 0x99;

    private int mBackgroundAlpha;
    private boolean isOpen;
    private FrameLayout mHeaderMenubar;

    public PanelHeaderLayout(Context context) {
        this(context, null);
    }

    public PanelHeaderLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PanelHeaderLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final boolean isLightTheme = ThemeHelper.isLightTheme(getContext());
        if (isLightTheme) {
            mBackgroundAlpha = LIGHT_BG_ALHPA;
            setBackgroundColor(getResources().getColor(R.color.app_background_light));
        } else {
            mBackgroundAlpha = DARK_BG_ALHPA;
            setBackgroundColor(getResources().getColor(R.color.app_background_dark));
        }
    }

    private View mButtonBarClosed;
    private View mButtonBarOpen;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeaderMenubar = ButterKnife.findById(this, R.id.header_menu_bar);
        mButtonBarClosed = findViewById(R.id.header_closed_button_bar);
        mButtonBarOpen = findViewById(R.id.header_open_button_bar);
        mButtonBarOpen.setVisibility(GONE);
        isOpen = false;
    }

    public void transitionToClosed() {
        if (isOpen) {
            setBackgroundAlpha(mBackgroundAlpha, 0xff);
            mButtonBarOpen.setVisibility(GONE);
            mButtonBarClosed.setVisibility(VISIBLE);
            isOpen = false;
        }
    }

    public void transitionToOpen() {
        if (!isOpen) {
            setBackgroundAlpha(0xff, mBackgroundAlpha);
            mButtonBarClosed.setVisibility(GONE);
            mButtonBarOpen.setVisibility(VISIBLE);
            isOpen = true;
        }
    }

    public void makeClosed() {
        getBackground().setAlpha(0xff);
        mButtonBarOpen.setVisibility(GONE);
        mButtonBarClosed.setVisibility(VISIBLE);
        isOpen = false;
    }

    public void makeOpen() {
        getBackground().setAlpha(mBackgroundAlpha);
        mButtonBarOpen.setVisibility(VISIBLE);
        mButtonBarClosed.setVisibility(GONE);
        isOpen = true;
    }

    private void setBackgroundAlpha(int from, int to) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
//        animator.setDuration(TRANSITION_DURATION * 2);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final int value = (Integer) animation.getAnimatedValue();
                getBackground().setAlpha(value);
            }
        });
        animator.start();
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
