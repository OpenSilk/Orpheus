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

import android.annotation.TargetApi;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 10/18/15.
 */
@TargetApi(21)
public class MediaControllerProxyImplL implements IMediaControllerProxy {
    private final MediaController mController;
    private final Set<MediaControllerCallback> mCallbacks = new LinkedHashSet<>();

    public MediaControllerProxyImplL(MediaController mController) {
        this.mController = mController;
    }

    @Override
    public void sendCommand(@NonNull String command, @Nullable Bundle args, @Nullable ResultReceiver cb) {
        mController.sendCommand(command, args, cb);
    }

    @Override
    public PlaybackStateCompat getPlaybackState() {
        return PlaybackStateCompat.fromPlaybackState(mController.getPlaybackState());
    }

    @Override
    public MediaMetadataCompat getMetadata() {
        return MediaMetadataCompat.fromMediaMetadata(mController.getMetadata());
    }

    @Override
    public List<MediaSessionCompat.QueueItem> getQueue() {
        List<MediaSession.QueueItem> queue = mController.getQueue();
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        List<MediaSessionCompat.QueueItem> list = new ArrayList<>(queue.size());
        for (MediaSession.QueueItem item : queue) {
            MediaSessionCompat.QueueItem qi = MediaSessionCompat.QueueItem.obtain(item);
            list.add(qi);
        }
        return list;
    }

    @Override
    public void dispatchMediaButtonEvent(KeyEvent event) {
        mController.dispatchMediaButtonEvent(event);
    }

    @Override
    public void registerCallback(Callback cb, Handler handler) {
        MediaControllerCallback callback = new MediaControllerCallback(cb);
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
        mController.registerCallback(callback, handler);
    }

    @Override
    public void unregisterCallback(Callback cb) {
        MediaControllerCallback callback = null;
        synchronized (mCallbacks) {
            Iterator<MediaControllerCallback> ii = mCallbacks.iterator();
            while (ii.hasNext()) {
                MediaControllerCallback c = ii.next();
                if (c.mCallback == cb) {
                    callback = c;
                    ii.remove();
                    break;
                }
            }
        }
        if (callback != null) {
            mController.unregisterCallback(callback);
        }
    }

    @Override
    public MediaController.PlaybackInfo getPlaybackInfo() {
        return mController.getPlaybackInfo();
    }

    @Override
    public void setVolumeTo(int volume, int flags) {
        mController.setVolumeTo(volume, flags);
    }

    static class MediaControllerCallback extends MediaController.Callback {

        final IMediaControllerProxy.Callback mCallback;

        public MediaControllerCallback(Callback mCallback) {
            this.mCallback = mCallback;
        }

        @Override
        public void onSessionDestroyed() {
            mCallback.onSessionDestroyed();
        }

        @Override
        @DebugLog
        public void onSessionEvent(@NonNull String event, Bundle extras) {
            mCallback.onSessionEvent(event, extras);
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            mCallback.onPlaybackStateChanged(PlaybackStateCompat.fromPlaybackState(state));
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mCallback.onMetadataChanged(MediaMetadataCompat.fromMediaMetadata(metadata));
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            if (queue == null || queue.isEmpty()) {
                mCallback.onQueueChanged(null);
            } else {
                List<MediaSessionCompat.QueueItem> list = new ArrayList<>(queue.size());
                for (MediaSession.QueueItem item : queue) {
                    MediaSessionCompat.QueueItem qi = MediaSessionCompat.QueueItem.obtain(item);
                    list.add(qi);
                }
                mCallback.onQueueChanged(list);
            }
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            mCallback.onQueueTitleChanged(title);
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            mCallback.onExtrasChanged(extras);
        }

        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
            mCallback.onAudioInfoChanged(info);
        }
    }
}
