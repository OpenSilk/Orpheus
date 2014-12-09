
package com.andrew.apollo;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

import timber.log.Timber;

public class MultiPlayer implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener {

    private final WeakReference<MusicPlaybackService> mService;
    private CompatMediaPlayer mCurrentMediaPlayer = new CompatMediaPlayer();
    private CompatMediaPlayer mNextMediaPlayer;
    private Handler mHandler;
    private boolean mIsInitialized = false;

    public MultiPlayer(final MusicPlaybackService service) {
        mService = new WeakReference<>(service);
        mCurrentMediaPlayer.setWakeMode(service, PowerManager.PARTIAL_WAKE_LOCK);
    }

    /**
     * @param path The path of the file, or the http/rtsp URL of the stream
     *            you want to play
     */
    public void setDataSource(final String path) {
        mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
        if (mIsInitialized) {
            setNextDataSource(null);
        }
    }

    /**
     * @param player The {@link MediaPlayer} to use
     * @param path The path of the file, or the http/rtsp URL of the stream
     *            you want to play
     * @return True if the <code>player</code> has been prepared and is
     *         ready to play, false otherwise
     */
    private boolean setDataSourceImpl(final MediaPlayer player, final String path) {
        MusicPlaybackService service = mService.get();
        if (service == null) {
            return false;
        }
        try {
            player.reset();
            player.setOnPreparedListener(null);
            if (path.startsWith("content://")) {
                player.setDataSource(service, Uri.parse(path));
            } else {
                player.setDataSource(path);
            }
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.prepare();
        } catch (IOException|IllegalArgumentException|SecurityException|IllegalStateException e) {
            Timber.w(e, "setDataSourceImpl");
            return false;
        }
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, service.getPackageName());
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
        service.sendBroadcast(intent);
        return true;
    }

    /**
     * @param path The path of the file, or the http/rtsp URL of the stream
     *            you want to play
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setNextDataSource(final String path) {
        MusicPlaybackService service = mService.get();
        if (service == null) {
            return;
        }
        try {
            mCurrentMediaPlayer.setNextMediaPlayer(null);
        } catch (IllegalArgumentException e) {
            Timber.i("Next media player is current one, continuing");
        } catch (IllegalStateException e) {
            Timber.w(e, "Media player not initialized!");
            return;
        }
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.release();
            mNextMediaPlayer = null;
        }
        if (path == null) {
            return;
        }
        mNextMediaPlayer = new CompatMediaPlayer();
        mNextMediaPlayer.setWakeMode(service, PowerManager.PARTIAL_WAKE_LOCK);
        mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
        if (setDataSourceImpl(mNextMediaPlayer, path)) {
            try {
                mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            } catch (IllegalArgumentException|IllegalStateException e) {
                Timber.w(e, "setNextDataSource: setNextMediaPlayer()");
                if (mNextMediaPlayer != null) {
                    mNextMediaPlayer.release();
                    mNextMediaPlayer = null;
                }
            }
        } else {
            if (mNextMediaPlayer != null) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
        }
    }

    public CompatMediaPlayer getCurrentPlayer() {
        return mCurrentMediaPlayer;
    }

    /**
     * @param handler The handler to use
     */
    public void setHandler(final Handler handler) {
        mHandler = handler;
    }

    /**
     * @return True if the player is ready to go, false otherwise
     */
    public boolean isInitialized() {
        return mIsInitialized;
    }

    /**
     * Starts or resumes playback.
     */
    public boolean start() {
        try {
            mCurrentMediaPlayer.start();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Resets the MediaPlayer to its uninitialized state.
     */
    public void stop() {
        mCurrentMediaPlayer.reset();
        mIsInitialized = false;
    }

    /**
     * Releases resources associated with this MediaPlayer object.
     */
    public void release() {
        stop();
        mCurrentMediaPlayer.release();
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.release();
        }
    }

    /**
     * Pauses playback. Call start() to resume.
     */
    public boolean pause() {
        try {
            mCurrentMediaPlayer.pause();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * @return The duration in milliseconds
     */
    public long duration() {
        try {
            return mCurrentMediaPlayer.getDuration();
        } catch (IllegalStateException e) {
            return -1;
        }
    }

    /**
     * @return The current position in milliseconds
     */
    public long position() {
        try {
            return mCurrentMediaPlayer.getCurrentPosition();
        } catch (IllegalStateException e) {
            return -1;
        }
    }

    /**
     * @param whereto The offset in milliseconds from the start to seek to
     * @return The offset in milliseconds from the start to seek to
     */
    public long seek(final long whereto) {
        try {
            mCurrentMediaPlayer.seekTo((int)whereto);
            return whereto;
        } catch (IllegalStateException e) {
            return -1;
        }
    }

    public boolean setVolume(final float vol) {
        try {
            mCurrentMediaPlayer.setVolume(vol, vol);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public boolean setAudioSessionId(final int sessionId) {
        try {
            mCurrentMediaPlayer.setAudioSessionId(sessionId);
            return true;
        } catch (IllegalArgumentException|IllegalStateException e) {
            return false;
        }
    }

    public int getAudioSessionId() {
        return mCurrentMediaPlayer.getAudioSessionId();
    }

    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        MusicPlaybackService service = mService.get();
        if (service == null) {
            return false;
        }
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                mIsInitialized = false;
                mCurrentMediaPlayer.release();
                mCurrentMediaPlayer = new CompatMediaPlayer();
                mCurrentMediaPlayer.setWakeMode(service, PowerManager.PARTIAL_WAKE_LOCK);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MusicPlayerHandler.SERVER_DIED), 2000);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onCompletion(final MediaPlayer mp) {
        MusicPlaybackService service = mService.get();
        if (service == null) {
            return;
        }
        if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
            mCurrentMediaPlayer.release();
            mCurrentMediaPlayer = mNextMediaPlayer;
            mNextMediaPlayer = null;
            mHandler.sendEmptyMessage(MusicPlayerHandler.TRACK_WENT_TO_NEXT);
        } else {
            service.acquireWakeLock(30000);
            mHandler.sendEmptyMessage(MusicPlayerHandler.TRACK_ENDED);
            mHandler.sendEmptyMessage(MusicPlayerHandler.RELEASE_WAKELOCK);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    private static final class CompatMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener {

        private boolean mCompatMode = true;
        private MediaPlayer mNextPlayer;
        private OnCompletionListener mCompletion;

        public CompatMediaPlayer() {
            try {
                MediaPlayer.class.getMethod("setNextMediaPlayer", MediaPlayer.class);
                mCompatMode = false;
            } catch (NoSuchMethodException e) {
                mCompatMode = true;
                super.setOnCompletionListener(this);
            }
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public void setNextMediaPlayer(MediaPlayer next) {
            if (mCompatMode) {
                mNextPlayer = next;
            } else {
                super.setNextMediaPlayer(next);
            }
        }

        @Override
        public void setOnCompletionListener(OnCompletionListener listener) {
            if (mCompatMode) {
                mCompletion = listener;
            } else {
                super.setOnCompletionListener(listener);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (mNextPlayer != null) {
                // as it turns out, starting a new MediaPlayer on the completion
                // of a previous player ends up slightly overlapping the two
                // playbacks, so slightly delaying the start of the next player
                // gives a better user experience
                SystemClock.sleep(50);
                mNextPlayer.start();
            }
            mCompletion.onCompletion(this);
        }
    }

}
