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

import com.andrew.apollo.R;

/**
 * Created by drew on 7/7/14.
 */
public class PaletteableThumbnailArtworkImageView extends ThumbnailArtworkImageView {

    protected Palette.PaletteAsyncListener mListener;

    public PaletteableThumbnailArtworkImageView(Context context) {
        super(context);
    }

    public PaletteableThumbnailArtworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PaletteableThumbnailArtworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mListener = null;
    }

    public void installListener(Palette.PaletteAsyncListener l) {
        mListener = l;
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
        if (mListener != null) {
            Palette.generateAsync(b, mListener);
        }
    }
}
