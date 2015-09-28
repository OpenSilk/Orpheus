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
import android.media.session.PlaybackState;
import android.net.Uri;
import android.net.wifi.WifiManager;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.model.Track;
import org.opensilk.music.playback.player.IPlayer;
import org.opensilk.music.playback.service.PlaybackService;

import java.io.IOException;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * A class that implements local media playback using {@link android.media.MediaPlayer}
 */
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
    private final PlaybackService mService;
    private final AudioManager mAudioManager;
    private final WifiManager.WifiLock mWifiLock;
    private int mState;
    private boolean mPlayOnFocusGain;
    private Callback mCallback;
    private boolean mAudioNoisyReceiverRegistered;
    private long mCurrentPosition = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private IMediaPlayer mMediaPlayer;
    private IMediaPlayer mNextMediaPlayer;
    private boolean mNextPlayerPrepared;

    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Timber.d("Headphones disconnected.");
                if (isPlaying()) {
                    Intent i = new Intent(context, PlaybackService.class);
                    i.setAction(PlaybackConstants.SERVICECMD);
                    i.putExtra(PlaybackConstants.CMDNAME, PlaybackConstants.CMDPAUSE);
                    mService.startService(i);
                }
            }
        }
    };

    @Inject
    public Playback(
            @ForApplication Context context,
            PlaybackService service,
            AudioManager audioManager,
            WifiManager wifiManager
    ) {
        this.mContext = context;
        this.mService = service;
        this.mAudioManager = audioManager;
        this.mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "OrpheusPlayback");
        this.mWifiLock.setReferenceCounted(false);
    }

    public void start() {
    }

    public void stop(boolean notifyListeners) {
        mState = PlaybackState.STATE_STOPPED;
        mCurrentPosition = getCurrentStreamPosition();
        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        // Relax all resources
        relaxResources(true);
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

    public boolean isConnected() {
        return mState != PlaybackState.STATE_CONNECTING;
    }

    public boolean isPlaying() {
        return mPlayOnFocusGain || (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    public long getCurrentStreamPosition() {
        return mMediaPlayer != null ?
                mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }

    public long getDuration() {
        return mMediaPlayer != null ?
                mMediaPlayer.getDuration() : PlaybackState.PLAYBACK_POSITION_UNKNOWN;
    }

    public void prepareForTrack() {
        stop(false);
        mState = PlaybackState.STATE_CONNECTING;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    public void loadTrack(Track.Res item, IMediaPlayer.Factory factory) {
        if (mState != PlaybackState.STATE_CONNECTING) {
            throw  new IllegalStateException("Must call prepareForTrack() first");
        }
        relaxResources(true); // release everything
        mPlayOnFocusGain = false;
        try {
            mMediaPlayer = factory.create(mContext);
            mMediaPlayer.setCallback(this);

            mState = PlaybackState.STATE_BUFFERING;

            mMediaPlayer.setDataSource(mContext, item.getUri(), item.getHeaders());

            // Starts preparing the media player in the background. When
            // it's done, it will call our OnPreparedListener (that is,
            // the onPrepared() method on this class, since we set the
            // listener to 'this'). Until the media player is prepared,
            // we *cannot* call start() on it!
            mMediaPlayer.prepareAsync();

            // If we are streaming from the internet, we want to hold a
            // Wifi lock, which prevents the Wifi radio from going to
            // sleep while the song is playing.
            String scheme = item.getUri().getScheme();
            if (StringUtils.startsWith(scheme, "http")) {
                mWifiLock.acquire();
            } else if (mWifiLock.isHeld()) {
                mWifiLock.release();
            }

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
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.reset();
            mNextMediaPlayer.release();
            mNextMediaPlayer = null;
            mNextPlayerPrepared = false;
        }
    }

    public void loadNextTrack(Track.Res item, IMediaPlayer.Factory factory) {
        if (mMediaPlayer == null) {
            throw new IllegalStateException("called loadNextTrack with no current track");
        }
        if (mNextMediaPlayer != null) {
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

    public void play() {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        if (mState == PlaybackState.STATE_PAUSED && mMediaPlayer != null) {
            configMediaPlayerState();
        } //else wait for prepared
    }

    public boolean hasNext() {
        return mNextMediaPlayer != null;
    }

    public void goToNext() {
        if (!hasNext()) {
            throw  new IllegalStateException("Next player not initialized");
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
        }
        mMediaPlayer = mNextMediaPlayer;
        mNextMediaPlayer = null;
        if (mNextPlayerPrepared) {
            //if next is prepared start playing
            mNextPlayerPrepared = false;
            mPlayOnFocusGain = true;
            configMediaPlayerState();
        } // else wait for callback
        if (mCallback != null) {
            mCallback.onWentToNext();
        }
    }

    public void pause() {
        if (mState == PlaybackState.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
            }
            // while paused, retain the MediaPlayer but give up audio focus
            relaxResources(false);
            giveUpAudioFocus();
        }
        mPlayOnFocusGain = false;
        unregisterAudioNoisyReceiver();
        mState = PlaybackState.STATE_PAUSED;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    public void seekTo(long position) {
        Timber.d("seekTo called with %d", position);

        if (mMediaPlayer == null) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else {
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackState.STATE_BUFFERING;
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
    private void tryToGetAudioFocus() {
        Timber.d("tryToGetAudioFocus");
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
    private void giveUpAudioFocus() {
        Timber.d("giveUpAudioFocus");
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
    private void configMediaPlayerState() {
        Timber.d("configMediaPlayerState. mAudioFocus=%d", mAudioFocus);
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackState.STATE_PLAYING) {
                pause();
            }
        } else {  // we have audio focus:
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
            } else {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                    Timber.d("configMediaPlayerState startMediaPlayer. seeking to ",
                            mCurrentPosition);
                    if (mCurrentPosition == mMediaPlayer.getCurrentPosition()) {
                        mMediaPlayer.start();
                        mState = PlaybackState.STATE_PLAYING;
                    } else {
                        mMediaPlayer.seekTo(mCurrentPosition);
                        mState = PlaybackState.STATE_BUFFERING;
                    }
                }
                mPlayOnFocusGain = false;
            } else {
                mState = PlaybackState.STATE_PAUSED;
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
            if (mState == PlaybackState.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            Timber.d("onAudioFocusChange: Ignoring unsupported focusChange: ", focusChange);
        }
        configMediaPlayerState();
    }

    /**
     * Called when MediaPlayer has completed a seek
     *
     * @see android.media.MediaPlayer.OnSeekCompleteListener
     */
    @Override
    public void onSeekComplete(IMediaPlayer player) {
        Timber.d("onSeekComplete from MediaPlayer: %s", player.getCurrentPosition());
        if (player == mMediaPlayer) {
            mCurrentPosition = player.getCurrentPosition();
            if (mState == PlaybackState.STATE_BUFFERING) {
                mMediaPlayer.start();
                mState = PlaybackState.STATE_PLAYING;
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
    public void onCompletion(IMediaPlayer player) {
        Timber.d("onCompletion from MediaPlayer");
        if (player == mMediaPlayer) {
            if (hasNext()) {
                goToNext();
            } else {
                // The media player finished playing the current song
                stop(false);
                mCurrentPosition = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
            }
        }
        if (mCallback != null) {
            mCallback.onCompletion();
        }
    }

    /**
     * Called when media player is done preparing.
     *
     * @see android.media.MediaPlayer.OnPreparedListener
     */
    @Override
    public void onPrepared(IMediaPlayer player) {
        Timber.d("onPrepared from MediaPlayer");
        if (player == mMediaPlayer) {
            // The media player is done preparing. That means we can start playing if we
            // have audio focus.
            configMediaPlayerState();
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
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        Timber.d("Media player error: what=" + what + ", extra=" + extra);
        stop(false);
        if (mCallback != null) {
            mCallback.onError("MediaPlayer error " + what + " (" + extra + ")");
        }
        return true; // true indicates we handled the error
    }

    /**
     * Releases resources used by the service for playback.
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        Timber.d("relaxResources. releaseMediaPlayer=%s", releaseMediaPlayer);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (releaseMediaPlayer && mNextMediaPlayer != null) {
            prepareForNextTrack();
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
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

    }

}
