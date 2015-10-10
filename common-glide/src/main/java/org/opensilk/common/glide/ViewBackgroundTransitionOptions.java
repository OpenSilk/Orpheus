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

package org.opensilk.common.glide;

import android.support.v7.graphics.Palette;

import com.bumptech.glide.TransitionOptions;

/**
 * Created by drew on 10/9/15.
 */
public class ViewBackgroundTransitionOptions extends TransitionOptions<ViewBackgroundTransitionOptions, Palette> {
    public static ViewBackgroundTransitionOptions withCrossFade() {
        return new ViewBackgroundTransitionOptions().crossFade();
    }
    public ViewBackgroundTransitionOptions crossFade() {
        return transition(PaletteableDrawableCrossFadeFactory.create());
    }
}
