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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.playback.session.IMediaControllerProxy;
import org.opensilk.music.playback.session.IMediaSessionProxy;

import java.util.List;

import javax.inject.Inject;

/**
 * Created by drew on 9/25/15.
 */
@PlaybackServiceScope
public class MediaSessionHolder {

    private final IMediaSessionProxy mProxy;

    @Inject
    public MediaSessionHolder(@ForApplication Context context) {
        if (VersionUtils.hasLollipop()) {
            mProxy = new MediaSessionHolderL(context);
        } else {
            mProxy = new MediaSessionHolderK(context);
        }
    }

    public void setCallback(IMediaSessionProxy.Callback cb, Handler handler) {
        mProxy.setCallback(cb, handler);
    }

    public void sendSessionEvent(String event, Bundle args) {
        mProxy.sendSessionEvent(event, args);
    }

    public void setQueue(List<MediaSessionCompat.QueueItem> list) {
        mProxy.setQueue(list);
    }

    public void setActive(boolean active) {
        mProxy.setActive(active);
    }

    public void dispatchMediaButtonEvent(KeyEvent event) {
        getController().dispatchMediaButtonEvent(event);
    }

    public MediaMetadataCompat getMetadata() {
        return getController().getMetadata();
    }

    public void setMetadata(MediaMetadataCompat metadata) {
        mProxy.setMetadata(metadata);
    }

    public PlaybackStateCompat getPlaybackState() {
        return getController().getPlaybackState();
    }

    public void setPlaybackState(PlaybackStateCompat state) {
        mProxy.setPlaybackState(state);
    }

    public Object getSessionToken() {
        return mProxy.getSessionToken();
    }

    public IMediaControllerProxy getController() {
        return mProxy.getController();
    }

    public IMediaControllerProxy.TransportControlsProxy getTransportControls() {
        return mProxy.getTransportControls();
    }

    public void release() {
        mProxy.release();
    }

    public void setPlaybackToLocal() {
        mProxy.setPlaybackToLocal();
    }

    public void setPlaybackToRemote(VolumeProviderCompat volumeProviderCompat) {
        mProxy.setPlaybackToRemote(volumeProviderCompat);
    }

}
