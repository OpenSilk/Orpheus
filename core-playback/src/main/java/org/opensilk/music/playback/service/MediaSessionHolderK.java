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

package org.opensilk.music.playback.service;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.opensilk.music.playback.NavUtils;
import org.opensilk.music.playback.session.IMediaControllerProxy;
import org.opensilk.music.playback.session.IMediaSessionProxy;
import org.opensilk.music.playback.session.MediaControllerProxyImplK;
import org.opensilk.music.playback.session.TransportControlsProxyImplK;

import java.util.List;

/**
 * Created by drew on 10/18/15.
 */
class MediaSessionHolderK implements IMediaSessionProxy {

    private final MediaSessionCompat mSession;
    private final IMediaControllerProxy mControllerProxy;
    private final IMediaControllerProxy.TransportControlsProxy mTransportControlsProxy;

    public MediaSessionHolderK(Context context) {
        final ComponentName mediaButtonReceiverComponent
                = new ComponentName(context, MediaButtonIntentReceiver.class);
        final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(mediaButtonReceiverComponent);
        final PendingIntent mediaButtonReceiverIntent = PendingIntent.getBroadcast(
                context, 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        this.mSession = new MediaSessionCompat(
                context,
                context.getPackageName() + "/" + PlaybackService.NAME,
                mediaButtonReceiverComponent,
                mediaButtonReceiverIntent
        );
        configureSession(context);
        mControllerProxy = new MediaControllerProxyImplK(mSession.getController());
        mTransportControlsProxy = new TransportControlsProxyImplK(
                mSession.getController().getTransportControls());
    }

    private void configureSession(Context context) {
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setSessionActivity(NavUtils.makePlayerIntent(context, 2));
    }

    public MediaSessionCompat.Token getSessionToken() {
        return mSession.getSessionToken();
    }

    public void release() {
        mSession.release();
    }

    @Override
    public void sendSessionEvent(String event, Bundle args) {
        mSession.sendSessionEvent(event, args);
    }

    @Override
    public void setQueue(List<MediaSessionCompat.QueueItem> list) {
        mSession.setQueue(list);
    }

    @Override
    public void setActive(boolean active) {
        mSession.setActive(active);
    }

    @Override
    public void setMetadata(MediaMetadataCompat metadata) {
        mSession.setMetadata(metadata);
    }

    @Override
    public void setPlaybackState(PlaybackStateCompat state) {
        mSession.setPlaybackState(state);
    }

    @Override
    public void setCallback(Callback cb, Handler handler) {
        mSession.setCallback(new MediaSessionCallback(cb), handler);
    }

    @Override
    public IMediaControllerProxy getController() {
        return mControllerProxy;
    }

    @Override
    public IMediaControllerProxy.TransportControlsProxy getTransportControls() {
        return mTransportControlsProxy;
    }

    @Override
    public void setPlaybackToLocal() {
        mSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void setPlaybackToRemote(VolumeProviderCompat volumeProviderCompat) {
        mSession.setPlaybackToRemote(volumeProviderCompat);
    }

    static class MediaSessionCallback extends MediaSessionCompat.Callback {

        final IMediaSessionProxy.Callback mCallback;

        public MediaSessionCallback(Callback mCallback) {
            this.mCallback = mCallback;
        }

        @Override
        public void onCommand(String command, Bundle args, ResultReceiver cb) {
            mCallback.onCommand(command, args, cb);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            return super.onMediaButtonEvent(mediaButtonIntent);
        }

        @Override
        public void onPlay() {
            mCallback.onPlay();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            mCallback.onPlayFromMediaId(mediaId, extras);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
        }

        @Override
        public void onSkipToQueueItem(long id) {
            mCallback.onSkipToQueueItem(id);
        }

        @Override
        public void onPause() {
            mCallback.onPause();
        }

        @Override
        public void onSkipToNext() {
            mCallback.onSkipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            mCallback.onSkipToPrevious();
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
        }

        @Override
        public void onRewind() {
            super.onRewind();
        }

        @Override
        public void onStop() {
            mCallback.onStop();
        }

        @Override
        public void onSeekTo(long pos) {
            mCallback.onSeekTo(pos);
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            mCallback.onSetRating(rating);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            mCallback.onCustomAction(action, extras);
        }
    }
}
