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

package org.opensilk.music.ui3.theme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.andrew.apollo.R;

import org.opensilk.music.ui3.theme.widget.CompatSeekBar;

/**
 * Created by drew on 10/12/14.
 */
public class Themer {

    public static TypedValue resolveAttr(Context context, int attr) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, outValue, true);
        return outValue;
    }

    public static int getPrimaryColor(Context context) {
        return resolveAttr(context, R.attr.colorPrimary).data;
    }

    public static int getAccentColor(Context context) {
        return resolveAttr(context, R.attr.colorAccent).data;
    }

    public static int setColorAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static void themeSeekBar(SeekBar seekBar) {
        int color = getAccentColor(seekBar.getContext());
        seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        } else if (seekBar instanceof CompatSeekBar) {
            Drawable thumb = ((CompatSeekBar) seekBar).getThumb();
            if (thumb != null) thumb.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    public static void themeProgressBar(ProgressBar progressBar) {
        int color = getAccentColor(progressBar.getContext());
        progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
}
