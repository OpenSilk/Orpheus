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

package com.andrew.apollo.helper;

import android.media.AudioManager;
import android.media.MediaPlayer;

import com.andrew.apollo.PlaybackConstants;
import com.andrew.apollo.PlaybackService;

import org.opensilk.common.util.VersionUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Created by drew on 4/22/15.
 */
@Singleton
public class AudioManagerHelper {

    final PlaybackService mService;
    final AudioManager mAudioManager;

    @Inject
    public AudioManagerHelper(
            PlaybackService mService,
            AudioManager mAudioManager
    ) {
        this.mService = mService;
        this.mAudioManager = mAudioManager;
    }

    public boolean requestFocus() {
        final int hint =  AudioManager.AUDIOFOCUS_GAIN;
        final int status = mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC, hint);
        boolean granted = status == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        Timber.v("audio focus request status granted = " + status);
        mAudioManager.generateAudioSessionId();
        return granted;
    }

    public void abandonFocus() {
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
    }

    public int getAudioSessionId() {
        if (VersionUtils.hasLollipop()) {
            return mAudioManager.generateAudioSessionId();
        }
        MediaPlayer mp = new MediaPlayer();
        final int id = mp.getAudioSessionId();
        mp.release();
        return id;
    }

    private final AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(final int focusChange) {
            mService.getController().sendCommand(PlaybackConstants.CMD.FOCUSCHANGE,
                    BundleHelper.builder().putInt(focusChange).get(), null);
        }
    };
}
