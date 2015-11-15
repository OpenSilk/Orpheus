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
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.List;

/**
 * Created by drew on 10/18/15.
 */
public interface IMediaSessionProxy {
    Object getSessionToken();
    void release();
    void sendSessionEvent(String event, Bundle args);
    void setQueue(List<MediaSessionCompat.QueueItem> list);
    void setActive(boolean active);
    void setMetadata(MediaMetadataCompat metadata);
    void setPlaybackState(PlaybackStateCompat state);
    void setCallback(Callback cb, Handler handler);
    IMediaControllerProxy getController();
    IMediaControllerProxy.TransportControlsProxy getTransportControls();
    void setPlaybackToLocal();
    void setPlaybackToRemote(VolumeProviderCompat volumeProviderCompat);
    interface Callback {
        void onCommand(String command, Bundle args, ResultReceiver cb);
        void onPlay();
        void onPlayFromMediaId(String mediaId, Bundle extras);
        void onSkipToQueueItem(long id);
        void onPause();
        void onSkipToNext();
        void onSkipToPrevious();
        void onStop();
        void onSeekTo(long pos);
        void onSetRating(@NonNull RatingCompat rating);
        void onCustomAction(@NonNull String action, Bundle extras);
    }
}
