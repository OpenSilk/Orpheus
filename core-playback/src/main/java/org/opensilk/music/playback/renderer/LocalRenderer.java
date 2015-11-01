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
package org.opensilk.music.playback.renderer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.model.Track;
import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.service.IntentHelper;
import org.opensilk.music.playback.service.PlaybackServiceScope;

import java.io.IOException;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * A class that implements local media playback using {@link android.media.MediaPlayer}
 */
@PlaybackServiceScope
public class LocalRenderer implements IMusicRenderer,
        AudioManager.OnAudioFocusChangeListener, IMediaPlayer.Callback {

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
    private IMediaPlayer.Factory mDefaultMediaPlayerFactory;

    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Timber.d("Headphones disconnected.");
                if (isPlaying()) {
                    final Intent i = new Intent().setComponent(IntentHelper.getComponent(mContext));
                    i.setAction(PlaybackConstants.SERVICECMD);
                    i.putExtra(PlaybackConstants.CMDNAME, PlaybackConstants.CMDPAUSE);
                    mContext.startService(i);
                }
            }
        }
    };

    @Inject
    public LocalRenderer(
            @ForApplication Context context,
            AudioManager audioManager,
            DefaultMediaPlayer.Factory defaultMediaPlayerFactory
    ) {
        this.mContext = context;
        this.mAudioManager = audioManager;
        this.mDefaultMediaPlayerFactory = defaultMediaPlayerFactory;
    }

    public void start() {
    }

    public void stop(boolean notifyListeners) {
        if (hasCurrent()) {
            mCurrentPosition = getCurrentStreamPosition();
        }
        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        // Relax all resources
        releaseMediaPlayer(true);
        mPlayOnFocusGain = false;
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (notifyListeners) {
            notifyOnPlaybackStatusChanged(mState);
        }
    }

    public void setState(int state) {
        this.mState = state;
    }

    public int getState() {
        return mState;
    }

    public boolean isPlaying() {
        return mPlayOnFocusGain || (hasCurrent() && mMediaPlayer.isPlaying());
    }

    public long getCurrentStreamPosition() {
        //we don't actually seek to the saved position until playback starts
        //so we only ask the player where it is if we are playing
        return (hasCurrent() && PlaybackStateHelper.isPlaying(mState)) ?
                mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }

    public long getDuration() {
        return (hasCurrent() && mPlayerPrepared) ?
                mMediaPlayer.getDuration() : PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
    }

    public void prepareForTrack() {
        stop(false);
        mCurrentPosition = 0;
        mState = PlaybackStateCompat.STATE_CONNECTING;
        notifyOnPlaybackStatusChanged(mState);
    }

    @DebugLog
    public boolean loadTrack(Bundle trackBundle) {
        if (mState != PlaybackStateCompat.STATE_CONNECTING) {
            throw  new IllegalStateException("Must call prepareForTrack() first");
        }
        try {
            Track track = Track.BUNDLE_CREATOR.fromBundle(trackBundle);
            Track.Res item = track.getResources().get(0);

            mMediaPlayer = mDefaultMediaPlayerFactory.create(mContext);
            mMediaPlayer.setCallback(this);

            mMediaPlayer.setDataSource(mContext, item.getUri(), item.getHeaders());

            // Starts preparing the media player in the background. When
            // it's done, it will call our OnPreparedListener (that is,
            // the onPrepared() method on this class, since we set the
            // listener to 'this'). Until the media player is prepared,
            // we *cannot* call start() on it!
            mMediaPlayer.prepareAsync();

            mState = PlaybackStateCompat.STATE_BUFFERING;
            notifyOnPlaybackStatusChanged(mState);

            return true;
        } catch (IOException ex) {
            Timber.e(ex, "Exception loading track");
            return false;
        }
    }

    public void prepareForNextTrack() {
        releaseNextMediaPlayer();
    }

    @DebugLog
    public boolean loadNextTrack(Bundle trackBundle) {
        if (!hasCurrent()) {
            throw new IllegalStateException("called loadNextTrack with no current track");
        }
        if (hasNext()) {
            throw new IllegalStateException("Must call prepareForNextTrack() first");
        }
        try {
            Track track = Track.BUNDLE_CREATOR.fromBundle(trackBundle);
            Track.Res item = track.getResources().get(0);

            mNextMediaPlayer = mDefaultMediaPlayerFactory.create(mContext);
            mNextMediaPlayer.setCallback(this);

            mNextMediaPlayer.setDataSource(mContext, item.getUri(), item.getHeaders());

            // Starts preparing the media player in the background. When
            // it's done, it will call our OnPreparedListener (that is,
            // the onPrepared() method on this class, since we set the
            // listener to 'this'). Until the media player is prepared,
            // we *cannot* call start() on it!
            mNextMediaPlayer.prepareAsync();

            return true;
        } catch (IOException ex) {
            Timber.e(ex, "Exception loading next track");
            return false;
        }
    }

    @DebugLog
    public void play() {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        if (mState == PlaybackStateCompat.STATE_PAUSED && hasCurrent() && mPlayerPrepared) {
            configMediaPlayerState();
        } //else wait for prepared
    }

    public boolean hasCurrent() {
        return mMediaPlayer != null;
    }

    public boolean hasNext() {
        return mNextMediaPlayer != null;
    }

    public boolean goToNext() {
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
        notifyOnAudioSessionId(sessionId);
        notifyOnWentToNext();
        return true;
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
        notifyOnPlaybackStatusChanged(mState);
    }

    @DebugLog
    public void seekTo(long position) {
        if (!hasCurrent() || !mPlayerPrepared) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else {
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackStateCompat.STATE_BUFFERING;
            }
            mMediaPlayer.seekTo(position);
            notifyOnPlaybackStatusChanged(mState);
        }
    }

    @Override
    public boolean isRemotePlayback() {
        return false;
    }

    @Override
    public VolumeProviderCompat getVolumeProvider() {
        return null;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void setAccessor(PlaybackServiceAccessor accessor) {
        //pass
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
                if (hasCurrent()) {
                    mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
                }
            } else {
                if (hasCurrent()) {
                    mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (hasCurrent() && !mMediaPlayer.isPlaying()) {
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
        notifyOnPlaybackStatusChanged(mState);
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
            notifyOnPlaybackStatusChanged(mState);
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
                notifyOnCompletion();
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
                notifyOnPlaybackStatusChanged(mState);
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
        notifyOnError("MediaPlayer error " + what + " (" + extra + ")");
        return true; // true indicates we handled the error
    }

    @Override
    @DebugLog
    public void onAudioSessionId(IMediaPlayer mp, int audioSessionId) {
        if (mp == mMediaPlayer) {
            notifyOnAudioSessionId(audioSessionId);
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

    private void notifyOnPlaybackStatusChanged(int state) {
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(state);
        }
    }

    private void notifyOnWentToNext() {
        if (mCallback != null) {
            mCallback.onWentToNext();
        }
    }

    private void notifyOnCompletion() {
        if (mCallback != null) {
            mCallback.onCompletion();
        }
    }

    private void notifyOnError(String error) {
        if (mCallback != null) {
            mCallback.onError(error);
        }
    }

    private void notifyOnAudioSessionId(int audioSessionId) {
        if (mCallback != null) {
            mCallback.onAudioSessionId(audioSessionId);
        }
    }

}
