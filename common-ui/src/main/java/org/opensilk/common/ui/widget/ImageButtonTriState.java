/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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
import android.widget.ImageButton;

import timber.log.Timber;

/**
 * Created by drew on 10/2/15.
 */
public class ImageButtonTriState extends ImageButton {

    private static final int[] STATE_NONE = new int[]{};
    private static final int[] STATE_MIDDLE = new int[]{android.R.attr.state_middle};
    private static final int[] STATE_LAST = new int[]{android.R.attr.state_last};

    private int mState;

    public ImageButtonTriState(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void goToNextState() {
        switch (mState) {
            case 0:
                setState(1);
                break;
            case 1:
                setState(2);
                break;
            case 2:
                setState(0);
        }
    }

    public void gotoPreviousState() {
        switch (mState) {
            case 2:
                setState(1);
                break;
            case 1:
                setState(0);
                break;
            case 0:
                setState(2);
        }
    }

    public void setState(int state) {
        switch (state) {
            case 0:
                setImageState(STATE_NONE, true);
                mState = state;
                break;
            case 1:
                setImageState(STATE_MIDDLE, true);
                mState = state;
                break;
            case 2:
                setImageState(STATE_LAST, true);
                mState = state;
                break;
        }
    }

    public int getState() {
        return mState;
    }
}
