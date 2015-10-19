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
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;

/**
 * Created by drew on 10/18/15.
 */
public class TransportControlsProxyImplK implements IMediaControllerProxy.TransportControlsProxy {
    final MediaControllerCompat.TransportControls mControls;

    public TransportControlsProxyImplK(MediaControllerCompat.TransportControls mControls) {
        this.mControls = mControls;
    }

    @Override
    public void play() {
        mControls.play();
    }

    @Override
    public void playFromMediaId(String mediaId, Bundle extras) {
        mControls.playFromMediaId(mediaId, extras);
    }

    @Override
    public void playFromSearch(String query, Bundle extras) {
        mControls.playFromSearch(query, extras);
    }

    @Override
    public void skipToQueueItem(long id) {
        mControls.skipToQueueItem(id);
    }

    @Override
    public void pause() {
        mControls.pause();
    }

    @Override
    public void stop() {
        mControls.stop();
    }

    @Override
    public void seekTo(long pos) {
        mControls.seekTo(pos);
    }

    @Override
    public void fastForward() {
        mControls.fastForward();
    }

    @Override
    public void skipToNext() {
        mControls.skipToNext();
    }

    @Override
    public void rewind() {
        mControls.rewind();
    }

    @Override
    public void skipToPrevious() {
        mControls.skipToPrevious();
    }

    @Override
    public void setRating(RatingCompat rating) {
        mControls.setRating(rating);
    }

    @Override
    public void sendCustomAction(String action, Bundle args) {
        mControls.sendCustomAction(action, args);
    }
}
