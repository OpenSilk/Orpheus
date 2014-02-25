/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.widgets.theme;

import android.content.Context;
import android.support.v4.view.PagerTabStrip;
import android.util.AttributeSet;

import com.andrew.apollo.utils.ThemeHelper;

/**
 * This is a custom {@link android.support.v4.view.PagerTabStrip} that is made themeable by
 * allowing developers to choose the background and the text colors.
 */
public class ThemeablePagerTabStrip extends PagerTabStrip {

    /**
     * @param context The {@link Context} to use
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    @SuppressWarnings("deprecation")
    public ThemeablePagerTabStrip(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        int color = ThemeHelper.getInstance(getContext()).getThemeColor();
        // Theme the text color
        setTextColor(color);
        // Theme the footer
        setTabIndicatorColor(color);
    }

}
