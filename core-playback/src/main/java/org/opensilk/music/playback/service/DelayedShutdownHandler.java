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

package org.opensilk.music.playback.service;

import android.content.Context;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Message;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.playback.PlaybackStateHelper;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import timber.log.Timber;

import static org.opensilk.music.playback.PlaybackConstants.IDLE_DELAY;

/**
 * Created by drew on 4/22/15.
 */
@PlaybackServiceScope
public class DelayedShutdownHandler extends Handler {

    static final int MSG_STOP = 1;

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_STOP: {
                mShutdownScheduled = false;
                PlaybackService s = mService.get();
                if (s != null) {
                    PlaybackState state =
                            s.getMediaSession().getController().getPlaybackState();
                    if (state == null
                            || PlaybackStateHelper.isStoppedOrInactive(state)
                            || PlaybackStateHelper.isPaused(state)) {
                        s.stopSelf();
                    }
                }
            }
        }
    }

    WeakReference<PlaybackService> mService;
    boolean mShutdownScheduled;

    @Inject
    public DelayedShutdownHandler(
            PlaybackService service
    ) {
        mService = new WeakReference<PlaybackService>(service);
    }

    public void scheduleDelayedShutdown() {
        Timber.v("Scheduling shutdown in %d ms", IDLE_DELAY);
        cancelDelayedShutdown();
        sendEmptyMessageDelayed(MSG_STOP, IDLE_DELAY);
        mShutdownScheduled = true;
    }

    public void cancelDelayedShutdown() {
        Timber.d("Cancelling delayed shutdown, scheduled = %s", mShutdownScheduled);
        removeCallbacksAndMessages(null);
        mShutdownScheduled = false;
    }

    public boolean isShutdownScheduled() {
        return mShutdownScheduled;
    }

}
