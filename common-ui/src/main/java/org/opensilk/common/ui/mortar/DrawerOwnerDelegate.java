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

package org.opensilk.common.ui.mortar;

import android.app.Activity;
import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import org.opensilk.common.core.mortar.HasScope;

import mortar.MortarScope;

/**
 * Created by drew on 9/19/15.
 */
public class DrawerOwnerDelegate<A extends Activity & HasScope>
        implements DrawerOwnerActivity, ToolbarOwnerDelegate.Callback {

    final A mActivity;
    final DrawerOwner mDrawerOwner;
    final DrawerLayout mDrawerLayout;
    final int openContentDesc;
    final int closedContentDesc;

    Toolbar mToolbar;
    Toggle mDrawerToggle;

    public DrawerOwnerDelegate(
            A mActivity,
            DrawerOwner mDrawerOwner,
            DrawerLayout mDrawerLayout,
            int openContentDesc,
            int closedContentDesc
    ) {
        this.mActivity = mActivity;
        this.mDrawerOwner = mDrawerOwner;
        this.mDrawerLayout = mDrawerLayout;
        this.openContentDesc = openContentDesc;
        this.closedContentDesc = closedContentDesc;
    }

    /*
     * lifecycle
     */

    public void onCreate() {
        mDrawerToggle = new Toggle();
        mDrawerOwner.takeView(this);
    }

    public void onDestroy() {
        mDrawerOwner.dropView(this);
    }

    public void onPostCreate(Bundle savedInstanceState) {
        mDrawerToggle.syncState();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return mToolbar != null && mDrawerToggle.onOptionsItemSelected(item);
    }

    public boolean onBackPressed() {
        if (isAnyOpen()) {
            closeDrawers();
            return true;
        } else {
            return false;
        }
    }

    /*
     * end lifecycle
     */

    @Override
    public void onToolbarAttached(Toolbar toolbar) {
        setNewToolbar(toolbar);
    }

    @Override
    public void onToolbarDetached(Toolbar toolbar) {
        setNewToolbar(null);
    }

    private void setNewToolbar(@Nullable Toolbar toolbar) {
        mToolbar = toolbar;
        if (mDrawerLayout != null) {
            mDrawerToggle = new Toggle();
            mDrawerToggle.syncState();
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }
    }

    @Override
    public void openDrawer(int gravity) {
        if (mDrawerLayout != null && !mDrawerLayout.isDrawerOpen(gravity)) {
            mDrawerLayout.openDrawer(gravity);
        }
    }

    @Override
    public void openDrawers() {
        openDrawer(GravityCompat.START);
        openDrawer(GravityCompat.END);
    }

    @Override
    public void closeDrawer(int gravity) {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(gravity)) {
            mDrawerLayout.closeDrawer(gravity);
        }
    }

    @Override
    public void closeDrawers() {
        closeDrawer(GravityCompat.START);
        closeDrawer(GravityCompat.END);
    }

    public void enableDrawer(int gravity, boolean enable) {
        int lockmode = enable ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
        if (mDrawerLayout != null) mDrawerLayout.setDrawerLockMode(lockmode, gravity);
        if (gravity == GravityCompat.START && mDrawerToggle != null && mToolbar != null)
            mDrawerToggle.setDrawerIndicatorEnabled(enable);
    }

    @Override
    public void enableDrawers(boolean enable) {
        enableDrawer(GravityCompat.START, enable);
        enableDrawer(GravityCompat.END, enable);
    }

    @Override
    public MortarScope getScope() {
        return mActivity.getScope();
    }

    private boolean isAnyOpen() {
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
                return true;
            } else if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                return true;
            }
        }
        return false;
    }

    class Toggle extends ActionBarDrawerToggle {

        public Toggle() {
            super(mActivity, mDrawerLayout, mToolbar, openContentDesc, closedContentDesc);
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            super.onDrawerSlide(drawerView, slideOffset);
            mDrawerOwner.onDrawerSlide(drawerView, slideOffset);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            mDrawerOwner.onDrawerOpened(drawerView);
        }

        @Override
        public void onDrawerClosed(View view) {
            super.onDrawerClosed(view);
            mDrawerOwner.onDrawerClosed(view);
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            super.onDrawerStateChanged(newState);
            mDrawerOwner.onDrawerStateChanged(newState);
        }
    }

}
