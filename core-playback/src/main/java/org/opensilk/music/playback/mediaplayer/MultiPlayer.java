
package org.opensilk.music.playback.mediaplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.os.Process;

import org.opensilk.music.playback.player.IPlayer;
import org.opensilk.music.playback.player.IPlayerCallback;
import org.opensilk.music.playback.player.IPlayerCallbackDelegate;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class MultiPlayer implements
        IPlayer, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final Context mContext;
    private final int mAudioSessionId;
    private final IPlayerCallbackDelegate mCallback;

    private final HandlerThread mHandlerThread;
    private final MultiHandler mHandler;

    private final Object mLock = new Object();

    private CompatMediaPlayer mCurrentMediaPlayer;
    private CompatMediaPlayer mNextMediaPlayer;

    public MultiPlayer(Context context, int audioSessionId) {
        mContext = context;
        mAudioSessionId = audioSessionId;
        mCallback = new IPlayerCallbackDelegate();
        mHandlerThread = new HandlerThread("MultiPlayer", Process.THREAD_PRIORITY_DEFAULT);
        mHandlerThread.start();
        mHandler = new MultiHandler(mHandlerThread.getLooper(), this);
    }

    @Override
    public void play() {
        mHandler.removeMessages(E.FADEDOWN);
        mHandler.sendEmptyMessage(E.FADEUP);
        synchronized (mLock) {
            if (isInitializedLocked()) {
                try {
                    mCurrentMediaPlayer.start();
                    mCallback.onPlaying();
                } catch (IllegalStateException e) {
                    Timber.e(e, "play()");
                    releaseCurrentLocked();
                }
            }
        }
    }

    @Override
    public void pause() {
        mHandler.removeMessages(E.FADEUP);
        synchronized (mLock) {
            if (isInitializedLocked()) {
                try {
                    mCurrentMediaPlayer.pause();
                    mCallback.onPaused();
                } catch (IllegalStateException e) {
                    Timber.e(e, "pause()");
                    releaseCurrentLocked();
                }
            }
        }
    }

    @Override
    public void stop() {
        mHandler.removeCallbacksAndMessages(null);
        synchronized (mLock) {
            if (isInitializedLocked()) {
                mCurrentMediaPlayer.reset();
            }
            releaseCurrentLocked();
            mCallback.onStopped();
        }
    }

    @Override
    public boolean seekTo(long pos) {
        synchronized (mLock) {
            if (isInitializedLocked()) {
                try {
                    mCurrentMediaPlayer.seekTo((int) pos);
                    return true;
                } catch (IllegalStateException e) {
                    Timber.e(e, "seekTo()");
                    releaseCurrentLocked();
                }
            }
        }
        return false;
    }

    @Override
    public void skipToNext() {
        mHandler.sendEmptyMessage(E.SKIPTONEXT);
    }

    @Override
    public long getPosition() {
        synchronized (mLock) {
            if (isInitializedLocked()) {
                try {
                    return mCurrentMediaPlayer.getCurrentPosition();
                } catch (IllegalStateException e) {
                    Timber.e(e, "getPosition()");
                    releaseCurrentLocked();
                }
            }
        }
        return -1;
    }

    @Override
    public long getDuration() {
        synchronized (mLock) {
            if (isInitializedLocked()) {
                try {
                    return mCurrentMediaPlayer.getDuration();
                } catch (IllegalStateException e) {
                    Timber.e(e, "getDuration()");
                    releaseCurrentLocked();
                }
            }
        }
        return -1;
    }

    @Override
    public void setDataSource(Uri uri) {
        mHandler.obtainMessage(E.SETDATASOURCE, uri).sendToTarget();
    }

    @Override
    public void setNextDataSource(Uri uri) {
        mHandler.obtainMessage(E.SETNEXTDATASOURCE, uri).sendToTarget();
    }

    @Override
    public void duck(boolean down) {
        if (down) {
            mHandler.removeMessages(E.FADEUP);
            mHandler.sendEmptyMessage(E.FADEDOWN);
        } else {
            mHandler.removeMessages(E.FADEDOWN);
            mHandler.sendEmptyMessage(E.FADEUP);
        }
    }

    @Override
    public void release() {
        synchronized (mLock) {
            releaseCurrentLocked();
            releaseNextLocked();
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quit();
    }

    @Override
    public void setCallback(IPlayerCallback callback, Handler handler) {
        mCallback.setCallback(callback, handler);
    }

    void setVolume(float vol) {
        synchronized (mLock) {
            if (isInitializedLocked()) {
                try {
                    mCurrentMediaPlayer.setVolume(vol, vol);
                } catch (IllegalStateException e) {
                    Timber.e(e, "setVolume");
                    releaseCurrentLocked();
                }
            }
        }
    }

    boolean isInitializedLocked() {
        return mCurrentMediaPlayer != null;
    }

    void releaseCurrentLocked() {
        if (mCurrentMediaPlayer != null) {
            mCurrentMediaPlayer.release();
            mCurrentMediaPlayer = null;
        }
    }

    boolean isNextInitializedLocked() {
        return mNextMediaPlayer != null;
    }

    void releaseNextLocked() {
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.release();
            mNextMediaPlayer = null;
        }
    }

    boolean setDataSourceInternal(MediaPlayer player, Uri uri, boolean notify) {
        try {
            player.reset();
            player.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioSessionId(mAudioSessionId);
            player.setDataSource(mContext, uri);
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            if (notify) {
                mCallback.onLoading();
            }
            player.prepare();
        } catch (Exception e) {
            Timber.e(e, "setDataSourceInternal");
            return false;
        }
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        return true;
    }

    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                synchronized (mLock) {
                    releaseCurrentLocked();
                }
                mCallback.onStopped();
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onCompletion(final MediaPlayer mp) {
        synchronized (mLock) {
            if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
                releaseCurrentLocked();
                mCurrentMediaPlayer = mNextMediaPlayer;
                mNextMediaPlayer = null;
                mCallback.onWentToNext();
            } else {
                mCallback.onStopped();
            }
        }
    }

    private interface E {
        int SETDATASOURCE = 1;
        int SETNEXTDATASOURCE = 2;
        int SKIPTONEXT = 3;

        int FADEDOWN = 1001;
        int FADEUP = 1002;
    }

    private static final class MultiHandler extends Handler {
        private final WeakReference<MultiPlayer> mPlayer;
        private float mCurrentVolume = 1.0f;

        public MultiHandler(Looper looper, MultiPlayer mPlayer) {
            super(looper);
            this.mPlayer = new WeakReference<>(mPlayer);
        }

        @Override
        public void handleMessage(final Message msg) {
            final MultiPlayer player = mPlayer.get();
            if (player == null) {
                return;
            }
            switch (msg.what) {
                case E.SETDATASOURCE: {
                    synchronized (player.mLock) {
                        player.releaseCurrentLocked();
                        player.releaseNextLocked();
                    }
                    final CompatMediaPlayer mp = new CompatMediaPlayer();
                    if (player.setDataSourceInternal(mp, (Uri)msg.obj, true)) {
                        synchronized (player.mLock) {
                            player.mCurrentMediaPlayer = mp;
                            player.mCallback.onReady();
                        }
                    } else {
                        player.mCallback.onErrorOpenCurrentFailed("Unable to open media");
                    }
                    return;
                } case E.SETNEXTDATASOURCE: {
                    synchronized (player.mLock) {
                        player.releaseNextLocked();
                        if (player.isInitializedLocked()) {
                            try {
                                player.mCurrentMediaPlayer.setNextMediaPlayer(null);
                            } catch (IllegalArgumentException e) {
                                Timber.i("Next media player is current one, continuing");
                            } catch (IllegalStateException e) {
                                Timber.w(e, "Media player not initialized!");
                                player.stop();
                                return;
                            }
                        } else {
                            player.stop();
                            return;
                        }
                    }
                    final CompatMediaPlayer mp = new CompatMediaPlayer();
                    if (player.setDataSourceInternal(mp, (Uri) msg.obj, false)) {
                        synchronized (player.mLock) {
                            if (player.isInitializedLocked()) {
                                try {
                                    player.mNextMediaPlayer = mp;
                                    player.mCurrentMediaPlayer.setNextMediaPlayer(mp);
                                    return;
                                } catch (IllegalArgumentException | IllegalStateException e) {
                                    Timber.w(e, "setNextDataSource: setNextMediaPlayer()");
                                    player.releaseNextLocked();
                                    player.mCallback.onErrorOpenNextFailed("Unable to open media");
                                }
                            } else {
                                mp.release();
                                player.stop();
                            }
                        }
                    } else {
                        player.mCallback.onErrorOpenNextFailed("Unable to open media");
                    }
                    return;
                } case E.SKIPTONEXT: {
                    synchronized (player.mLock) {
                        if (player.isInitializedLocked() && player.isNextInitializedLocked()) {
                            try {
                                player.releaseCurrentLocked();
                                player.mNextMediaPlayer.start();
                                player.mCurrentMediaPlayer = player.mNextMediaPlayer;
                                player.mNextMediaPlayer = null;
                                player.mCallback.onWentToNext();
                            } catch (IllegalStateException e) {
                                Timber.e(e, "Skipping to next");
                                player.releaseCurrentLocked();
                                player.releaseNextLocked();
                                player.mCallback.onStopped();
                            }
                        } else {
                            player.stop();
                        }
                    }
                    return;
                } case E.FADEDOWN: {
                    mCurrentVolume -= 0.1f;
                    if (mCurrentVolume > 0.2f) {
                        sendEmptyMessageDelayed(E.FADEDOWN, 10);
                    } else {
                        mCurrentVolume = 0.2f;
                    }
                    player.setVolume(mCurrentVolume);
                    return;
                } case E.FADEUP: {
                    mCurrentVolume += 0.01f;
                    if (mCurrentVolume < 1.0f) {
                        sendEmptyMessageDelayed(E.FADEUP, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    player.setVolume(mCurrentVolume);
                    return;
                } default: {
                    break;
                }
            }
        }
    }

    private static final class CompatMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener {

        private boolean mCompatMode = true;
        private MediaPlayer mNextPlayer;
        private OnCompletionListener mCompletion;

        public CompatMediaPlayer() {
            super();
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
            if (mCompletion != null) {
                mCompletion.onCompletion(this);
            }
        }
    }

}
