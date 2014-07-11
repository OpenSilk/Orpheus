/*
 * Copyright (C) 2012 Andrew Neal
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

package org.opensilk.music.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * A custom {@link ImageButton} that represents the "repeat" button.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RepeatButton extends ImageButton implements OnClickListener, OnLongClickListener {

    private final Drawable mRepeatDrawable;
    private final Drawable mRepeatAllDrawable;
    private final Drawable mRepeatOneDrawable;

    public RepeatButton(Context context) {
        this(context, null);
    }

    public RepeatButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.borderlessButtonStyle);
    }

    public RepeatButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final boolean isLightTheme = ThemeHelper.isLightTheme(getContext());
        if (isLightTheme) {
            mRepeatDrawable = getResources().getDrawable(R.drawable.ic_action_playback_repeat_black);
        } else {
            mRepeatDrawable = getResources().getDrawable(R.drawable.ic_action_playback_repeat_white);
        }
        final int accentColor = ThemeHelper.getAccentColor(getContext());
        mRepeatAllDrawable = ThemeHelper.themeDrawable(getContext(),
                R.drawable.ic_action_playback_repeat_white, accentColor);
        mRepeatOneDrawable = ThemeHelper.themeDrawable(getContext(),
                R.drawable.ic_action_playback_repeat_1_black, accentColor);
        setOnClickListener(this);
        setOnLongClickListener(this);
        updateRepeatState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(final View v) {
        MusicUtils.cycleRepeat();
        updateRepeatState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onLongClick(final View view) {
        if (TextUtils.isEmpty(view.getContentDescription())) {
            return false;
        } else {
            ApolloUtils.showCheatSheet(view);
            return true;
        }
    }

    /**
     * Sets the correct drawable for the repeat state.
     */
    public void updateRepeatState() {
        switch (MusicUtils.getRepeatMode()) {
            case MusicPlaybackService.REPEAT_ALL:
                setContentDescription(getResources().getString(R.string.accessibility_repeat_all));
                setImageDrawable(mRepeatAllDrawable);
                break;
            case MusicPlaybackService.REPEAT_CURRENT:
                setContentDescription(getResources().getString(R.string.accessibility_repeat_one));
                setImageDrawable(mRepeatOneDrawable);
                break;
            case MusicPlaybackService.REPEAT_NONE:
                setContentDescription(getResources().getString(R.string.accessibility_repeat));
                setImageDrawable(mRepeatDrawable);
                break;
            default:
                break;
        }
    }
}
