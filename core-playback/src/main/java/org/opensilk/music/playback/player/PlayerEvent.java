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

/**
 * Created by drew on 4/25/15.
 */
public class PlayerEvent {
    public static final int WENT_TO_NEXT = 1;
    public static final int OPEN_NEXT_FAILED = 2;

    public static final int POSITION = 3;
    public static final int DURATION = 4;

    private final int mEvent;
    private final long mLongExtra;

    public PlayerEvent(int mEvent) {
        this(mEvent, -1);
    }

    public PlayerEvent(int mEvent, long mLongExtra) {
        this.mEvent = mEvent;
        this.mLongExtra = mLongExtra;
    }

    public int getEvent() {
        return mEvent;
    }

    public long getLongExtra() {
        return mLongExtra;
    }

    @Override
    public String toString() {
        return "{e="+stringifyEvent()+", ex="+mLongExtra+"}";
    }

    private String stringifyEvent() {
        switch (mEvent) {
            case WENT_TO_NEXT:
                return "WENT_TO_NEXT";
            case OPEN_NEXT_FAILED:
                return "OPEN_NEXT_FAILED";
            case POSITION:
                return "POSITION";
            case DURATION:
                return "DURATION";
            default:
                return "";
        }
    }

    public static PlayerEvent wentToNext() {
        return new PlayerEvent(WENT_TO_NEXT);
    }

    public static PlayerEvent openNextFailed() {
        return new PlayerEvent(OPEN_NEXT_FAILED);
    }

    public static PlayerEvent position(long pos) {
        return new PlayerEvent(POSITION, pos);
    }

    public static PlayerEvent duration(long dur) {
        return new PlayerEvent(DURATION, dur);
    }
}
