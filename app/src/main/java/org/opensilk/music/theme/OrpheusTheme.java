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

package org.opensilk.music.theme;

import org.opensilk.music.R;

/**
 * Created by drew on 11/15/14.
 */
public enum OrpheusTheme {
    DEFAULT(
            R.style.Theme_Light,
            R.style.Theme_Dark
    ),
    RED_YELLOW(
            R.style.Theme_Light_RedYellow,
            R.style.Theme_Dark_RedYellow
    ),
    RED_BLUE(
            R.style.Theme_Light_RedBlue,
            R.style.Theme_Dark_RedBlue
    ),
    PURPLE_ORANGE(
            R.style.Theme_Light_PurpleOrange,
            R.style.Theme_Dark_PurpleOrange
    ),
    INDIGO_YELLOW(
            R.style.Theme_Light_IndigoYellow,
            R.style.Theme_Dark_IndigoYellow
    ),
    INDIGO_RED(
            R.style.Theme_Light_IndigoRed,
            R.style.Theme_Dark_IndigoRed
    ),
    PINK_YELLOW(
            R.style.Theme_Light_PinkYellow,
            R.style.Theme_Dark_PinkYellow
    ),
    PINK_BLUE(
            R.style.Theme_Light_PinkBlue,
            R.style.Theme_Dark_PinkBlue
    ),
    BLUE_YELLOW(
            R.style.Theme_Light_BlueYellow,
            R.style.Theme_Dark_BlueYellow
    ),
    BLUE_ORANGE(
            R.style.Theme_Light_BlueOrange,
            R.style.Theme_Dark_BlueOrange
    ),
    BLUE_RED(
            R.style.Theme_Light_BlueRed,
            R.style.Theme_Dark_BlueRed
    ),
    TEAL_ORANGE(
            R.style.Theme_Light_TealOrange,
            R.style.Theme_Dark_TealOrange
    ),
    TEAL_PURPLE(
            R.style.Theme_Light_TealPurple,
            R.style.Theme_Dark_TealPurple
    );

    public final int light;
    public final int dark;

    OrpheusTheme(int light, int dark) {
        this.light = light;
        this.dark = dark;
    }
}
