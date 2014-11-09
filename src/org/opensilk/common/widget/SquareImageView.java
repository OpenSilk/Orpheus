/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.opensilk.common.rx.HoldsSubscription;
import org.opensilk.music.R;

import rx.Subscription;

/**
 * Created by drew on 10/22/14.
 */
public class SquareImageView extends ImageView implements HoldsSubscription, AnimatedImageView {

    public static final int TRANSITION_DURATION = 300;
    protected Subscription subscription;
    protected boolean defaultImageSet;

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSize == 0 && heightSize == 0) {
            // If there are no constraints on size, let FrameLayout measure
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // Now use the smallest of the measured dimensions for both dimensions
            final int minSize = Math.min(getMeasuredWidth(), getMeasuredHeight());
            setMeasuredDimension(minSize, minSize);
            return;
        }

        final int size;
        if (widthSize == 0 || heightSize == 0) {
            // If one of the dimensions has no restriction on size, set both dimensions to be the
            // on that does
            size = Math.max(widthSize, heightSize);
        } else {
            // Both dimensions have restrictions on size, set both dimensions to be the
            // smallest of the two
            size = Math.min(widthSize, heightSize);
        }

        final int newMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(newMeasureSpec, newMeasureSpec);
        requestLayout();
    }

    /**
     * improves the performance by not passing
     * requestLayout() to its parent, taking advantage of knowing that image size
     * won't change once set.
     */
    //TODO revisit
//    @Override
//    public void requestLayout() {
//        forceLayout();
//    }

    /*
     * Holds subscription
     */

    @Override
    public void addSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void removeSubscription(Subscription subscription) {
        this.subscription = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    /*
     * AnimatedImageView
     */

    @Override
    public void setDefaultImage() {
        setImageResource(R.drawable.default_artwork);
        defaultImageSet = true;
    }

    @Override
    public void setImageBitmap(Bitmap bm, boolean shouldAnimate) {
        if (shouldAnimate) {
            if (defaultImageSet) {
                defaultImageSet = false;
                Drawable layer1 = getDrawable();
                Drawable layer2 = createBitmapDrawable(bm);
                TransitionDrawable td = new TransitionDrawable(new Drawable[]{ layer1, layer2 });
                td.setCrossFadeEnabled(true);
                setImageDrawable(td);
                td.startTransition(TRANSITION_DURATION);
            } else if (getDrawable() != null) {
                Drawable layer1 = getDrawable();
                if (layer1 instanceof TransitionDrawable) {
                    layer1 = ((TransitionDrawable) layer1).getDrawable(1);
                }
                Drawable layer2 = createBitmapDrawable(bm);
                TransitionDrawable td = new TransitionDrawable(new Drawable[]{layer1, layer2});
                td.setCrossFadeEnabled(true);
                setImageDrawable(td);
                td.startTransition(TRANSITION_DURATION);
            } else {
                setAlpha(0.0f);
                setImageBitmap(bm);
                animate().alpha(1.0f).setDuration(TRANSITION_DURATION).start();
            }
        } else {
            setImageBitmap(bm);
        }
    }

    protected Drawable createBitmapDrawable(Bitmap bm) {
        return new BitmapDrawable(getResources(), bm);
    }

}
