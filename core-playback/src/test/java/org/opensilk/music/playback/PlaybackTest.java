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

package org.opensilk.music.playback;

import android.media.AudioManager;
import android.media.session.PlaybackState;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.music.playback.renderer.LocalRenderer;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by drew on 9/30/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(
        constants = org.opensilk.music.index.BuildConfig.class,
        sdk = Build.VERSION_CODES.LOLLIPOP
//        application = PlaybackTestApplication.class
)
public class PlaybackTest {

    AudioManager mAudiomanager;
    LocalRenderer mPlayback;
    LocalRenderer.Callback mCallback;

    @Before
    public void setup() {
        mAudiomanager = mock(AudioManager.class);
        mPlayback = new LocalRenderer(
                RuntimeEnvironment.application,
                mAudiomanager
        );
        mPlayback.setState(PlaybackState.STATE_NONE);
        mCallback = mock(LocalRenderer.Callback.class);
        mPlayback.setCallback(mCallback);
    }

    @Test
    public void testIdleState() {
        assertThat(mPlayback.getState()).isEqualTo(PlaybackState.STATE_NONE);
    }
}
