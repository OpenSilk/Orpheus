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
import android.util.AttributeSet;
import android.widget.Checkable;

/**
 * Created by drew on 10/30/14.
 */
public class FloatingActionButtonCheckable extends FloatingActionButton implements Checkable {

    private static final int[] STATE_CHECKED = new int[]{android.R.attr.state_checked};
    private static final int[] STATE_UNCHECKED = new int[]{};

    private boolean mChecked;

    public FloatingActionButtonCheckable(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingActionButtonCheckable(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setChecked(final boolean checked) {
        if (checked == mChecked) return;
        mChecked = checked;
        setImageState(checked ? STATE_CHECKED : STATE_UNCHECKED, true);
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }
}
