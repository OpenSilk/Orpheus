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

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;

import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.playback.service.PlaybackService;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Created by drew on 4/22/15.
 */
public class AudioManagerHelper {

    final AudioManager mAudioManager;

    OnFocusChangedListener mChangeListener;
    Handler mCallbackHandler;

    @Inject
    public AudioManagerHelper(
            AudioManager mAudioManager
    ) {
        this.mAudioManager = mAudioManager;
    }

    public boolean requestFocus() {
        final int hint =  AudioManager.AUDIOFOCUS_GAIN;
        final int status = mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC, hint);
        boolean granted = status == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        Timber.v("audio focus request status granted = " + status);
        return granted;
    }

    public void abandonFocus() {
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
    }

    public int getAudioSessionId() {
        if (VersionUtils.hasLollipop()) {
            return mAudioManager.generateAudioSessionId();
        } else {
            MediaPlayer mp = new MediaPlayer();
            try {
                return mp.getAudioSessionId();
            } finally {
                mp.release();
            }
        }
    }

    public void setChangeListener(OnFocusChangedListener mChangeListener, Handler mCallbackHandler) {
        this.mChangeListener = mChangeListener;
        this.mCallbackHandler = mCallbackHandler;
    }

    public interface OnFocusChangedListener {
        void onFocusLost();
        void onFocusLostTransient();
        void onFocusLostDuck();
        void onFocusGain();
    }

    private final AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(final int focusChange) {
            if (mChangeListener != null) {
                final OnFocusChangedListener l = mChangeListener;
                Runnable r = null;
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        r = new Runnable() {
                            @Override
                            public void run() {
                                l.onFocusLost();
                            }
                        };
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        r = new Runnable() {
                            @Override
                            public void run() {
                                l.onFocusLostTransient();
                            }
                        };
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        r = new Runnable() {
                            @Override
                            public void run() {
                                l.onFocusLostDuck();
                            }
                        };
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        r = new Runnable() {
                            @Override
                            public void run() {
                                l.onFocusGain();
                            }
                        };
                        break;
                    default:
                        break;
                }
                if (r != null) {
                    if (mCallbackHandler != null) {
                        mCallbackHandler.post(r);
                    } else {
                        r.run();
                    }
                }
            }

        }
    };
}
