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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.music.R;

/**
 * ImageView with spinning drawable
 *
 * Created by drew on 4/17/14.
 */
public class PlayingIndicator extends ImageView {

    /** draw interval on pre Jellybean devices */
    protected static final int ANIMATION_RESOLUTION = 200;

    /** Decrease this for faster rotation */
    protected static final int ANIMATION_DURATION = 4000;

    protected RotateDrawable mRotateDrawable;
    protected Transformation mTransformation;
    protected Interpolator mInterpolator;
    protected AlphaAnimation mAnimation;

    protected long mLastDrawTime;
    private boolean isAnimating;
    private boolean mShouldAnimate;

    public PlayingIndicator(Context context) {
        super(context);
        init();
    }

    public PlayingIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlayingIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        if (ThemeUtils.isLightTheme(getContext())) {
            setImageResource(R.drawable.playing_indicator_light);
        } else {
            setImageResource(R.drawable.playing_indicator_dark);
        }
        LayerDrawable ld = (LayerDrawable) getDrawable();
        mRotateDrawable = (RotateDrawable) ld.getDrawable(0);
    }

    /**
     * @see android.widget.ProgressBar#draw(android.graphics.Canvas) for rotation logic
     * @param canvas
     */
    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isAnimating) {
            long time = getDrawingTime();
            mAnimation.getTransformation(time, mTransformation);
            float scale = mTransformation.getAlpha();
            mRotateDrawable.setLevel((int) (scale*10000.0f));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                postInvalidateOnAnimation();
            } else {
                if (SystemClock.uptimeMillis() - mLastDrawTime >= ANIMATION_RESOLUTION) {
                    mLastDrawTime = SystemClock.uptimeMillis();
                    postInvalidateDelayed(ANIMATION_RESOLUTION);
                }
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mShouldAnimate) {
            startAnimating();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isAnimating) {
            stopAnimating();
            mShouldAnimate = true;
        }
    }

    /**
     * Starts the animation and sets view visible
     * @see android.widget.ProgressBar#startAnimation() for rotation logic
     */
    public void startAnimating() {
        if (!isAnimating) {
            isAnimating = true;
            setVisibility(VISIBLE);

            if (mInterpolator == null) {
                mInterpolator = new LinearInterpolator();
            }

            if (mTransformation == null) {
                mTransformation = new Transformation();
            } else {
                mTransformation.clear();
            }

            if (mAnimation == null) {
                mAnimation = new AlphaAnimation(0.0f, 1.0f);
            } else {
                mAnimation.reset();
            }

            mAnimation.setRepeatMode(AlphaAnimation.RESTART);
            mAnimation.setRepeatCount(Animation.INFINITE);
            mAnimation.setDuration(ANIMATION_DURATION);
            mAnimation.setInterpolator(mInterpolator);
            mAnimation.setStartTime(Animation.START_ON_FIRST_FRAME);
        }
    }

    /**
     * Stops animation and hides the view
     *
     */
    public void stopAnimating() {
        if (isAnimating) {
            isAnimating = false;
            setVisibility(GONE);
            postInvalidate(); //TODO needed?
        }
    }

    /**
     * @return true if currently animating
     */
    public boolean isAnimating() {
        return isAnimating;
    }

}
