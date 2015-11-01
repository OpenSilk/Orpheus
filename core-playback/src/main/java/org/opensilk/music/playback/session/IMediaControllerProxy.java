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

package org.opensilk.music.playback.session;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import java.util.List;

/**
 * Created by drew on 10/18/15.
 */
public interface IMediaControllerProxy {
    void sendCommand(@NonNull String command, @Nullable Bundle args,
                     @Nullable ResultReceiver cb);
    PlaybackStateCompat getPlaybackState();
    MediaMetadataCompat getMetadata();
    List<MediaSessionCompat.QueueItem> getQueue();
    void dispatchMediaButtonEvent(KeyEvent event);
    void registerCallback(Callback cb, Handler handler);
    void unregisterCallback(Callback cb);
    Object getPlaybackInfo();
    void setVolumeTo(int volume, int flags);
    interface TransportControlsProxy {
        void play();
        void playFromMediaId(String mediaId, Bundle extras);
        void playFromSearch(String query, Bundle extras);
        void skipToQueueItem(long id);
        void pause();
        void stop();
        void seekTo(long pos);
        void fastForward();
        void skipToNext();
        void rewind();
        void skipToPrevious();
        void setRating(RatingCompat rating);
        void sendCustomAction(String action, Bundle args);
    }
    interface Callback {
        void onSessionDestroyed();
        void onSessionEvent(@NonNull String event, Bundle extras);
        void onPlaybackStateChanged(@NonNull PlaybackStateCompat state);
        void onMetadataChanged(MediaMetadataCompat metadata);
        void onQueueChanged(List<MediaSessionCompat.QueueItem> queue);
        void onQueueTitleChanged(CharSequence title);
        void onExtrasChanged(Bundle extras);
        void onAudioInfoChanged(Object info);
    }
}
