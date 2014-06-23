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
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import com.andrew.apollo.R;

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
    public void setBackgroundResource(int resid) {
        super.setBackgroundResource(resid);
    }

    public void init(String title) {
        if (TextUtils.isEmpty(title)) {
            throw new NullPointerException("Cannot init with null title");
        }
        Character c = title.toUpperCase().charAt(0);
        Integer color = COLORS.get(c);
        if (color == null) {
            color = R.color.red;
        }
//        setTextColor(getResources().getColor(color));
        setText(c.toString());
        setBackgroundColor(getResources().getColor(color));
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
