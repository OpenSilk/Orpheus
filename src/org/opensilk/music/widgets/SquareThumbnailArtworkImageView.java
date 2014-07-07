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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.PaletteItem;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.andrew.apollo.R;

/**
 * Created by drew on 7/7/14.
 */
public class SquareThumbnailArtworkImageView extends ThumbnailArtworkImageView {
    public SquareThumbnailArtworkImageView(Context context) {
        super(context);
    }

    public SquareThumbnailArtworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareThumbnailArtworkImageView(Context context, AttributeSet attrs, int defStyle) {
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

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (drawable != null && (drawable instanceof TransitionDrawable)) {
            TransitionDrawable d = (TransitionDrawable)drawable;
            Drawable d2 = d.getDrawable(1);
            if (d2 != null && (d2 instanceof BitmapDrawable)) {
                Bitmap b = ((BitmapDrawable)d2).getBitmap();
                makePalette(b);
            }
        }
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        if (bm != null) {
            makePalette(bm);
        }
    }

    protected void makePalette(Bitmap b) {
        Palette.generateAsync(b, new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                View parent = (View) getParent();
                if (parent != null) {
                    View overlay = parent.findViewById(R.id.griditem_desc_overlay);
                    if (overlay != null) {
                        PaletteItem item = palette.getDarkVibrantColor();
                        if (item == null) {
                            item = palette.getVibrantColor();
                        }
                        if (item != null) {
                            overlay.setBackgroundColor(item.getRgb());
                        }
                    }
                }
            }
        });
    }

}
