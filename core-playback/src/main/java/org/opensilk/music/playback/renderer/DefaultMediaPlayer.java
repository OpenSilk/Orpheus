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

package org.opensilk.music.playback.renderer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;

import org.opensilk.music.playback.service.PlaybackServiceScope;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

/**
 * Created by drew on 9/27/15.
 */
public class DefaultMediaPlayer implements IMediaPlayer, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener {

    private MediaPlayer mMediaPlayer;
    private Callback mCallback;

    @PlaybackServiceScope
    public static class Factory implements IMediaPlayer.Factory {
        @Inject
        public Factory() {
        }

        @Override
        public IMediaPlayer create(Context context) {
            return new DefaultMediaPlayer();
        }
    }

    void ensurePlayer() {
        if (mMediaPlayer != null) {
            reset();
            release();
        }
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        if (mCallback != null) {
            mCallback.onAudioSessionId(this, mMediaPlayer.getAudioSessionId());
        }
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException {
        ensurePlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setDataSource(context, uri, headers);
    }

    @Override
    public void prepareAsync() {
        mMediaPlayer.prepareAsync();
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public void seekTo(long pos) {
        mMediaPlayer.seekTo((int)pos);
    }

    @Override
    public void setVolume(float left, float right) {
        mMediaPlayer.setVolume(left, right);
    }

    @Override
    public void start() {
        mMediaPlayer.start();
    }

    @Override
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void reset() {
        mMediaPlayer.reset();
    }

    @Override
    public void release() {
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    @Override
    public long getDuration() {
        return mMediaPlayer.getDuration();
    }

    boolean hasCallback() {
        return mCallback != null;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (hasCallback()) {
            mCallback.onCompletion(this);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return hasCallback() && mCallback.onError(this, what, extra);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (hasCallback()) {
            mCallback.onPrepared(this);
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (hasCallback()) {
            mCallback.onSeekComplete(this);
        }
    }
}
