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

package org.opensilk.music.ui3.panel;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opensilk.music.R;

import org.opensilk.music.widgets.PlayPauseButton;
import org.opensilk.music.widgets.RepeatButton;
import org.opensilk.music.widgets.RepeatingImageButton;
import org.opensilk.music.widgets.ShuffleButton;
import org.opensilk.silkdagger.support.ActivityScopedDaggerFragment;

import butterknife.InjectView;

/**
 * Created by drew on 10/12/14.
 */
public class NowPlayingFragment extends ActivityScopedDaggerFragment {

    /*
     * Panel Footer
     */
    // Play and pause button
    @InjectView(R.id.footer_action_button_play)
    ImageButton mFooterPlayPauseButton;
    // Repeat button
    @InjectView(R.id.footer_action_button_repeat)
    ImageButton mFooterRepeatButton;
    // Shuffle button
    @InjectView(R.id.footer_action_button_shuffle)
    ImageButton mFooterShuffleButton;
    // Previous button
    @InjectView(R.id.footer_action_button_previous)
    ImageButton mFooterPreviousButton;
    // Next button
    @InjectView(R.id.footer_action_button_next)
    ImageButton mFooterNextButton;
    // Progess
    @InjectView(R.id.footer_progress_bar)
    SeekBar mFooterProgress;
    // Current time
    @InjectView(R.id.footer_player_current_time)
    TextView mFooterCurrentTime;
    // Total time
    @InjectView(R.id.footer_player_total_time)
    TextView mFooterTotalTime;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nowplaying, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
