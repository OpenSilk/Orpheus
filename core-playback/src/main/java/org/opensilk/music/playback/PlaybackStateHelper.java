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

import android.media.session.PlaybackState;
import android.os.SystemClock;

import javax.inject.Inject;

import static android.media.session.PlaybackState.ACTION_PAUSE;
import static android.media.session.PlaybackState.ACTION_PLAY;
import static android.media.session.PlaybackState.ACTION_PLAY_PAUSE;
import static android.media.session.PlaybackState.ACTION_SEEK_TO;
import static android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT;
import static android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS;
import static android.media.session.PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM;
import static android.media.session.PlaybackState.ACTION_STOP;
import static android.media.session.PlaybackState.PLAYBACK_POSITION_UNKNOWN;
import static android.media.session.PlaybackState.STATE_BUFFERING;
import static android.media.session.PlaybackState.STATE_CONNECTING;
import static android.media.session.PlaybackState.STATE_ERROR;
import static android.media.session.PlaybackState.STATE_FAST_FORWARDING;
import static android.media.session.PlaybackState.STATE_NONE;
import static android.media.session.PlaybackState.STATE_PAUSED;
import static android.media.session.PlaybackState.STATE_PLAYING;
import static android.media.session.PlaybackState.STATE_REWINDING;
import static android.media.session.PlaybackState.STATE_SKIPPING_TO_NEXT;
import static android.media.session.PlaybackState.STATE_SKIPPING_TO_PREVIOUS;
import static android.media.session.PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM;
import static android.media.session.PlaybackState.STATE_STOPPED;

//import static android.support.v4.media.session.PlaybackStateCompat.*;

/**
 * Created by drew on 4/23/15.
 */
public class PlaybackStateHelper {

    private int mState;
    private long mPosition;
    private long mBufferedPosition;
    private float mSpeed;
    private long mActions;
    private CharSequence mErrorMessage;
    private long mUpdateTime;
    private long mQueuePos;

    public static final float PLAYBACK_SPEED = 1.0f;

    @Inject
    public PlaybackStateHelper() {
        mState = STATE_NONE;
        mPosition = PLAYBACK_POSITION_UNKNOWN;
        mSpeed = PLAYBACK_SPEED;
        mBufferedPosition = PLAYBACK_POSITION_UNKNOWN;
        mActions = ACTION_PLAY
                | ACTION_PAUSE
                | ACTION_PLAY_PAUSE
                | ACTION_STOP
                | ACTION_SEEK_TO
                | ACTION_SKIP_TO_NEXT
                | ACTION_SKIP_TO_PREVIOUS
                | ACTION_SKIP_TO_QUEUE_ITEM;
        mErrorMessage = "";
        mUpdateTime = 0;
        mQueuePos = -1;
    }

    public PlaybackState getState() {
        return new PlaybackState.Builder()
                .setActions(mActions)
                .setState(mState, mPosition, mSpeed, mUpdateTime)
                .setErrorMessage(mErrorMessage)
                .setBufferedPosition(mBufferedPosition)
                .setActiveQueueItemId(mQueuePos)
                //.setExtras(mExtras)
                .build();
    }

    public void updatePosition(long position) {
        mPosition = position;
        //mBufferedPosition = position;
        mUpdateTime = SystemClock.elapsedRealtime();
    }

    public long getPosition() {
        return mPosition;
    }

    public void updateDuration(long duration) {
        //mExtras.putLong(PlaybackConstants.EXTRA.DURATION, duration);
        mBufferedPosition = duration;
    }

    public void updateQueuePos(int pos) {
        mQueuePos = pos;
    }

    public void gotoStopped() {
        mState = STATE_STOPPED;
    }

    public boolean isStopped() {
        return mState == STATE_STOPPED;
    }

    public void gotoPaused() {
        mState = STATE_PAUSED;
    }

    public boolean isPaused() {
        return mState == STATE_PAUSED;
    }

    public void gotoPlaying() {
        mState = STATE_PLAYING;
    }

    public boolean isPlaying() {
        return mState == STATE_PLAYING;
    }

    public void gotoFastForwarding() {
        mState = STATE_FAST_FORWARDING;
    }

    public boolean isFastForwarding() {
        return mState == STATE_FAST_FORWARDING;
    }

    public void gotoRewinding() {
        mState = STATE_REWINDING;
    }

    public boolean isRewinding() {
        return mState == STATE_REWINDING;
    }

    public void gotoBuffering() {
        mState = STATE_BUFFERING;
    }

    public boolean isBuffering() {
        return mState == STATE_BUFFERING;
    }

    public void gotoError(CharSequence msg) {
        mState = STATE_ERROR;
        mErrorMessage = msg != null ? msg : "";
    }

    public boolean isError() {
        return mState == STATE_ERROR;
    }

    public CharSequence getError() {
        return mErrorMessage;
    }

    public void gotoConnecting() {
        mState = STATE_CONNECTING;
    }

    public boolean isConnecting() {
        return mState == STATE_CONNECTING;
    }

    public void gotoSkippingPrevious() {
        mState = STATE_SKIPPING_TO_PREVIOUS;
    }

    public boolean isSkippingPrevious() {
        return mState == STATE_SKIPPING_TO_PREVIOUS;
    }

    public void gotoSkippingNext() {
        mState = STATE_SKIPPING_TO_NEXT;
    }

    public boolean isSkippingNext() {
        return mState == STATE_SKIPPING_TO_NEXT;
    }

    public boolean isActive() {
        switch (mState) {
            case STATE_FAST_FORWARDING:
            case STATE_REWINDING:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_NEXT:
            case STATE_BUFFERING:
            case STATE_CONNECTING:
            case STATE_PLAYING:
                return true;
            default:
                return false;
        }
    }


    public boolean isLoading() {
        switch (mState) {
            case STATE_CONNECTING:
            case STATE_BUFFERING:
                return true;
            default:
                return false;
        }
    }

    public boolean isLoadingOrSkipping() {
        switch (mState) {
            case STATE_SKIPPING_TO_NEXT:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_QUEUE_ITEM:
            case STATE_CONNECTING:
            case STATE_BUFFERING:
                return true;
            default:
                return false;
        }
    }

    public boolean shouldShowPauseButton() {
        switch (mState) {
            case STATE_FAST_FORWARDING:
            case STATE_REWINDING:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_NEXT:
            case STATE_PLAYING:
                return true;
            default:
                return false;
        }
    }


    public static boolean isStopped(PlaybackState state) {
        return state.getState() == STATE_STOPPED;
    }


    public static boolean isPaused(PlaybackState state) {
        return state.getState() == STATE_PAUSED;
    }


    public static boolean isPlaying(PlaybackState state) {
        return state.getState() == STATE_PLAYING;
    }

    public static boolean isFastForwarding(PlaybackState state) {
        return state.getState() == STATE_FAST_FORWARDING;
    }

    public static boolean isRewinding(PlaybackState state) {
        return state.getState() == STATE_REWINDING;
    }

    public static boolean isBuffering(PlaybackState state) {
        return state.getState() == STATE_BUFFERING;
    }

    public static boolean isError(PlaybackState state) {
        return state.getState() == STATE_ERROR;
    }

    public static boolean isConnecting(PlaybackState state) {
        return state.getState() == STATE_CONNECTING;
    }

    public static boolean isSkippingPrevious(PlaybackState state) {
        return state.getState() == STATE_SKIPPING_TO_PREVIOUS;
    }

    public static boolean isSkippingNext(PlaybackState state) {
        return state.getState() == STATE_SKIPPING_TO_NEXT;
    }

}
