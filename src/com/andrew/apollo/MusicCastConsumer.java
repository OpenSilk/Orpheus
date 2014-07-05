/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrew.apollo;

import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.ConnectionResult;

import org.opensilk.cast.callbacks.IMediaCastConsumer;
import org.opensilk.cast.callbacks.MediaCastConsumerImpl;
import org.opensilk.cast.exceptions.NoConnectionException;
import org.opensilk.cast.exceptions.TransientNetworkDisconnectionException;
import org.opensilk.cast.manager.BaseCastManager;
import org.opensilk.cast.manager.MediaCastManager;
import org.opensilk.cast.util.Utils;
import org.opensilk.music.cast.CastUtils;

/**
 * Created by drew on 7/4/14.
 */
public class MusicCastConsumer extends MediaCastConsumerImpl {
    private static final String TAG = "MusicCastConsumer";
    private static final boolean D = true;

    private MusicPlaybackService mService;
    private MediaCastManager mCastManager;

    public MusicCastConsumer(MusicPlaybackService service, MediaCastManager manager) {
        mService = service;
        mCastManager= manager;
    }

    /**
     * If we had changed the volume previously restore its value
     * This is mostly because the cast device keeps getting reset
     * to max volume on every reconnect
     */
    private void restoreRemoteVolumeLevel() {
        try {
            double curVol = mCastManager.getVolume();
            float oldVol = Utils.getFloatFromPreference(mService, BaseCastManager.PREFS_KEY_REMOTE_VOLUME);
            if (oldVol == Float.MIN_VALUE) return; //No preference
            if (oldVol != curVol) {
                mCastManager.setVolume(oldVol);
            }
        } catch (Exception ignored) {
        }
    }



    /**
     * Called when we disconnect or think we are disconnectetd from the cast device
     */
//    private void restoreLocalState() {
//        if (mPlaybackLocation == PlaybackLocation.LOCAL) {
//            return;//Connect failed
//        }
//        updatePlaybackLocation(PlaybackLocation.LOCAL);
//        stopCastServer();
//        //Local should already be paused
//        //pause();
//        synchronized (this) {
//            //Do some stuff pause() does to notify the activity
//            scheduleDelayedShutdown();
//            mIsSupposedToBePlaying = false;
//            notifyChange(PLAYSTATE_CHANGED);
//        }
//    }

    @Override
    //@DebugLog
    public void onConnected() {
        // Nothing i can think of to do here, we are still waiting for the app to connect
    }

    @Override
    //@DebugLog
    public void onDisconnected() {
        mService.switchToLocalPlayer();;
    }

    @Override
    //@DebugLog
    public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
        mService.switchToCastPlayer();
        restoreRemoteVolumeLevel();
    }

    @Override
    //@DebugLog
    public boolean onApplicationConnectionFailed(int errorCode) {
        //Nothing for us to do, the user must manually try again
        return false;
    }

    @Override
    //@DebugLog
    public void onApplicationDisconnected(int errorCode) {
        mService.switchToLocalPlayer();;
    }

    /** Called when stopApplication() succeeds*/
    @Override
    //@DebugLog
    public void onApplicationStopped() {
        mCastManager.onDeviceSelected(null);
    }

    /** Called when stopApplication() fails */
    @Override
    //@DebugLog
    public void onApplicationStopFailed(int errorCode) {
        // As far as the activity is concered we are disconnected;
        mCastManager.onDeviceSelected(null);
    }

    @Override
    //@DebugLog
    public void onApplicationStatusChanged(String appStatus) {

    }

    @Override
    //@DebugLog
    public void onConnectionSuspended(int cause) {
        //restoreLocalState();
    }

    @Override
    //@DebugLog
    public void onConnectivityRecovered() {
        mService.recoverFromTransientDisconnect();

    }

    @Override
    //@DebugLog
    public void onVolumeChanged(double value, boolean isMute) {

    }

    @Override
    //@DebugLog
    public void onRemoteMediaPlayerMetadataUpdated() {
        try {
            MediaInfo mediaInfo = mCastManager.getRemoteMediaInformation();
            //TODO update lockscreen
        } catch (TransientNetworkDisconnectionException e) {
            Log.e(TAG, "Failed to update lock screen metadaa due to a network issue", e);
        } catch (NoConnectionException e) {
            Log.e(TAG, "Failed to update lock screen metadaa due to a network issue", e);
        }
    }

    @Override
    //@DebugLog
    public void onRemoteMediaPlayerStatusUpdated() {
        MediaStatus status = mCastManager.getRemoteMediaPlayer().getMediaStatus();
        int mState = status.getPlayerState();
        int mIdleReason = status.getIdleReason();
        if (mState == MediaStatus.PLAYER_STATE_PLAYING) {
            if (D) Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = playing");
            //HACK
            if (!mService.isSupposedToBePlaying()) {
                mService.setSupposedToBePlaying(true);
                mService.notifyChange(MusicPlaybackService.PLAYSTATE_CHANGED);
            }
        } else if (mState == MediaStatus.PLAYER_STATE_PAUSED) {
            if (D) Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = paused");
        } else if (mState == MediaStatus.PLAYER_STATE_IDLE) {
            if (D) Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = idle");
            if (mIdleReason == MediaStatus.IDLE_REASON_CANCELED) {
                if (D) Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): IDLE reason = CANCELLED");
                //TODO
            }
        } else if (mState == MediaStatus.PLAYER_STATE_BUFFERING) {
            if (D) Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = buffering");
        } else {
            if (D) Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = unknown");
        }
    }

    @Override
    //@DebugLog
    public boolean onConnectionFailed(ConnectionResult result) {
        return false;
    }

    @Override
    //@DebugLog
    public void onCastDeviceDetected(MediaRouter.RouteInfo info) {

    }

    @Override
    //@DebugLog
    public void onFailed(int resourceId, int statusCode) {
        Log.e(TAG, "onFailed " + mService.getString(resourceId));
        switch (resourceId) {
            //load
            case R.string.failed_load:
                mService.pause();
                break;
            //onRemoteMediaPlayerStatusUpdated
            case R.string.failed_receiver_player_error:
                mService.gotoNext(true);
                break;
            //onApplicationConnected;
            case R.string.failed_no_connection_trans:
            case R.string.failed_no_connection:
            case R.string.failed_status_request:
                break;
            //setVolume
            case R.string.failed_setting_volume:
                break;
            //seek
            case R.string.failed_seek:
                break;
            //pause
            case R.string.failed_to_pause:
                break;
            //stop
            case R.string.failed_to_stop:
                break;
            //play
            case R.string.failed_to_play:
                if (mService.isSupposedToBePlaying()) {
                    mService.setSupposedToBePlaying(false);
                    mService.notifyChange(MusicPlaybackService.PLAYSTATE_CHANGED);
                }
                break;
        }
    }
}
