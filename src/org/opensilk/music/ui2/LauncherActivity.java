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
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.transition.Explode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortarflow.AppFlowPresenter;
import org.opensilk.common.mortarflow.MortarContextFactory;
import org.opensilk.common.util.ThemeUtils;
import org.opensilk.common.util.VersionUtils;
import org.opensilk.common.util.ViewUtils;
import org.opensilk.music.AppModule;
import org.opensilk.music.R;

import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.bus.events.IABQueryResult;
import org.opensilk.music.dialogs.SleepTimerDialog;
import org.opensilk.music.iab.IabUtil;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui2.library.PluginConnectionManager;
import org.opensilk.music.ui2.loader.LoaderModule;
import org.opensilk.music.ui2.main.DrawerOwner;
import org.opensilk.music.ui2.main.MainScreen;
import org.opensilk.music.ui2.main.NavScreen;

import javax.inject.Inject;
import javax.inject.Singleton;

import butterknife.ButterKnife;
import butterknife.InjectView;
import dagger.Provides;
import flow.Parcer;
import hugo.weaving.DebugLog;
import mortar.Blueprint;
import timber.log.Timber;


public class LauncherActivity extends BaseSwitcherActivity implements
        DrawerOwner.Activity {

    public static class Blueprint extends BaseMortarActivity.Blueprint {

        public Blueprint(String scopeName) {
            super(scopeName);
        }

        @Override
        public Object getDaggerModule() {
            return new Module();
        }

    }

    @dagger.Module (
            includes = {
                    BaseSwitcherActivity.Module.class,
                    MainScreen.Module.class,
                    NavScreen.Module.class
            },
            injects = LauncherActivity.class
    )
    public static class Module {

    }

    @Inject DrawerOwner mDrawerOwner;
    @Inject PluginConnectionManager mPluginConnectionManager;

    @InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @InjectView(R.id.drawer_container) ViewGroup mNavContainer;

    ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected Blueprint getBlueprint(String scopeName) {
        return new Blueprint(scopeName);
    }

    @Override
    public Screen getDefaultScreen() {
        return new GalleryScreen();
    }

    @Override
    protected void setupView() {
        setContentView(R.layout.activity_launcher);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHelper.getInstance(this).getTheme());
        if (VersionUtils.hasLollipop()) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
//            getWindow().setSharedElementExitTransition(new Explode());
        }
        super.onCreate(savedInstanceState);

        mDrawerOwner.takeView(this);
        setupDrawer();

        AppFlow.loadInitialScreen(this);

        // Update count for donate dialog
        IabUtil.incrementAppLaunchCount(mSettings);
        // check for donations
        IabUtil.queryDonateAsync(getApplicationContext(), mBus);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDrawerOwner != null) mDrawerOwner.dropView(this);
        if (!mConfigurationChangeIncoming) {
            // Release service connection
            mPluginConnectionManager.onDestroy();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPluginConnectionManager.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPluginConnectionManager.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sleep_timer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_sleep_timer:
                new SleepTimerDialog().show(getSupportFragmentManager(), "SleepTimer");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.v("onActivityResult");
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
                        killServiceOnExit = true;
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

    /*
     * Events
     */

    public void onEventMainThread(StartActivityForResult req) {
        req.intent.putExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, ThemeUtils.isLightTheme(this));
        startActivityForResult(req.intent, req.reqCode);
    }

    @DebugLog
    public void onEventMainThread(IABQueryResult r) {
        if (r.error == IABQueryResult.Error.NO_ERROR) {
            if (!r.isApproved) {
                IabUtil.maybeShowDonateDialog(this);
            }
        }
        //TODO handle faliurs
    }

    @Override
    public void setUpButtonEnabled(boolean enabled) {
        if (enabled) {
            disableDrawer(true);
        } else {
            enableDrawer();
        }
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
     * Theme stuff
     */

    protected void initThemeables() {
//        Themer.themeToolbar(mToolbar);
    }

}
