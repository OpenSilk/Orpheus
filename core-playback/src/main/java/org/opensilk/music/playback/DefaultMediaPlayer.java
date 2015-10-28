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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.playback.renderer.Headers;
import org.opensilk.music.playback.renderer.IMediaPlayer;
import org.opensilk.music.playback.renderer.IMediaPlayerCallback;
import org.opensilk.music.playback.renderer.IMediaPlayerFactory;

import java.io.IOException;

import javax.inject.Inject;

/**
 * Created by drew on 9/27/15.
 */
public class DefaultMediaPlayer implements IMediaPlayer, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener {

    public static class Factory implements IMediaPlayerFactory {
        final Context context;

        @Inject
        public Factory(@ForApplication Context context) {
            this.context = context;
        }
        @Override
        public IMediaPlayer create() {
            return new DefaultMediaPlayer(context);
        }

        @Override
        public IBinder asBinder() {
            throw new UnsupportedOperationException("Local process only");
        }
    }

    private MediaPlayer mMediaPlayer;
    private IMediaPlayerCallback mCallback;
    private int mErrorCount;

    private final Context mContext;

    public DefaultMediaPlayer(Context mContext) {
        this.mContext = mContext;
    }

    void ensurePlayer() {
        if (mMediaPlayer != null) {
            reset();
            release();
        }
        mErrorCount = 0;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        if (mCallback != null) {
            try {
                mCallback.onAudioSessionId(this, mMediaPlayer.getAudioSessionId());
            } catch (RemoteException ignored) {}
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
    public boolean setDataSource(Uri uri, Headers headers) {
        ensurePlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        try {
            mMediaPlayer.setDataSource(mContext, uri, headers);
            return true;
        } catch (IOException e) {
            return false;
        }
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
    public void setCallback(IMediaPlayerCallback callback) {
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

    @Override
    public IBinder asBinder() {
        throw new UnsupportedOperationException("Local process only");
    }

    boolean hasCallback() {
        return mCallback != null;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (hasCallback()) {
            try {
                mCallback.onCompletion(this);
            } catch (RemoteException ignored){}
        }
    }

    /**
     * Called to indicate an error.
     *
     * @param mp      the MediaPlayer the error pertains to
     * @param what    the type of error that has occurred:
     * <ul>
     * <li>{@link #MEDIA_ERROR_UNKNOWN}
     * <li>{@link #MEDIA_ERROR_SERVER_DIED}
     * </ul>
     * @param extra an extra code, specific to the error. Typically
     * implementation dependent.
     * <ul>
     * <li>{@link #MEDIA_ERROR_IO}
     * <li>{@link #MEDIA_ERROR_MALFORMED}
     * <li>{@link #MEDIA_ERROR_UNSUPPORTED}
     * <li>{@link #MEDIA_ERROR_TIMED_OUT}
     * <li><code>MEDIA_ERROR_SYSTEM (-2147483648)</code> - low-level system error.
     * </ul>
     * @return True if the method handled the error, false if it didn't.
     * Returning false, or not having an OnErrorListener at all, will
     * cause the OnCompletionListener to be called.
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        String msg;
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                msg = "MEDIA_ERROR_UNKNOWN";
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                msg = "MEDIA_ERROR_SERVER_DIED";
                break;
            default:
                msg = "Non standard error code " + what;
        }
        if (hasCallback()) {
            try {
                mCallback.onError(this, msg, extra);
            } catch (RemoteException ignored) {}
        }
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (hasCallback()) {
            try {
                mCallback.onPrepared(this);
            } catch (RemoteException ignored) {}
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (hasCallback()) {
            try {
                mCallback.onSeekComplete(this);
            } catch (RemoteException ignored) {}
        }
    }
}
