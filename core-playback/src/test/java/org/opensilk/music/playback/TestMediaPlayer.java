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

import org.opensilk.music.playback.renderer.IMediaPlayer;

import java.io.IOException;
import java.util.Map;

/**
 * Created by drew on 9/30/15.
 */
public class TestMediaPlayer implements IMediaPlayer {

    public static class Factory implements IMediaPlayer.Factory {
        @Override
        public IMediaPlayer create(Context context) {
            return new TestMediaPlayer();
        }
    }

    final Handler myHandler;

    public TestMediaPlayer() {
        myHandler = new Handler(Looper.myLooper());
    }

    Callback mCallback;
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
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException {

    }

    @Override
    public void prepareAsync() {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallback.onPrepared(TestMediaPlayer.this);
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
                mCallback.onSeekComplete(TestMediaPlayer.this);
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
    public void setCallback(Callback callback) {
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
