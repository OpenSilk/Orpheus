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

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.text.TextUtils;
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
import org.opensilk.music.bus.events.MusicServiceConnectionChanged;
import org.opensilk.music.bus.events.PanelStateChanged;
import org.opensilk.music.cast.CastUtils;
import org.opensilk.music.cast.dialogs.StyledMediaRouteDialogFactory;
import org.opensilk.music.iab.IabUtil;
import org.opensilk.music.ui.fragments.NowPlayingFragment;
import org.opensilk.music.ui.fragments.SearchFragment;
import org.opensilk.silkdagger.support.ScopedDaggerActionBarActivity;

import java.lang.ref.WeakReference;
import java.util.Locale;

import hugo.weaving.DebugLog;

import static android.app.SearchManager.QUERY;
import static org.opensilk.cast.CastMessage.*;

/**
 *
 */
public abstract class BaseSlidingActivity extends ScopedDaggerActionBarActivity implements
        ServiceConnection,
        SlidingUpPanelLayout.PanelSlideListener {

    private static final String TAG = BaseSlidingActivity.class.getSimpleName();

    public static final int RESULT_RESTART_APP = RESULT_FIRST_USER << 1;
    public static final int RESULT_RESTART_FULL = RESULT_FIRST_USER << 2;

    /** The service token */
    private ServiceToken mToken;

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
    protected ThemeHelper mThemeHelper;

    protected PreferenceUtils mPreferences;

    protected boolean mIsLargeLandscape;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set our theme
        mThemeHelper = ThemeHelper.getInstance(this);
        setTheme(mThemeHelper.getPanelTheme());

        // Set the layout
        setContentView(getLayoutId());

        // Setup action bar
        ActionBar ab = getSupportActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ab.setDisplayShowTitleEnabled(true);

        // Fade it in
        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Control the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // get preferences
        mPreferences = PreferenceUtils.getInstance(this);

        // Bind Apollo's service
        mToken = MusicUtils.bindToService(this, this);

        //Cancel any pending clear cache requests
        //TODO
//        startService(new Intent(this, ArtworkService.class));

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

        // Update count for donate dialog
        IabUtil.incrementAppLaunchCount(BaseSlidingActivity.this);

        // Initialize the sliding pane
        mSlidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingPanel.setDragView(findViewById(R.id.panel_header));
        mSlidingPanel.setPanelSlideListener(this);
        mSlidingPanel.setEnableDragViewTouchEvents(true);

        // Get panel fragment reference
        mNowPlayingFragment = (NowPlayingFragment) getSupportFragmentManager().findFragmentById(R.id.now_playing_fragment);

        mIsLargeLandscape = findViewById(R.id.landscape_dummy) != null;
        // Pinn the sliding pane open on landscape layouts
        if (mIsLargeLandscape && savedInstanceState == null) {
            mSlidingPanel.setSlidingEnabled(false);
            mSlidingPanel.setInitialState(SlidingUpPanelLayout.SlideState.EXPANDED);
            EventBus.getInstance().post(new PanelStateChanged(PanelStateChanged.Action.SYSTEM_EXPAND));
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(QUERY);
            if (!TextUtils.isEmpty(query)) {
                SearchFragment f = (SearchFragment) getSupportFragmentManager().findFragmentByTag("search");
                if (f != null) {
                    f.onNewQuery(query);
                    return;
                }
            }
        }
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
        EventBus.getInstance().post(new MusicServiceConnectionChanged(true));
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        EventBus.getInstance().post(new MusicServiceConnectionChanged(false));
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
        MusicUtils.notifyForegroundStateChanged(this, true);
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        // TODO
//        startService(new Intent(ArtworkService.ACTION_CLEAR_CACHE,
//                null, this, ArtworkService.class));

        //Unbind from cast service
        if (mCastServiceToken != null) {
            RemoteCastServiceManager.unbindFromService(mCastServiceToken);
            mCastServiceToken = null;
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("panel_open", mSlidingPanel.isExpanded());
        outState.putBoolean("queue_showing", mNowPlayingFragment.isQueueShowing());
        outState.putBoolean("panel_needs_collapse", mIsLargeLandscape);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            if (mIsLargeLandscape) {
                // Coming from portrait, need to pin the panel open
                mSlidingPanel.setSlidingEnabled(false);
                mSlidingPanel.setInitialState(SlidingUpPanelLayout.SlideState.EXPANDED);
                EventBus.getInstance().post(new PanelStateChanged(PanelStateChanged.Action.SYSTEM_EXPAND));
                if (savedInstanceState.getBoolean("queue_showing", false)) {
                    mNowPlayingFragment.onQueueVisibilityChanged(true);
                }
            } else if (savedInstanceState.getBoolean("panel_needs_collapse", false)) {
                // Coming back from landscape we should collapse the panel
                mSlidingPanel.setInitialState(SlidingUpPanelLayout.SlideState.COLLAPSED);
                EventBus.getInstance().post(new PanelStateChanged(PanelStateChanged.Action.SYSTEM_COLLAPSE));
                if (savedInstanceState.getBoolean("queue_showing", false)) {
                    mNowPlayingFragment.popQueueFragment();
                }
            } else if (savedInstanceState.getBoolean("panel_open", false)) {
                EventBus.getInstance().post(new PanelStateChanged(PanelStateChanged.Action.SYSTEM_EXPAND));
                if (savedInstanceState.getBoolean("queue_showing", false)) {
                    mNowPlayingFragment.onQueueVisibilityChanged(true);
                }
            }
        }
    }

    @Override
    @DebugLog
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_RESTART_APP) {
                    // Hack to force a refresh for our activity for eg theme change
                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    PendingIntent pi = PendingIntent.getActivity(this, 0,
                            getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName()),
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+700, pi);
                    finish();
                } else if (resultCode == RESULT_RESTART_FULL) {
                    killServiceOnExit = true;
                    onActivityResult(0, RESULT_RESTART_APP, data);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onBackPressed() {
        if (mIsLargeLandscape) {
            // We don't close the panel on landscape
            if (!getSupportFragmentManager().popBackStackImmediate()) {
                finish();
            }
        } else if (mSlidingPanel.isExpanded()) {
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
        //Dont hide action bar on tablets
        if (mIsLargeLandscape) {
            return;
        }
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
        EventBus.getInstance().post(new PanelStateChanged(PanelStateChanged.Action.USER_EXPAND));
    }

    @Override
    public void onPanelCollapsed(View panel) {
        EventBus.getInstance().post(new PanelStateChanged(PanelStateChanged.Action.USER_COLLAPSE));
    }

    @Override
    public void onPanelAnchored(View panel) {
        //not implemented
    }

    public void maybeClosePanel() {
        // On tablets panel is pinned open
        if (mIsLargeLandscape) {
            return;
        }
        if (mSlidingPanel.isExpanded()) {
            mSlidingPanel.collapsePane();
        }
    }

    public void maybeOpenPanel() {
        if (!mSlidingPanel.isExpanded()) {
            mSlidingPanel.expandPane();
        }
    }

    /**
     * Hides action bar if panel is expanded
     */
    protected void maybeHideActionBar() {
        //Dont hide action bar on tablets
        if (mIsLargeLandscape) {
            return;
        }
        if (mSlidingPanel.isExpanded()
                && getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
        }
    }

    public boolean isLargeLandscape() {
        return mIsLargeLandscape;
    }

    protected abstract int getLayoutId();

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

}
