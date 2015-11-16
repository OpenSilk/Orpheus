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

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;

import org.opensilk.music.playback.service.PlaybackServiceL;
import org.opensilk.music.playback.session.IMediaControllerProxy;
import org.opensilk.music.playback.session.MediaControllerProxyImplL;
import org.opensilk.music.playback.session.TransportControlsProxyImplL;

import timber.log.Timber;

/**
 * Created by drew on 10/18/15.
 */
@TargetApi(21)
class PlaybackControllerL implements PlaybackControllerImpl.IMPL {

    final Context mAppContext;
    final PlaybackControllerImpl mController;
    final MediaBrowser mMediaBrowser;

    boolean mWaitingForService = false;
    IMediaControllerProxy mMediaController;
    IMediaControllerProxy.TransportControlsProxy mTransportControls;

    public PlaybackControllerL(Context mAppContext, PlaybackControllerImpl controller) {
        this.mAppContext = mAppContext;
        this.mController = controller;
        this.mMediaBrowser = new MediaBrowser(
                this.mAppContext, new ComponentName(this.mAppContext, PlaybackServiceL.class),
                mConnectionCallback, null);
    }

    public boolean isConnected() {
        if (mMediaController == null) {
            connect();
            return false;
        } else {
            return true;
        }
    }

    public void connect() {
        if (mMediaBrowser.isConnected() || mWaitingForService) {
            return;
        }
        mWaitingForService = true;
        mMediaBrowser.connect();
    }

    public void disconnect() {
        mMediaBrowser.disconnect();
        onDisconnect();
    }

    void onDisconnect() {
        mMediaController = null;
        mTransportControls = null;
        mWaitingForService = false;
    }

    @Override
    public IMediaControllerProxy.TransportControlsProxy getTransportControls() {
        return mTransportControls;
    }

    @Override
    public IMediaControllerProxy getMediaController() {
        return mMediaController;
    }

    @Override
    public PlaybackInfoCompat getPlaybackInfo(Object info) {
        MediaController.PlaybackInfo playbackInfo = (MediaController.PlaybackInfo) info;
        return new PlaybackInfoCompat(playbackInfo.getPlaybackType(), playbackInfo.getVolumeControl(),
                playbackInfo.getMaxVolume(), playbackInfo.getCurrentVolume());
    }

    final MediaBrowser.ConnectionCallback mConnectionCallback = new MediaBrowser.ConnectionCallback() {
        @Override
        public void onConnected() {
            try {
                MediaController mediaController = new MediaController(mAppContext, mMediaBrowser.getSessionToken());
                mMediaController = new MediaControllerProxyImplL(mediaController);
                mTransportControls = new TransportControlsProxyImplL(mediaController.getTransportControls());
                mWaitingForService = false;
                mController.onConnected();
            } catch (IllegalArgumentException e) {
                Timber.e(e, "onConnected()");
                onDisconnect();
            }
        }

        @Override
        public void onConnectionSuspended() {
            onDisconnect();
        }

        @Override
        public void onConnectionFailed() {
            onDisconnect();
            Timber.e(new IllegalStateException("Shouldn't be here"), "onConnectionFailed()");
        }
    };

}
