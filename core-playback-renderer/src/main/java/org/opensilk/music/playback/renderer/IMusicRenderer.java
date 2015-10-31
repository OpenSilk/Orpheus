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

package org.opensilk.music.playback.renderer;

import android.os.Bundle;
import android.support.v4.media.VolumeProviderCompat;

/**
 * Created by drew on 10/28/15.
 */
public interface IMusicRenderer {
    void start();
    void stop(boolean notifyListeners);
    void setState(int state);
    int getState();
    boolean isPlaying();
    long getCurrentStreamPosition();
    long getDuration();
    void prepareForTrack();
    boolean loadTrack(Bundle trackBundle);
    void prepareForNextTrack();
    boolean loadNextTrack(Bundle trackBundle);
    void play();
    boolean hasCurrent();
    boolean hasNext();
    void goToNext();
    void pause();
    void seekTo(long position);
    boolean isRemotePlayback();
    VolumeProviderCompat getVolumeProvider();
    void setCallback(Callback callback);
    void setAccessor(PlaybackServiceAccessor accessor);
    interface Callback {
        /**
         * on Playback status changed
         * Implementations can use this callback to update
         * playback state on the media sessions.
         */
        void onPlaybackStatusChanged(int state);

        /**
         * player has started playing track loaded with setNextTrack
         */
        void onWentToNext();

        /**
         * On current music completed.
         */
        void onCompletion();

        /**
         * @param error to be added to the PlaybackState
         */
        void onError(String error);

        /**
         * Invoked when the audio session id becomes known
         */
        void onAudioSessionId(int audioSessionId);
    }
}
