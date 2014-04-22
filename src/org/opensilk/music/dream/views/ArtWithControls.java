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

package org.opensilk.music.dream.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.widgets.PlayPauseButton;

/**
 * Created by drew on 4/13/14.
 */
public class ArtWithControls extends ArtOnly {

    protected TextView mTrackTitle;
    protected PlayPauseButton mPlayPauseButton;

    public ArtWithControls(Context context) {
        super(context);
    }

    public ArtWithControls(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ArtWithControls(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTrackTitle = (TextView) findViewById(R.id.track_title);
        mPlayPauseButton = (PlayPauseButton) findViewById(R.id.footer_action_button_play);
//        update();
    }

    @Override
    public void update() {
        super.update();
        mPlayPauseButton.updateState();
        mTrackTitle.setText(MusicUtils.getTrackName());
    }

}
