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
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.v4.util.LruCache;
import android.util.AttributeSet;
import android.widget.ImageButton;

import org.opensilk.common.ui.R;

/**
 * Allows tinting drawables on API < 21
 *
 * Created by drew on 10/26/14.
 */
public class TintImageButton extends ImageButton {

    private static final ColorFilterLruCache COLOR_FILTER_CACHE = new ColorFilterLruCache(6);
    static final PorterDuff.Mode TINT_MODE = PorterDuff.Mode.SRC_IN;

    public TintImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TintImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs == null) return;

        final TypedArray a =  context.obtainStyledAttributes(attrs,
                R.styleable.TintImageButton, defStyleAttr, 0);

        if (a == null) return;

        if (a.hasValue(R.styleable.TintImageButton_tibTint)) {
            final int color = a.getColor(R.styleable.TintImageButton_tibTint, 0);
            if (color != 0) {
                // XXX From AOSP see TintManager

                // First, lets see if the cache already contains the color filter
                PorterDuffColorFilter filter = COLOR_FILTER_CACHE.get(color, TINT_MODE);

                if (filter == null) {
                    // Cache miss, so create a color filter and add it to the cache
                    filter = new PorterDuffColorFilter(color, TINT_MODE);
                    COLOR_FILTER_CACHE.put(color, TINT_MODE, filter);
                }

                // Finally set the color filter
                getDrawable().setColorFilter(filter);
            }
        }

        a.recycle();
    }

    private static class ColorFilterLruCache extends LruCache<Integer, PorterDuffColorFilter> {

        public ColorFilterLruCache(int maxSize) {
            super(maxSize);
        }

        PorterDuffColorFilter get(int color, PorterDuff.Mode mode) {
            return get(generateCacheKey(color, mode));
        }

        PorterDuffColorFilter put(int color, PorterDuff.Mode mode, PorterDuffColorFilter filter) {
            return put(generateCacheKey(color, mode), filter);
        }

        private static int generateCacheKey(int color, PorterDuff.Mode mode) {
            int hashCode = 1;
            hashCode = 31 * hashCode + color;
            hashCode = 31 * hashCode + mode.hashCode();
            return hashCode;
        }
    }
}
