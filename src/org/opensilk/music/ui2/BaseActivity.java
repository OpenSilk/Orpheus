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

package org.opensilk.music.ui2;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import org.opensilk.music.AppModule;
import org.opensilk.music.R;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.utils.MusicUtils;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastStatusCodes;

import org.opensilk.cast.helpers.RemoteCastServiceManager;
import org.opensilk.cast.util.CastPreferences;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.cast.CastUtils;
import org.opensilk.music.cast.dialogs.StyledMediaRouteDialogFactory;
import org.opensilk.music.MusicServiceConnection;

import java.lang.ref.WeakReference;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

import static org.opensilk.cast.CastMessage.CAST_APPLICATION_CONNECTION_FAILED;
import static org.opensilk.cast.CastMessage.CAST_APPLICATION_DISCONNECTED;
import static org.opensilk.cast.CastMessage.CAST_CONNECTION_SUSPENDED;
import static org.opensilk.cast.CastMessage.CAST_CONNECTIVITY_RECOVERED;
import static org.opensilk.cast.CastMessage.CAST_DISCONNECTED;
import static org.opensilk.cast.CastMessage.CAST_FAILED;

/**
 * Created by drew on 8/10/14.
 */
public class BaseActivity extends ActionBarActivity {

    @dagger.Module(addsTo = AppModule.class, library = true)
    public static class Module {
        @Provides @Singleton @Named("activity")
        public EventBus provideEventBus() {
            return new EventBus();
        }
    }

    // Cast stuff
    private RemoteCastServiceManager.ServiceToken mCastServiceToken;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private boolean mTransientNetworkDisconnection = false;
    protected boolean isCastingEnabled;
    protected boolean killServiceOnExit;

    protected boolean mIsResumed;
    protected boolean mConfigurationChangeIncoming;

    @Inject protected AppPreferences mSettings;
    @Inject protected MusicServiceConnection mMusicService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind Apollo's service
        mMusicService.bind();

        // Control the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        isCastingEnabled = checkCastingEnabled();

        if (isCastingEnabled) {
            // Bind cast service
            mCastServiceToken = RemoteCastServiceManager.bindToService(this,
                    new Messenger(new CastManagerCallbackHandler(this)),
                    null);
            // Initialize the media router
            mMediaRouter = MediaRouter.getInstance(this);
            mMediaRouteSelector = new MediaRouteSelector.Builder()
                    .addControlCategory(CastMediaControlIntent.categoryForCast(getString(R.string.cast_id)))
                            //.addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                    .build();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mConfigurationChangeIncoming) {
            Timber.d("Activity is finishing()");
            // Unbind from the service
            mMusicService.unbind();
            if (killServiceOnExit) {
                stopService(new Intent(this, MusicPlaybackService.class));
            }
        }
        //Unbind from cast service
        if (mCastServiceToken != null) {
            RemoteCastServiceManager.unbindFromService(mCastServiceToken);
            mCastServiceToken = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        MusicUtils.notifyForegroundStateChanged(this, true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MusicUtils.notifyForegroundStateChanged(this, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsResumed = true;
        // Start scanning for routes
        if (isCastingEnabled) {
            mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsResumed = false;
        if (isCastingEnabled) {
            // stop scanning for routes
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        mConfigurationChangeIncoming = true;
        return getObjectForRetain();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Media router
        if (isCastingEnabled) {
            getMenuInflater().inflate(R.menu.cast_mediarouter_button, menu);
            // init router button
            MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
            MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider)
                    MenuItemCompat.getActionProvider(mediaRouteMenuItem);
            mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
            mediaRouteActionProvider.setDialogFactory(new StyledMediaRouteDialogFactory());
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (MusicUtils.isRemotePlayback()) {
            double increment = 0;
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                increment = 0.05;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                increment = -0.05;
            }
            if (increment != 0) {
                CastUtils.changeRemoteVolume(increment);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean checkCastingEnabled() {
        return CastPreferences.getBoolean(this, CastPreferences.KEY_CAST_ENABLED, true);
    }

    protected Object getObjectForRetain() {
        return new Object();
    }

    /**
     * Handle mediarouter callbacks, responsible for keeping our mediarouter instance
     * in sync with the cast managers instance.
     */
    private final MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback() {

        //@DebugLog
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            if (!CastUtils.notifyRouteSelected(BaseActivity.this, route)) {
                // If we couldnt notify the service we need to reset the router so
                // the user can try again
                router.selectRoute(router.getDefaultRoute());
            }
        }

        //@DebugLog
        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            //
            if (!mTransientNetworkDisconnection) {
                if (MusicUtils.isPlaying()) {
                    MusicUtils.playOrPause();
                }
                CastUtils.notifyRouteUnselected();
            }
        }

        //@DebugLog
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {

        }

        //@DebugLog
        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {

        }

        //@DebugLog
        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {

        }

    };

    /**
     * Handle messages sent from CastService notifying of CastManager events
     */
    private static final class CastManagerCallbackHandler extends Handler {
        private final WeakReference<BaseActivity> reference;

        private CastManagerCallbackHandler(BaseActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            BaseActivity activity = reference.get();
            if (activity == null) {
                Timber.e("CastManagerCallbackHandler: activity was null");
                return;
            }

            switch (msg.what) {
                case CAST_APPLICATION_CONNECTION_FAILED:
                    final String errorMsg;
                    switch (msg.arg1) {
                        case CastStatusCodes.APPLICATION_NOT_FOUND:
                            errorMsg = "ERROR_APPLICATION_NOT_FOUND";
                            break;
                        case CastStatusCodes.TIMEOUT:
                            errorMsg = "ERROR_TIMEOUT";
                            break;
                        default:
                            errorMsg = "UNKNOWN ERROR: " + msg.arg1;
                            break;
                    }
                    Timber.d("onApplicationConnectionFailed(): failed due to: " + errorMsg);
                    resetDefaultMediaRoute(activity);
                    // notify if possible
                    if (activity.mIsResumed) {
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.cast_error)
                                .setMessage(String.format(Locale.getDefault(),
                                        activity.getString(R.string.cast_failed_to_connect), errorMsg))
                                .setNeutralButton(android.R.string.ok, null)
                                .show();
                    }
                    break;
                case CAST_APPLICATION_DISCONNECTED:
                    // This is just in case
                    resetDefaultMediaRoute(activity);
                    break;
                case CAST_CONNECTION_SUSPENDED:
                    activity.mTransientNetworkDisconnection = true;
                    break;
                case CAST_CONNECTIVITY_RECOVERED:
                    activity.mTransientNetworkDisconnection = false;
                    break;
                case CAST_DISCONNECTED:
                    activity.mTransientNetworkDisconnection = false;
                    break;
                case CAST_FAILED:
                    // notify if possible
                    if (activity.mIsResumed) {
                        switch (msg.arg1) {
                            case (R.string.failed_load):
                                new AlertDialog.Builder(activity)
                                        .setTitle(R.string.cast_error)
                                        .setMessage(R.string.failed_load)
                                        .setNeutralButton(android.R.string.ok, null)
                                        .show();
                                break;
                        }
                    }
                    break;
            }
        }

        /**
         * We only do this to reset the mediarouter buttons, the cast manager
         * will have already done this, but our buttons dont know about it
         */
        private void resetDefaultMediaRoute(BaseActivity activity) {
            // Reset the route
            activity.mMediaRouter.selectRoute(activity.mMediaRouter.getDefaultRoute());
        }
    };

}
