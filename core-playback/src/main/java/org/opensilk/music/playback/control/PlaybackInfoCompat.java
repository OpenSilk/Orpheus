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

package org.opensilk.music.playback.control;

/**
 * Created by drew on 11/1/15.
 */
public class PlaybackInfoCompat {
    /**
     * The session uses remote playback.
     */
    public static final int PLAYBACK_TYPE_REMOTE = 2;
    /**
     * The session uses local playback.
     */
    public static final int PLAYBACK_TYPE_LOCAL = 1;

    private final int mVolumeType;
    private final int mVolumeControl;
    private final int mMaxVolume;
    private final int mCurrentVolume;

    public PlaybackInfoCompat(int type, int control, int max, int current) {
        mVolumeType = type;
        mVolumeControl = control;
        mMaxVolume = max;
        mCurrentVolume = current;
    }

    /**
     * Get the type of playback which affects volume handling. One of:
     * <ul>
     * <li>{@link #PLAYBACK_TYPE_LOCAL}</li>
     * <li>{@link #PLAYBACK_TYPE_REMOTE}</li>
     * </ul>
     *
     * @return The type of playback this session is using.
     */
    public int getPlaybackType() {
        return mVolumeType;
    }

    /**
     * Get the type of volume control that can be used. One of:
     * <ul>
     * <li>{@link android.media.VolumeProvider#VOLUME_CONTROL_ABSOLUTE}</li>
     * <li>{@link android.media.VolumeProvider#VOLUME_CONTROL_RELATIVE}</li>
     * <li>{@link android.media.VolumeProvider#VOLUME_CONTROL_FIXED}</li>
     * </ul>
     *
     * @return The type of volume control that may be used with this
     *         session.
     */
    public int getVolumeControl() {
        return mVolumeControl;
    }

    /**
     * Get the maximum volume that may be set for this session.
     *
     * @return The maximum allowed volume where this session is playing.
     */
    public int getMaxVolume() {
        return mMaxVolume;
    }

    /**
     * Get the current volume for this session.
     *
     * @return The current volume where this session is playing.
     */
    public int getCurrentVolume() {
        return mCurrentVolume;
    }
}
