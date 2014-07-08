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
import android.util.AttributeSet;

/**
 * This image view is specifically designed for use inside the {@link org.opensilk.music.widgets.SquareFrameLayout}
 * We take advantage of knowing the parent will be a perfect square and simply take
 * half its height/width to make ourselves into a square, we can then use layout_gravity in the xml
 * to place ourselves in a quadrant of the parent.
 *
 * Created by drew on 7/7/14.
 */
public class HalfSizeSquareThumbnailArtworkImageView extends ThumbnailArtworkImageView {
    public HalfSizeSquareThumbnailArtworkImageView(Context context) {
        super(context);
    }

    public HalfSizeSquareThumbnailArtworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HalfSizeSquareThumbnailArtworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSize == 0 && heightSize == 0) {
            // If there are no constraints on size, let FrameLayout measure
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // Now use the smallest of the measured dimensions for both dimensions
            final int minSize = Math.min(getMeasuredWidth(), getMeasuredHeight()) / 2;
            setMeasuredDimension(minSize, minSize);
            return;
        }

        final int size;
        if (widthSize == 0 || heightSize == 0) {
            // If one of the dimensions has no restriction on size, set both dimensions to be the
            // on that does
            size = Math.max(widthSize, heightSize) /2;
        } else {
            // Both dimensions have restrictions on size, set both dimensions to be the
            // smallest of the two
            size = Math.min(widthSize, heightSize) /2;
        }

        final int newMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(newMeasureSpec, newMeasureSpec);
    }

    /**
     * improves the performance by not passing
     * requestLayout() to its parent, taking advantage of knowing that image size
     * won't change once set.
     */
    @Override
    public void requestLayout() {
        forceLayout();
    }

}
