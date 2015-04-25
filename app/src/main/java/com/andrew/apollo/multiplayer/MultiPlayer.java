
package com.andrew.apollo.multiplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.*;

import com.andrew.apollo.helper.PlaybackStateHelper;
import com.andrew.apollo.player.IPlayer;
import com.andrew.apollo.player.IPlayerCallback;
import com.andrew.apollo.player.PlayerEvent;
import com.andrew.apollo.player.PlayerStatus;

import org.opensilk.common.util.ObjectUtils;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class MultiPlayer implements
        IPlayer,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private final Context mContext;
    private final int mAudioSessionId;
    private final IPlayerCallback mCallback;

    private final HandlerThread mHandlerThread;
    private final Handler mHandler;

    private final Object mLock = new Object();

    private CompatMediaPlayer mCurrentMediaPlayer;
    private CompatMediaPlayer mNextMediaPlayer;
    private boolean mIsInitialized = false;


    public MultiPlayer(Context context, int audioSessionId, IPlayerCallback callback) {
        mContext = context;
        mAudioSessionId = audioSessionId;
        mCallback = callback;
        mHandlerThread = new HandlerThread("MultiPlayer", android.os.Process.THREAD_PRIORITY_AUDIO);
        mHandlerThread.start();
        mHandler = new MultiHandler(mHandlerThread.getLooper(), this);
    }

    @Override
    public void play() {
        synchronized (mLock) {
            if (isInitialized()) {
                try {
                    mCurrentMediaPlayer.start();
                    mCallback.onPlayerStatus(PlayerStatus.playing());
                } catch (IllegalStateException e) {
                    mCallback.onPlayerStatus(PlayerStatus.error(e.getMessage()));
                    setInitializedLocked(false);
                }
            }
        }
    }

    @Override
    public void pause() {
        synchronized (mLock) {
            if (isInitialized()) {
                try {
                    mCurrentMediaPlayer.pause();
                    mCallback.onPlayerStatus(PlayerStatus.paused());
                } catch (IllegalStateException e) {
                    mCallback.onPlayerStatus(PlayerStatus.error(e.getMessage()));
                    setInitializedLocked(false);
                }
            }
        }
    }

    @Override
    public void stop() {
        synchronized (mLock) {
            if (isInitialized()) {
                mCurrentMediaPlayer.reset();
                setInitializedLocked(false);
                mCallback.onPlayerStatus(PlayerStatus.stopped());
            }
        }
    }

    @Override
    public void seekTo(long pos) {
        synchronized (mLock) {
            if (isInitialized()) {
                try {
                    mCurrentMediaPlayer.seekTo((int) pos);
                } catch (IllegalStateException e) {
                    mCallback.onPlayerStatus(PlayerStatus.error(e.getMessage()));
                    setInitializedLocked(false);
                }
            }
        }
    }

    @Override
    public void skipToNext() {
        synchronized (mLock) {
            if (isInitialized()) {

            }
        }
    }

    @Override
    public void getPosition() {
        long pos = -1;
        synchronized (mLock) {
            if (isInitialized()) {
                try {
                    pos = mCurrentMediaPlayer.getCurrentPosition();
                } catch (IllegalStateException e) {
                    mCallback.onPlayerStatus(PlayerStatus.error(e.getMessage()));
                    setInitializedLocked(false);
                    return;
                }
            }
        }
        mCallback.onPlayerEvent(PlayerEvent.position(pos));
    }

    @Override
    public void getDuration() {
        long dur = -1;
        synchronized (mLock) {
            if (isInitialized()) {
                try {
                    dur = mCurrentMediaPlayer.getDuration();
                } catch (IllegalStateException e) {
                    mCallback.onPlayerStatus(PlayerStatus.error(e.getMessage()));
                    setInitializedLocked(false);
                    return;
                }
            }
        }
        mCallback.onPlayerEvent(PlayerEvent.duration(dur));
    }

    @Override
    public void setDataSource(Uri uri) {
        mCallback.onPlayerStatus(PlayerStatus.loading());
        mHandler.obtainMessage(E.SETDATASOURCE, uri).sendToTarget();
    }

    @Override
    public void setNextDataSource(Uri uri) {
        mHandler.obtainMessage(E.SETNEXTDATASOURCE, uri).sendToTarget();
    }

    @Override
    public void duck() {
        mHandler.sendEmptyMessage(E.FADEDOWN);
    }

    @Override
    public void release() {
        synchronized (mLock) {
            setInitializedLocked(false);
            releaseCurrentLocked();
            releaseNextLocked();
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quit();
    }

    void setVolume(float vol) {
        synchronized (mLock) {
            if (isInitialized()) {
                try {
                    mCurrentMediaPlayer.setVolume(vol, vol);
                } catch (IllegalStateException e) {
                    mCallback.onPlayerStatus(PlayerStatus.error(e.getMessage()));
                    setInitializedLocked(false);
                }
            }
        }
    }

    boolean isInitialized() {
        synchronized (mLock) {
            return mIsInitialized && mCurrentMediaPlayer != null;
        }
    }

    void setInitializedLocked(boolean initialized) {
        mIsInitialized = initialized;
    }

    void releaseCurrentLocked() {
        if (mCurrentMediaPlayer != null) {
            mCurrentMediaPlayer.release();
            mCurrentMediaPlayer = null;
        }
    }

    void releaseNextLocked() {
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.release();
            mNextMediaPlayer = null;
        }
    }

    boolean setDataSourceInternal(MediaPlayer player, Uri uri) {
        try {
            player.reset();
            player.setDataSource(mContext, uri);
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioSessionId(mAudioSessionId);
            player.prepare();
        } catch (Exception e) {
            Timber.w(e, "setDataSourceInternal");
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
                    setInitializedLocked(false);
                    releaseCurrentLocked();
                }
                mCallback.onPlayerStatus(PlayerStatus.error("Media Server Died"));
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
                mCallback.onPlayerEvent(PlayerEvent.wentToNext());
            } else {
                mCallback.onPlayerStatus(PlayerStatus.stopped());
            }
        }
    }

    private interface E {
        int SETDATASOURCE = 1;
        int SETNEXTDATASOURCE = 2;

        int FADEDOWN = 1001;
        int FADEUP = 1002;
    }

    private static final class MultiHandler extends Handler {
        private final WeakReference<MultiPlayer> mPlayer;
        private float mCurrentVolume = 1.0f;

        public MultiHandler(Looper looper, MultiPlayer mPlayer) {
            super(looper);
            this.mPlayer = new WeakReference<MultiPlayer>(mPlayer);
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
                        player.setInitializedLocked(false);
                        player.releaseCurrentLocked();
                    }
                    final CompatMediaPlayer mp = new CompatMediaPlayer();
                    if (player.setDataSourceInternal(mp, (Uri)msg.obj)) {
                        synchronized (player.mLock) {
                            player.mCurrentMediaPlayer = mp;
                            player.setInitializedLocked(true);
                            player.releaseNextLocked();
                            player.mCallback.onPlayerStatus(PlayerStatus.ready());
                        }
                    } else {
                        player.mCallback.onPlayerStatus(PlayerStatus.error("Unable to open media"));
                    }
                    return;
                } case E.SETNEXTDATASOURCE: {
                    synchronized (player.mLock) {
                        if (player.mCurrentMediaPlayer != null) {
                            try {
                                player.mCurrentMediaPlayer.setNextMediaPlayer(null);
                            } catch (IllegalArgumentException e) {
                                Timber.i("Next media player is current one, continuing");
                            } catch (IllegalStateException e) {
                                Timber.w(e, "Media player not initialized!");
                                player.mCallback.onPlayerEvent(PlayerEvent.openNextFailed());
                                return;
                            }
                        }
                        player.releaseNextLocked();
                    }
                    final CompatMediaPlayer mp = new CompatMediaPlayer();
                    if (player.setDataSourceInternal(mp, (Uri)msg.obj)) {
                        synchronized (player.mLock) {
                            try {
                                player.mNextMediaPlayer = mp;
                                player.mCurrentMediaPlayer.setNextMediaPlayer(mp);
                            } catch (IllegalArgumentException|IllegalStateException e) {
                                Timber.w(e, "setNextDataSource: setNextMediaPlayer()");
                                player.releaseNextLocked();
                                player.mCallback.onPlayerEvent(PlayerEvent.openNextFailed());
                            }
                        }
                    } else {
                        player.mCallback.onPlayerEvent(PlayerEvent.openNextFailed());
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
