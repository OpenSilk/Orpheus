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

package org.opensilk.music.renderer.googlecast;

import android.os.Binder;
import android.os.Bundle;
import android.support.v4.media.VolumeProviderCompat;

import org.opensilk.music.playback.renderer.IMusicRenderer;
import org.opensilk.music.playback.renderer.PlaybackServiceAccessor;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

/**
 * Created by drew on 10/29/15.
 */
public class CastRendererServiceBinder extends Binder implements IMusicRenderer {

    private final WeakReference<CastRendererService> mService;

    @Inject
    public CastRendererServiceBinder(CastRendererService mService) {
        this.mService = new WeakReference<CastRendererService>(mService);
    }

    @Override
    public void start() {
        mService.get().start();
    }

    @Override
    public void stop(boolean notifyListeners) {
        mService.get().stop(notifyListeners);
    }

    @Override
    public void setState(int state) {
        mService.get().setState(state);
    }

    @Override
    public int getState() {
        return mService.get().getState();
    }

    @Override
    public boolean isPlaying() {
        return mService.get().isPlaying();
    }

    @Override
    public long getCurrentStreamPosition() {
        return mService.get().getCurrentStreamPosition();
    }

    @Override
    public long getDuration() {
        return mService.get().getDuration();
    }

    @Override
    public void prepareForTrack() {
        mService.get().prepareForTrack();
    }

    @Override
    public boolean loadTrack(Bundle trackBundle) {
        return mService.get().loadTrack(trackBundle);
    }

    @Override
    public void prepareForNextTrack() {
        mService.get().prepareForNextTrack();
    }

    @Override
    public boolean loadNextTrack(Bundle trackBundle) {
        return mService.get().loadNextTrack(trackBundle);
    }

    @Override
    public void play() {
        mService.get().play();
    }

    @Override
    public boolean hasCurrent() {
        return mService.get().hasCurrent();
    }

    @Override
    public boolean hasNext() {
        return mService.get().hasNext();
    }

    @Override
    public boolean goToNext() {
        return mService.get().goToNext();
    }

    @Override
    public void pause() {
        mService.get().pause();
    }

    @Override
    public void seekTo(long position) {
        mService.get().seekTo(position);
    }

    @Override
    public boolean isRemotePlayback() {
        return mService.get().isRemotePlayback();
    }

    @Override
    public VolumeProviderCompat getVolumeProvider() {
        return mService.get().getVolumeProvider();
    }

    @Override
    public void setCallback(Callback callback) {
        mService.get().setCallback(callback);
    }

    @Override
    public void setAccessor(PlaybackServiceAccessor accessor) {
        mService.get().setAccessor(accessor);
    }
}
