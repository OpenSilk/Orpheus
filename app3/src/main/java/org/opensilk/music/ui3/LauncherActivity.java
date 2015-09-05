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
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.common.ui.mortar.DrawerOwnerActivity;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryCapability;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.loader.LibraryProviderInfoLoader;
import org.opensilk.music.settings.SettingsActivity;
import org.opensilk.music.ui3.common.ActivityRequestCodes;
import org.opensilk.music.ui3.gallery.GalleryPage;
import org.opensilk.music.ui3.gallery.GalleryScreenFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.MortarScope;
import rx.functions.Action1;

import static org.opensilk.music.library.provider.LibraryMethods.LIBRARYCONF;

/**
 * Created by drew on 4/30/15.
 */
public class LauncherActivity extends MusicActivity implements DrawerOwnerActivity {

    @Inject DrawerOwner mDrawerOwner;
    @Inject AppPreferences mSettings;
    @Inject @Named("IndexProviderAuthority") String mIndexProviderAuthority;
    @Inject @Named("mediaStoreLibraryAuthority") String mMediaStoreLibraryAuthority;
    @Inject FragmentManagerOwner mFm;
    @Inject LibraryProviderInfoLoader mLibraryProviderLoader;

//    @InjectView(R.id.main_toolbar) Toolbar mToolbar;
    @InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @InjectView(R.id.navigation) NavigationView mNavigation;

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
    protected void themeActivity(AppPreferences preferences) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        ActionBarConfig config = ActionBarConfig.builder()
//                .setTitle("")
//                .build();
//        mToolbarOwner.setConfig(config);

        if (mDrawerLayout != null) {
            setToolbar(null);
            mDrawerOwner.takeView(this);
            mNavigation.setNavigationItemSelectedListener(mNavigaitonClickListener);
            mNavigaitonClickListener.onNavigationItemSelected(mNavigation.getMenu().getItem(0));
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
        if (!isDrawerOpen()) mDrawerLayout.openDrawer(mNavigation);
    }

    public void closeDrawer() {
        if (isDrawerOpen()) mDrawerLayout.closeDrawer(mNavigation);
    }

    @Override
    public void disableDrawer(boolean hideIndicator) {
        if (mDrawerToggle != null) mDrawerToggle.setDrawerIndicatorEnabled(!hideIndicator);
        closeDrawer();
        if (mDrawerLayout != null) mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mNavigation);
    }

    @Override
    public void enableDrawer() {
        if (mDrawerToggle != null) mDrawerToggle.setDrawerIndicatorEnabled(true);
        if (mDrawerLayout != null) mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mNavigation);
    }

    /*
     * Toolbar
     */

    @Override
    public void onToolbarAttached(Toolbar toolbar) {
        setToolbar(toolbar);
    }

    @Override
    public void onToolbarDetached(Toolbar toolbar) {
        setToolbar(null);
    }

    /*
     * drawer helpers
     */

    private void setToolbar(Toolbar toolbar) {
        if (mDrawerLayout != null) {
            mDrawerToggle = new Toggle(this, mDrawerLayout, toolbar);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }
    }

    private boolean isDrawerOpen() {
        return mNavigation != null && mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mNavigation);
    }

    final NavigationView.OnNavigationItemSelectedListener mNavigaitonClickListener =
            new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.my_library: {
                    menuItem.setChecked(true);
                    Bundle config = getApplicationContext().getContentResolver().call(
                            LibraryUris.call(mMediaStoreLibraryAuthority), LIBRARYCONF, null, null);
                    if (config == null) {
                        throw new RuntimeException("Got null config for index provider");
                    }
                    LibraryConfig libraryConfig = LibraryConfig.materialize(config);
                    List<GalleryPage> pages = new ArrayList<>();
                    if (libraryConfig.hasAbility(LibraryCapability.PLAYLISTS)) {
                        pages.add(GalleryPage.PLAYLIST);
                    }
                    if (libraryConfig.hasAbility(LibraryCapability.ARTISTS)) {
                        pages.add(GalleryPage.ARTIST);
                    }
                    if (libraryConfig.hasAbility(LibraryCapability.ALBUMS)) {
                        pages.add(GalleryPage.ALBUM);
                    }
                    if (libraryConfig.hasAbility(LibraryCapability.GENRES)) {
                        pages.add(GalleryPage.GENRE);
                    }
                    if (libraryConfig.hasAbility(LibraryCapability.TRACKS)) {
                        pages.add(GalleryPage.SONG);
                    }
                    LibraryInfo currentSelection = mSettings.getLibraryInfo(libraryConfig,
                            AppPreferences.DEFAULT_LIBRARY);
                    GalleryScreenFragment f = GalleryScreenFragment.ni(LauncherActivity.this,
                            libraryConfig, currentSelection, pages);
                    mFm.killBackStack();
                    mFm.replaceMainContent(f, false);
                    break;
                }
                case R.id.folders:
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
            closeDrawer();
            return true;
        }

    };

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
