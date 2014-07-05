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

package com.andrew.apollo;

import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by drew on 7/4/14.
 */
public final class MusicPlayerHandler extends Handler {
    private static final String TAG = "MusicPlayerHandler";
    private static final boolean D = BuildConfig.DEBUG;

    /**
     * Indicates when the track ends
     */
    public static final int TRACK_ENDED = 1;

    /**
     * Indicates that the current track was changed the next track
     */
    public static final int TRACK_WENT_TO_NEXT = 2;

    /**
     * Indicates when the release the wake lock
     */
    public static final int RELEASE_WAKELOCK = 3;

    /**
     * Indicates the player died
     */
    public static final int SERVER_DIED = 4;

    /**
     * Indicates some sort of focus change, maybe a phone call
     */
    public static final int FOCUSCHANGE = 5;

    /**
     * Indicates to fade the volume down
     */
    public static final int FADEDOWN = 6;

    /**
     * Indicates to fade the volume back up
     */
    public static final int FADEUP = 7;

    private final WeakReference<MusicPlaybackService> mService;
    private float mCurrentVolume = 1.0f;

    /**
     * Constructor of <code>MusicPlayerHandler</code>
     *
     * @param service The service to use.
     * @param looper The thread to run on.
     */
    public MusicPlayerHandler(final MusicPlaybackService service, final Looper looper) {
        super(looper);
        mService = new WeakReference<>(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleMessage(final Message msg) {
        final MusicPlaybackService service = mService.get();
        if (service == null) {
            return;
        }

        switch (msg.what) {
            case FADEDOWN:
                mCurrentVolume -= .05f;
                if (mCurrentVolume > .2f) {
                    sendEmptyMessageDelayed(FADEDOWN, 10);
                } else {
                    mCurrentVolume = .2f;
                }
                service.getPlayer().setVolume(mCurrentVolume);
                break;
            case FADEUP:
                mCurrentVolume += .01f;
                if (mCurrentVolume < 1.0f) {
                    sendEmptyMessageDelayed(FADEUP, 10);
                } else {
                    mCurrentVolume = 1.0f;
                }
                service.getPlayer().setVolume(mCurrentVolume);
                break;
            case SERVER_DIED:
                if (service.isPlaying()) {
                    service.gotoNext(true);
                } else {
                    service.openCurrentAndNext();
                }
                break;
            case TRACK_WENT_TO_NEXT:
                service.wentToNext();
                break;
            case TRACK_ENDED:
                if (service.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
                    service.seekAndPlay(0);
                } else {
                    service.gotoNext(false);
                }
                break;
            case RELEASE_WAKELOCK:
                service.releaseWakeLock();
                break;
            case FOCUSCHANGE:
                if (D) Log.d(TAG, "Received audio focus change event " + msg.arg1);
                switch (msg.arg1) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        if (service.isPlaying()) {
                            service.setPausedByTransientLossOfFocus(msg.arg1 == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
                        }
                        service.pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        removeMessages(FADEUP);
                        sendEmptyMessage(FADEDOWN);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        if (!service.isPlaying() && service.isPausedByTransientLossOfFocus()) {
                            service.setPausedByTransientLossOfFocus(false);
                            mCurrentVolume = 0f;
                            service.getPlayer().setVolume(mCurrentVolume);
                            service.play();
                        } else {
                            removeMessages(FADEDOWN);
                            sendEmptyMessage(FADEUP);
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }
}
