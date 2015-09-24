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

package org.opensilk.music.ui3.library;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortar.LayoutCreator;
import org.opensilk.common.ui.mortar.MortarContextFactory;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.common.ui.util.ViewUtils;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.loader.LibraryProviderInfoLoader;
import org.opensilk.music.model.Container;

import java.util.List;

import javax.inject.Inject;

import mortar.ViewPresenter;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 9/6/15.
 */
@ScreenScope
public class LibraryScreenPresenter extends ViewPresenter<LibraryScreenView> {

    final Context appContext;
    final LibraryProviderInfoLoader loader;
    final AppPreferences settings;
    final FragmentManagerOwner fm;
    final ActivityResultsController activityResultsController;

    //TODO inject these
    final MortarContextFactory contextFactory = new MortarContextFactory();
    final LayoutCreator layoutCreator = new LayoutCreator();

    @Inject
    public LibraryScreenPresenter(
            @ForApplication Context context,
            LibraryProviderInfoLoader loader,
            AppPreferences settings,
            FragmentManagerOwner fm,
            ActivityResultsController activityResultsController
    ) {
        this.appContext = context;
        this.loader = loader;
        this.settings = settings;
        this.fm = fm;
        this.activityResultsController = activityResultsController;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        //Note this happens synchronously
        loader.getActivePlugins().subscribe(new Action1<List<LibraryProviderInfo>>() {
            @Override
            public void call(List<LibraryProviderInfo> libraryProviderInfos) {
                for (LibraryProviderInfo info : libraryProviderInfos) {
                    LibraryRootScreen screen = new LibraryRootScreen(info);
                    ViewGroup scrollView = getView().getContentContainer();
                    Context newChildContext = contextFactory.setUpContext(screen, scrollView.getContext());
                    ViewUtils.inflate(newChildContext, layoutCreator.getLayout(screen), scrollView, true);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Timber.e(throwable, "onLoad");
            }
        });
    }

    void onRootClick(View view, Container root) {
        Uri callUri = LibraryUris.call(root.getUri().getAuthority());
        Bundle config = appContext.getContentResolver().call(callUri,
                LibraryMethods.CONFIG, null, null);
        if (config == null) {
            Timber.e("Unable to get library config");
            return; //TODO notify user
        }
        MortarFragment f = FoldersScreenFragment.ni(appContext, LibraryConfig.materialize(config), root);
        fm.replaceMainContent(f, true);
    }

}
