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

package org.opensilk.common.ui.widget;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import org.opensilk.common.ui.R;

import java.util.Locale;

/**
 * Created by drew on 6/23/14.
 */
public class ColorCodedThumbnail extends TextView {

    public ColorCodedThumbnail(Context context) {
        super(context);
    }

    public ColorCodedThumbnail(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorCodedThumbnail(Context context, AttributeSet attrs, int defStyle) {
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

    public void init(String title) {
        Integer color = null;
        if (!TextUtils.isEmpty(title)) {
            if (title.equals("..")) {
                setText(title);
                color = R.color.red;
            } else {
                Character c = title.toUpperCase(Locale.US).charAt(0);
                setText(c.toString());
                if (c.compareTo('A') >= 0 && c.compareTo('Z') <= 0) {
                    color = COLORS.get(c);
                } else if (c.compareTo('0') >= 0 && c.compareTo('9') <= 0) {
                    color = COLORS.get(COLORS.keyAt(Integer.valueOf(c.toString())));
                }
            }
        }
        if (color == null) {
            color = R.color.gray;
        }
        ShapeDrawable bg = new ShapeDrawable(new OvalShape());
        bg.getPaint().setColor(getResources().getColor(color));
        setBackgroundDrawable(bg);
    }

    // http://www.christianfaur.com/conceptual/colorAlphabet/image3.html
    public static final SimpleArrayMap<Character, Integer> COLORS = new SimpleArrayMap<>(26);
    static {
        COLORS.put('A', R.color.blue);
        COLORS.put('B', R.color.red_violet);
        COLORS.put('C', R.color.green_yellow);
        COLORS.put('D', R.color.yellow_orange);
        COLORS.put('E', R.color.orange);
        COLORS.put('F', R.color.light_gray);
        COLORS.put('G', R.color.off_white);
        COLORS.put('H', R.color.gray);
        COLORS.put('I', R.color.yellow);
        COLORS.put('J', R.color.dark_purple);
        COLORS.put('K', R.color.light_yellow);
        COLORS.put('L', R.color.dark_pink);
        COLORS.put('M', R.color.dark_orange);
        COLORS.put('N', R.color.teal);
        COLORS.put('O', R.color.red);
        COLORS.put('P', R.color.dark_yellow);
        COLORS.put('Q', R.color.black);
        COLORS.put('R', R.color.dark_green);
        COLORS.put('S', R.color.purple);
        COLORS.put('T', R.color.light_blue);
        COLORS.put('U', R.color.green);
        COLORS.put('V', R.color.cyan);
        COLORS.put('W', R.color.pink);
        COLORS.put('X', R.color.dark_blue);
        COLORS.put('Y', R.color.olive_green);
        COLORS.put('Z', R.color.red_brown);
    }

}
