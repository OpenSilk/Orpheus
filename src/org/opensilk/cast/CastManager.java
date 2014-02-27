/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
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

package org.opensilk.cast;

import static org.opensilk.cast.util.LogUtils.LOGD;
import static org.opensilk.cast.util.LogUtils.LOGE;

import android.content.Context;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.text.TextUtils;

import com.andrew.apollo.R;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.CastOptions.Builder;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.opensilk.cast.callbacks.IVideoCastConsumer;
import org.opensilk.cast.callbacks.VideoCastConsumerImpl;
import org.opensilk.cast.exceptions.CastException;
import org.opensilk.cast.exceptions.NoConnectionException;
import org.opensilk.cast.exceptions.OnFailedListener;
import org.opensilk.cast.exceptions.TransientNetworkDisconnectionException;
import org.opensilk.cast.util.LogUtils;

import org.json.JSONObject;
import org.opensilk.cast.util.Utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A concrete subclass of {@link org.opensilk.cast.BaseCastManager} that is suitable for casting video contents (it
 * also provides a single custom data channel/namespace if an out-of-bound communication is needed).
 * <p>
 * This is a singleton that needs to be "initialized" (by calling <code>initialize()</code>) prior
 * to usage. Subsequent to initialization, an easier way to get access to the singleton class is to
 * call a variant of <code>getInstance()</code>. After initialization, callers can enable a number
 * of features (all features are off by default). To do so, call <code>enableFeature()</code> and
 * pass an OR-ed expression built from one ore more of the following constants:
 * <p>
 * <ul>
 * <li>FEATURE_DEBUGGING: to enable GMS level logging</li>
 * <li>FEATURE_NOTIFICATION: to enable system notifications</li>
 * <li>FEATURE_LOCKSCREEN: to enable lock-screen controls on supported versions</li>
 * </ul>
 * Callers can add {@link MiniController} components to their application pages by adding the
 * corresponding widget to their layout xml and then calling <code>addMiniController()</code>. This
 * class manages various states of the remote cast device.Client applications, however, can
 * complement the default behavior of this class by hooking into various callbacks that it provides
 * (see {@link IVideoCastConsumer}). Since the number of these callbacks is usually much larger than
 * what a single application might be interested in, there is a no-op implementation of this
 * interface (see {@link VideoCastConsumerImpl}) that applications can subclass to override only
 * those methods that they are interested in. Since this library depends on the cast functionalities
 * provided by the Google Play services, the library checks to ensure that the right version of that
 * service is installed. It also provides a simple static method
 * <code>checkGooglePlaySevices()</code> that clients can call at an early stage of their
 * applications to provide a dialog for users if they need to update/activate their GMS library. To
 * learn more about this library, please read the documentation that is distributed as part of this
 * library.
 */
public class CastManager extends BaseCastManager
        implements OnFailedListener {

    /**
     * Volume can be controlled at two different layers, one is at the "stream" level and one at the
     * "device" level. <code>VolumeType</code> encapsulates these two types.
     */
    public static enum VolumeType {
        STREAM,
        DEVICE;
    }

    private static final String TAG = LogUtils.makeLogTag(CastManager.class);
    private static CastManager sInstance;
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private VolumeType mVolumeType = VolumeType.DEVICE;
    private int mState = MediaStatus.PLAYER_STATE_IDLE;
    private int mIdleReason;
    private final String mDataNamespace;
    private Cast.MessageReceivedCallback mDataChannel;
    protected Set<IVideoCastConsumer> mVideoConsumers;

    /**
     * Initializes the VideoCastManager for clients. Before clients can use VideoCastManager, they
     * need to initialize it by calling this static method. Then clients can obtain an instance of
     * this singleton class by calling {@link getInstance()} or {@link getInstance(Context)}.
     * Failing to initialize this class before requesting an instance will result in a
     * {@link CastException} exception.
     *
     * @see getInstance()
     * @param context
     * @param applicationId the unique ID for your application
     * @param targetActivity this points to the activity that should be invoked when user clicks on
     *            the icon in the {@link MiniController}. Often this is the activity that hosts the
     *            local player.
     * @param dataNamespace if not <code>null</code>, a custom data channel with this namespace will
     *            be created.
     * @return
     */
    public static synchronized CastManager initialize(Context context,
            String applicationId, Class<?> targetActivity, String dataNamespace) {
        if (null == sInstance) {
            LOGD(TAG, "New instance of VideoCastManager is created");
            if (ConnectionResult.SUCCESS != GooglePlayServicesUtil
                    .isGooglePlayServicesAvailable(context)) {
                String msg = "Couldn't find the appropriate version of Goolge Play Services";
                LOGE(TAG, msg);
            }
            sInstance = new CastManager(context, applicationId, targetActivity, dataNamespace);
            mCastManager = sInstance;
        }
        return sInstance;
    }

    /**
     * Returns the initialized instances of this class. If it is not initialized yet, a
     * {@link CastException} will be thrown.
     *
     * @see initialze()
     * @return
     * @throws CastException
     */
    public static CastManager getInstance() throws CastException {
        if (null == sInstance) {
            LOGE(TAG, "No VideoCastManager instance was built, you need to build one first");
            throw new CastException();
        }
        return sInstance;
    }

    /**
     * Returns the initialized instances of this class. If it is not initialized yet, a
     * {@link CastException} will be thrown. The {@link Context} that is passed as the argument will
     * be used to update the context for the <code>VideoCastManager
     * </code> instance. The main purpose of updating context is to enable the library to provide
     * {@link Context} related functionalities, e.g. it can create an error dialog if needed. This
     * method is preferred over the similar one without a context argument.
     *
     * @see {@link initialize()}, {@link setContext()}
     * @param context the current Context
     * @return
     * @throws CastException
     */
    public static CastManager getInstance(Context context) throws CastException {
        if (null == sInstance) {
            LOGE(TAG, "No VideoCastManager instance was built, you need to build one first");
            throw new CastException();
        }
        LOGD(TAG, "Updated context to: " + context.getClass().getName());
        sInstance.mContext = context;
        return sInstance;
    }

    private CastManager(Context context, String applicationId, Class<?> targetActivity,
                        String dataNamespace) {
        super(context, applicationId);
        LOGD(TAG, "VideoCastManager is instantiated");
        mVideoConsumers = new HashSet<IVideoCastConsumer>();
        mDataNamespace = dataNamespace;
    }

    /*************************************************************************/
    /************** Utility Methods ******************************************/
    /*************************************************************************/

    /**
     * Returns the active {@link RemoteMediaPlayer} instance. Since there are a number of media
     * control APIs that this library do not provide a wrapper for, client applications can call
     * those methods directly after obtaining an instance of the active {@link RemoteMediaPlayer}.
     *
     * @return
     */
    public final RemoteMediaPlayer getRemoteMediaPlayer() {
        return mRemoteMediaPlayer;
    }

    /*
     * A simple check to make sure mRemoteMediaPlayer is not null
     */
    private void checkRemoteMediaPlayerAvailable() throws NoConnectionException {
        if (null == mRemoteMediaPlayer) {
            throw new NoConnectionException();
        }
    }

    /**
     * Sets the type of volume.
     *
     * @param vType
     */
    public final void setVolumeType(VolumeType vType) {
        mVolumeType = vType;
    }

    /**
     * Returns the url for the movie that is currently playing on the remote device. If there is no
     * connection, this will return <code>null</code>.
     *
     * @return
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     *             possibly transient loss of network
     */
    public String getRemoteMovieUrl() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        if (null != mRemoteMediaPlayer && null != mRemoteMediaPlayer.getMediaInfo()) {
            MediaInfo info = mRemoteMediaPlayer.getMediaInfo();
            mRemoteMediaPlayer.getMediaStatus().getPlayerState();
            return info.getContentId();
        } else {
            throw new NoConnectionException();
        }
    }

    /**
     * Indicates if the remote movie is currently playing (or buffering).
     *
     * @return
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public boolean isRemoteMoviePlaying() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        return mState == MediaStatus.PLAYER_STATE_BUFFERING
                || mState == MediaStatus.PLAYER_STATE_PLAYING;

    }

    /**
     * Returns <code>true</code> if the remote connected device is playing a movie.
     *
     * @return
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public boolean isRemoteMoviePaused() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        return mState == MediaStatus.PLAYER_STATE_PAUSED;
    }

    /**
     * Returns <code>true</code> only if there is a media on the remote being played, paused or
     * buffered.
     *
     * @return
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public boolean isRemoteMediaLoaded() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        return isRemoteMoviePaused() || isRemoteMoviePlaying();
    }

    /**
     * Returns the {@link MediaInfo} for the current media
     *
     * @return
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     *             possibly transient loss of network
     */
    public MediaInfo getRemoteMediaInformation() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        checkRemoteMediaPlayerAvailable();
        return mRemoteMediaPlayer.getMediaInfo();
    }

    /**
     * Gets the remote's system volume. If no device is connected to, or if an exception is thrown,
     * this returns -1. It internally detects what type of volume is used.
     *
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     *             possibly transient loss of network
     */
    public double getVolume() throws TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        if (mVolumeType == VolumeType.STREAM) {
            checkRemoteMediaPlayerAvailable();
            return mRemoteMediaPlayer.getMediaStatus().getStreamVolume();
        } else {
            return Cast.CastApi.getVolume(mApiClient);
        }
    }

    /**
     * Sets the volume. It internally determines if this should be done for <code>stream</code> or
     * <code>device</code> volume.
     *
     * @param volume Should be a value between 0 and 1, inclusive.
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     * @throws CastException If setting system volume fails
     */
    public void setVolume(double volume) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        if (volume > 1.0) {
            volume = 1.0;
        } else if (volume < 0) {
            volume = 0.0;
        }
        // Save our new volume so we can restore it later
        Utils.saveFloatToPreference(mContext, PREFS_KEY_REMOTE_VOLUME, (float) volume);
        if (mVolumeType == VolumeType.STREAM) {
            checkRemoteMediaPlayerAvailable();
            mRemoteMediaPlayer.setStreamVolume(mApiClient, volume).setResultCallback(
                    new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {

                        @Override
                        public void onResult(MediaChannelResult result) {
                            if (!result.getStatus().isSuccess()) {
                                onFailed(R.string.failed_setting_volume,
                                        result.getStatus().getStatusCode());
                            }
                        }
                    });
        } else {
            try {
                Cast.CastApi.setVolume(mApiClient, volume);
            } catch (IOException e) {
                throw new CastException(e);
            } catch (IllegalStateException e) {
                throw new CastException(e);
            } catch (IllegalArgumentException e) {
                throw new CastException(e);
            }
        }
    }

    /**
     * Increments (or decrements) the volume by the given amount. It internally determines if this
     * should be done for <code>stream</code> or <code>device</code> volume.
     *
     * @param delta
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     *             possibly transient loss of network
     * @throws CastException
     */
    public void incrementVolume(double delta) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        double vol = getVolume() + delta;
        if (vol > 1) {
            vol = 1;
        } else if (vol < 0) {
            vol = 0;
        }
        setVolume(vol);
    }

    /**
     * Increments or decrements volume by <code>delta</code> if <code>delta &gt; 0</code> or
     * <code>delta &lt; 0</code>, respectively. Note that the volume range is between 0 and {@link
     * RouteInfo.getVolumeMax()}.
     *
     * @param delta
     */
    public void updateVolume(int delta) {
        if (null != mMediaRouter.getSelectedRoute()) {
            RouteInfo info = mMediaRouter.getSelectedRoute();
            info.requestUpdateVolume(delta);
        }
    }

    /**
     * Returns <code>true</code> if remote device is muted. It internally determines if this should
     * be done for <code>stream</code> or <code>device</code> volume.
     *
     * @return
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public boolean isMute() throws TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        if (mVolumeType == VolumeType.STREAM) {
            checkRemoteMediaPlayerAvailable();
            return mRemoteMediaPlayer.getMediaStatus().isMute();
        } else {
            return Cast.CastApi.isMute(mApiClient);
        }
    }

    /**
     * Mutes or un-mutes the volume. It internally determines if this should be done for
     * <code>stream</code> or <code>device</code> volume.
     *
     * @param mute
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void setMute(boolean mute) throws CastException, TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        if (mVolumeType == VolumeType.STREAM) {
            checkRemoteMediaPlayerAvailable();
            mRemoteMediaPlayer.setStreamMute(mApiClient, mute);
        } else {
            try {
                Cast.CastApi.setMute(mApiClient, mute);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CastException("Failed to set volume", e);
            }
        }
    }

    /**
     * Returns the duration of the media that is loaded, in seconds. If there is no connection or if
     * there is no channel established, this method returns -1.
     *
     * @return
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public double getMediaDuration() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        checkRemoteMediaPlayerAvailable();
        return mRemoteMediaPlayer.getStreamDuration();
    }

    /**
     * Returns the current (approximate) position of the current media, in seconds. If there is no
     * channel established, this method returns -1.
     *
     * @return
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public double getCurrentMediaPosition() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        checkRemoteMediaPlayerAvailable();
        return mRemoteMediaPlayer.getApproximateStreamPosition();
    }

    /*
     * This is called when ui visibility of the client has changed
     */
    @Override
    protected void onUiVisibilityChanged(boolean visible) {
        super.onUiVisibilityChanged(visible);

    }

    /************************************************************/
    /*********** Implementing Cast.Listener *********************/
    /************************************************************/

    private void onApplicationDisconnected(int errorCode) {
        LOGD(TAG, "onApplicationDisconnected() reached with error code: " + errorCode);
        for (IVideoCastConsumer consumer : mVideoConsumers) {
            try {
                consumer.onApplicationDisconnected(errorCode);
            } catch (Exception e) {
                LOGE(TAG, "onApplicationDisconnected(): Failed to inform " + consumer, e);
            }
        }
        int ii = mListeners.beginBroadcast();
        while (ii-->0) {
            try {
                mListeners.getBroadcastItem(ii).onApplicationDisconnected(errorCode);
            } catch (RemoteException e) {
                LOGE(TAG, "onApplicationDisconnected(): ", e);
            }
        }
        mListeners.finishBroadcast();
        if (null != mMediaRouter) {
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
        selectDevice(null);
    }

    private void onApplicationStatusChanged() {
        String appStatus = null;
        if (!isConnected()) {
            return;
        }
        try {
            appStatus = Cast.CastApi.getApplicationStatus(mApiClient);
            LOGD(TAG, "onApplicationStatusChanged() reached: "
                    + Cast.CastApi.getApplicationStatus(mApiClient));

            for (IVideoCastConsumer consumer : mVideoConsumers) {
                try {
                    consumer.onApplicationStatusChanged(appStatus);
                } catch (Exception e) {
                    LOGE(TAG, "onApplicationStatusChanged(): Failed to inform " + consumer, e);
                }
            }
        } catch (IllegalStateException e1) {
            // no use in logging this
        }
    }

    private void onVolumeChanged() {
        LOGD(TAG, "onVolumeChanged() reached");
        double volume = 0;
        try {
            volume = getVolume();
            boolean isMute = isMute();
            for (IVideoCastConsumer consumer : mVideoConsumers) {
                try {
                    consumer.onVolumeChanged(volume, isMute);
                } catch (Exception e) {
                    LOGE(TAG, "onVolumeChanged(): Failed to inform " + consumer, e);
                }
            }
        } catch (Exception e1) {
            LOGE(TAG, "Failed to get volume", e1);
        }

    }

    @Override
    void onApplicationConnected(ApplicationMetadata appMetadata,
            String applicationStatus, String sessionId, boolean wasLaunched) {
        LOGD(TAG, "onApplicationConnected() reached with sessionId: " + sessionId
                + ", and mReconnectionStatus=" + mReconnectionStatus);

        if (mReconnectionStatus == ReconnectionStatus.IN_PROGRESS) {
            // we have tried to reconnect and successfully launched the app, so
            // it is time to select the route and make the cast icon happy :-)
            List<RouteInfo> routes = mMediaRouter.getRoutes();
            if (null != routes) {
                String routeId = Utils.getStringFromPreference(mContext, PREFS_KEY_ROUTE_ID);
                for (RouteInfo routeInfo : routes) {
                    if (routeId.equals(routeInfo.getId())) {
                        // found the right route
                        LOGD(TAG, "Found the correct route during reconnection attempt");
                        mReconnectionStatus = ReconnectionStatus.FINALIZE;
                        mMediaRouter.selectRoute(routeInfo);
                        break;
                    }
                }
            }
        }
        try {
            attachDataChannel();
            attachMediaChannel();
            // saving device for future retrieval; we only save the last session info
            Utils.saveStringToPreference(mContext, PREFS_KEY_SESSION_ID, sessionId);
            mRemoteMediaPlayer.requestStatus(mApiClient).
                    setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {

                        @Override
                        public void onResult(MediaChannelResult result) {
                            if (!result.getStatus().isSuccess()) {
                                onFailed(R.string.failed_status_request,
                                        result.getStatus().getStatusCode());
                            }

                        }
                    });
            for (IVideoCastConsumer consumer : mVideoConsumers) {
                try {
                    consumer.onApplicationConnected(appMetadata, sessionId, wasLaunched);
                } catch (Exception e) {
                    LOGE(TAG, "onApplicationConnected(): Failed to inform " + consumer, e);
                }
            }
        } catch (TransientNetworkDisconnectionException e) {
            LOGE(TAG, "Failed to attach media/data channel due to network issues", e);
            onFailed(R.string.failed_no_connection_trans, NO_STATUS_CODE);
        } catch (NoConnectionException e) {
            LOGE(TAG, "Failed to attach media/data channel due to network issues", e);
            onFailed(R.string.failed_no_connection, NO_STATUS_CODE);
        }

    }

    /*
     * (non-Javadoc)
     * @see com.google.sample.castcompanionlibrary.cast.BaseCastManager# onConnectivityRecovered()
     */
    @Override
    public void onConnectivityRecovered() {
        reattachMediaChannel();
        super.onConnectivityRecovered();
    }

    /*
     * called when we sucessfully stopped application
     */
    @Override
    void onApplicationStopped() {
        for (IVideoCastConsumer consumer : mVideoConsumers) {
            try {
                consumer.onApplicationStopped();
            } catch (Exception e) {
                LOGE(TAG, "onApplicationLaunched(): Failed to inform " + consumer, e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.gms.cast.CastClient.Listener#onApplicationStopFailed (int)
     */
    @Override
    public void onApplicationStopFailed(int errorCode) {
        for (IVideoCastConsumer consumer : mVideoConsumers) {
            try {
                consumer.onApplicationStopFailed(errorCode);
            } catch (Exception e) {
                LOGE(TAG, "onApplicationLaunched(): Failed to inform " + consumer, e);
            }
        }
    }

    @Override
    public void onApplicationConnectionFailed(int errorCode) {
        LOGD(TAG, "onApplicationConnectionFailed() reached with errorCode: " + errorCode);
        if (mReconnectionStatus == ReconnectionStatus.IN_PROGRESS) {
            if (errorCode == CastStatusCodes.APPLICATION_NOT_RUNNING) {
                // while trying to re-establish session, we
                // found out that the app is not running so we need
                // to disconnect
                mReconnectionStatus = ReconnectionStatus.INACTIVE;
                selectDevice(null);
            }
            return;
        } else {
            for (IVideoCastConsumer consumer : mVideoConsumers) {
                try {
                    consumer.onApplicationConnectionFailed(errorCode);
                } catch (Exception e) {
                    LOGE(TAG, "onApplicationLaunchFailed(): Failed to inform " + consumer, e);
                }
            }
            int ii = mListeners.beginBroadcast();
            while (ii-->0) {
                try {
                    mListeners.getBroadcastItem(ii).onApplicationConnectionFailed(errorCode);
                } catch (RemoteException e) {
                    LOGE(TAG, "onApplicationLaunchFailed(): ", e);
                }
            }
            mListeners.finishBroadcast();
            selectDevice(null);
            if (null != mMediaRouter) {
                mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            }
        }
    }

    /*************************************************************************/
    /************** Playback methods *****************************************/
    /*************************************************************************/

    /**
     * Loads a media. For this to succeed, you need to have successfully launched the application.
     *
     * @param media
     * @param autoPlay If <code>true</code>, playback starts after load
     * @param position Where to start the playback (only used if autoPlay is <code>true</code>.
     *            Units is milliseconds.
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void loadMedia(MediaInfo media, boolean autoPlay, int position)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        loadMedia(media, autoPlay, position, null);
    }

    /**
     * Loads a media. For this to succeed, you need to have successfully launched the application.
     *
     * @param media
     * @param autoPlay If <code>true</code>, playback starts after load
     * @param position Where to start the playback (only used if autoPlay is <code>true</code>.
     *            Units is milliseconds.
     * @param customData Optional JSONObject data to be passed to the cast device
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void loadMedia(MediaInfo media, boolean autoPlay, int position, JSONObject customData)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        LOGD(TAG, "loadMedia: " + media);
        checkConnectivity();
        if (media == null) {
            return;
        }
        if (mRemoteMediaPlayer == null) {
            LOGE(TAG, "Trying to load a video with no active media session");
            throw new NoConnectionException();
        }

        mRemoteMediaPlayer.load(mApiClient, media, autoPlay, position, customData)
                .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {

                    @Override
                    public void onResult(MediaChannelResult result) {
                        if (!result.getStatus().isSuccess()) {
                            onFailed(R.string.failed_load, result.getStatus().getStatusCode());
                        }

                    }
                });
    }

    /**
     * Plays the loaded media.
     *
     * @param position Where to start the playback. Units is milliseconds.
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void play(int position) throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        LOGD(TAG, "attempting to play media at position " + position + " seconds");
        if (mRemoteMediaPlayer == null) {
            LOGE(TAG, "Trying to play a video with no active media session");
            throw new NoConnectionException();
        }
        seekAndPlay(position);
    }

    /**
     * Resumes the playback from where it was left (can be the beginning).
     *
     * @param customData Optional JSONObject data to be passed to the cast device
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void play(JSONObject customData) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        LOGD(TAG, "play()");
        checkConnectivity();
        try {
            if (mRemoteMediaPlayer == null) {
                LOGE(TAG, "Trying to play a video with no active media session");
                throw new NoConnectionException();
            }
            mRemoteMediaPlayer.play(mApiClient);
        } catch (IOException e) {
            LOGE(TAG, "Failed to play media", e);
            throw new CastException(mContext.getString(R.string.failed_to_play), e);
        }
    }

    /**
     * Resumes the playback from where it was left (can be the beginning).
     *
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void play() throws CastException, TransientNetworkDisconnectionException,
            NoConnectionException {
        play(null);
    }

    /**
     * Pauses the playback.
     *
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void pause() throws CastException, TransientNetworkDisconnectionException,
            NoConnectionException {
        pause(null);
    }

    /**
     * Pauses the playback.
     *
     * @param customData Optional JSONObject data to be passed to the cast device
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void pause(JSONObject customData) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        LOGD(TAG, "attempting to pause media");
        checkConnectivity();
        if (mRemoteMediaPlayer == null) {
            LOGE(TAG, "Trying to pause a video with no active media session");
            throw new NoConnectionException();
        }
        try {
            mRemoteMediaPlayer.pause(mApiClient, customData);
        } catch (IOException e) {
            LOGE(TAG, "Failed to pause media", e);
            throw new CastException(mContext, R.string.failed_to_pause, e);
        }
    }

    /**
     * Seeks to the given point without changing the state of the player, i.e. after seek is
     * completed, it resumes what it was doing before the start of seek.
     *
     * @param position in milliseconds
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     * @throws CastException
     */
    public void seek(int position) throws TransientNetworkDisconnectionException,
            NoConnectionException {
        LOGD(TAG, "attempting to seek media");
        checkConnectivity();
        if (mRemoteMediaPlayer == null) {
            LOGE(TAG, "Trying to seek a video with no active media session");
            throw new NoConnectionException();
        }
        mRemoteMediaPlayer.seek(mApiClient,
                position,
                RemoteMediaPlayer.RESUME_STATE_UNCHANGED).
                setResultCallback(new ResultCallback<MediaChannelResult>() {

                    @Override
                    public void onResult(MediaChannelResult result) {
                        if (!result.getStatus().isSuccess()) {
                            onFailed(R.string.failed_seek, result.getStatus().getStatusCode());
                        }
                    }

                });
    }

    /**
     * Seeks to the given point and starts playback regardless of the starting state.
     *
     * @param position in milliseconds
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     * @throws CastException
     */
    public void seekAndPlay(int position) throws TransientNetworkDisconnectionException,
            NoConnectionException {
        LOGD(TAG, "attempting to seek media");
        checkConnectivity();
        if (mRemoteMediaPlayer == null) {
            LOGE(TAG, "Trying to seekAndPlay a video with no active media session");
            throw new NoConnectionException();
        }
        ResultCallback<MediaChannelResult> resultCallback =
                new ResultCallback<MediaChannelResult>() {

                    @Override
                    public void onResult(MediaChannelResult result) {
                        if (!result.getStatus().isSuccess()) {
                            onFailed(R.string.failed_seek, result.getStatus().getStatusCode());
                        }
                    }

                };
        mRemoteMediaPlayer.seek(mApiClient,
                position,
                RemoteMediaPlayer.RESUME_STATE_PLAY).setResultCallback(resultCallback);
    }

    /**
     * Toggles the playback of the movie.
     *
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void togglePlayback() throws CastException, TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        boolean isPlaying = isRemoteMoviePlaying();
        if (isPlaying) {
            pause();
        } else {
            if (mState == MediaStatus.PLAYER_STATE_IDLE
                    && mIdleReason == MediaStatus.IDLE_REASON_FINISHED) {
                loadMedia(getRemoteMediaInformation(), true, 0);
            } else {
                play();
            }
        }
    }

    private void attachMediaChannel() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        LOGD(TAG, "attachMedia()");
        checkConnectivity();
        if (null == mRemoteMediaPlayer) {
            mRemoteMediaPlayer = new RemoteMediaPlayer();

            mRemoteMediaPlayer.setOnStatusUpdatedListener(
                    new RemoteMediaPlayer.OnStatusUpdatedListener() {

                        @Override
                        public void onStatusUpdated() {
                            LOGD(TAG, "RemoteMediaPlayer::onStatusUpdated() is reached");
                            CastManager.this.onRemoteMediaPlayerStatusUpdated();
                        }
                    });

            mRemoteMediaPlayer.setOnMetadataUpdatedListener(
                    new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                        @Override
                        public void onMetadataUpdated() {
                            LOGD(TAG, "RemoteMediaPlayer::onMetadataUpdated() is reached");
                            CastManager.this.onRemoteMediaPlayerMetadataUpdated();
                        }
                    }
                    );

        }
        try {
            LOGD(TAG, "Registering MediaChannel namespace");
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mRemoteMediaPlayer.getNamespace(),
                    mRemoteMediaPlayer);
        } catch (Exception e) {
            LOGE(TAG, "Failed to set up media channel", e);
        }
    }

    private void reattachMediaChannel() {
        if (null != mRemoteMediaPlayer && null != mApiClient) {
            try {
                LOGD(TAG, "Registering MediaChannel namespace");
                Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                        mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
            } catch (IOException e) {
                LOGE(TAG, "Failed to setup media channel", e);
            } catch (IllegalStateException e) {
                LOGE(TAG, "Failed to setup media channel", e);
            }
        }
    }

    private void detachMediaChannel() {
        LOGD(TAG, "trying to detach media channel");
        if (null != mRemoteMediaPlayer) {
            if (null != mRemoteMediaPlayer && null != Cast.CastApi) {
                try {
                    Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,
                            mRemoteMediaPlayer.getNamespace());
                } catch (IOException e) {
                    LOGE(TAG, "Failed to detach media channel", e);
                }
            }
            mRemoteMediaPlayer = null;
        }
    }

    /**
     * Returns the playback status of the remote device.
     *
     * @return Returns one of the values
     *         <ul>
     *         <li> <code>MediaStatus.PLAYER_STATE_PLAYING</code>
     *         <li> <code>MediaStatus.PLAYER_STATE_PAUSED</code>
     *         <li> <code>MediaStatus.PLAYER_STATE_BUFFERING</code>
     *         <li> <code>MediaStatus.PLAYER_STATE_IDLE</code>
     *         <li> <code>MediaStatus.PLAYER_STATE_UNKNOWN</code>
     *         </ul>
     */
    public int getPlaybackStatus() {
        return mState;
    }

    /**
     * Returns the Idle reason, defined in <code>MediaStatus.IDLE_*</code>. Note that the returned
     * value is only meaningful if the status is truly <code>MediaStatus.PLAYER_STATE_IDLE
     * </code>
     *
     * @return
     */
    public int getIdleReason() {
        return mIdleReason;
    }

    /*************************************************************************/
    /************** DataChannel callbacks and methods ************************/
    /**
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     ***********************************************************************/

    /*
     * If a data namespace was provided when initializing this class, we set things up for a data
     * channel
     */
    private void attachDataChannel() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        if (TextUtils.isEmpty(mDataNamespace)) {
            return;
        }
        if (mDataChannel != null) {
            return;
        }
        checkConnectivity();
        mDataChannel = new MessageReceivedCallback() {

            @Override
            public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
                for (IVideoCastConsumer consumer : mVideoConsumers) {
                    try {
                        consumer.onDataMessageReceived(message);
                    } catch (Exception e) {
                        LOGE(TAG, "onMessageReceived(): Failed to inform " + consumer, e);
                    }
                }
            }
        };
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mDataNamespace, mDataChannel);
        } catch (Exception e) {
            LOGE(TAG, "Failed to add data channel", e);
        }
    }

    private void onMessageSendFailed(int errorCode) {
        for (IVideoCastConsumer consumer : mVideoConsumers) {
            try {
                consumer.onDataMessageSendFailed(errorCode);
            } catch (Exception e) {
                LOGE(TAG, "onMessageSendFailed(): Failed to inform " + consumer, e);
            }
        }
    }

    /**
     * Sends the <code>message</code> on the data channel for the namespace that was provided during
     * the initialization of this class. If <code>messageId &gt; 0</code>, then it has to be a
     * unique identifier for the message; this id will be returned if an error occurs. If
     * <code>messageId == 0</code>, then an auto-generated unique identifier will be created and
     * returned for the message.
     *
     * @param message
     * @return
     * @throws IllegalStateException If the namesapace is empty or null
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     *             possibly transient loss of network
     */
    public void sendDataMessage(String message) throws TransientNetworkDisconnectionException,
            NoConnectionException {
        if (TextUtils.isEmpty(mDataNamespace)) {
            throw new IllegalStateException("No Data Namespace is configured");
        }
        checkConnectivity();
        Cast.CastApi.sendMessage(mApiClient, mDataNamespace, message)
                .setResultCallback(new ResultCallback<Status>() {

                    @Override
                    public void onResult(Status result) {
                        if (!result.isSuccess()) {
                            CastManager.this.onMessageSendFailed(result.getStatusCode());
                        }
                    }
                });
    }

    /**
     * Remove the custom data channel, if any. It returns <code>true</code> if it succeeds otherwise
     * if it encounters an error or if no connection exists or if no custom data channel exists,
     * then it returns <code>false</code>
     *
     * @return
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public boolean removeDataChannel() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        if (TextUtils.isEmpty(mDataNamespace)) {
            return false;
        }
        try {
            Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, mDataNamespace);
            return true;
        } catch (Exception e) {
            LOGE(TAG, "Failed to remove namespace: " + mDataNamespace, e);
        }
        return false;

    }

    /*************************************************************************/
    /************** MediaChannel callbacks ***********************************/
    /*************************************************************************/

    /*
     * This is called by onStatusUpdated() of the RemoteMediaPlayer
     */
    private void onRemoteMediaPlayerStatusUpdated() {
        LOGD(TAG, "onRemoteMediaPlayerStatusUpdated() reached");
        if (null == mApiClient || null == mRemoteMediaPlayer ||
                null == mRemoteMediaPlayer.getMediaStatus()) {
            LOGD(TAG, "mApiClient or mRemoteMediaPlayer is null, so will not proceed");
            return;
        }
        mState = mRemoteMediaPlayer.getMediaStatus().getPlayerState();
        mIdleReason = mRemoteMediaPlayer.getMediaStatus().getIdleReason();
//        try {
//            double volume = getVolume();
//            boolean isMute = isMute();
            boolean idle = false;
            if (mState == MediaStatus.PLAYER_STATE_PLAYING) {
                LOGD(TAG,"status: playing");
            } else if (mState == MediaStatus.PLAYER_STATE_PAUSED) {
                LOGD(TAG,"status: paused");
            } else if (mState == MediaStatus.PLAYER_STATE_IDLE) {
                idle = true;
                if (mIdleReason == MediaStatus.IDLE_REASON_FINISHED) {
                    //Anything?
                } else if (mIdleReason == MediaStatus.IDLE_REASON_ERROR) {
                    // something bad happened on the cast device
                    LOGD(TAG, "Player on the receiver has thrown an error");
                    //TODO not sure we should do this, i still havent encountered it yet though
                    onFailed(R.string.failed_receiver_player_error, NO_STATUS_CODE);
                }
            } else if (mState == MediaStatus.PLAYER_STATE_BUFFERING) {
                LOGD(TAG,"status: buffering");
            } else {
                LOGD(TAG,"status: unknown");
                idle = true;
            }
            for (IVideoCastConsumer consumer : mVideoConsumers) {
                try {
                    consumer.onRemoteMediaPlayerStatusUpdated();
//                    consumer.onVolumeChanged(volume, isMute); //Dont know why they were calling this every damn time
                } catch (Exception e) {
                    LOGE(TAG, "onRemoteMediaplayerStatusUpdated(): Failed to inform "
                            + consumer, e);
                }
            }
//        } catch (TransientNetworkDisconnectionException e) {
//            LOGE(TAG, "Failed to get volume state due to network issues", e);
//        } catch (NoConnectionException e) {
//            LOGE(TAG, "Failed to get volume state due to network issues", e);
//        }

    }

    /*
     * This is called by onMetadataUpdated() of RemoteMediaPlayer
     */
    public void onRemoteMediaPlayerMetadataUpdated() {
        LOGD(TAG, "onRemoteMediaPlayerMetadataUpdated() reached");
        for (IVideoCastConsumer consumer : mVideoConsumers) {
            try {
                consumer.onRemoteMediaPlayerMetadataUpdated();
            } catch (Exception e) {
                LOGE(TAG, "onRemoteMediaPlayerMetadataUpdated(): Failed to inform " + consumer, e);
            }
        }
    }

    /*************************************************************/
    /***** Registering IVideoCastConsumer listeners **************/
    /*************************************************************/
    /**
     * Registers an {@link IVideoCastConsumer} interface with this class. Registered listeners will
     * be notified of changes to a variety of lifecycle and media status changes through the
     * callbacks that the interface provides.
     *
     * @see VideoCastConsumerImpl
     * @param listener
     */
    public synchronized void addVideoCastConsumer(IVideoCastConsumer listener) {
        if (null != listener) {
            super.addBaseCastConsumer(listener);
            mVideoConsumers.add(listener);
            LOGD(TAG, "Successfully added the new CastConsumer listener " + listener);
        }
    }

    /**
     * Unregisters an {@link IVideoCastConsumer}.
     *
     * @param listener
     */
    public synchronized void removeVideoCastConsumer(IVideoCastConsumer listener) {
        if (null != listener) {
            super.removeBaseCastConsumer(listener);
            mVideoConsumers.remove(listener);
        }
    }


    /*************************************************************/
    /***** Implementing abstract methods of BaseCastManager ******/
    /*************************************************************/

    @Override
    void onDeviceUnselected() {
        try {
            detachMediaChannel();
            removeDataChannel();
        } catch (TransientNetworkDisconnectionException e) {
            LOGD(TAG, "Failed to clean up on onDeviceUnselected()", e);
        } catch (NoConnectionException e) {
            LOGD(TAG, "Failed to clean up on onDeviceUnselected()", e);
        }
    }

    @Override
    Builder getCastOptionBuilder(CastDevice device) {
        Builder builder = Cast.CastOptions.builder(mSelectedCastDevice, new CastListener());
        if (isFeatureEnabled(FEATURE_DEBUGGING)) {
            builder.setDebuggingEnabled();
        }
        return builder;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        super.onConnectionFailed(result);

    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
    }


    class CastListener extends Cast.Listener {

        /*
         * (non-Javadoc)
         * @see com.google.android.gms.cast.Cast.Listener#onApplicationDisconnected (int)
         */
        @Override
        public void onApplicationDisconnected(int statusCode) {
            CastManager.this.onApplicationDisconnected(statusCode);
        }

        /*
         * (non-Javadoc)
         * @see com.google.android.gms.cast.Cast.Listener#onApplicationStatusChanged ()
         */
        @Override
        public void onApplicationStatusChanged() {
            CastManager.this.onApplicationStatusChanged();
        }

        @Override
        public void onVolumeChanged() {
            CastManager.this.onVolumeChanged();
        }
    }

    @Override
    public void onFailed(int resourceId, int statusCode) {
        LOGD(TAG, "onFailed: " + mContext.getString(resourceId) + ", code: " + statusCode);
        super.onFailed(resourceId, statusCode);
    }

}
