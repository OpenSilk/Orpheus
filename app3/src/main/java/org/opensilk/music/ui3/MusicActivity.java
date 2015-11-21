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
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionModeActivity;
import org.opensilk.common.ui.mortar.ActionModeDelegateCallback;
import org.opensilk.common.ui.mortar.ActionModePresenter;
import org.opensilk.common.ui.mortar.ActivityResultsActivity;
import org.opensilk.common.ui.mortar.ActivityResultsOwner;
import org.opensilk.common.ui.mortar.DialogFactory;
import org.opensilk.common.ui.mortar.DialogFactoryFragment;
import org.opensilk.common.ui.mortar.DialogPresenter;
import org.opensilk.common.ui.mortar.DialogPresenterActivity;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.common.ui.mortar.DrawerOwnerDelegate;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.common.ui.mortar.ToolbarOwnerDelegate;
import org.opensilk.common.ui.mortarfragment.MortarFragmentActivity;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.ActivityRequestCodes;
import org.opensilk.music.ui3.profile.playlist.PlaylistDetailsScreen;

import javax.inject.Inject;

import butterknife.ButterKnife;
import mortar.MortarScope;

/**
 * Created by drew on 5/1/15.
 */
public abstract class MusicActivity extends MortarFragmentActivity
        implements ActivityResultsActivity, ToolbarOwnerDelegate.Callback,
        ActionModeActivity, DialogPresenterActivity {

    @Inject protected ActivityResultsOwner mActivityResultsOwner;
    @Inject protected PlaybackController mPlaybackController;
    @Inject protected ToolbarOwner mToolbarOwner;
    @Inject protected DrawerOwner mDrawerOwner;
    @Inject protected ActionModePresenter mActionModePresenter;
    @Inject protected  DialogPresenter mDialogPresenter;

    protected ToolbarOwnerDelegate<MusicActivity> mToolbarOwnerDelegate;
    protected DrawerOwnerDelegate<MusicActivity> mDrawerOwnerDelegate;

    private ActionMode mActionMode;

    protected abstract void setupContentView();
    protected abstract void themeActivity(AppPreferences preferences);

    @Override
    protected void onScopeCreated(MortarScope scope) {
        MusicActivityComponent component = DaggerService.getDaggerComponent(scope);
        themeActivity(component.appPreferences());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlaybackController.connect();
        setupContentView();
        mActivityResultsOwner.takeView(this);
        DrawerLayout drawerLayout = ButterKnife.findById(this, R.id.drawer_layout);
        boolean hasdrawer = false;
        if (drawerLayout != null) {
            mDrawerOwnerDelegate = new DrawerOwnerDelegate<>(this, mDrawerOwner, drawerLayout,
                    R.string.navigation_drawer_close, R.string.navigation_drawer_open);
            mDrawerOwnerDelegate.onCreate();
            hasdrawer = true;
        }
        if (hasdrawer && hasLeftDrawer()) {
            mToolbarOwnerDelegate = new ToolbarOwnerDelegate<>(this, mToolbarOwner, mDrawerOwnerDelegate);
        }else {
            mToolbarOwnerDelegate = new ToolbarOwnerDelegate<>(this, mToolbarOwner, this);
        }
        mToolbarOwnerDelegate.onCreate();
        mActionModePresenter.takeView(this);
        mDialogPresenter.takeView(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerOwnerDelegate != null) mDrawerOwnerDelegate.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mActivityResultsOwner.dropView(this);
        mToolbarOwnerDelegate.onDestroy();
        if (mDrawerOwnerDelegate != null) mDrawerOwnerDelegate.onDestroy();
        mActionModePresenter.dropView(this);
        cancelActionMode();
        mDialogPresenter.dropView(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlaybackController.notifyForegroundStateChanged(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPlaybackController.notifyForegroundStateChanged(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mToolbarOwnerDelegate.onCreateOptionsMenu(menu) || super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return (mDrawerOwnerDelegate != null && mDrawerOwnerDelegate.onOptionsItemSelected(item))
                || mToolbarOwnerDelegate.onOptionsItemSelected(item)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerOwnerDelegate != null) mDrawerOwnerDelegate.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerOwnerDelegate != null && mDrawerOwnerDelegate.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public ActionMode startSupportActionMode(ActionMode.Callback callback) {
        mActionMode = super.startSupportActionMode(new ActionModeDelegateCallback(callback) {
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                super.onDestroyActionMode(mode);
                mActionMode = null;
            }
        });
        return mActionMode;
    }

    @Override
    public void cancelActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    /*
     * Activity results
     */

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, ThemeUtils.isLightTheme(this));
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        intent.putExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, ThemeUtils.isLightTheme(this));
        super.startActivityForResult(intent, requestCode, options);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ActivityRequestCodes.PLAYLIST_ADD: {
                if (resultCode == RESULT_OK) {
                    data.setExtrasClassLoader(MusicActivity.class.getClassLoader());
                    Bundle b = data.getBundleExtra("plist");
                    Playlist playlist = Playlist.BUNDLE_CREATOR.fromBundle(b);
                    mActivityResultsOwner.startActivityForResult(
                            ProfileActivity.makeIntent(this, new PlaylistDetailsScreen(playlist)),
                            ActivityRequestCodes.PROFILE, null);
                }
                break;
            }
            default:
                if (!mActivityResultsOwner.onActivityResult(requestCode, resultCode, data)) {
                    super.onActivityResult(requestCode, resultCode, data);
                }
        }
    }

    @Override
    public void setResultAndFinish(int resultCode, Intent data) {
        setResult(resultCode, data);
        finish();
    }

    /*
     * Toolbar/Drawer
     */

    @Override
    public void onToolbarAttached(Toolbar toolbar) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onToolbarDetached(Toolbar toolbar) {

    }

    protected boolean hasLeftDrawer() {
        return true;
    }

        /*
     * Dialog
     */

    @Override
    public void showDialog(DialogFactory factory) {
        dismissDialog();
        DialogFactoryFragment.ni(factory).show(getSupportFragmentManager(), "dialogg");
    }

    @Override
    public void dismissDialog() {
        DialogFragment f = (DialogFragment) getSupportFragmentManager().findFragmentByTag("dialogg");
        if (f != null) {
            f.dismiss();
        }
    }
}
