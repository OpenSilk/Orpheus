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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.SlideState;

import org.opensilk.music.ui.fragments.SearchFragment;
import org.opensilk.music.ui.home.HomeFragment;
import org.opensilk.music.ui.settings.SettingsPhoneActivity;
import org.opensilk.music.ui.settings.SettingsTabletActivity;
import org.opensilk.music.util.ConfigHelper;

import static android.app.SearchManager.QUERY;

/**
 * This class is used to display the {@link ViewPager} used to swipe between the
 * main {@link Fragment}s used to browse the user's music.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class HomeSlidingActivity extends BaseSlidingActivity {

    public static final int RESULT_RESTART_APP = RESULT_FIRST_USER << 1;

    private boolean mIsLargeLandscape;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsLargeLandscape = findViewById(R.id.landscape_dummy) != null;
        // Pinn the sliding pane open on landscape layouts
        if (mIsLargeLandscape) {
            mSlidingPanel.setSlidingEnabled(false);
            mSlidingPanel.setInitialState(SlideState.EXPANDED);
            onPanelExpanded(null);
        }

        // Load the music browser fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new HomeFragment()).commit();
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
                }
            }
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("panel_needs_collapse", mIsLargeLandscape);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            if (mIsLargeLandscape) {
                // Coming from portrait, need to pin the panel open
                mSlidingPanel.setInitialState(SlideState.EXPANDED);
                onPanelExpanded(null);
            } else if (savedInstanceState.getBoolean("panel_needs_collapse", false)) {
                // Coming back from landscape we should collapse the panel
                mSlidingPanel.setInitialState(SlideState.COLLAPSED);
                onPanelCollapsed(null);
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
        // search option
        getMenuInflater().inflate(R.menu.search, menu);
        // Settings
        getMenuInflater().inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivityForResult(new Intent(this, ConfigHelper.isXLargeScreen(getResources()) ?
                        SettingsTabletActivity.class : SettingsPhoneActivity.class), 0);
                return true;
            case R.id.menu_search:
                NavUtils.openSearch(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_RESTART_APP) {
            // Hack to force a refresh for our activity for eg theme change
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent pi = PendingIntent.getActivity(this, 0,
                    getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName()),
                    PendingIntent.FLAG_CANCEL_CURRENT);
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+700, pi);
            finish();
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
    protected void maybeHideActionBar() {
        //Dont hide action bar on tablets
        if (!mIsLargeLandscape) {
            super.maybeHideActionBar();
        }
    }

    @Override
    public void maybeClosePanel() {
        // On tablets panel is pinned open
        if (!mIsLargeLandscape) {
            super.maybeClosePanel();
        }
    }

}
