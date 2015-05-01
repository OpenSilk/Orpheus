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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.AbsSeekBar;
import android.widget.SeekBar;

import org.opensilk.common.core.util.VersionUtils;

import java.lang.reflect.Field;

import timber.log.Timber;

/**
 * Provides public access to thumb drawable to allow tinting for API < 16
 *
 * Created by drew on 10/12/14.
 */
public class CompatSeekBar extends SeekBar {

    public CompatSeekBar(Context context) {
        super(context);
    }

    public CompatSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompatSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override @SuppressWarnings("NewApi")
    public Drawable getThumb() {
        if (VersionUtils.hasApi16()) {
            return super.getThumb();
        } else {
            try {
                Field f = AbsSeekBar.class.getDeclaredField("mThumb");
                f.setAccessible(true);
                return (Drawable) f.get(this);
            } catch (Exception e) {
                Timber.e(e, "CompatSeekBar.getThumb() API=%d", Build.VERSION.SDK_INT);
                return null;
            }
        }
    }
}
