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

package org.opensilk.music.playback.player;

import android.media.session.PlaybackState;

/**
 * Created by drew on 4/24/15.
 */
public class PlayerStatus {
    public static final int NONE = 0;
    public static final int LOADING = 1;
    public static final int READY = 2;
    public static final int PLAYING = 3;
    public static final int PAUSED = 4;
    public static final int STOPPED = 5;
    public static final int ERROR = 6;

    private final int mState;
    private final String mErrorMsg;

    public PlayerStatus(int mState) {
        this(mState, null);
    }

    public PlayerStatus(int mState, String mErrorMsg) {
        this.mState = mState;
        this.mErrorMsg = mErrorMsg;
    }

    public int getState() {
        return mState;
    }

    public String getErrorMsg() {
        return mErrorMsg;
    }

    @Override
    public String toString() {
        return "{"+stringifyState()+", err="+mErrorMsg+"}";
    }

    private String stringifyState() {
        switch (mState) {
            case NONE:
                return "NONE";
            case LOADING:
                return "LOADING";
            case READY:
                return "READY";
            case PLAYING:
                return "PLAYING";
            case PAUSED:
                return "PAUSED";
            case STOPPED:
                return "STOPPED";
            case ERROR:
                return "ERROR";
            default:
                return "";
        }
    }

    public static PlayerStatus none() {
        return new PlayerStatus(NONE);
    }

    public static PlayerStatus loading() {
        return new PlayerStatus(LOADING);
    }

    public static PlayerStatus ready() {
        return new PlayerStatus(READY);
    }

    public static PlayerStatus playing() {
        return new PlayerStatus(PLAYING);
    }

    public static PlayerStatus paused() {
        return new PlayerStatus(PAUSED);
    }

    public static PlayerStatus stopped() {
        return new PlayerStatus(STOPPED);
    }

    public static PlayerStatus error(String msg) {
        return new PlayerStatus(ERROR, msg);
    }
}
