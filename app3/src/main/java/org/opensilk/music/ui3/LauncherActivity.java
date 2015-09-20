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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.common.ui.mortar.DrawerController;
import org.opensilk.common.ui.mortar.DrawerOwnerActivity;
import org.opensilk.common.ui.mortar.DrawerOwnerDelegate;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.settings.SettingsActivity;
import org.opensilk.music.ui3.common.ActivityRequestCodes;
import org.opensilk.music.ui3.index.GalleryScreenFragment;
import org.opensilk.music.ui3.library.LibraryScreenFragment;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.MortarScope;

/**
 * Created by drew on 4/30/15.
 */
public class LauncherActivity extends MusicActivity {

    @Inject DrawerOwner mDrawerOwner;
    @Inject AppPreferences mSettings;
    @Inject FragmentManagerOwner mFm;

    @InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @InjectView(R.id.navigation) NavigationView mNavigation;

    DrawerOwnerDelegate<LauncherActivity> mDrawerOwnerDelegate;

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

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mDrawerLayout != null) {
            mDrawerOwnerDelegate = new DrawerOwnerDelegate<>(this, mDrawerOwner,
                    mDrawerLayout, R.string.app_name, R.string.app_name);
            mDrawerOwnerDelegate.onCreate();
            mNavigation.setNavigationItemSelectedListener(mNavigaitonClickListener);
            mNavigaitonClickListener.onNavigationItemSelected(mNavigation.getMenu().getItem(0));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDrawerOwnerDelegate != null) mDrawerOwnerDelegate.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerOwnerDelegate != null) mDrawerOwnerDelegate.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerOwnerDelegate != null) mDrawerOwnerDelegate.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return (mDrawerOwnerDelegate != null && mDrawerOwnerDelegate.onOptionsItemSelected(item))
                || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerOwnerDelegate != null && mDrawerOwnerDelegate.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onToolbarAttached(Toolbar toolbar) {
        if (mDrawerOwnerDelegate != null) {
            mDrawerOwnerDelegate.onToolbarAttached(toolbar);
        } else {
            super.onToolbarAttached(toolbar);
        }
    }

    @Override
    public void onToolbarDetached(Toolbar toolbar) {
        if (mDrawerOwnerDelegate != null) {
            mDrawerOwnerDelegate.onToolbarDetached(toolbar);
        } else {
            super.onToolbarDetached(toolbar);
        }
    }

    final NavigationView.OnNavigationItemSelectedListener mNavigaitonClickListener =
            new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
            mDrawerOwnerDelegate.closeDrawer(GravityCompat.START);
            switch (menuItem.getItemId()) {
                case R.id.my_library: {
                    mFm.killBackStack();
                    mFm.replaceMainContent(GalleryScreenFragment.ni(LauncherActivity.this), false);
                    menuItem.setChecked(true);
                    break;
                }
                case R.id.folders:
                    mFm.killBackStack();
                    mFm.replaceMainContent(LibraryScreenFragment.ni(), false);
                    menuItem.setChecked(true);
                    break;
                case R.id.settings: {
                    Intent i = new Intent(LauncherActivity.this, SettingsActivity.class);
                    startActivityForResult(i, ActivityRequestCodes.APP_SETTINGS, null);
                    break;
                }
                default:
                    return false;
            }
            return true;
        }

    };


}
