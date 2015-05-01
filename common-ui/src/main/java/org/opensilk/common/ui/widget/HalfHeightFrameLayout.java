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
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by drew on 7/7/14.
 */
public class HalfHeightFrameLayout extends FrameLayout {
    public HalfHeightFrameLayout(Context context) {
        super(context);
    }

    public HalfHeightFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HalfHeightFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int newMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize/2, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, newMeasureSpec);
    }
}
