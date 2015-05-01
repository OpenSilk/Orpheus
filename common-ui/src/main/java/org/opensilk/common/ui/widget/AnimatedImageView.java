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

package org.opensilk.common.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.DrawableRes;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.opensilk.common.core.rx.HoldsSubscription;

import rx.Subscription;

/**
 * Created by drew on 10/26/14.
 */
public class AnimatedImageView extends ImageView implements HoldsSubscription {

    public static final int TRANSITION_DURATION = 300;

    protected Subscription subscription;
    protected boolean defaultImageSet;

    public AnimatedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

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

    public void setDefaultImage(@DrawableRes int defImage) {
        setImageResource(defImage);
        defaultImageSet = true;
    }

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
