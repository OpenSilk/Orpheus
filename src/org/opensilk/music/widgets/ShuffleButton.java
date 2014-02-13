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
import com.andrew.apollo.utils.ThemeUtils;
import com.andrew.apollo.widgets.theme.HoloSelector;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ShuffleButton extends ImageButton implements OnClickListener, OnLongClickListener {

    public ShuffleButton(Context context) {
        this(context, null);
    }

    public ShuffleButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.borderlessButtonStyle);
    }

    public ShuffleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnClickListener(this);
        setOnLongClickListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(final View v) {
        MusicUtils.cycleShuffle();
        updateShuffleState();
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
     * Sets the correct drawable for the shuffle state.
     */
    public void updateShuffleState() {
        switch (MusicUtils.getShuffleMode()) {
            case MusicPlaybackService.SHUFFLE_NORMAL:
                setContentDescription(getResources().getString(R.string.accessibility_shuffle_all));
                setImageResource(R.drawable.ic_shuffle_dark_selected);
                break;
            case MusicPlaybackService.SHUFFLE_AUTO:
                setContentDescription(getResources().getString(R.string.accessibility_shuffle_all));
                setImageResource(R.drawable.ic_shuffle_dark_selected);
                break;
            case MusicPlaybackService.SHUFFLE_NONE:
                setContentDescription(getResources().getString(R.string.accessibility_shuffle));
                setImageResource(R.drawable.ic_shuffle_dark);
                break;
            default:
                break;
        }
    }

}
