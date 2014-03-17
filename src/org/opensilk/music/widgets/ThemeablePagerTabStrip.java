/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.support.v4.view.PagerTabStrip;
import android.util.AttributeSet;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

public class ThemeablePagerTabStrip extends PagerTabStrip {

    public ThemeablePagerTabStrip(Context context) {
        this(context, null);
    }

    public ThemeablePagerTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        boolean isLightTheme = ThemeHelper.isLightTheme(getContext());
        if (isLightTheme) {
            setBackgroundResource(R.drawable.ab_stacked_solid_orpheus);
        } else {
            setBackgroundResource(R.drawable.ab_stacked_solid_orpheusdark);
        }
        int color = ThemeHelper.getInstance(getContext()).getThemeColor();
        setTextColor(color);
        setTabIndicatorColor(color);
    }

}
