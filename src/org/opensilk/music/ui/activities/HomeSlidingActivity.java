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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.SlideState;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.PluginInfo;
import org.opensilk.music.bus.EventBus;
import org.opensilk.music.bus.events.NavDrawerEvent;
import org.opensilk.music.bus.events.PanelStateChanged;
import org.opensilk.music.ui.fragments.NavigationDrawerFragment;
import org.opensilk.music.ui.fragments.SearchFragment;
import org.opensilk.music.ui.home.HomeFragment;
import org.opensilk.music.ui.library.LibraryHomeFragment;
import org.opensilk.music.ui.settings.SettingsPhoneActivity;
import org.opensilk.music.util.RemoteLibraryUtil;

import hugo.weaving.DebugLog;

import static android.app.SearchManager.QUERY;

/**
 * This class is used to display the {@link ViewPager} used to swipe between the
 * main {@link Fragment}s used to browse the user's music.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class HomeSlidingActivity extends BaseSlidingActivity {

    public static final int RESULT_RESTART_APP = RESULT_FIRST_USER << 1;
    public static final int RESULT_RESTART_FULL = RESULT_FIRST_USER << 2;

    private boolean mIsLargeLandscape;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    protected NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    protected CharSequence mTitle;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the drawer
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mIsLargeLandscape = findViewById(R.id.landscape_dummy) != null;
        // Pinn the sliding pane open on landscape layouts
        if (mIsLargeLandscape && savedInstanceState == null) {
            mSlidingPanel.setSlidingEnabled(false);
            mSlidingPanel.setInitialState(SlideState.EXPANDED);
            EventBus.getInstance().post(new PanelStateChanged(PanelStateChanged.Action.SYSTEM_EXPAND));
        }

        // Load the music browser fragment
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.main, new HomeFragment()).commit();
//        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(QUERY);
            if (!TextUtils.isEmpty(query)) {
                SearchFragment f = (SearchFragment) getSupportFragmentManager().findFragmentByTag("search");
                if (f != null) {
                    f.onNewQuery(query);
                }
            }
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getInstance().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getInstance().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RemoteLibraryUtil.unBindAll(this);
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
                mSlidingPanel.setInitialState(SlideState.EXPANDED);
                EventBus.getInstance().post(new PanelStateChanged(PanelStateChanged.Action.SYSTEM_EXPAND));
                if (savedInstanceState.getBoolean("queue_showing", false)) {
                    mNowPlayingFragment.onQueueVisibilityChanged(true);
                }
            } else if (savedInstanceState.getBoolean("panel_needs_collapse", false)) {
                // Coming back from landscape we should collapse the panel
                mSlidingPanel.setInitialState(SlideState.COLLAPSED);
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
    public void onBackPressed() {
        if (mIsLargeLandscape) {
            // We don't close the panel on landscape
            if (!getSupportFragmentManager().popBackStackImmediate()) {
                finish();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // search option
            getMenuInflater().inflate(R.menu.search, menu);
            // Settings
            getMenuInflater().inflate(R.menu.settings, menu);

            return super.onCreateOptionsMenu(menu);
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivityForResult(new Intent(this, SettingsPhoneActivity.class), 0);
                return true;
            case R.id.menu_search:
                NavUtils.openSearch(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
    public void onPanelSlide(View panel, float slideOffset) {
        //Dont hide action bar on tablets
        if (!mIsLargeLandscape) {
            super.onPanelSlide(panel, slideOffset);
        }
    }

    @Override
    public void maybeClosePanel() {
        // On tablets panel is pinned open
        if (!mIsLargeLandscape) {
            super.maybeClosePanel();
        }
    }

    @Override
    protected void maybeHideActionBar() {
        //Dont hide action bar on tablets
        if (!mIsLargeLandscape) {
            super.maybeHideActionBar();
        }
    }

    /*
     * Abstract Methods
     */

    @Override
    protected int getLayoutId() {
        return R.layout.activity_base_sliding;
    }

    /*
     * Events
     */
    @Subscribe
    public void onDrawerItemSelected(NavDrawerEvent.ItemSelected e) {
        PluginInfo pi = e.pluginInfo;
        if (pi.componentName == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new HomeFragment()).commit();
        } else {
            if (!RemoteLibraryUtil.isBound(pi.componentName)) {
                RemoteLibraryUtil.bindToService(this, pi.componentName);
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, LibraryHomeFragment.newInstance(pi))
                    .commit();
        }
    }

    /**
     *
     */
    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    public boolean isLargeLandscape() {
        return mIsLargeLandscape;
    }

}
