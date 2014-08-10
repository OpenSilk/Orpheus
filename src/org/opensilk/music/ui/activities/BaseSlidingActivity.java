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
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.opensilk.music.bus.events.PanelStateChanged;
import org.opensilk.music.ui.fragments.NowPlayingFragment;
import org.opensilk.music.ui.home.SearchFragment;

import timber.log.Timber;

import static android.app.SearchManager.QUERY;

/**
 *
 */
public class BaseSlidingActivity extends BaseActivity implements
        SlidingUpPanelLayout.PanelSlideListener {

    public static final int RESULT_RESTART_APP = RESULT_FIRST_USER << 1;
    public static final int RESULT_RESTART_FULL = RESULT_FIRST_USER << 2;

    /** Sliding panel */
    protected SlidingUpPanelLayout mSlidingPanel;

    /** Sliding panel content */
    protected NowPlayingFragment mNowPlayingFragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        super.onServiceConnected(name, service);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Make sure we dont overlap the panel
        maybeHideActionBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (killServiceOnExit) {
            stopService(new Intent(this, MusicPlaybackService.class));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("panel_open", mSlidingPanel.isPanelExpanded());
        outState.putBoolean("queue_showing", mNowPlayingFragment.isQueueShowing());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("panel_open", false)) {
                mActivityBus.post(new PanelStateChanged(PanelStateChanged.Action.SYSTEM_EXPAND));
                if (savedInstanceState.getBoolean("queue_showing", false)) {
                    mNowPlayingFragment.onQueueVisibilityChanged(true);
                }
            }
        }
    }

    @Override
    //@DebugLog
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
        if (mSlidingPanel.isPanelExpanded()) {
            maybeClosePanel();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected int getThemeId() {
        return ThemeHelper.getInstance(this).getPanelTheme();
    }

    /*
     * implement SlidingUpPanelLayout.PanelSlideListener
     */

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        if (slideOffset > 0.84) {
            TypedValue out = new TypedValue();
            getTheme().resolveAttribute(R.attr.actionBarSize, out, true);
            final int actionBarSize = TypedValue.complexToDimensionPixelSize(out.data, getResources().getDisplayMetrics());
            Timber.d("actionBarSize=" + actionBarSize + " panelTop=" + panel.getTop());
            if (panel.getTop() < actionBarSize) {
                if (getSupportActionBar().isShowing()) {
                    getSupportActionBar().hide();
                }
            }
        } else {
            if (!getSupportActionBar().isShowing()) {
                getSupportActionBar().show();
            }
        }
    }

    @Override
    public void onPanelExpanded(View panel) {
        mActivityBus.post(new PanelStateChanged(PanelStateChanged.Action.USER_EXPAND));
    }

    @Override
    public void onPanelCollapsed(View panel) {
        mActivityBus.post(new PanelStateChanged(PanelStateChanged.Action.USER_COLLAPSE));
    }

    @Override
    public void onPanelAnchored(View panel) {
        //not implemented
    }

    @Override
    public void onPanelHidden(View panel) {

    }

    public void maybeClosePanel() {
        if (mSlidingPanel.isPanelExpanded()) {
            mSlidingPanel.collapsePanel();
        }
    }

    public void maybeOpenPanel() {
        if (!mSlidingPanel.isPanelExpanded()) {
            mSlidingPanel.expandPanel();
        }
    }

    /**
     * Hides action bar if panel is expanded
     */
    protected void maybeHideActionBar() {
        if (mSlidingPanel.isPanelExpanded()
                && getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
        }
    }

}
