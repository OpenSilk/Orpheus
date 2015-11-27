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
    private long mCurrentPosition = 0;
    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private IMediaPlayer.Factory mDefaultMediaPlayerFactory;

    private static class Player {
        IMediaPlayer player;
        boolean hasPlayer() {
            return player != null;
        }
        boolean prepared;
        boolean hasTrack;
        int sessionId;
        void reset(boolean release) {
            prepared = false;
            hasTrack = false;
            sessionId = 0;
            if (release && hasPlayer()) {
                player.reset();
                player.release();
                player = null;
            }
        }
    }

    private Player mCurrentPlayer = new Player();
    private Player mNextPlayer = new Player();

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
        resetHard();
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
        return mPlayOnFocusGain || (hasCurrent() && mCurrentPlayer.player.isPlaying());
    }

    public long getCurrentStreamPosition() {
        //we don't actually seek to the saved position until playback starts
        //so we only ask the player where it is if we are playing
        return (hasCurrent() && mCurrentPlayer.player.isPlaying()) ?
                mCurrentPlayer.player.getCurrentPosition() : mCurrentPosition;
    }

    public long getDuration() {
        return (hasCurrent() && mCurrentPlayer.prepared) ?
                mCurrentPlayer.player.getDuration() : PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
    }

    public void prepareForTrack() {
        resetSoft();
        mCurrentPlayer.reset(false);
        mCurrentPosition = 0;
        mState = PlaybackStateCompat.STATE_CONNECTING;
        notifyOnPlaybackStatusChanged(mState);
    }

    @DebugLog
    public boolean loadTrack(Bundle trackBundle) {
        if (hasCurrent()) {
            prepareForTrack();
        }
        try {
            Track track = Track.BUNDLE_CREATOR.fromBundle(trackBundle);
            Track.Res item = track.getResources().get(0);

            if (!mCurrentPlayer.hasPlayer()) {
                mCurrentPlayer.player = mDefaultMediaPlayerFactory.create(mContext);
                mCurrentPlayer.player.setCallback(this);
            }

            mCurrentPlayer.player.reset();
            mCurrentPlayer.player.setDataSource(mContext, item.getUri(), item.getHeaders());
            mCurrentPlayer.hasTrack = true;

            // Starts preparing the media player in the background. When
            // it's done, it will call our OnPreparedListener (that is,
            // the onPrepared() method on this class, since we set the
            // listener to 'this'). Until the media player is prepared,
            // we *cannot* call start() on it!
            mCurrentPlayer.player.prepareAsync();

            mState = PlaybackStateCompat.STATE_BUFFERING;
            notifyOnPlaybackStatusChanged(mState);

            return true;
        } catch (IOException ex) {
            Timber.e(ex, "Exception loading track");
            return false;
        }
    }

    public void prepareForNextTrack() {
        mNextPlayer.reset(false);
    }

    @DebugLog
    public boolean loadNextTrack(Bundle trackBundle) {
        if (!hasCurrent()) {
            resetWithError("called loadNextTrack with no current track");
            return false;
        }
        if (hasNext()) {
            prepareForNextTrack();
        }
        try {
            Track track = Track.BUNDLE_CREATOR.fromBundle(trackBundle);
            Track.Res item = track.getResources().get(0);

            if (!mNextPlayer.hasPlayer()) {
                mNextPlayer.player = mDefaultMediaPlayerFactory.create(mContext);
                mNextPlayer.player.setCallback(this);
            }

            mNextPlayer.player.reset();
            mNextPlayer.player.setDataSource(mContext, item.getUri(), item.getHeaders());
            mNextPlayer.hasTrack = true;

            // Starts preparing the media player in the background. When
            // it's done, it will call our OnPreparedListener (that is,
            // the onPrepared() method on this class, since we set the
            // listener to 'this'). Until the media player is prepared,
            // we *cannot* call start() on it!
            mNextPlayer.player.prepareAsync();

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
        if (hasCurrent() && mCurrentPlayer.prepared) {
            configMediaPlayerState();
        } else if (!hasCurrent()) {
            resetWithError("play called without a current track");
        } //else wait for prepared
    }

    public boolean hasCurrent() {
        return mCurrentPlayer.hasPlayer() && mCurrentPlayer.hasTrack;
    }

    public boolean hasNext() {
        return mNextPlayer.hasPlayer() && mNextPlayer.hasTrack;
    }

    public boolean goToNext() {
        if (!hasNext()) {
            notifyOnError("No next player");
        }
        final Player oldPlayer = mCurrentPlayer;
        try {
            mCurrentPosition = 0;
            mCurrentPlayer = mNextPlayer;
            mNextPlayer = new Player();
            mPlayOnFocusGain = true;
            mState = PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
            if (mCurrentPlayer.prepared) {
                configMediaPlayerState();
            } //else wait for prepared
            //todo is there a better place for this?
            notifyOnAudioSessionId(mCurrentPlayer.sessionId);
            notifyOnWentToNext();
        } finally {
            oldPlayer.reset(true);
        }
        return true;
    }

    public void pause() {
        resetSoft();
        mState = PlaybackStateCompat.STATE_PAUSED;
        notifyOnPlaybackStatusChanged(mState);
    }

    @DebugLog
    public void seekTo(long position) {
        if (!hasCurrent() || !mCurrentPlayer.prepared) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else if (hasCurrent()) {
            mCurrentPlayer.player.seekTo(position);
            mState = PlaybackStateCompat.STATE_BUFFERING;
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

    //resets our state but does not release mediaplayers
    private void resetSoft() {
        if (hasCurrent()) {
            if (mCurrentPlayer.player.isPlaying()) {
                mCurrentPlayer.player.pause();
            }
            mCurrentPosition = mCurrentPlayer.player.getCurrentPosition();
        }
        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        mPlayOnFocusGain = false;
        //TODO this isnt really a proper state since we still hold the players and must be released
        mState = PlaybackStateCompat.STATE_NONE;
    }

    private void resetHard() {
        resetSoft();
        mCurrentPlayer.reset(true);
        mNextPlayer.reset(true);
    }

    private void resetWithError(String error) {
        resetHard();
        mCurrentPosition = 0;
        mState = PlaybackStateCompat.STATE_ERROR;
        notifyOnError(error);
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
                    mCurrentPlayer.player.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
                }
            } else {
                if (hasCurrent()) {
                    mCurrentPlayer.player.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                }
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (hasCurrent() && !mCurrentPlayer.player.isPlaying()) {
                    if (mCurrentPosition == mCurrentPlayer.player.getCurrentPosition()) {
                        mCurrentPlayer.player.start();
                        mState = PlaybackStateCompat.STATE_PLAYING;
                    } else {
                        Timber.d("configMediaPlayerState startMediaPlayer. " +
                                "seeking to %s", mCurrentPosition);
                        mCurrentPlayer.player.seekTo(mCurrentPosition);
                        if (mState != PlaybackStateCompat.STATE_SKIPPING_TO_NEXT) {
                            mState = PlaybackStateCompat.STATE_BUFFERING;
                        }
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
        if (player == mCurrentPlayer.player) {
            mCurrentPosition = player.getCurrentPosition();
            switch (mState) {
                case PlaybackStateCompat.STATE_BUFFERING:
                case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                    player.start();
                    mState = PlaybackStateCompat.STATE_PLAYING;
                    break;
                default:
                    mState = PlaybackStateCompat.STATE_PAUSED;
                    break;
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
        if (player == mCurrentPlayer.player) {
            if (hasNext()) {
                goToNext();
            } else {
                // The media player finished playing the current song
                resetHard();
                mCurrentPosition = 0;
                notifyOnCompletion();
            }
        } else {
            Timber.e("Received onCompletion for media player that is not the current [isnext=%s]",
                    mNextPlayer.player == player);
            resetWithError("Received out of order onCompletion event");
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
        if (player == mCurrentPlayer.player) {
            mCurrentPlayer.prepared = true;
            // The media player is done preparing. That means we can start playing if we
            // have audio focus.
            if (mPlayOnFocusGain) {
                configMediaPlayerState();
            } else {
                mState = PlaybackStateCompat.STATE_PAUSED;
                notifyOnPlaybackStatusChanged(mState);
            }
        } else if (player == mNextPlayer.player) {
            mNextPlayer.prepared = true;
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
        resetHard();
        mCurrentPosition = 0;
        notifyOnError("MediaPlayer error " + what + " (" + extra + ")");
        return true; // true indicates we handled the error
    }

    @Override
    @DebugLog
    public void onAudioSessionId(IMediaPlayer mp, int audioSessionId) {
        if (mp == mCurrentPlayer.player) {
            mCurrentPlayer.sessionId = audioSessionId;
            notifyOnAudioSessionId(audioSessionId);
        } else {
            mNextPlayer.sessionId = audioSessionId;
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
