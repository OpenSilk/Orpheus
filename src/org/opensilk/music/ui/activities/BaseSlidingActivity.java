/*
 * Copyright (C) 2012 Andrew Neal
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
package org.opensilk.music.ui.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastStatusCodes;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.opensilk.cast.helpers.RemoteCastServiceManager;
import org.opensilk.music.artwork.ArtworkService;
import org.opensilk.music.bus.EventBus;
import org.opensilk.music.bus.events.MetaChanged;
import org.opensilk.music.bus.events.PlaybackModeChanged;
import org.opensilk.music.bus.events.PlaystateChanged;
import org.opensilk.music.bus.events.QueueChanged;
import org.opensilk.music.bus.events.Refresh;
import org.opensilk.music.cast.CastUtils;
import org.opensilk.music.cast.dialogs.StyledMediaRouteDialogFactory;
import org.opensilk.music.ui.fragments.NowPlayingFragment;

import java.lang.ref.WeakReference;
import java.util.Locale;

import hugo.weaving.DebugLog;

import static org.opensilk.cast.CastMessage.*;

/**
 * A base {@link FragmentActivity} used to update the bottom bar and
 * bind to Apollo's service.
 * <p>
 * {@link HomeSlidingActivity} extends from this skeleton.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public abstract class BaseSlidingActivity extends ActionBarActivity implements
        ServiceConnection,
        SlidingUpPanelLayout.PanelSlideListener {

    private static final String TAG = BaseSlidingActivity.class.getSimpleName();

    /** The service token */
    private ServiceToken mToken;

    /** Broadcast receiver */
    private PlaybackStatus mPlaybackStatus;

    /** Sliding panel */
    protected SlidingUpPanelLayout mSlidingPanel;

    /** Sliding panel content */
    protected NowPlayingFragment mNowPlayingFragment;

    /**
     * Cast stuff
     */
    private RemoteCastServiceManager.ServiceToken mCastServiceToken;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private boolean mTransientNetworkDisconnection = false;
    protected boolean isCastingEnabled;
    protected boolean killServiceOnExit;

    // Theme resourses
    private ThemeHelper mThemeHelper;

    protected PreferenceUtils mPreferences;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set our theme
        mThemeHelper = ThemeHelper.getInstance(this);
        setTheme(mThemeHelper.getPanelTheme());

        // Set the layout
        setContentView(R.layout.activity_base_sliding);

        // Setup action bar
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        // Fade it in
        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Control the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // get preferences
        mPreferences = PreferenceUtils.getInstance(this);

        // Bind Apollo's service
        mToken = MusicUtils.bindToService(this, this);

        //Cancel any pending clear cache requests
        startService(new Intent(this, ArtworkService.class));

        isCastingEnabled = mPreferences.isCastEnabled();

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

        // Initialize the sliding pane
        mSlidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingPanel.setDragView(findViewById(R.id.panel_header));
        mSlidingPanel.setPanelSlideListener(this);
        mSlidingPanel.setEnableDragViewTouchEvents(true);

        // Get panel fragment reference
        mNowPlayingFragment = (NowPlayingFragment) getSupportFragmentManager().findFragmentById(R.id.now_playing_fragment);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        boolean handled = mNowPlayingFragment.startPlayback(intent);
        if (handled) {
            setIntent(null);
        }
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        boolean handled = mNowPlayingFragment.startPlayback(getIntent());
        if (handled) {
            setIntent(null);
        }
        mNowPlayingFragment.onServiceConnected();
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        mNowPlayingFragment.onServiceDisconnected();
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start scanning for routes
        if (isCastingEnabled) {
            mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        }
        // Make sure we dont overlap the panel
        maybeHideActionBar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isCastingEnabled) {
            // stop scanning for routes
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Initialize the broadcast receiver
        mPlaybackStatus = new PlaybackStatus();
        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Shuffle and repeat changes
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicPlaybackService.REFRESH);
        // refresh queue
        filter.addAction(MusicPlaybackService.QUEUE_CHANGED);
        registerReceiver(mPlaybackStatus, filter);

        MusicUtils.notifyForegroundStateChanged(this, true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister the receiver
        try {
            unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable ignored) {
            //$FALL-THROUGH$
        } finally {
            mPlaybackStatus = null;
        }

        MusicUtils.notifyForegroundStateChanged(this, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unbind from the service
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }
        if (killServiceOnExit) {
            stopService(new Intent(this, MusicPlaybackService.class));
        }

        //Send request to clear cache
        startService(new Intent(ArtworkService.ACTION_CLEAR_CACHE,
                null, this, ArtworkService.class));

        //Unbind from cast service
        if (mCastServiceToken != null) {
            RemoteCastServiceManager.unbindFromService(mCastServiceToken);
            mCastServiceToken = null;
        }

    }

    @Override
    public void onBackPressed() {
        if (mSlidingPanel.isExpanded()) {
            maybeClosePanel();
        } else {
            super.onBackPressed();
        }
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

    /*
     * implement SlidingUpPanelLayout.PanelSlideListener
     */

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        if (slideOffset < 0.2) {
            if (getSupportActionBar().isShowing()) {
                getSupportActionBar().hide();
            }
        } else {
            if (!getSupportActionBar().isShowing()) {
                getSupportActionBar().show();
            }
        }
    }

    @Override
    public void onPanelExpanded(View panel) {
        mNowPlayingFragment.onPanelExpanded();
    }

    @Override
    public void onPanelCollapsed(View panel) {
        mNowPlayingFragment.onPanelCollapsed();
    }

    @Override
    public void onPanelAnchored(View panel) {
        //not implemented
    }

    public void maybeClosePanel() {
        if (mSlidingPanel.isExpanded()) {
            mSlidingPanel.collapsePane();
        }
    }

    public void maybeOpenPanel() {
        if (!mSlidingPanel.isExpanded()) {
            mSlidingPanel.expandPane();
        }
    }

    protected void setPanelExpanded() {
        mNowPlayingFragment.panelIsExpanded();
    }

    protected void setPanelCollapsed() {
        mNowPlayingFragment.panelIsCollapsed();
    }

    /**
     * Hides action bar if panel is expanded
     */
    protected void maybeHideActionBar() {
        if (mSlidingPanel.isExpanded()
                && getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
        }
    }

    /**
     * Handle mediarouter callbacks, responsible for keeping our mediarouter instance
     * in sync with the cast managers instance.
     */
    private final MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback() {

        @DebugLog
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            if (!CastUtils.notifyRouteSelected(BaseSlidingActivity.this, route)) {
                // If we couldnt notify the service we need to reset the router so
                // the user can try again
                router.selectRoute(router.getDefaultRoute());
            }
        }

        @DebugLog
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

        @DebugLog
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {

        }

        @DebugLog
        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {

        }

        @DebugLog
        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {

        }

    };

    /**
     * Handle messages sent from CastService notifying of CastManager events
     */
    private static final class CastManagerCallbackHandler extends Handler  {
        private final WeakReference<BaseSlidingActivity> reference;

        private CastManagerCallbackHandler(BaseSlidingActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            BaseSlidingActivity activity = reference.get();
            if (activity == null) {
                Log.e(TAG, "CastManagerCallbackHandler: activity was null");
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
                    Log.d(TAG, "onApplicationConnectionFailed(): failed due to: " + errorMsg);
                    resetDefaultMediaRoute(activity);
                    // notify if possible
                    if (MusicUtils.isForeground()) {
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
                    if (MusicUtils.isForeground()) {
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
        private void resetDefaultMediaRoute(BaseSlidingActivity activity) {
            // Reset the route
            activity.mMediaRouter.selectRoute(activity.mMediaRouter.getDefaultRoute());
        }
    };

    /**
     * Used to monitor the state of playback
     */
    private final class PlaybackStatus extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action == null) {
                return;
            } else if (action.equals(MusicPlaybackService.META_CHANGED)) {
                EventBus.getInstance().post(new MetaChanged());
            } else if (action.equals(MusicPlaybackService.PLAYSTATE_CHANGED)) {
                EventBus.getInstance().post(new PlaystateChanged());
            } else if (action.equals(MusicPlaybackService.REFRESH)) {
                EventBus.getInstance().post(new Refresh());
                // Cancel the broadcast so we aren't constantly refreshing
                context.removeStickyBroadcast(intent);
            } else if (action.equals(MusicPlaybackService.REPEATMODE_CHANGED)
                    || action.equals(MusicPlaybackService.SHUFFLEMODE_CHANGED)) {
               EventBus.getInstance().post(new PlaybackModeChanged());
            } else if (action.equals(MusicPlaybackService.QUEUE_CHANGED)) {
                EventBus.getInstance().post(new QueueChanged());
            }
        }
    }

}
