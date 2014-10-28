/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.PauseAndResumeActivity;
import org.opensilk.common.mortar.PauseAndResumePresenter;
import org.opensilk.common.theme.TintManager;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.music.R;

import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.library.PluginConnectionManager;
import org.opensilk.music.ui2.main.DrawerOwner;
import org.opensilk.music.ui2.main.MusicServiceConnection;
import org.opensilk.music.ui2.main2.AppFlowPresenter;
import org.opensilk.music.ui2.main2.FrameScreenSwitcherView;
import org.opensilk.music.ui2.theme.Themer;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import de.greenrobot.event.EventBus;
import flow.Flow;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;
import timber.log.Timber;


public class LauncherActivity extends ActionBarActivity implements
        SlidingUpPanelLayout.PanelSlideListener,
        PauseAndResumeActivity,
        AppFlowPresenter.Activity,
        ActionBarOwner.Activity,
        DrawerOwner.Activity {

    @Inject @Named("activity") EventBus mBus;
    @Inject PauseAndResumePresenter mPauseResumePresenter;
    @Inject ActionBarOwner mActionBarOwner;
    @Inject DrawerOwner mDrawerOwner;
    @Inject MusicServiceConnection mMusicService;
    @Inject PluginConnectionManager mPluginConnectionManager;
    @Inject AppFlowPresenter<LauncherActivity> mAppFlowPresenter;

    @InjectView(R.id.main) FrameScreenSwitcherView mContainer;
    @InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @InjectView(R.id.drawer_container) ViewGroup mNavContainer;
    @InjectView(R.id.main_toolbar) Toolbar mToolbar;
    @InjectView(R.id.sliding_panel) @Optional SlidingUpPanelLayout mSlidingPanelContainer;

    MortarActivityScope mActivityScope;

    ActionBarOwner.MenuConfig mMenuConfig;
    ActionBarDrawerToggle mDrawerToggle;
    boolean mConfigurationChangeIncoming;
    String mScopeName;
    boolean mIsResumed;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Dark);
        super.onCreate(savedInstanceState);

        MortarScope parentScope = Mortar.getScope(getApplication());
        mActivityScope = Mortar.requireActivityScope(parentScope, new ActivityBlueprint(getScopeName()));
        Mortar.inject(this, this);

        mActivityScope.onCreate(savedInstanceState);

        mBus.register(this);
        mMusicService.bind();

        mAppFlowPresenter.takeView(this);
        mPauseResumePresenter.takeView(this);
        mActionBarOwner.takeView(this);
        mDrawerOwner.takeView(this);

        setContentView(R.layout.activity_launcher);
        ButterKnife.inject(this);
        initThemeables();

        setSupportActionBar(mToolbar);
        setupDrawer();

        setupSlidingPanel();

        AppFlow.loadInitialScreen(this);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        mConfigurationChangeIncoming = true;
        return mActivityScope.getName();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBus != null) mBus.unregister(this);
        if (mAppFlowPresenter != null) mAppFlowPresenter.dropView(this);
        if (mPauseResumePresenter != null) mPauseResumePresenter.dropView(this);
        if (mActionBarOwner != null) mActionBarOwner.dropView(this);
        if (mDrawerOwner != null) mDrawerOwner.dropView(this);

        if (!mConfigurationChangeIncoming) {
            Timber.d("Activity is finishing()");
            // Release service connection
            mMusicService.unbind();
            mPluginConnectionManager.onDestroy();
            // Destroy our scope
            if (mActivityScope != null && !mActivityScope.isDestroyed()) {
                MortarScope parentScope = Mortar.getScope(getApplication());
                parentScope.destroyChild(mActivityScope);
            }
            mActivityScope = null;
        }
    }

    @Override
    protected void onStart() {
        Timber.v("onStart()");
        super.onStart();
        mPluginConnectionManager.onResume();
    }

    @Override
    protected void onStop() {
        Timber.v("onStop()");
        super.onStop();
        mPluginConnectionManager.onPause();
    }

    @Override
    protected void onResume() {
        Timber.v("onResume()");
        super.onResume();
        mIsResumed = true;
        if (mPauseResumePresenter != null) mPauseResumePresenter.activityResumed();
    }

    @Override
    protected void onPause() {
        Timber.v("onPause()");
        super.onPause();
        if (mPauseResumePresenter != null) mPauseResumePresenter.activityPaused();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mActivityScope.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isDrawerOpen()) {
            return false;
        } else {
            if (mMenuConfig != null) {
                for (int item : mMenuConfig.menus) {
                    getMenuInflater().inflate(item, menu);
                }
            }
            getMenuInflater().inflate(R.menu.sleep_timer, menu);
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (mMenuConfig != null && mMenuConfig.actionHandler != null
                && mMenuConfig.actionHandler.call(item.getItemId())) {
            return true;
        }
        switch (item.getItemId()) {
            case android.R.id.home:
                return closePanel() || mContainer.onUpPressed();
            case R.id.menu_sleep_timer:
                NavUtils.openSleepTimerDialog(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer();
        } else if (closePanel()) {
            return;
        } else if (!mContainer.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public Object getSystemService(String name) {
        if (Mortar.isScopeSystemService(name)) return mActivityScope;
        if (AppFlow.isAppFlowSystemService(name)) return mAppFlowPresenter.getAppFlow();
        return super.getSystemService(name);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case StartActivityForResult.APP_REQUEST_SETTINGS:
                switch (resultCode) {
                    case ActivityResult.RESULT_RESTART_APP:
                        // Hack to force a refresh for our activity for eg theme change
                        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                        PendingIntent pi = PendingIntent.getActivity(this, 0,
                                getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName()),
                                PendingIntent.FLAG_CANCEL_CURRENT);
                        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+700, pi);
                        finish();
                        break;
                    case ActivityResult.RESULT_RESTART_FULL:
                        //TODO
//                        killServiceOnExit = true;
                        onActivityResult(StartActivityForResult.APP_REQUEST_SETTINGS,
                                ActivityResult.RESULT_RESTART_APP, data);
                        break;
                }
                break;
            case StartActivityForResult.PLUGIN_REQUEST_LIBRARY:
            case StartActivityForResult.PLUGIN_REQUEST_SETTINGS:
                mBus.post(new ActivityResult(data, requestCode, resultCode));
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String getScopeName() {
        if (mScopeName == null) mScopeName = (String) getLastCustomNonConfigurationInstance();
        if (mScopeName == null) {
            mScopeName = ObjectUtils.<LauncherActivity>getClass(this).getName() + UUID.randomUUID().toString();
        }
        return mScopeName;
    }

    /*
     * Events
     */

    public void onEventMainThread(StartActivityForResult req) {
        req.intent.putExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, Themer.isLightTheme(this));
        startActivityForResult(req.intent, req.reqCode);
    }

    public void onEventMainThread(MakeToast e) {
        if (e.type == MakeToast.Type.PLURALS) {
            Toast.makeText(this, MusicUtils.makeLabel(this, e.resId, e.arg), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, e.resId, Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * PausesAndResumes
     */

    @Override
    public boolean isRunning() {
        return mIsResumed;
    }

    /*
     * HasScope
     */

    public MortarScope getScope() {
        return mActivityScope;
    }

    /*
     * AppFlowPresenter
     */

    @Override
    public void showScreen(Screen screen, Flow.Direction direction, Flow.Callback callback) {
        mContainer.showScreen(screen, direction, callback);
    }

    /*
     * ActionBarOwner.View
     */

    @Override
    public void setShowHomeEnabled(boolean enabled) {

    }

    @Override
    public void setUpButtonEnabled(boolean enabled) {

    }

    @Override
    public void setMenu(ActionBarOwner.MenuConfig menuConfig) {
        mMenuConfig = menuConfig;
        supportInvalidateOptionsMenu();
    }

    /*
     * DrawerPresenter.View
     */

    @Override
    public void openDrawer() {
        if (!isDrawerOpen()) mDrawerLayout.openDrawer(mNavContainer);
    }

    public void closeDrawer() {
        if (isDrawerOpen()) mDrawerLayout.closeDrawer(mNavContainer);
    }

    @Override
    public void disableDrawer(boolean hideIndicator) {
        if (mDrawerToggle != null) mDrawerToggle.setDrawerIndicatorEnabled(!hideIndicator);
        closeDrawer();
        if (mDrawerLayout != null) mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mNavContainer);
    }

    @Override
    public void enableDrawer() {
        if (mDrawerToggle != null) mDrawerToggle.setDrawerIndicatorEnabled(true);
        if (mDrawerLayout != null) mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mNavContainer);
    }

    // Drawer Helpers

    private boolean isDrawerOpen() {
        return mNavContainer != null && mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mNavContainer);
    }

    private void setupDrawer() {
        if (mDrawerLayout == null) return;
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
//        mDrawerLayout.setScrimColor(getResources().getColor(android.R.color.transparent));
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
//                supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
//                supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
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
    }

    /*
     * Sliding panel
     */

    @Override
    public void onPanelSlide(View view, float v) {

    }

    @Override
    public void onPanelCollapsed(View view) {
        enableDrawer();
    }

    @Override
    public void onPanelExpanded(View view) {
        disableDrawer(true);
    }

    @Override
    public void onPanelAnchored(View view) {

    }

    @Override
    public void onPanelHidden(View view) {

    }

    //Panel helpers

    void setupSlidingPanel() {
        if (mSlidingPanelContainer == null) return;
        mSlidingPanelContainer.setDragView(findViewById(R.id.footer_view));
        mSlidingPanelContainer.setPanelSlideListener(this);
        mSlidingPanelContainer.setEnableDragViewTouchEvents(true);
    }

    boolean closePanel() {
        if (mSlidingPanelContainer != null
                && mSlidingPanelContainer.isPanelExpanded()) {
            mSlidingPanelContainer.collapsePanel();
            return true;
        }
        return false;
    }

    /*
     * Theme stuff
     */

    protected void initThemeables() {
//        Themer.themeToolbar(mToolbar);
    }

    TintManager mTintManager;

    @Override
    public Resources getResources() {
        ensureTintManager();
        return mTintManager.getResources();
    }

    void ensureTintManager() {
        if (mTintManager == null) {
            mTintManager = new TintManager(this, super.getResources());
        }
    }
}
