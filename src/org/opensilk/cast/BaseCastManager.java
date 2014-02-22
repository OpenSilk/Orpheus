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

import android.app.Activity;
import android.content.Context;
import android.media.RemoteControlClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import org.opensilk.cast.callbacks.BaseCastConsumerImpl;
import org.opensilk.cast.callbacks.IBaseCastConsumer;
import org.opensilk.cast.exceptions.CastException;
import org.opensilk.cast.exceptions.NoConnectionException;
import org.opensilk.cast.exceptions.OnFailedListener;
import org.opensilk.cast.exceptions.TransientNetworkDisconnectionException;
import org.opensilk.cast.util.LogUtils;
import org.opensilk.cast.util.Utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hugo.weaving.DebugLog;

/**
 * An abstract class that manages connectivity to a cast device. Subclasses are expected to extend
 * the functionality of this class based on their purpose.
 */
public abstract class BaseCastManager implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        OnFailedListener {

    public static final int FEATURE_DEBUGGING = 1;
    public static final String PREFS_KEY_SESSION_ID = "session-id";
    public static final String PREFS_KEY_APPLICATION_ID = "application-id";
    public static final String PREFS_KEY_VOLUME_INCREMENT = "volume-increment";
    public static final String PREFS_KEY_ROUTE_ID = "route-id";

    public static final int NO_STATUS_CODE = -1;

    private static final String TAG = LogUtils.makeLogTag(BaseCastManager.class);
    private static final int SESSION_RECOVERY_TIMEOUT = 5; // in seconds

    protected Context mContext;
    protected MediaRouter mMediaRouter;
    protected MediaRouteSelector mMediaRouteSelector;
    protected CastMediaRouterCallback mMediaRouterCallback;
    protected CastDevice mSelectedCastDevice;
    protected String mDeviceName;
    private final Set<IBaseCastConsumer> mBaseCastConsumers = new HashSet<IBaseCastConsumer>();
    protected RemoteCallbackList<CastManagerCallback> mListeners = new RemoteCallbackList<CastManagerCallback>();
    private boolean mDestroyOnDisconnect = false;
    protected String mApplicationId;
    protected Handler mHandler;
    protected int mReconnectionStatus = ReconnectionStatus.INACTIVE;
    protected int mVisibilityCounter;
    protected boolean mUiVisible;
    protected GoogleApiClient mApiClient;
    protected AsyncTask<Void, Integer, Integer> mReconnectionTask;
    protected boolean mDebuggingEnabled;
    protected int mCapabilities;
    protected boolean mConnectionSuspened;
    private boolean mWifiConnectivity = true;
    protected static BaseCastManager mCastManager;

    /*************************************************************************/
    /************** Abstract Methods *****************************************/
    /*************************************************************************/

    /**
     * A chance for the subclasses to perform what needs to be done when a route is unselected. Most
     * of the logic is handled by the {@link BaseCastManager} but each subclass may have some
     * additional logic that can be done, e.g. detaching data or media channels that they may have
     * set up.
     */
    abstract void onDeviceUnselected();

    /**
     * Since application lifecycle callbacks are managed by subclasses, this abstract method needs
     * to be implemented by each subclass independently.
     *
     * @param device
     * @return
     */
    abstract Cast.CastOptions.Builder getCastOptionBuilder(CastDevice device);

    /**
     * Subclasses should implement this to react appropriately to the successful launch of their
     * application. This is called when the application is successfully launched.
     *
     * @param applicationMetadata
     * @param applicationStatus
     * @param sessionId
     * @param wasLaunched
     */
    abstract void onApplicationConnected(ApplicationMetadata applicationMetadata,
            String applicationStatus, String sessionId, boolean wasLaunched);

    /**
     * Called when the launch of application has failed. Subclasses need to handle this by doing
     * appropriate clean up.
     *
     * @param statusCode
     */
    abstract void onApplicationConnectionFailed(int statusCode);

    /**
     * Called when the attempt to stop application has failed.
     *
     * @param statusCode
     */
    abstract void onApplicationStopFailed(int statusCode);

    /************************************************************************/

    protected BaseCastManager(Context context, String applicationId) {
        LOGD(TAG, "BaseCastManager is instantiated");
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mApplicationId = applicationId;
        Utils.saveStringToPreference(mContext, PREFS_KEY_APPLICATION_ID, applicationId);

        LOGD(TAG, "Application ID is: " + mApplicationId);
        mMediaRouter = MediaRouter.getInstance(context);
        mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(mApplicationId)).build();

        mMediaRouterCallback = new CastMediaRouterCallback();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    public void onWifiConnectivityChanged(boolean connected) {
        LOGD(TAG, "WIFI connectivity changed to " + (connected ? "enabled" : "disabled"));
        if (connected && !mWifiConnectivity) {
            mWifiConnectivity = true;
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    reconnectSessionIfPossible(mContext, false, 10);
                }
            }, 1000);

        } else {
            mWifiConnectivity = connected;
        }
    }

    public static BaseCastManager getCastManager() {
        return mCastManager;
    }

    /**
     * Sets the {@link Context} for the subsequent calls. Setting context can help the library to
     * show error messages to the user.
     *
     * @param context
     */
    public void setContext(Context context) {
        mContext = context;
    }

    public void selectDevice(CastDevice device) {
        selectDevice(device, mDestroyOnDisconnect);
    }

    public void selectDevice(CastDevice device, boolean stopAppOnExit) {
        mSelectedCastDevice = device;
        mDeviceName = mSelectedCastDevice != null ? mSelectedCastDevice.getFriendlyName() : null;

        if (mSelectedCastDevice == null) {
            if (!mConnectionSuspened) {
                Utils.saveStringToPreference(mContext, PREFS_KEY_SESSION_ID, null);
                Utils.saveStringToPreference(mContext, PREFS_KEY_ROUTE_ID, null);
            }
            mConnectionSuspened = false;
            try {
                if (isConnected()) {
                    if (stopAppOnExit) {
                        LOGD(TAG, "Calling stopApplication");
                        stopApplication();
                    }
                }
            } catch (IllegalStateException e) {
                LOGE(TAG, "Failed to stop the application after disconecting route", e);
            } catch (IOException e) {
                LOGE(TAG, "Failed to stop the application after disconecting route", e);
            } catch (TransientNetworkDisconnectionException e) {
                LOGE(TAG, "Failed to stop the application after disconecting route", e);
            } catch (NoConnectionException e) {
                LOGE(TAG, "Failed to stop the application after disconecting route", e);
            }
            onDisconnected();
            onDeviceUnselected();
            if (null != mApiClient) {
                LOGD(TAG, "Trying to disconnect");
                mApiClient.disconnect();
                if (null != mMediaRouter) {
                    mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
                }
                mApiClient = null;
            }
        } else if (null == mApiClient) {
            LOGD(TAG, "acquiring a conenction to Google Play services for " + mSelectedCastDevice);
            Cast.CastOptions.Builder apiOptionsBuilder = getCastOptionBuilder(mSelectedCastDevice);
            mApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mApiClient.connect();
        } else if (!mApiClient.isConnected()) {
            mApiClient.connect();
        }
    }

    public void onCastDeviceDetected(RouteInfo info) {
        if (null != mBaseCastConsumers) {
            for (IBaseCastConsumer consumer : mBaseCastConsumers) {
                try {
                    consumer.onCastDeviceDetected(info);
                } catch (Exception e) {
                    LOGE(TAG, "onCastDeviceDetected(): Failed to inform " + consumer, e);
                }
            }
        }
    }

    /*************************************************************************/
    /************** UI Visibility Management *********************************/
    /*************************************************************************/

    /**
     * Calling this method signals the library that an activity page is made visible. In common
     * cases, this should be called in the "onResume()" method of each activity of the application.
     * The library keeps a counter and when at least one page of the application becomes visible,
     * the {@link onUiVisibilityChanged()} method is called.
     */
    public synchronized void incrementUiCounter() {
        mVisibilityCounter++;
        if (!mUiVisible) {
            mUiVisible = true;
            onUiVisibilityChanged(true);
        }
        if (mVisibilityCounter == 0) {
            LOGD(TAG, "UI is no longer visible");
        } else {
            LOGD(TAG, "UI is visible");
        }
    }

    /**
     * Calling this method signals the library that an activity page is made invisible. In common
     * cases, this should be called in the "onPause()" method of each activity of the application.
     * The library keeps a counter and when all pages of the application become invisible, the
     * {@link onUiVisibilityChanged()} method is called.
     */
    public synchronized void decrementUiCounter() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (--mVisibilityCounter == 0) {
                    LOGD(TAG, "UI is no longer visible");
                    if (mUiVisible) {
                        mUiVisible = false;
                        onUiVisibilityChanged(false);
                    }
                } else {
                    LOGD(TAG, "UI is visible");
                }
            }
        }, 300);
    }

    /**
     * This is called when UI visibility of the client has changed
     *
     * @param visible The updated visibility status
     */
    protected void onUiVisibilityChanged(boolean visible) {
        if (visible) {
            if (null != mMediaRouter && null != mMediaRouterCallback) {
                LOGD(TAG, "onUiVisibilityChanged() addCallback called");
                mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
            }
        } else {
            if (null != mMediaRouter) {
                LOGD(TAG, "onUiVisibilityChanged() removeCallback called");
                mMediaRouter.removeCallback(mMediaRouterCallback);
            }
        }
    }

    /*************************************************************************/
    /************** Utility Methods ******************************************/
    /*************************************************************************/

    /**
     * A utility method to validate that the appropriate version of the Google Play Services is
     * available on the device. If not, it will open a dialog to address the issue. The dialog
     * displays a localized message about the error and upon user confirmation (by tapping on
     * dialog) will direct them to the Play Store if Google Play services is out of date or missing,
     * or to system settings if Google Play services is disabled on the device.
     *
     * @param activity
     * @return
     */
    public static boolean checkGooglePlaySevices(final Activity activity) {
        return Utils.checkGooglePlaySevices(activity);
    }

    /**
     * can be used to find out if the application is connected to the service or not.
     *
     * @return <code>true</code> if connected, <code>false</code> otherwise.
     */
    public boolean isConnected() {
        return (null != mApiClient) && mApiClient.isConnected();
    }

    /**
     * Disconnects from the cast device and stops the application on the cast device.
     */
    public void disconnect() {
        if (isConnected()) {
            selectDevice(null, true);
        }
    }

    /**
     * Returns the assigned human-readable name of the device, or <code>null</code> if no device is
     * connected.
     *
     * @return
     */
    public final String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Sets a flag to control whether disconnection form a cast device should result in stopping the
     * running application or not. If <code>true</code> is passed, then application will be stopped.
     * Default behavior is not to stop the app.
     *
     * @param stopOnExit
     */
    public final void setStopOnDisconnect(boolean stopOnExit) {
        mDestroyOnDisconnect = stopOnExit;
    }

    /**
     * Returns the {@link MediaRouteSelector} object.
     *
     * @return
     */
    public final MediaRouteSelector getMediaRouteSelector() {
        return mMediaRouteSelector;
    }

    /**
     * Turns on configurable features in the library. All the supported features are turned off by
     * default and clients, prior to using them, need to turn them on; it is best to do is
     * immediately after initialization of the library. Bitwise OR combination of features should be
     * passed in if multiple features are needed
     * <p/>
     * Current set of configurable features are:
     * <ul>
     * <li>FEATURE_DEBUGGING : turns on debugging in Google Play services
     * <li>FEATURE_NOTIFICATION : turns notifications on
     * <li>FEATURE_LOCKSCREEN : turns on Lock Screen using {@link RemoteControlClient} in supported
     * versions (JB+)
     * </ul>
     *
     * @param capabilities
     */
    public void enableFeatures(int capabilities) {
        mCapabilities = capabilities;
    }

    /*
     * Returns true if and only if the feature is turned on
     */
    protected boolean isFeatureEnabled(int feature) {
        return (feature & mCapabilities) > 0;
    }

    /**
     * Sets the device (system) volume.
     *
     * @param volume Should be a value between 0 and 1, inclusive.
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void setDeviceVolume(double volume) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        try {
            Cast.CastApi.setVolume(mApiClient, volume);
        } catch (Exception e) {
            LOGE(TAG, "Failed to set volume", e);
            throw new CastException("Failed to set volume");
        }
    }

    /**
     * Gets the remote's system volume, a number between 0 and 1, inclusive.
     *
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public final double getDeviceVolume() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        return Cast.CastApi.getVolume(mApiClient);
    }

    /**
     * Increments (or decrements) the device volume by the given amount.
     *
     * @param delta
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void incrementDeviceVolume(double delta) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        double vol = getDeviceVolume();
        if (vol >= 0) {
            setDeviceVolume(vol + delta);
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
    public final boolean isDeviceMute() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        return Cast.CastApi.isMute(mApiClient);
    }

    /**
     * Mutes or un-mutes the device volume.
     *
     * @param mute
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void setDeviceMute(boolean mute) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        try {
            Cast.CastApi.setMute(mApiClient, mute);
        } catch (Exception e) {
            LOGE(TAG, "Failed to set mute to: " + mute, e);
            throw new CastException("Failed to mute");
        }
    }

    /*************************************************************************/
    /************** Session Recovery Methods *********************************/
    /*************************************************************************/

    /**
     * Returns the current {@link ReconnectionStatus}
     *
     * @return
     */
    public int getReconnectionStatus() {
        return mReconnectionStatus;
    }

    /**
     * Sets the {@link ReconnectionStatus}
     *
     * @param status
     */
    public final void setReconnectionStatus(int status) {
        mReconnectionStatus = status;
        // Do this ourselfs since the activity cant reach it
        if (mReconnectionStatus == ReconnectionStatus.INACTIVE) {
            cancelReconnectionTask();
        }
    }

    /**
     * Returns <code>true</code> if there is enough persisted information to attempt a session
     * recovery. For this to return <code>true</code>, there needs to be persisted session ID and
     * route ID from the last successful launch.
     *
     * @return
     */
    public final boolean canConsiderSessionRecovery() {
        String sessionId = Utils.getStringFromPreference(mContext, PREFS_KEY_SESSION_ID);
        String routeId = Utils.getStringFromPreference(mContext, PREFS_KEY_ROUTE_ID);
        if (null == sessionId || null == routeId) {
            return false;
        }
        LOGD(TAG, "Found session info in the preferences, so proceed with an "
                + "attempt to reconnect if possible");
        return true;
    }

    private void reconnectSessionIfPossibleInternal(RouteInfo theRoute) {
        if (isConnected()) {
            return;
        }
        String sessionId = Utils.getStringFromPreference(mContext, PREFS_KEY_SESSION_ID);
        String routeId = Utils.getStringFromPreference(mContext, PREFS_KEY_ROUTE_ID);
        LOGD(TAG, "reconnectSessionIfPossible() Retrieved from preferences: " + "sessionId="
                + sessionId + ", routeId=" + routeId);
        if (null == sessionId || null == routeId) {
            return;
        }
        mReconnectionStatus = ReconnectionStatus.IN_PROGRESS;
        CastDevice device = CastDevice.getFromBundle(theRoute.getExtras());

        if (null != device) {
            LOGD(TAG, "trying to acquire Cast Client for " + device);
            selectDevice(device);
        }
    }

    /*
     * Cancels the task responsible for recovery of prior sessions, is used internally.
     */
    void cancelReconnectionTask() {
        LOGD(TAG, "cancelling reconnection task");
        if (null != mReconnectionTask && !mReconnectionTask.isCancelled()) {
            mReconnectionTask.cancel(true);
        }
    }

    /**
     * This method tries to automatically re-establish connection to a session if
     * <ul>
     * <li>User had not done a manual disconnect in the last session
     * <li>The Cast Device that user had connected to previously is still running the same session
     * </ul>
     * Under these conditions, a best-effort attempt will be made to continue with the same session.
     * This attempt will go on for <code>timeoutInSeconds</code> seconds. During this period, an
     * optional dialog can be shown if <code>showDialog</code> is set to <code>true</code>. The
     * message in this dialog can be changed by overriding the resource
     * <code>R.string.session_reconnection_attempt</code>
     *
     * @param context
     * @param showDialog
     * @param timeoutInSeconds
     */
    public void reconnectSessionIfPossible(final Context context, final boolean showDialog,
            final int timeoutInSeconds) {
        if (isConnected()) {
            return;
        }
        String routeId = Utils.getStringFromPreference(mContext, PREFS_KEY_ROUTE_ID);
        if (canConsiderSessionRecovery()) {
            List<RouteInfo> routes = mMediaRouter.getRoutes();
            RouteInfo theRoute = null;
            if (null != routes && !routes.isEmpty()) {
                for (RouteInfo route : routes) {
                    if (route.getId().equals(routeId)) {
                        theRoute = route;
                        break;
                    }
                }
            }
            if (null != theRoute) {
                // route has already been discovered, so lets just get the
                // device, etc
                reconnectSessionIfPossibleInternal(theRoute);
            } else {
                // we set a flag so if the route is discovered within a short
                // period, we let onRouteAdded callback of
                // CastMediaRouterCallback take
                // care of that
                mReconnectionStatus = ReconnectionStatus.STARTED;
            }

            // we may need to reconnect to an existing session
            mReconnectionTask = new AsyncTask<Void, Integer, Integer>() {
                private final int SUCCESS = 1;
                private final int FAILED = 2;

                @Override
                protected void onCancelled() {
                    super.onCancelled();
                }

                @Override
                protected void onPreExecute() {

                }

                @Override
                protected Integer doInBackground(Void... params) {
                    for (int i = 0; i < timeoutInSeconds; i++) {
                        if (mReconnectionTask.isCancelled()) {
                            return SUCCESS;
                        }
                        try {
                            if (isConnected()) {
                                cancel(true);
                            }
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    return FAILED;
                }

                @Override
                protected void onPostExecute(Integer result) {
                    if (null != result) {
                        if (result == FAILED) {
                            mReconnectionStatus = ReconnectionStatus.INACTIVE;
                            selectDevice(null);
                        }
                    }
                }

            };
            mReconnectionTask.execute();
        }
    }

    /**
     * This method tries to automatically re-establish re-establish connection to a session if
     * <ul>
     * <li>User had not done a manual disconnect in the last session
     * <li>Device that user had connected to previously is still running the same session
     * </ul>
     * Under these conditions, a best-effort attempt will be made to continue with the same session.
     * This attempt will go on for 5 seconds. During this period, an optional dialog can be shown if
     * <code>showDialog</code> is set to <code>true
     * </code>.
     *
     * @param context
     * @param showDialog if set to <code>true</code>, a dialog will be shown
     */
    public void reconnectSessionIfPossible(final Context context, final boolean showDialog) {
        reconnectSessionIfPossible(context, showDialog, SESSION_RECOVERY_TIMEOUT);
    }

    /************************************************************/
    /***** GoogleApiClient.ConnectionCallbacks ******************/
    /************************************************************/
    /**
     * This is called by the library when a connection is re-established after a transient
     * disconnect. Note: this is not called by SDK.
     */
    public void onConnectivityRecovered() {
        for (IBaseCastConsumer consumer : mBaseCastConsumers) {
            try {
                consumer.onConnectivityRecovered();
            } catch (Exception e) {
                LOGE(TAG, "onConnectivityRecovered: Failed to inform " + consumer, e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.gms.GoogleApiClient.ConnectionCallbacks#onConnected
     * (android.os.Bundle)
     */
    @Override
    public void onConnected(Bundle arg0) {
        LOGD(TAG, "onConnected() reached with prior suspension: " + mConnectionSuspened);
        if (mConnectionSuspened) {
            mConnectionSuspened = false;
            onConnectivityRecovered();
            return;
        }
        if (!isConnected()) {
            if (mReconnectionStatus == ReconnectionStatus.IN_PROGRESS) {
                mReconnectionStatus = ReconnectionStatus.INACTIVE;
            }
            return;
        }
        try {
            Cast.CastApi.requestStatus(mApiClient);
            launchApp();

            if (null != mBaseCastConsumers) {
                for (IBaseCastConsumer consumer : mBaseCastConsumers) {
                    try {
                        consumer.onConnected();
                    } catch (Exception e) {
                        LOGE(TAG, "onConnected: Failed to inform " + consumer, e);
                    }
                }
            }

        } catch (IOException e) {
            LOGE(TAG, "error requesting status", e);
        } catch (IllegalStateException e) {
            LOGE(TAG, "error requesting status", e);
        } catch (TransientNetworkDisconnectionException e) {
            LOGE(TAG, "error requesting status due to network issues", e);
        } catch (NoConnectionException e) {
            LOGE(TAG, "error requesting status due to network issues", e);
        }

    }

    /*
     * Note: this is not called by the SDK anymore but this library calls this in the appropriate
     * time.
     */
    protected void onDisconnected() {
        LOGD(TAG, "onDisconnected() reached");
        mDeviceName = null;
        if (null != mBaseCastConsumers) {
            for (IBaseCastConsumer consumer : mBaseCastConsumers) {
                try {
                    consumer.onDisconnected();
                } catch (Exception e) {
                    LOGE(TAG, "onDisconnected(): Failed to inform " + consumer, e);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.gms.GoogleApiClient.OnConnectionFailedListener#
     * onConnectionFailed(com.google.android.gms.common.ConnectionResult)
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        LOGD(TAG, "onConnectionFailed() reached, error code: " + result.getErrorCode()
                + ", reason: " + result.toString());
        mSelectedCastDevice = null;
        if (null != mMediaRouter) {
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
        boolean showError = false;
        if (null != mBaseCastConsumers) {
            for (IBaseCastConsumer consumer : mBaseCastConsumers) {
                try {
                    consumer.onConnectionFailed(result);
                } catch (Exception e) {
                    LOGE(TAG, "onConnectionFailed(): Failed to inform " + consumer, e);
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mConnectionSuspened = true;
        LOGD(TAG, "onConnectionSuspended() was called with cause: " + cause);
        for (IBaseCastConsumer consumer : mBaseCastConsumers) {
            try {
                consumer.onConnectionSuspended(cause);
            } catch (Exception e) {
                LOGE(TAG, "onConnectionSuspended(): Failed to inform " + consumer, e);
            }
        }
        int ii = mListeners.beginBroadcast();
        while (ii-->0) {
            try {
                mListeners.getBroadcastItem(ii).onConnectionSuspended(cause);
            } catch (RemoteException e) {
                LOGE(TAG, "onConnectionSuspended(): ", e);
            }
        }
        mListeners.finishBroadcast();
    }

    /*
     * Launches application. For this succeed, a connection should be already established by the
     * CastClient.
     */
    private void launchApp() throws TransientNetworkDisconnectionException, NoConnectionException {
        LOGD(TAG, "launchApp() is called");
        if (!isConnected()) {
            if (mReconnectionStatus == ReconnectionStatus.IN_PROGRESS) {
                mReconnectionStatus = ReconnectionStatus.INACTIVE;
                return;
            }
            checkConnectivity();
        }

        if (mReconnectionStatus == ReconnectionStatus.IN_PROGRESS) {
            LOGD(TAG, "Attempting to join a previously interrupted session...");
            String sessionId = Utils.getStringFromPreference(mContext, PREFS_KEY_SESSION_ID);
            LOGD(TAG, "joinApplication() -> start");
            Cast.CastApi.joinApplication(mApiClient, mApplicationId, sessionId).setResultCallback(
                    new ResultCallback<Cast.ApplicationConnectionResult>() {

                        @Override
                        public void onResult(ApplicationConnectionResult result) {
                            if (result.getStatus().isSuccess()) {
                                LOGD(TAG, "joinApplication() -> success");
                                onApplicationConnected(result.getApplicationMetadata(),
                                        result.getApplicationStatus(), result.getSessionId(),
                                        result.getWasLaunched());
                            } else {
                                LOGD(TAG, "joinApplication() -> failure");
                                onApplicationConnectionFailed(result.getStatus().getStatusCode());
                            }
                        }
                    });
        } else {
            LOGD(TAG, "Launching app");
            Cast.CastApi.launchApplication(mApiClient, mApplicationId).setResultCallback(
                    new ResultCallback<Cast.ApplicationConnectionResult>() {

                        @Override
                        public void onResult(ApplicationConnectionResult result) {
                            if (result.getStatus().isSuccess()) {
                                LOGD(TAG, "launchApplication() -> success result");
                                onApplicationConnected(result.getApplicationMetadata(),
                                        result.getApplicationStatus(), result.getSessionId(),
                                        result.getWasLaunched());
                            } else {
                                LOGD(TAG, "launchApplication() -> failure result");
                                onApplicationConnectionFailed(result.getStatus().getStatusCode());
                            }
                        }
                    });
        }
    }

    /**
     * Stops the application on the receiver device.
     *
     * @throws IllegalStateException
     * @throws IOException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public void stopApplication() throws IllegalStateException, IOException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        Cast.CastApi.stopApplication(mApiClient).setResultCallback(new ResultCallback<Status>() {

            @Override
            public void onResult(Status result) {
                if (!result.isSuccess()) {
                    LOGD(TAG, "stopApplication -> onResult: stopping " + "application failed");
                    onApplicationStopFailed(result.getStatusCode());
                } else {
                    LOGD(TAG, "stopApplication -> onResult Stopped application " + "successfully");
                }
            }
        });
    }

    /*************************************************************/
    /***** Registering IBaseCastConsumer listeners **************/
    /*************************************************************/
    /**
     * Registers an {@link IBaseCastConsumer} interface with this class. Registered listeners will
     * be notified of changes to a variety of lifecycle callbacks that the interface provides.
     *
     * @see BaseCastConsumerImpl
     * @param listener
     */
    public synchronized void addBaseCastConsumer(IBaseCastConsumer listener) {
        if (null != listener) {
            if (mBaseCastConsumers.add(listener)) {
                LOGD(TAG, "Successfully added the new BaseCastConsumer listener " + listener);
            }
        }
    }

    /**
     * Unregisters an {@link IBaseCastConsumer}.
     *
     * @param listener
     */
    public synchronized void removeBaseCastConsumer(IBaseCastConsumer listener) {
        if (null != listener) {
            if (mBaseCastConsumers.remove(listener)) {
                LOGD(TAG, "Successfully removed the existing BaseCastConsumer listener " +
                        listener);
            }
        }
    }


    /*
     * Registering cross process callbacks, these are used by activities
     * to show user messages pertaining to state.
     */

    public synchronized void registerListener(CastManagerCallback cb) {
        if (cb != null) {
            mListeners.register(cb);
        }
    }

    public synchronized void unregisterListener(CastManagerCallback cb) {
        if (cb != null) {
            mListeners.unregister(cb);
        }
    }

    /**
     * A simple method that throws an exception of there is no connectivity to the cast device.
     *
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover
     * @throws NoConnectionException If no connectivity to the device exists
     */
    public void checkConnectivity() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        if (!isConnected()) {
            if (mConnectionSuspened) {
                throw new TransientNetworkDisconnectionException();
            } else {
                throw new NoConnectionException();
            }
        }
    }

    @Override
    public void onFailed(int resourceId, int statusCode) {
        LOGD(TAG, "onFailed() was called with statusCode: " + statusCode);
        for (IBaseCastConsumer consumer : mBaseCastConsumers) {
            try {
                consumer.onFailed(resourceId, statusCode);
            } catch (Exception e) {
                LOGE(TAG, "onFailed(): Failed to inform " + consumer, e);
            }
        }

    }

    /**
     * Provides a handy implementation of {@link MediaRouter.Callback}. When a {@link RouteInfo} is
     * selected by user from the list of available routes, this class will call the
     * {@link DeviceSelectionListener#setDevice(CastDevice))} of the listener that was passed to it in
     * the constructor. In addition, as soon as a non-default route is discovered, the
     * {@link DeviceSelectionListener#onCastDeviceDetected(CastDevice))} is called.
     * <p>
     * There is also logic in this class to help with the process of previous session recovery.
     */
    private class CastMediaRouterCallback extends MediaRouter.Callback {
        private final String TAG = LogUtils.makeLogTag(CastMediaRouterCallback.class);

        public CastMediaRouterCallback() {
        }

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            LOGD(TAG, "onRouteSelected: info=" + info);
            if (getReconnectionStatus() == ReconnectionStatus.FINALIZE) {
                setReconnectionStatus(ReconnectionStatus.INACTIVE);
//                cancelReconnectionTask();
                return;
            }
            Utils.saveStringToPreference(mContext, PREFS_KEY_ROUTE_ID, info.getId());
            CastDevice device = CastDevice.getFromBundle(info.getExtras());
            selectDevice(device);
            LOGD(TAG, "onResult: mSelectedDevice=" + device.getFriendlyName());
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            LOGD(TAG, "onRouteUnselected: route=" + route);
            selectDevice(null);
        }

        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo route) {
            super.onRouteAdded(router, route);
            if (!router.getDefaultRoute().equals(route)) {
                onCastDeviceDetected(route);
            }
            if (getReconnectionStatus() == ReconnectionStatus.STARTED) {
                String routeId = Utils.getStringFromPreference(mContext, PREFS_KEY_ROUTE_ID);
                if (route.getId().equals(routeId)) {
                    // we found the route, so lets go with that
                    LOGD(TAG, "onRouteAdded: Attempting to recover a session with info=" + route);
                    setReconnectionStatus(ReconnectionStatus.IN_PROGRESS);

                    CastDevice device = CastDevice.getFromBundle(route.getExtras());
                    LOGD(TAG, "onRouteAdded: Attempting to recover a session with device: " + device.getFriendlyName());
                    selectDevice(device);
                }
            }
        }

        @DebugLog
        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
            super.onRouteRemoved(router, route);
        }

        @DebugLog
        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            super.onRouteChanged(router, route);
        }

        @DebugLog
        @Override
        public void onProviderAdded(MediaRouter router, MediaRouter.ProviderInfo provider) {
            super.onProviderAdded(router, provider);
        }

        @DebugLog
        @Override
        public void onProviderRemoved(MediaRouter router, MediaRouter.ProviderInfo provider) {
            super.onProviderRemoved(router, provider);
        }

        @DebugLog
        @Override
        public void onProviderChanged(MediaRouter router, MediaRouter.ProviderInfo provider) {
            super.onProviderChanged(router, provider);
        }

    }
}
