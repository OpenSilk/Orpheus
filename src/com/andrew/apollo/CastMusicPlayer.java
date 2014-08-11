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

import android.database.Cursor;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.ResultCallback;

import org.opensilk.cast.callbacks.IMediaCastConsumer;
import org.opensilk.cast.callbacks.MediaCastConsumerImpl;
import org.opensilk.cast.exceptions.CastException;
import org.opensilk.cast.exceptions.NoConnectionException;
import org.opensilk.cast.exceptions.TransientNetworkDisconnectionException;
import org.opensilk.cast.manager.BaseCastManager;
import org.opensilk.cast.manager.MediaCastManager;
import org.opensilk.cast.util.CastPreferences;
import org.opensilk.music.cast.CastUtils;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 7/4/14.
 */
public class CastMusicPlayer implements IMusicPlayer {
    private static final String TAG = "CastMusicPlayre";

    private MusicPlaybackService mService;
    private MediaCastManager mCastManager;

    private Handler mHandler;

    private volatile MediaInfo mCurrentMediaInfo;
    private volatile MediaInfo mNextMediaInfo;
    private boolean mMarkforLoad;
    private boolean mIsLoading;

    public CastMusicPlayer(MusicPlaybackService service, MediaCastManager manager) {
        mService = service;
        mCastManager = manager;
        mCastManager.addCastConsumer(mCastConsumer);
    }

    @Override
    public long seek(long position) {
        try {
            mCastManager.seek((int)position);
            return position;
        } catch (TransientNetworkDisconnectionException e) {
            Log.w(TAG, "seek(1) TransientNetworkDisconnection");
        } catch (NoConnectionException e) {
            handleCastError(e);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public long position() {
        try {
            return (long) mCastManager.getCurrentMediaPosition();
        } catch (TransientNetworkDisconnectionException e) {
            Log.w(TAG, "position(1) TransientNetworkDisconnection");
        } catch (NoConnectionException e) {
            handleCastError(e);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public long duration() {
        try {
            return (long) mCastManager.getMediaDuration();
        } catch (TransientNetworkDisconnectionException e) {
            Log.w(TAG, "duration(1) TransientNetworkDisconnection");
        } catch (NoConnectionException e) {
            handleCastError(e);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void play() {
        try {
            if (mMarkforLoad) {
                loadRemoteCurrent(0);
            } else if (mCastManager.isRemoteMediaLoaded()) {
                mCastManager.play();
            } else {
                mCurrentMediaInfo = null; //TODO
            }
        } catch (TransientNetworkDisconnectionException e) {
            Log.w(TAG, "play(1) TransientNetworkDisconnection");
        } catch (NoConnectionException|CastException e) {
            handleCastError(e);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {
        try {
            mCastManager.pause();
        } catch (CastException |NoConnectionException e) {
            handleCastError(e);
        } catch (TransientNetworkDisconnectionException e) {
            Log.w(TAG, "pause(1) TransientNetworkDisconnection");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canGoNext() {
        return !mIsLoading;
    }

    @Override
    //@DebugLog
    public boolean isInitialized() {
        return mCurrentMediaInfo != null;
    }

    @Override
    public void stop(boolean goToIdle) {
        try {
            if (goToIdle) {
                mCastManager.stop();
            } else {
                mCastManager.pause();
            }
        } catch (CastException|NoConnectionException e) {
            handleCastError(e);
        } catch (TransientNetworkDisconnectionException e) {
            Log.w(TAG, "stop(1) TransientNetworkDisconnection");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        mCurrentMediaInfo = null;
        mNextMediaInfo = null;
    }

    @Override
    public void setNextDataSource(long songId) {
        mNextMediaInfo = CastUtils.buildMediaInfo(mService, songId);
    }

    @Override
    public void setDataSource(Cursor cursor) {
        mCurrentMediaInfo = CastUtils.buildMediaInfo(mService, cursor);
        mMarkforLoad = true;
    }

    @Override
    public void setDataSource(String path) {
        mCurrentMediaInfo = null;
    }

    public long seekAndPlay(long position) {
        if (mMarkforLoad) {
            loadRemoteCurrent(position);
        } else {
            try {
                mCastManager.seekAndPlay((int)position);
            } catch (TransientNetworkDisconnectionException e) {
                Log.w(TAG, "seekAndPlay(1) TransientNetworkDisconnection");
            } catch (NoConnectionException e) {
                handleCastError(e);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return position; //TODO return real error
    }

    @Override
    public boolean canGoPrev() {
        return !mIsLoading;
    }

    @Override
    public void setNextDataSource(String path) {
        mNextMediaInfo = null;
    }

    @Override
    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void setVolume(float volume) {
        try {
            mCastManager.setVolume(volume);
        } catch (CastException|NoConnectionException e) {
            //ignore
        } catch (TransientNetworkDisconnectionException e) {
            Log.w(TAG, "setVolume(1) TransientNetworkDisconnection");
        }
    }

    @Override
    public float getMaxVolume() {
        // TODO find reasonable way to cache this value
        return CastPreferences.getFloat(mService, CastPreferences.KEY_REMOTE_VOLUME, 1.0f);
    }

    public boolean isConnected() {
        try {
            mCastManager.checkConnectivity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Initiates remote playback for current track
     */
    //@DebugLog
    private void loadRemoteCurrent(long position) {
        if (mCurrentMediaInfo == null) {
            Log.w(TAG, "Tried to load null media");
            return;
        }
        loadRemote(mCurrentMediaInfo, true, (int) position);
        mMarkforLoad = false;
    }

    /**
     * Initiates remote playback for next track
     */
    //@DebugLog
    private void loadRemoteNext() {
        if (mNextMediaInfo == null) {
            Timber.w("Tried to load null media");
            return;
        }
        loadRemote(mNextMediaInfo, true, 0);
                // Force the local player onto the next track
//                mPlayer.onCompletion(mPlayer.getCurrentPlayer());
        mHandler.sendEmptyMessage(MusicPlayerHandler.TRACK_WENT_TO_NEXT);
    }

    //@DebugLog
    private void loadRemote(MediaInfo info, boolean autoplay, int startPos) {
        try {
            mIsLoading = true;
            mCastManager.loadMedia(info, autoplay, startPos, null,
                    new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                            mIsLoading = false;
                            if (!result.getStatus().isSuccess()) {
                                mCastManager.onFailed(R.string.failed_load, result.getStatus().getStatusCode());
                                mCurrentMediaInfo = null;
                            } else {

                            }
                        }
                    });
        } catch (TransientNetworkDisconnectionException e) {
            Log.w(TAG, "loadRemote(1) TransientNetworkDisconnection");
        } catch (NoConnectionException e) {
            e.printStackTrace();
        }
    }

    public void release() {
        mCastManager.removeCastConsumer(mCastConsumer);
    }

    /**
     * Handles CastError or NoConnectionException
     * only called if the service thinks its still connected but isn't
     * eg if we were in transient disconnect and the framework failed
     * to notify after giving up on reconnecting
     */
    private void handleCastError(Throwable c) {
        Log.w(TAG, "Disconnected from cast device " + c.getClass().getName());
        // Will initiate reset of mediarouter and restore local state
        mCastManager.onDeviceSelected(null);
        mCurrentMediaInfo = null;
        mNextMediaInfo = null;
    }

    private final IMediaCastConsumer mCastConsumer = new MediaCastConsumerImpl() {
        @Override
        public void onRemoteMediaPlayerStatusUpdated() {
            MediaStatus status = mCastManager.getRemoteMediaPlayer().getMediaStatus();
            int mState = status.getPlayerState();
            int mIdleReason = status.getIdleReason();
            switch (mState) {
                case MediaStatus.PLAYER_STATE_IDLE:
                    if (mIdleReason == MediaStatus.IDLE_REASON_FINISHED) {
                        loadRemoteNext();
                    } else if (mIdleReason == MediaStatus.IDLE_REASON_ERROR) {
                        // something bad happened on the cast device
                        Log.e(TAG, "onRemoteMediaPlayerStatusUpdated(): IDLE reason = ERROR");
                        mCastManager.onFailed(R.string.failed_receiver_player_error, -1);
                    }
                    break;
            }
        }
    };

}
