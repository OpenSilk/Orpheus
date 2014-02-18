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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

/**
 * Created by drew on 2/18/14.
 */
public class NowPlayingAnimation extends LinearLayout {

    private ImageView mBar1;
    private ImageView mBar2;
    private ImageView mBar3;

    private AnimationDrawable mBar1Anim;
    private AnimationDrawable mBar2Anim;
    private AnimationDrawable mBar3Anim;

    private boolean mWasAnimating;
    private long mTrackId;

    /**
     * Yea google i ripped you off big time here... sorry
     * It's just that this animation is kickass, and i wanted to be cool like you
     * and not use a layout file.
     *
     * p.s. smali is fucking hard to read. Took me like 45 min to figure out what the hell
     * was going on with the layout params.
     */
    public NowPlayingAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        mBar1 = new ImageView(context);
        mBar1.setBackgroundResource(R.anim.peak_meter_1);
        mBar1.setLayoutParams(params);
        addView(mBar1);
        mBar2 = new ImageView(getContext());
        mBar2.setBackgroundResource(R.anim.peak_meter_2);
        mBar2.setLayoutParams(params);
        addView(mBar2);
        mBar3 = new ImageView(getContext());
        mBar3.setBackgroundResource(R.anim.peak_meter_3);
        addView(mBar3);

        mBar1Anim = (AnimationDrawable) mBar1.getBackground();
        mBar2Anim = (AnimationDrawable) mBar2.getBackground();
        mBar3Anim = (AnimationDrawable) mBar3.getBackground();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mWasAnimating) {
            if (MusicUtils.getCurrentAudioId() == mTrackId) {
                startAnimating();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimating();
    }

    /**
     * Called by cards to start the animation
     * @param trackId
     */
    public void startAnimating(long trackId) {
        mTrackId = trackId;
        startAnimating();
    }

    private void startAnimating() {
        mWasAnimating = true;
        setVisibility(VISIBLE);
        mBar1Anim.start();
        mBar2Anim.start();
        mBar3Anim.start();
    }

    public void stopAnimating() {
        mWasAnimating = false;
        setVisibility(GONE);
        mBar1Anim.stop();
        mBar2Anim.stop();
        mBar3Anim.stop();
    }

    public boolean isAnimating() {
        return mWasAnimating;
    }
}
