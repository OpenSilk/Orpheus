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

package org.opensilk.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageButton;

import org.opensilk.common.theme.TintTypedArray;
import org.opensilk.music.R;

/**
 * Created by drew on 10/26/14.
 */
public class TintImageButton extends ImageButton {

    public TintImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TintImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a =  context.obtainStyledAttributes(attrs,
                R.styleable.TintImageButton, defStyleAttr, 0);

        //Be advised this only works because we override getResources() in the Activity
        //a more smarter way i havent figured out yet would be to override the theme
        //so TypedArray.getDrawable would work and no subclasses would be needed
        final int drawableId = a.getResourceId(R.styleable.TintImageButton_tintSrc, -1);
        if (drawableId >= 0) {
            setImageDrawable(context.getResources().getDrawable(drawableId));
        }

        a.recycle();
    }
}
