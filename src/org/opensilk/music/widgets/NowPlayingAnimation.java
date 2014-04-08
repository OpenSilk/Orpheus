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

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by drew on 2/18/14.
 */
public class NowPlayingAnimation extends LinearLayout {

    private AnimationDrawable mBar1Anim;
    private AnimationDrawable mBar2Anim;
    private AnimationDrawable mBar3Anim;

    private boolean isAnimating;

    public NowPlayingAnimation(Context context) {
        this(context, null);
    }

    public NowPlayingAnimation(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Based on the playing indicator in Google Music
     */
    public NowPlayingAnimation(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOrientation(HORIZONTAL);
        final int animRes1;
        final int animRes2;
        final int animRes3;
        if (ThemeHelper.isLightTheme(getContext())) {
            animRes1 = R.anim.playing_indicator_1_dark;
            animRes2 = R.anim.playing_indicator_2_dark;
            animRes3 = R.anim.playing_indicator_3_dark;
        } else {
            animRes1 = R.anim.playing_indicator_1_light;
            animRes2 = R.anim.playing_indicator_2_light;
            animRes3 = R.anim.playing_indicator_3_light;
        }
        final LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        params.gravity = Gravity.BOTTOM;
        final ImageView bar1 = new ImageView(getContext());
        bar1.setBackgroundResource(animRes1);
        bar1.setLayoutParams(params);
        addView(bar1);
        final ImageView bar2 = new ImageView(getContext());
        bar2.setBackgroundResource(animRes2);
        bar2.setLayoutParams(params);
        addView(bar2);
        final ImageView bar3 = new ImageView(getContext());
        bar3.setBackgroundResource(animRes3);
        bar3.setLayoutParams(params);
        addView(bar3);

        mBar1Anim = (AnimationDrawable) bar1.getBackground();
        mBar2Anim = (AnimationDrawable) bar2.getBackground();
        mBar3Anim = (AnimationDrawable) bar3.getBackground();
    }

    /**
     * Called by cards to start the animation
     */
    public void startAnimating() {
        if (!isAnimating) {
            isAnimating = true;
            setVisibility(VISIBLE);
            mBar1Anim.start();
            mBar2Anim.start();
            mBar3Anim.start();
        }
    }

    /**
     * Called by cards to stop the animation
     */
    public void stopAnimating() {
        if (isAnimating) {
            isAnimating = false;
            setVisibility(GONE);
            mBar1Anim.stop();
            mBar2Anim.stop();
            mBar3Anim.stop();
        }
    }

    public boolean isAnimating() {
        return isAnimating;
    }
}
