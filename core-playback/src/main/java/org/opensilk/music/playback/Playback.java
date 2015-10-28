/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensilk.music.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.media.session.PlaybackStateCompat;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.model.Track;
import org.opensilk.music.playback.player.IPlayer;
import org.opensilk.music.playback.renderer.IMediaPlayer;
import org.opensilk.music.playback.service.PlaybackServiceK;
import org.opensilk.music.playback.service.PlaybackServiceL;
import org.opensilk.music.playback.service.PlaybackServiceScope;

import java.io.IOException;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * A class that implements local media playback using {@link android.media.MediaPlayer}
 */
@PlaybackServiceScope
public class Playback implements AudioManager.OnAudioFocusChangeListener, IMediaPlayer.Callback {

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED  = 2;

    private final Context mContext;
    private final AudioManager mAudioManager;
    private int mState;
    private boolean mPlayOnFocusGain;
    private Callback mCallback;
    private boolean mAudioNoisyReceiverRegistered;
    private long mCurrentPosition = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private IMediaPlayer mMediaPlayer;
    private IMediaPlayer mNextMediaPlayer;
    private boolean mPlayerPrepared;
    private boolean mNextPlayerPrepared;
    private int mNextAudioSessionId;

    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Timber.d("Headphones disconnected.");
                if (isPlaying()) {
                    final Intent i;
                    if (VersionUtils.hasLollipop()) {
                        i = new Intent(context, PlaybackServiceL.class);
                    } else {
                        i = new Intent(context, PlaybackServiceK.class);
                    }
                    i.setAction(PlaybackConstants.SERVICECMD);
                    i.putExtra(PlaybackConstants.CMDNAME, PlaybackConstants.CMDPAUSE);
                    mContext.startService(i);
                }
            }
        }
    };

    @Inject
    public Playback(
            @ForApplication Context context,
            AudioManager audioManager
    ) {
        this.mContext = context;
        this.mAudioManager = audioManager;
    }

    public void start() {
    }

    public void stop(boolean notifyListeners) {
        if (hasPlayer()) {
            mCurrentPosition = getCurrentStreamPosition();
        }
        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        // Relax all resources
        releaseMediaPlayer(true);
        mPlayOnFocusGain = false;
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (notifyListeners && mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    public void setState(int state) {
        this.mState = state;
    }

    public int getState() {
        return mState;
    }

    public boolean isPlaying() {
        return mPlayOnFocusGain || (hasPlayer() && mMediaPlayer.isPlaying());
    }

    public long getCurrentStreamPosition() {
        //we don't actually seek to the saved position until playback starts
        //so we only ask the player where it is if we are playing
        return (hasPlayer() && PlaybackStateHelper.isPlaying(mState)) ?
                mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }

    public long getDuration() {
        return (hasPlayer() && mPlayerPrepared) ?
                mMediaPlayer.getDuration() : PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
    }

    public void prepareForTrack() {
        stop(false);
        mCurrentPosition = 0;
        mState = PlaybackStateCompat.STATE_CONNECTING;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    @DebugLog
    public void loadTrack(Track.Res item, IMediaPlayer.Factory factory) {
        if (mState != PlaybackStateCompat.STATE_CONNECTING) {
            throw  new IllegalStateException("Must call prepareForTrack() first");
        }
        try {
            mMediaPlayer = factory.create(mContext);
            mMediaPlayer.setCallback(this);

            mState = PlaybackStateCompat.STATE_BUFFERING;

            mMediaPlayer.setDataSource(mContext, item.getUri(), item.getHeaders());

            // Starts preparing the media player in the background. When
            // it's done, it will call our OnPreparedListener (that is,
            // the onPrepared() method on this class, since we set the
            // listener to 'this'). Until the media player is prepared,
            // we *cannot* call start() on it!
            mMediaPlayer.prepareAsync();

            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(mState);
            }

        } catch (IOException ex) {
            Timber.e(ex, "Exception loading track");
            if (mCallback != null) {
                mCallback.onErrorOpenCurrentFailed(ex.getMessage());
            }
        }
    }

    public void prepareForNextTrack() {
        releaseNextMediaPlayer();
    }

    @DebugLog
    public void loadNextTrack(Track.Res item, IMediaPlayer.Factory factory) {
        if (!hasPlayer()) {
            throw new IllegalStateException("called loadNextTrack with no current track");
        }
        if (hasNext()) {
            throw new IllegalStateException("Must call prepareForNextTrack() first");
        }
        try {
            mNextMediaPlayer = factory.create(mContext);
            mNextMediaPlayer.setCallback(this);

            mNextMediaPlayer.setDataSource(mContext, item.getUri(), item.getHeaders());

            // Starts preparing the media player in the background. When
            // it's done, it will call our OnPreparedListener (that is,
            // the onPrepared() method on this class, since we set the
            // listener to 'this'). Until the media player is prepared,
            // we *cannot* call start() on it!
            mNextMediaPlayer.prepareAsync();

        } catch (IOException ex) {
            Timber.e(ex, "Exception loading next track");
            if (mCallback != null) {
                mCallback.onErrorOpenNextFailed(ex.getMessage());
            }
        }
    }

    @DebugLog
    public void play() {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        if (mState == PlaybackStateCompat.STATE_PAUSED && hasPlayer() && mPlayerPrepared) {
            configMediaPlayerState();
        } //else wait for prepared
    }

    public boolean hasPlayer() {
        return mMediaPlayer != null;
    }

    public boolean hasNext() {
        return mNextMediaPlayer != null;
    }

    public void goToNext() {
        if (!hasNext()) {
            throw  new IllegalStateException("Next player not initialized");
        }
        releaseMediaPlayer(false);
        mCurrentPosition = 0;
        mMediaPlayer = mNextMediaPlayer;
        mNextMediaPlayer = null;
        mPlayerPrepared = mNextPlayerPrepared;
        mNextPlayerPrepared = false;
        mPlayOnFocusGain = true;
        if (mPlayerPrepared) {
            configMediaPlayerState();
        } else {
            mState = PlaybackStateCompat.STATE_BUFFERING;
            //and wait for prepared
        }
        final int sessionId = mNextAudioSessionId;
        mNextAudioSessionId = 0;
        if (mCallback != null) {
            mCallback.onAudioSessionId(sessionId);
            mCallback.onWentToNext();
        }
    }

    public void pause() {
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
            }
        }
        // while paused, retain the MediaPlayer but give up audio focus
        giveUpAudioFocus();
        mPlayOnFocusGain = false;
        unregisterAudioNoisyReceiver();
        mState = PlaybackStateCompat.STATE_PAUSED;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    @DebugLog
    public void seekTo(long position) {
        if (!hasPlayer() || !mPlayerPrepared) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else {
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackStateCompat.STATE_BUFFERING;
            }
            mMediaPlayer.seekTo(position);
            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(mState);
            }
        }
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    /**
     * Try to get the system audio focus.
     */
    @DebugLog
    private void tryToGetAudioFocus() {
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
            }
        }
    }

    /**
     * Give up the audio focus.
     */
    @DebugLog
    private void giveUpAudioFocus() {
        if (mAudioFocus == AUDIO_FOCUSED) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    @DebugLog
    private void configMediaPlayerState() {
        Timber.d("configMediaPlayerState. mAudioFocus=%d", mAudioFocus);
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            pause();
        } else {  // we have audio focus:
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                if (hasPlayer()) {
                    mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
                }
            } else {
                if (hasPlayer()) {
                    mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (hasPlayer() && !mMediaPlayer.isPlaying()) {
                    if (mCurrentPosition == mMediaPlayer.getCurrentPosition()) {
                        mMediaPlayer.start();
                        mState = PlaybackStateCompat.STATE_PLAYING;
                    } else {
                        Timber.d("configMediaPlayerState startMediaPlayer. " +
                                        "seeking to %s", mCurrentPosition);
                        mMediaPlayer.seekTo(mCurrentPosition);
                        mState = PlaybackStateCompat.STATE_BUFFERING;
                    }
                }
                mPlayOnFocusGain = false;
            }
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of {@link android.media.AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    @DebugLog
    public void onAudioFocusChange(int focusChange) {
        Timber.d("onAudioFocusChange. focusChange=%d", focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AUDIO_FOCUSED;
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mState == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            Timber.d("onAudioFocusChange: Ignoring unsupported focusChange: %d", focusChange);
        }
        configMediaPlayerState();
    }

    /**
     * Called when MediaPlayer has completed a seek
     *
     * @see android.media.MediaPlayer.OnSeekCompleteListener
     */
    @Override
    @DebugLog
    public void onSeekComplete(IMediaPlayer player) {
        if (player == mMediaPlayer) {
            mCurrentPosition = player.getCurrentPosition();
            if (mState == PlaybackStateCompat.STATE_BUFFERING) {
                mMediaPlayer.start();
                mState = PlaybackStateCompat.STATE_PLAYING;
            }
            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(mState);
            }
        }
    }

    /**
     * Called when media player is done playing current song.
     *
     * @see android.media.MediaPlayer.OnCompletionListener
     */
    @Override
    @DebugLog
    public void onCompletion(IMediaPlayer player) {
        if (player == mMediaPlayer) {
            if (hasNext()) {
                goToNext();
            } else {
                // The media player finished playing the current song
                stop(false);
                mCurrentPosition = 0;
                if (mCallback != null) {
                    mCallback.onCompletion();
                }
            }
        }
    }

    /**
     * Called when media player is done preparing.
     *
     * @see android.media.MediaPlayer.OnPreparedListener
     */
    @Override
    @DebugLog
    public void onPrepared(IMediaPlayer player) {
        if (player == mMediaPlayer) {
            mPlayerPrepared = true;
            // The media player is done preparing. That means we can start playing if we
            // have audio focus.
            if (mPlayOnFocusGain) {
                configMediaPlayerState();
            } else {
                mState = PlaybackStateCompat.STATE_PAUSED;
                if (mCallback != null) {
                    mCallback.onPlaybackStatusChanged(mState);
                }
            }
        } else if (player == mNextMediaPlayer) {
            mNextPlayerPrepared = true;
        }
    }

    /**
     * Called when there's an error playing media. When this happens, the media
     * player goes to the Error state. We warn the user about the error and
     * reset the media player.
     *
     * @see android.media.MediaPlayer.OnErrorListener
     */
    @Override
    @DebugLog
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        Timber.d("Media player error: what=" + what + ", extra=" + extra);
        stop(false);
        mCurrentPosition = 0;
        if (mCallback != null) {
            mCallback.onError("MediaPlayer error " + what + " (" + extra + ")");
        }
        return true; // true indicates we handled the error
    }

    @Override
    @DebugLog
    public void onAudioSessionId(IMediaPlayer mp, int audioSessionId) {
        if (mp == mMediaPlayer) {
            if (mCallback != null) {
                mCallback.onAudioSessionId(audioSessionId);
            }
        } else {
            mNextAudioSessionId = audioSessionId;
        }
    }

    /**
     * Releases resources used by the service for playback.
     */
    private void releaseMediaPlayer(boolean andNext) {
        // stop and release the Media Player, if it's available
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mPlayerPrepared = false;
        }
        if (andNext) {
            releaseNextMediaPlayer();
        }
    }

    private void releaseNextMediaPlayer() {
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.reset();
            mNextMediaPlayer.release();
            mNextMediaPlayer = null;
            mNextPlayerPrepared = false;
            mNextAudioSessionId = 0;
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mContext.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            try {
                mContext.unregisterReceiver(mAudioNoisyReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            mAudioNoisyReceiverRegistered = false;
        }
    }

    public interface Callback {
        /**
         * on Playback status changed
         * Implementations can use this callback to update
         * playback state on the media sessions.
         */
        void onPlaybackStatusChanged(int state);

        /**
         * player has started playing track loaded with {@link IPlayer#setNextDataSource(Uri)}
         */
        void onWentToNext();

        /**
         * On current music completed.
         */
        void onCompletion();

        /**
         * player has failed to open uri
         */
        void onErrorOpenCurrentFailed(String msg);

        /**
         * player has failed to open uri
         */
        void onErrorOpenNextFailed(String msg);

        /**
         * @param error to be added to the PlaybackState
         */
        void onError(String error);

        /**
         * Invoked when the audio session id becomes known
         */
        void onAudioSessionId(int audioSessionId);

    }

}
