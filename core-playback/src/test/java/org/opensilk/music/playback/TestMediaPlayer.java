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
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import org.opensilk.music.playback.renderer.Headers;
import org.opensilk.music.playback.renderer.IMediaPlayer;
import org.opensilk.music.playback.renderer.IMediaPlayerCallback;
import org.opensilk.music.playback.renderer.IMediaPlayerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Created by drew on 9/30/15.
 */
public class TestMediaPlayer extends IMediaPlayer.Stub {

    public static class Factory extends IMediaPlayerFactory.Stub {
        @Override
        public IMediaPlayer create() {
            return new TestMediaPlayer();
        }
    }

    final Handler myHandler;

    public TestMediaPlayer() {
        myHandler = new Handler(Looper.myLooper());
    }

    IMediaPlayerCallback mCallback;
    boolean isPlaying;

    @Override
    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    public long getCurrentPosition() {
        return 0;
    }

    @Override
    public boolean setDataSource(Uri uri, Headers headers) {
        return false;
    }

    @Override
    public void prepareAsync() {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.onPrepared(TestMediaPlayer.this);
                } catch (RemoteException e) {}
            }
        });
    }

    @Override
    public void pause() {
        isPlaying = false;
    }

    @Override
    public void seekTo(long pos) {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.onSeekComplete(TestMediaPlayer.this);
                } catch (RemoteException e) {}
            }
        });
    }

    @Override
    public void setVolume(float left, float right) {

    }

    @Override
    public void start() {
        isPlaying = true;
    }

    @Override
    public void setCallback(IMediaPlayerCallback callback) {
        mCallback = callback;
    }

    @Override
    public void reset() {
        isPlaying = false;
    }

    @Override
    public void release() {

    }

    @Override
    public long getDuration() {
        return 0;
    }
}
