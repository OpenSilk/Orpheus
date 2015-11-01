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

import android.os.SystemClock;
import android.support.v4.media.session.PlaybackStateCompat;

import timber.log.Timber;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_CONNECTING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_FAST_FORWARDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_REWINDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;

/**
 * Created by drew on 4/23/15.
 */
public class PlaybackStateHelper {

    public static final float PLAYBACK_SPEED = 1.0f;

    private PlaybackStateHelper() {}

    public static boolean isInactive(PlaybackStateCompat state) {
        return isInactive(state.getState());
    }

    public static boolean isInactive(int state) {
        return state == STATE_NONE;
    }

    public static boolean isStopped(PlaybackStateCompat state) {
        return isStopped(state.getState());
    }

    public static boolean isStopped(int state) {
        return state == STATE_STOPPED;
    }

    public static boolean isPaused(PlaybackStateCompat state) {
        return isPaused(state.getState());
    }

    public static boolean isPaused(int state) {
        return state == STATE_PAUSED;
    }

    public static boolean isPlaying(PlaybackStateCompat state) {
        return isPlaying(state.getState());
    }

    public static boolean isPlaying(int state) {
        return state == STATE_PLAYING;
    }

    public static boolean isFastForwarding(PlaybackStateCompat state) {
        return isFastForwarding(state.getState());
    }

    public static boolean isFastForwarding(int state) {
        return state == STATE_FAST_FORWARDING;
    }

    public static boolean isRewinding(PlaybackStateCompat state) {
        return isRewinding(state.getState());
    }

    public static boolean isRewinding(int state) {
        return state == STATE_REWINDING;
    }

    public static boolean isBuffering(PlaybackStateCompat state) {
        return isBuffering(state.getState());
    }

    public static boolean isBuffering(int state) {
        return state == STATE_BUFFERING;
    }

    public static boolean isError(PlaybackStateCompat state) {
        return isError(state.getState());
    }

    public static boolean isError(int state) {
        return state == STATE_ERROR;
    }

    public static boolean isConnecting(PlaybackStateCompat state) {
        return isConnecting(state.getState());
    }

    public static boolean isConnecting(int state) {
        return state == STATE_CONNECTING;
    }

    public static boolean isSkippingPrevious(PlaybackStateCompat state) {
        return isSkippingPrevious(state.getState());
    }

    public static boolean isSkippingPrevious(int state) {
        return state == STATE_SKIPPING_TO_PREVIOUS;
    }

    public static boolean isSkippingNext(PlaybackStateCompat state) {
        return isSkippingNext(state.getState());
    }

    public static boolean isSkippingNext(int state) {
        return state == STATE_SKIPPING_TO_NEXT;
    }

    public static boolean isLoading(PlaybackStateCompat state) {
        return isLoading(state.getState());
    }

    public static boolean isPlayingOrPaused(PlaybackStateCompat state) {
        return isPlayingOrPaused(state.getState());
    }

    public static boolean isPlayingOrPaused(int state) {
        return isPlaying(state) || isPaused(state);
    }

    public static boolean isStoppedOrInactive(PlaybackStateCompat state) {
        return isStoppedOrInactive(state.getState());
    }

    public static boolean isStoppedOrInactive(int state) {
        return isStopped(state) || isInactive(state);
    }

    public static boolean isLoading(int state) {
        return isConnecting(state) || isBuffering(state);
    }

    public static boolean isLoadingOrSkipping(PlaybackStateCompat state) {
        return isLoadingOrSkipping(state.getState());
    }

    public static boolean isLoadingOrSkipping(int state) {
        switch (state) {
            case STATE_SKIPPING_TO_NEXT:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_QUEUE_ITEM:
                return true;
            default:
                return isLoading(state);
        }
    }

    public static boolean shouldShowPauseButton(PlaybackStateCompat state) {
        return shouldShowPauseButton(state.getState());
    }

    public static boolean shouldShowPauseButton(int state) {
        switch (state) {
            case STATE_FAST_FORWARDING:
            case STATE_REWINDING:
            case STATE_PLAYING:
                return true;
            default:
                return isLoadingOrSkipping(state);
        }
    }

    public static long getAdjustedSeekPos(PlaybackStateCompat state) {
        long now = SystemClock.elapsedRealtime();
        long then = state.getLastPositionUpdateTime();
        long seekPos = state.getPosition();
        Timber.d("geetAdjustedSeekPos now=%d, then=%d, pos=%d, adj=%d",
                now, then, seekPos, seekPos + (now - then));
        if (isPlaying(state)) {
            return then == 0 ? 0 : Math.max(0, seekPos + (now - then));
        } else {
            return then == 0 ? 0 : Math.max(0, state.getPosition());
        }
    }

    public static String stringifyState(int state) {
        switch (state) {
            case STATE_NONE:
                return "STATE_NONE";
            case STATE_STOPPED:
                return "STATE_STOPPED";
            case STATE_PAUSED:
                return "STATE_PAUSED";
            case STATE_PLAYING:
                return "STATE_PLAYING";
            case STATE_FAST_FORWARDING:
                return "STATE_FAST_FORWARDING";
            case STATE_REWINDING:
                return "STATE_REWINDING";
            case STATE_BUFFERING:
                return "STATE_BUFFERING";
            case STATE_ERROR:
                return "STATE_ERROR";
            case STATE_CONNECTING:
                return "STATE_CONNECTING";
            case STATE_SKIPPING_TO_PREVIOUS:
                return "STATE_SKIPPING_TO_PREVIOUS";
            case STATE_SKIPPING_TO_NEXT:
                return "STATE_SKIPPING_TO_NEXT";
            case STATE_SKIPPING_TO_QUEUE_ITEM:
                return "STATE_SKIPPING_TO_QUEUE_ITEM";
            default:
                return "UNKNOWN #" + state;
        }
    }

}
