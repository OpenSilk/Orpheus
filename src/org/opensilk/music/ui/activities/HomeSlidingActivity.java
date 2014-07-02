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

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;
import com.squareup.otto.Bus;

import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.artwork.ArtworkService;
import org.opensilk.music.loaders.NavigationLoader;
import org.opensilk.music.ui.library.LibraryFragment;
import org.opensilk.music.ui.modules.BackButtonListener;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.music.ui.settings.SettingsPhoneActivity;
import org.opensilk.music.util.PluginUtil;
import org.opensilk.music.util.RemoteLibraryUtil;
import org.opensilk.silkdagger.qualifier.ForActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 *
 */
public class HomeSlidingActivity extends BaseSlidingActivity implements
        LoaderManager.LoaderCallbacks<List<PluginInfo>>,
        DrawerHelper {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @InjectView(R.id.drawer_list)
    ListView mDrawerListView;
    @InjectView(R.id.drawer_container)
    View mDrawerContainerView;

    private ArrayAdapter<PluginInfo> mDrawerAdapter;

    private int mCurrentSelectedPosition = 0;
    private CharSequence mTitle;

    @Inject
    protected ArtworkService mArtworkService;
    @Inject @ForActivity
    protected Bus mActivityBus; // See comment in BaseSlidingActivity;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.inject(this);

        // cancel any pending clear cache request
        mArtworkService.cancelCacheClear();

        // Init drawer adapter
        // Add default plugin now since we know it will always be there
        List<PluginInfo> devices = new ArrayList<>();
        devices.add(PluginUtil.getDefaultPluginInfo(this));
        mDrawerAdapter = new ArrayAdapter<>(getSupportActionBar().getThemedContext(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                devices);
        mDrawerListView.setAdapter(mDrawerAdapter);
        // Start loader
        getSupportLoaderManager().initLoader(0, null, this);

        // set up the drawer's list view with items and click listener
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);

        // setup action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        // Load the music browser fragment
        if (savedInstanceState == null) {
            mDrawerLayout.post(new Runnable() {
                @Override
                public void run() {
                    selectItem(0);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            // safety measure to ensure we dont maintain remote connections
            RemoteLibraryUtil.unBindAll(this);
            // schedule cache clear
            mArtworkService.scheduleCacheClear();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isDrawerOpen()) {
            // search option
            getMenuInflater().inflate(R.menu.search, menu);
            // Settings
            getMenuInflater().inflate(R.menu.settings, menu);

            restoreActionBar();

            return super.onCreateOptionsMenu(menu);
        } else {
            showGlobalContextActionBar();
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivityForResult(new Intent(this, SettingsPhoneActivity.class), 0);
                return true;
            case R.id.menu_search:
                NavUtils.openSearch(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    //@DebugLog
    public void onBackPressed() {
        if (!mIsLargeLandscape && mSlidingPanel.isExpanded()) {
            maybeClosePanel();
        } else {
            Fragment f = getSupportFragmentManager().findFragmentByTag("library");
            if (f != null && (f instanceof BackButtonListener) && f.isResumed()) {
                BackButtonListener l = (BackButtonListener) f;
                if (l.onBackButtonPressed()) {
                    return;
                }
            }
            super.onBackPressed();
        }
    }

    @Override
    public void onPanelExpanded(View panel) {
        if (!mIsLargeLandscape) {
            // The drawer interferes with the queue swipe to remove
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerContainerView);
        }
        super.onPanelExpanded(panel);
    }

    @Override
    public void onPanelCollapsed(View panel) {
        if (!mIsLargeLandscape) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerContainerView);
        }
        super.onPanelCollapsed(panel);
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mDrawerContainerView);
        }
        PluginInfo pi = mDrawerAdapter.getItem(position);
        if (pi.componentName.equals(new ComponentName(this, org.opensilk.music.ui.home.HomeFragment.class))) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new org.opensilk.music.ui.home.HomeFragment(), "home").commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, LibraryFragment.newInstance(pi), "library")
                    .commit();
        }
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mTitle = actionBar.getTitle();
        actionBar.setTitle(R.string.app_name);
    }


    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        if (!TextUtils.isEmpty(mTitle)) {
            actionBar.setTitle(mTitle);
        }
    }

    /*
     * Abstract Methods
     */

    @Override
    protected int getLayoutId() {
        return R.layout.activity_homesliding;
    }

    @Override
    protected Bus provideBus() {
        return mActivityBus;
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new ActivityModule(this),
                new HomeModule(this)
        };
    }

    /*
     * DrawerHelper
     */

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawerContainerView);
    }

    /*
     * Loader callbacks
     */

    @Override
    public Loader<List<PluginInfo>> onCreateLoader(int id, Bundle args) {
        return new NavigationLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<PluginInfo>> loader, List<PluginInfo> data) {
        if (data != null && !data.isEmpty()) {
            for (PluginInfo pi : data) {
                if (mDrawerAdapter.getPosition(pi) < 0) {
                    mDrawerAdapter.add(pi);
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<List<PluginInfo>> loader) {
        PluginInfo pi = mDrawerAdapter.getItem(0);
        mDrawerAdapter.clear();
        mDrawerAdapter.add(pi);
    }

}
