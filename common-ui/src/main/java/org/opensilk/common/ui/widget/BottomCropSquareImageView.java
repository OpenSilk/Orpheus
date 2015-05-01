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
import android.graphics.Matrix;
import android.util.AttributeSet;

/**
 * Created by andrew on 2/26/14.
 */
public class BottomCropSquareImageView extends AnimatedImageView {

    public BottomCropSquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        configureBounds(l, t,  r, b);
        return super.setFrame(l, t, r, b);
    }

    private void configureBounds(int l, int t, int r, int b) {
        if (getDrawable() == null || getImageMatrix() == null) {
            return;
        }

        float width = r - l;
        float height = b - t;

        Matrix matrix = getImageMatrix();
        float scaleFactor, scaleFactorWidth, scaleFactorHeight;
        scaleFactorWidth =  width / (float) getDrawable().getIntrinsicWidth();
        scaleFactorHeight = height / (float) getDrawable().getIntrinsicHeight();

        if(scaleFactorHeight > scaleFactorWidth) {
            scaleFactor = scaleFactorHeight;
        } else {
            scaleFactor = scaleFactorWidth;
        }

        matrix.setScale(scaleFactor, scaleFactor, 0, 0);
        setImageMatrix(matrix);
    }

}