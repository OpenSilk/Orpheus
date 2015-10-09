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
import android.net.Uri;

import java.io.IOException;
import java.util.Map;

/**
 * Created by drew on 9/27/15.
 */
public interface IMediaPlayer {
    boolean isPlaying();
    long getCurrentPosition();
    void setDataSource(Context context, Uri uri, Map<String,String> headers) throws IOException;
    void prepareAsync();
    void pause();
    void seekTo(long pos);
    void setVolume(float left, float right);
    void start();
    void setCallback(Callback callback);
    void reset();
    void release();
    long getDuration();
    interface Callback {
        /**
         * Called when MediaPlayer has completed a seek
         *
         * @see android.media.MediaPlayer.OnSeekCompleteListener
         */
        void onSeekComplete(IMediaPlayer mp);

        /**
         * Called when media player is done playing current song.
         *
         * @see android.media.MediaPlayer.OnCompletionListener
         */
        void onCompletion(IMediaPlayer mp);

        /**
         * Called when media player is done preparing.
         *
         * @see android.media.MediaPlayer.OnPreparedListener
         */
        void onPrepared(IMediaPlayer mp);

        /**
         * Called when there's an error playing media. When this happens, the media
         * player goes to the Error state. We warn the user about the error and
         * reset the media player.
         *
         * @see android.media.MediaPlayer.OnErrorListener
         */
        boolean onError(IMediaPlayer mp, int what, int extra);

        /**
         * Invoked when the audio session id becomes known
         */
        void onAudioSessionId(IMediaPlayer mp, int audioSessionId);
    }
    interface Factory {
        IMediaPlayer create(Context context);
    }
}
