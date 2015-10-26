/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.ui3;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.view.MenuItem;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.settings.SettingsActivity;
import org.opensilk.music.ui.theme.OrpheusTheme;
import org.opensilk.music.ui3.common.ActivityRequestCodes;
import org.opensilk.music.ui3.common.ActivityResultCodes;
import org.opensilk.music.ui3.index.GalleryScreenFragment;
import org.opensilk.music.ui3.index.playlists.PlaylistsScreenFragment;
import org.opensilk.music.ui3.library.LibraryScreenFragment;
import org.opensilk.music.ui3.profile.playlist.PlaylistDetailsScreen;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 4/30/15.
 */
public class LauncherActivity extends MusicActivity {

    @Inject AppPreferences mSettings;
    @Inject FragmentManagerOwner mFm;

    @InjectView(R.id.navigation) NavigationView mNavigation;

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent appComponent = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE, LauncherActivityComponent.FACTORY.call(appComponent));
    }

    @Override
    protected void performInjection() {
        LauncherActivityComponent activityComponent = DaggerService.getDaggerComponent(this);
        activityComponent.inject(this);
    }

    @Override
    public int getContainerViewId() {
        return R.id.main;
    }

    @Override
    protected void setupContentView() {
        setContentView(R.layout.activity_launcher);
        ButterKnife.inject(this);
    }

    @Override
    protected void themeActivity(AppPreferences preferences) {
        boolean darkTheme = preferences.isDarkTheme();
        OrpheusTheme theme = preferences.getTheme();
        setTheme(darkTheme ? theme.dark : theme.light);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNavigation.setNavigationItemSelectedListener(mNavigaitonClickListener);
        if (savedInstanceState == null) {
            mNavigaitonClickListener.onNavigationItemSelected(
                    mNavigation.getMenu().getItem(mSettings.getInt(AppPreferences.LAST_NAVIGATION_ITEM, 0)));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        for (int ii=0; ii<mNavigation.getMenu().size(); ii++) {
            if (mNavigation.getMenu().getItem(ii).isChecked()) {
                mSettings.putInt(AppPreferences.LAST_NAVIGATION_ITEM, ii);
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.v("onActivityResult");
        switch (requestCode) {
            case ActivityRequestCodes.APP_SETTINGS:
                switch (resultCode) {
                    case ActivityResultCodes.RESULT_RESTART_APP:
                        // Hack to force a refresh for our activity for eg theme change
                        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                        PendingIntent pi = PendingIntent.getActivity(this, 0,
                                getBaseContext().getPackageManager()
                                        .getLaunchIntentForPackage(getBaseContext().getPackageName()),
                                PendingIntent.FLAG_CANCEL_CURRENT);
                        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+700, pi);
                        finish();
                        break;
                }
                break;
            case ActivityRequestCodes.PLAYLIST_ADD: {
                if (resultCode == RESULT_OK) {
                    Playlist playlist = Playlist.BUNDLE_CREATOR.fromBundle(data.getBundleExtra("plist"));
                    mActivityResultsOwner.startActivityForResult(
                            ProfileActivity.makeIntent(this, new PlaylistDetailsScreen(playlist)),
                            ActivityRequestCodes.PROFILE, null);
                }
                break;
            }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    final NavigationView.OnNavigationItemSelectedListener mNavigaitonClickListener =
            new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
            mDrawerOwnerDelegate.closeDrawer(GravityCompat.START);
            switch (menuItem.getItemId()) {
                case R.id.nav_my_library: {
                    mFm.killBackStack();
                    mFm.replaceMainContent(GalleryScreenFragment.ni(LauncherActivity.this), false);
                    menuItem.setChecked(true);
                    break;
                }
                case R.id.nav_libraries: {
                    mFm.killBackStack();
                    mFm.replaceMainContent(LibraryScreenFragment.ni(), false);
                    menuItem.setChecked(true);
                    break;
                }
                case R.id.nav_playlists: {
                    mFm.killBackStack();
                    mFm.replaceMainContent(PlaylistsScreenFragment.ni(LauncherActivity.this), false);
                    menuItem.setChecked(true);
                    break;
                }
                case R.id.nav_settings: {
                    Intent i = new Intent(LauncherActivity.this, SettingsActivity.class);
                    mActivityResultsOwner.startActivityForResult(i, ActivityRequestCodes.APP_SETTINGS, null);
                    break;
                }
                default:
                    return false;
            }
            return true;
        }

    };


}
