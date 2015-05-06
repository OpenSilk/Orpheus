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

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.common.ui.mortar.DrawerOwnerActivity;
import org.opensilk.common.ui.mortarfragment.MortarFragmentActivity;
import org.opensilk.music.AppComponent;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.MortarScope;

/**
 * Created by drew on 4/30/15.
 */
public class LauncherActivity extends MusicActivity implements DrawerOwnerActivity {

    @Inject DrawerOwner mDrawerOwner;

//    @InjectView(R.id.main_toolbar) Toolbar mToolbar;
    @InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @InjectView(R.id.drawer_container) ViewGroup mDrawer;

    private ActionBarDrawerToggle mDrawerToggle;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBarConfig config = ActionBarConfig.builder()
                .setTitle("")
                .build();
        mActionBarOwner.setConfig(config);

        if (mDrawerLayout != null) {
            mDrawerToggle = new Toggle(this, mDrawerLayout, mToolbar);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            mDrawerOwner.takeView(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDrawerOwner.dropView(this);//Noop if no view taken
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    /*
     * DrawerOwnerActivity
     */

    @Override
    public void openDrawer() {
        if (!isDrawerOpen()) mDrawerLayout.openDrawer(mDrawer);
    }

    public void closeDrawer() {
        if (isDrawerOpen()) mDrawerLayout.closeDrawer(mDrawer);
    }

    @Override
    public void disableDrawer(boolean hideIndicator) {
        if (mDrawerToggle != null) mDrawerToggle.setDrawerIndicatorEnabled(!hideIndicator);
        closeDrawer();
        if (mDrawerLayout != null) mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawer);
    }

    @Override
    public void enableDrawer() {
        if (mDrawerToggle != null) mDrawerToggle.setDrawerIndicatorEnabled(true);
        if (mDrawerLayout != null) mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawer);
    }

    /*
     * drawer helpers
     */

    private boolean isDrawerOpen() {
        return mDrawer != null && mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawer);
    }

    static class Toggle extends ActionBarDrawerToggle {
        final LauncherActivity activity;
        public Toggle(LauncherActivity activity, DrawerLayout drawerLayout, Toolbar toolbar) {
            super(activity, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
            this.activity = activity;
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
        }

        @Override
        public void onDrawerClosed(View view) {
            super.onDrawerClosed(view);
        }
    }
}
