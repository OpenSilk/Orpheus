package org.opensilk.music.ui2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.common.mortar.PauseAndResumeActivity;
import org.opensilk.common.mortar.PauseAndResumePresenter;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.music.R;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.library.PluginConnectionManager;
import org.opensilk.music.ui2.main.DrawerPresenter;
import org.opensilk.music.ui2.main.MainScreen;
import org.opensilk.music.ui2.main.MainView;
import org.opensilk.music.ui2.main.MusicServiceConnection;
import org.opensilk.music.ui2.main.NavScreen;
import org.opensilk.music.ui2.theme.Themer;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import flow.Flow;
import flow.Layouts;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;
import rx.functions.Func1;
import timber.log.Timber;


public class LauncherActivity extends ActionBarActivity implements
        PauseAndResumeActivity,
        ActionBarOwner.View,
        DrawerPresenter.View {

    @Inject @Named("activity") Bus mBus;
    @Inject PauseAndResumePresenter mPauseResumePresenter;
    @Inject ActionBarOwner mActionBarOwner;
    @Inject DrawerPresenter mDrawerPresenter;
    @Inject MusicServiceConnection mMusicService;
    @Inject PluginConnectionManager mPluginConnectionManager;

    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @InjectView(R.id.drawer_container)
    ViewGroup mNavContainer;
    @InjectView(R.id.mainview)
    MainView mMainView;
    @InjectView(R.id.sliding_layout) @Optional
    SlidingUpPanelLayout mSlidingPanel;
    @InjectView(R.id.main_toolbar)
    Toolbar mToolbar;

    MortarActivityScope mActivityScope;

    Flow mFlow;
    ActionBarOwner.MenuConfig mMenuConfig;
    ActionBarDrawerToggle mDrawerToggle;
    boolean mConfigurationChangeIncoming;
    String mScopeName;
    boolean mIsResumed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Light);
        super.onCreate(savedInstanceState);

        MortarScope parentScope = Mortar.getScope(getApplication());
        mActivityScope = Mortar.requireActivityScope(parentScope, new MainScreen(getScopeName()));
        mActivityScope.onCreate(savedInstanceState);
        Mortar.inject(this, this);

        mBus.register(this);
        mPauseResumePresenter.takeView(this);
        mMusicService.bind();

        setContentView(R.layout.activity_launcher);
        ButterKnife.inject(this);
        initThemeables();

        mFlow = mMainView.getFlow();
        mActionBarOwner.takeView(this);
        mDrawerPresenter.takeView(this);

        setSupportActionBar(mToolbar);

        setupDrawer();
        setupNavigation();

        setupSlindingPanel();

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
        if (mPauseResumePresenter != null) mPauseResumePresenter.dropView(this);
        if (mActionBarOwner != null) mActionBarOwner.dropView(this);
        if (mDrawerPresenter != null) mDrawerPresenter.dropView(this);

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
        if (mSlidingPanel != null) outState.putBoolean("panel_open", mSlidingPanel.isPanelExpanded());
//        outState.putBoolean("queue_showing", mNowPlayingFragment.isQueueShowing());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getBoolean("panel_open", false)) {
//            mActivityBus.post(new PanelStateChanged(PanelStateChanged.Action.SYSTEM_EXPAND));
//            if (savedInstanceState.getBoolean("queue_showing", false)) {
//                mNowPlayingFragment.onQueueVisibilityChanged(true);
//            }
        }
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
//            showGlobalContextActionBar();
            return false;
        } else {
//            restoreActionBar();
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
        } else if (isPanelOpen()) {
            maybeClosePanel();
        } else if (!mFlow.goBack()) {
            super.onBackPressed();
        }
    }

    @Override
    public Object getSystemService(String name) {
        if (Mortar.isScopeSystemService(name)) {
            return mActivityScope;
        }
        return super.getSystemService(name);
    }

    @Subscribe
    public void onStartActivityForResultEvent(StartActivityForResult req) {
        req.intent.putExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, ThemeHelper.isLightTheme(this));
        startActivityForResult(req.intent, req.reqCode);
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
    }

    private void setupNavigation() {
        Blueprint navScreen = new NavScreen();
        MortarScope newChildScope = mActivityScope.requireChild(navScreen);
        View newChild = Layouts.createView(newChildScope.createContext(this), navScreen);
        mNavContainer.addView(newChild);
    }

        /*
     * implement SlidingUpPanelLayout.PanelSlideListener
     */

//    @Override
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

//    @Override
    public void onPanelExpanded(View panel) {
//        mActivityBus.post(new PanelStateChanged(PanelStateChanged.Action.USER_EXPAND));
        disableDrawer(false);
    }

//    @Override
    public void onPanelCollapsed(View panel) {
//        mActivityBus.post(new PanelStateChanged(PanelStateChanged.Action.USER_COLLAPSE));
        enableDrawer();
    }

//    @Override
    public void onPanelAnchored(View panel) {
        //not implemented
    }

//    @Override
    public void onPanelHidden(View panel) {

    }

    // panel helpers

    public boolean isPanelOpen() {
        return mSlidingPanel != null && mSlidingPanel.isPanelExpanded();
    }

    public void maybeClosePanel() {
        if (mSlidingPanel != null && mSlidingPanel.isPanelExpanded()) {
            mSlidingPanel.collapsePanel();
        }
    }

    public void maybeOpenPanel() {
        if (mSlidingPanel != null && !mSlidingPanel.isPanelExpanded()) {
            mSlidingPanel.expandPanel();
        }
    }

    protected void maybeHideActionBar() {
        if (mSlidingPanel != null && mSlidingPanel.isPanelExpanded()
                && getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
        }
    }

    private void setupSlindingPanel() {
//        if (mSlidingPanel == null) return;
//        mSlidingPanel.setDragView(findViewById(R.id.panel_header));
//        mSlidingPanel.setPanelSlideListener(this);
//        mSlidingPanel.setEnableDragViewTouchEvents(true);
    }

    protected void initThemeables() {
//        Themer.themeToolbar(mToolbar);
    }

}
