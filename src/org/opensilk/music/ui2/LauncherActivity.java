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
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.util.ThemeUtils;
import org.opensilk.music.R;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.iab.event.IABQueryResult;
import com.andrew.apollo.menu.SleepTimerDialog;
import org.opensilk.music.iab.IabUtil;
import org.opensilk.music.theme.OrpheusTheme;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui2.library.PluginConnectionManager;
import org.opensilk.music.ui2.core.android.DrawerOwner;
import org.opensilk.music.ui2.main.Main;
import org.opensilk.music.ui2.main.Nav;

import javax.inject.Inject;

import butterknife.InjectView;
import hugo.weaving.DebugLog;
import timber.log.Timber;


public class LauncherActivity extends BaseSwitcherToolbarActivity implements
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
                    BaseSwitcherToolbarActivity.Module.class,
                    Main.Module.class,
                    Nav.Module.class
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
    protected mortar.Blueprint getBlueprint(String scopeName) {
        return new Blueprint(scopeName);
    }

    @Override
    protected void setupTheme() {
        OrpheusTheme orpheusTheme = mSettings.getTheme();
        setTheme(mSettings.isDarkTheme() ? orpheusTheme.dark : orpheusTheme.light);
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
        super.onCreate(savedInstanceState);

        mDrawerOwner.takeView(this);
        setupDrawer();

        AppFlow.loadInitialScreen(this);

        // Update count for donate dialog
        IabUtil.incrementAppLaunchCount(mSettings);
        // check for donations
        IabUtil.queryDonateAsync(getApplicationContext(), mBus);

        handleIntent();
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
    protected void onNewIntent(Intent intent) {
        if (intent == null) return;
        setIntent(intent);
        handleIntent();
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

    void handleIntent() {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        String mimeType = intent.getType();
        boolean handled = false;
        if (uri != null && uri.toString().length() > 0) {
            mMusicService.playFile(uri);
            handled = true;
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            long id = intent.getLongExtra("playlistId", -1);
            if (id < 0) {
                String idString = intent.getStringExtra("playlist");
                if (idString != null) {
                    try {
                        id = Long.parseLong(idString);
                    } catch (NumberFormatException ignored) { }
                }
            }
            if (id >= 0) {
                mMusicService.playPlaylist(getApplicationContext(), id, false);
                handled = true;
            }
        }
        if (handled) {
            getIntent().setData(null);
            getIntent().setType(null);
        } else {
            mBus.post(new MakeToast(R.string.err_generic));
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
                IabUtil.maybeShowDonateDialog(mSettings, this);
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

}
