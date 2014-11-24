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

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.widget.ImageButton;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.common.util.VersionUtils;
import org.opensilk.music.R;

import static org.opensilk.common.util.ThemeUtils.getColorAccent;

/**
 * Created by drew on 11/18/14.
 */
public class PlaybackDrawableTint {

    public static void shuffleDrawable24(ImageButton v) {
        doShuffleDrawable(v, true);
    }

    public static void shuffleDrawable36(ImageButton v) {
        doShuffleDrawable(v, false);
    }

    static void doShuffleDrawable(ImageButton v, boolean is24) {
        if (VersionUtils.hasLollipop()) return;
        int active = is24 ? R.drawable.ic_shuffle_white_24dp : R.drawable.ic_shuffle_white_36dp;
        LevelListDrawable d = (LevelListDrawable) v.getDrawable();
        // dont know why this doesnt work
//        Drawable d1 = getResources().getDrawable(R.drawable.ic_shuffle_black_36dp);
//        d1.mutate().setColorFilter(Themer.getColorAccent(getContext()), PorterDuff.Mode.MULTIPLY);
        Drawable d1 = ThemeUtils.colorizeBitmapDrawableCopy(v.getContext(), active, getColorAccent(v.getContext()));
        d.addLevel(1, 2, d1);
    }

    public static void repeatDrawable24(ImageButton v) {
        doRepeatDrawable(v, true);
    }

    public static void repeatDrawable36(ImageButton v) {
        doRepeatDrawable(v, false);
    }

    static void doRepeatDrawable(ImageButton v, boolean is24) {
        if (VersionUtils.hasLollipop()) return;
        int one = is24 ? R.drawable.ic_repeat_one_white_24dp : R.drawable.ic_repeat_one_white_36dp;
        int all = is24 ? R.drawable.ic_repeat_white_24dp : R.drawable.ic_repeat_white_36dp;
        LevelListDrawable d = (LevelListDrawable) v.getDrawable();
        Drawable d1 = ThemeUtils.colorizeBitmapDrawableCopy(v.getContext(), one, getColorAccent(v.getContext()));
        d.addLevel(1, 1, d1);
        Drawable d2 = ThemeUtils.colorizeBitmapDrawableCopy(v.getContext(), all, getColorAccent(v.getContext()));
        d.addLevel(2, 2, d2);
    }

}
