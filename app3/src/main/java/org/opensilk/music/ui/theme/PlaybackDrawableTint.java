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

package org.opensilk.music.ui.theme;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Build;
import android.widget.ImageButton;

import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.R;

import static org.opensilk.common.ui.util.ThemeUtils.getColorAccent;

/**
 * Created by drew on 11/18/14.
 */
public class PlaybackDrawableTint {

    public static void shuffleDrawable24(ImageButton v) {
        if (VersionUtils.hasLollipop()) return;
        doShuffleDrawable(v, true, getColorAccent(v.getContext()));
    }

    public static void shuffleDrawable36(ImageButton v) {
        if (VersionUtils.hasLollipop()) return;
        doShuffleDrawable(v, false, getColorAccent(v.getContext()));
    }

    public static LevelListDrawable getShuffleDrawable36(Context context, int color) {
        final int res = R.drawable.ic_shuffle_white_36dp;
        final LevelListDrawable d = new LevelListDrawable();
        d.addLevel(0,0, context.getResources().getDrawable(res));
        d.addLevel(1, 2, tintDrawable(context, res, color));
        return d;
    }

    public static void repeatDrawable24(ImageButton v) {
        if (VersionUtils.hasLollipop()) return;
        doRepeatDrawable(v, true, getColorAccent(v.getContext()));
    }

    public static void repeatDrawable36(ImageButton v) {
        if (VersionUtils.hasLollipop()) return;
        doRepeatDrawable(v, false, getColorAccent(v.getContext()));
    }

    public static LevelListDrawable getRepeatDrawable36(Context context, int color) {
        final int one = R.drawable.ic_repeat_once_white_36dp;
        final int all = R.drawable.ic_repeat_white_36dp;
        final LevelListDrawable d = new LevelListDrawable();
        d.addLevel(0, 0, context.getResources().getDrawable(all));
        d.addLevel(1, 1, tintDrawable(context, one, color));
        d.addLevel(2, 2, tintDrawable(context, all, color));
        return d;
    }

    static void doShuffleDrawable(ImageButton v, boolean is24, int color) {
        int active = is24 ? R.drawable.ic_shuffle_white_24dp : R.drawable.ic_shuffle_white_36dp;
        LevelListDrawable d = (LevelListDrawable) v.getDrawable();
        d.addLevel(1, 2, tintDrawable(v.getContext(), active, color));
    }

    static void doRepeatDrawable(ImageButton v, boolean is24, int color) {
        int one = is24 ? R.drawable.ic_repeat_once_white_24dp : R.drawable.ic_repeat_once_white_36dp;
        int all = is24 ? R.drawable.ic_repeat_white_24dp : R.drawable.ic_repeat_white_36dp;
        LevelListDrawable d = (LevelListDrawable) v.getDrawable();
        d.addLevel(1, 1, tintDrawable(v.getContext(), one, color));
        d.addLevel(2, 2, tintDrawable(v.getContext(), all, color));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static Drawable tintDrawable(Context context, int res, int color) {
        if (VersionUtils.hasLollipop()) {
            final Drawable d = context.getDrawable(res);
            d.mutate().setTint(color);
            return d;
        } else {
            // dont know why this isnt working
//            Drawable d = context.getResources().getDrawable(res);
//            d.mutate().setColorFilter(getColorAccent(context), PorterDuff.Mode.SRC_IN);
//            return d;
            return ThemeUtils.colorizeBitmapDrawableCopy(context, res, color);
        }
    }

}
