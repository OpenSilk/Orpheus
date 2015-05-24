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

package org.opensilk.music.ui3.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.settings.SettingsActivity;
import org.opensilk.music.ui3.common.ActivityRequestCodes;
import org.opensilk.music.ui3.library.LandingScreenFragment;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import mortar.ViewPresenter;
import rx.functions.Action1;
import timber.log.Timber;

import static org.opensilk.music.library.provider.LibraryMethods.*;

/**
 * Created by drew on 5/1/15.
 */
@ScreenScope
public class DrawerScreenPresenter extends ViewPresenter<DrawerScreenView> {

    final Context appContext;
    final LibraryProviderInfoLoader loader;
    final AppPreferences settings;
    final FragmentManagerOwner fm;
    final DrawerOwner drawerOwner;
    final String mediaStoreAuthority;
    final ActivityResultsController activityResultsController;

    LibraryProviderInfo currentSelection;

    @Inject
    public DrawerScreenPresenter(
            @ForApplication Context context,
            LibraryProviderInfoLoader loader,
            AppPreferences settings,
            FragmentManagerOwner fm,
            DrawerOwner drawerOwner,
            @Named("mediaStoreLibraryAuthority") String mediaStoreAuthority,
            ActivityResultsController activityResultsController
    ) {
        this.appContext = context;
        this.loader = loader;
        this.settings = settings;
        this.fm = fm;
        this.drawerOwner = drawerOwner;
        this.mediaStoreAuthority = mediaStoreAuthority;
        this.activityResultsController = activityResultsController;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        //Note this happens synchronously
        loader.getActivePlugins().subscribe(new Action1<List<LibraryProviderInfo>>() {
            @Override
            public void call(List<LibraryProviderInfo> libraryProviderInfos) {
                if (hasView()) {
                    getView().getAdapter().replaceAll(libraryProviderInfos);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Timber.e(throwable, "onLoad");
            }
        });
        if (savedInstanceState != null) {
            currentSelection = savedInstanceState.getParcelable("lastselection");
        } else {
            String lastAuthority = settings.getString(AppPreferences.LAST_PLUGIN_AUTHORITY, null);
            if (lastAuthority == null) {
                lastAuthority = mediaStoreAuthority;
            }
            for (LibraryProviderInfo info : getView().getAdapter().getItems()) {
                if (StringUtils.equals(info.authority, lastAuthority)) {
                    currentSelection = info;
                    break;
                }
            }
            if (currentSelection != null) {
                goToCurrent();
            }
        }
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        if (currentSelection != null) {
            outState.putParcelable("lastselection", currentSelection);
            settings.putString(AppPreferences.LAST_PLUGIN_AUTHORITY, currentSelection.authority);
        }
    }

    @DebugLog
    void onItemClick(LibraryProviderInfo item) {
        currentSelection = item;
        goToCurrent();
    }

    void goToCurrent() {
        Bundle config = appContext.getContentResolver().call(
                LibraryUris.call(currentSelection.authority), LIBRARYCONF, null, null);
        if (config == null) {
            Timber.e("Got null config");
            //TODO toast
            return;
        }
        fm.killBackStack();
        fm.replaceMainContent(LandingScreenFragment.ni(config), false);
        drawerOwner.closeDrawer();
    }

    void openSettings() {
        Intent i = new Intent(appContext, SettingsActivity.class);
        activityResultsController.startActivityForResult(i, ActivityRequestCodes.APP_SETTINGS, null);
    }
}
