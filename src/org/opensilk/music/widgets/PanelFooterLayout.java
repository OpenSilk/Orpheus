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
import android.widget.LinearLayout;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by drew on 3/16/14.
 */
public class PanelFooterLayout extends LinearLayout {

    public PanelFooterLayout(Context context) {
        this(context, null);
    }

    public PanelFooterLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PanelFooterLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final boolean isLightTheme = ThemeHelper.isLightTheme(getContext());
        if (isLightTheme) {
            setBackgroundColor(getResources().getColor(R.color.app_background_light_transparent));
        } else {
            setBackgroundColor(getResources().getColor(R.color.app_background_dark_transparent));
        }
    }

    public void updateBackground(int color) {
        setBackgroundColor(ThemeHelper.setColorAlpha(color, 0xB3)); //70%
    }

}
