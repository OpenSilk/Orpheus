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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.AttributeSet;

/**
 * Created by drew on 10/26/14.
 */
public class CircleImageView extends SquareImageView {

    protected int radius;

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        radius = Math.max(w, h) * 2; // *2 what? id think it would be /2;
        Drawable d = getDrawable();
        if (d != null) {
            if (d instanceof RoundedBitmapDrawable) {
                ((RoundedBitmapDrawable)d).setCornerRadius(radius);
                d.invalidateSelf();
            } else if (d instanceof TransitionDrawable) {
                //Todo anyway to check size of transitiondrawable?
                Drawable d1 = ((TransitionDrawable) d).getDrawable(1);
                if (d1 instanceof RoundedBitmapDrawable) {
                    ((RoundedBitmapDrawable) d1).setCornerRadius(radius);
                    d.invalidateSelf();
                }
            }
        }
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        setImageDrawable(createRoundDrawable(bm));
    }

    @Override
    public void setImageResource(int resId) {
        setImageDrawable(createRoundDrawable(((BitmapDrawable) getResources().getDrawable(resId)).getBitmap()));
    }

    @Override
    protected Drawable createBitmapDrawable(Bitmap bm) {
        return createRoundDrawable(bm);
    }

    protected RoundedBitmapDrawable createRoundDrawable(Bitmap bm) {
        RoundedBitmapDrawable d = RoundedBitmapDrawableFactory.create(getResources(), bm);
        d.setCornerRadius(radius);
        return d;
    }
}
