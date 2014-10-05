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
import android.widget.ListView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.bus.EventBus;
import org.opensilk.music.bus.events.IABQueryResult;
import org.opensilk.music.iab.IabUtil;
import org.opensilk.music.ui.modules.BackButtonListener;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.music.ui.nav.adapter.NavAdapter;
import org.opensilk.music.ui.nav.loader.NavLoader;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

/**
 *
 */
public class HomeSlidingActivity extends BaseSlidingActivity implements
        LoaderManager.LoaderCallbacks<List<PluginInfo>>,
        DrawerHelper {

    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private ActionBarDrawerToggle mDrawerToggle;

    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @InjectView(R.id.drawer_list)
    ListView mDrawerListView;
    @InjectView(R.id.drawer_container)
    View mDrawerContainerView;

    private GlobalBusMonitor mBusMonitor;
    private NavAdapter mDrawerAdapter;

    private int mCurrentSelectedPosition = 0;
    private CharSequence mTitle;
    private CharSequence mSubTitle;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.inject(this);

        // setup action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(" ");

        // Load the music browser fragment
        if (savedInstanceState == null) {
            // todo save/restore previous
            mDrawerLayout.post(new Runnable() {
                @Override
                public void run() {
                    selectItem(1);
                }
            });
        }

        // register with global bus
        mBusMonitor = new GlobalBusMonitor();
        EventBus.getInstance().register(mBusMonitor);

        // Update count for donate dialog
        IabUtil.incrementAppLaunchCount(this);
        // check for donations
        IabUtil.queryDonateAsync(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getInstance().unregister(mBusMonitor);
        if (isFinishing()) {
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
    //@DebugLog
    public void onBackPressed() {
        if (mSlidingPanel.isPanelExpanded()) {
            maybeClosePanel();
        } else {
            Fragment f = getSupportFragmentManager().findFragmentByTag("library");
            if (f == null) {
                f = getSupportFragmentManager().findFragmentByTag("folders");
            }
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
        // The drawer interferes with the queue swipe to remove
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerContainerView);
        super.onPanelExpanded(panel);
    }

    @Override
    public void onPanelCollapsed(View panel) {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerContainerView);
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
        Runnable action = mDrawerAdapter.getItem(position).action;
        if (action != null && mIsResumed) {
            action.run();
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
        actionBar.setTitle(null);
        mSubTitle = actionBar.getSubtitle();
        actionBar.setSubtitle(null);
    }


    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        if (!TextUtils.isEmpty(mTitle)) {
            actionBar.setTitle(mTitle);
        }
        if (!TextUtils.isEmpty(mSubTitle)) {
            actionBar.setSubtitle(mSubTitle);
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
        return new NavLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<PluginInfo>> loader, List<PluginInfo> data) {
        if (data != null && !data.isEmpty()) {
            mDrawerAdapter.addPlugins(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<PluginInfo>> loader) {
        mDrawerAdapter.clear();
        mDrawerAdapter.addAll(NavAdapter.makeDefaultNavList(this));
    }

    class GlobalBusMonitor {
        @DebugLog
        @Subscribe
        public void onIABResult(IABQueryResult r) {
            if (r.error == IABQueryResult.Error.NO_ERROR) {
                if (!r.isApproved) {
                    IabUtil.maybeShowDonateDialog(HomeSlidingActivity.this);
                }
            }
            //TODO handle faliurs
        }
    }

}
