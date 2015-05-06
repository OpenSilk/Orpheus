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
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.ui3.library.LandingScreen;
import org.opensilk.music.ui3.library.LandingScreenFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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

    LibraryProviderInfo currentSelection;

    @Inject
    public DrawerScreenPresenter(
            @ForApplication Context context,
            LibraryProviderInfoLoader loader,
            AppPreferences settings,
            FragmentManagerOwner fm,
            DrawerOwner drawerOwner
    ) {
        this.appContext = context;
        this.loader = loader;
        this.settings = settings;
        this.fm = fm;
        this.drawerOwner = drawerOwner;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (savedInstanceState != null) {
            currentSelection = savedInstanceState.getParcelable("lastselection");
        }
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
        if (currentSelection != null) {
        }
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        outState.putParcelable("lastselection", currentSelection);
    }

    @DebugLog
    void onItemClick(LibraryProviderInfo item) {
        Bundle config = appContext.getContentResolver().call(LibraryUris.call(item.authority), LIBRARYCONF, null, null);
        if (config == null) {
            //TODO toast
            return;
        }
        currentSelection = item;
        LandingScreenFragment lsf = LandingScreenFragment.ni(config);
        fm.killBackstack();
        fm.replaceMainContent(lsf, LandingScreenFragment.TAG+currentSelection.authority, false);
        drawerOwner.closeDrawer();
    }
}
