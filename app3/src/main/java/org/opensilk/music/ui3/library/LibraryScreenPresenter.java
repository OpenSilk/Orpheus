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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortar.LayoutCreator;
import org.opensilk.common.ui.mortar.MortarContextFactory;
import org.opensilk.common.ui.mortar.ScreenScoper;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.common.ui.util.ViewUtils;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.library.client.LibraryProviderInfoLoader;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Container;
import org.opensilk.music.ui3.common.ActivityRequestCodes;

import java.util.List;

import javax.inject.Inject;

import mortar.MortarScope;
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

    MortarContextFactory contextFactory;
    LayoutCreator layoutCreator;

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
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        if (scope.hasService(LayoutCreator.SERVICE_NAME)) {
            layoutCreator = LayoutCreator.getService(scope);
        } else {
            layoutCreator = new LayoutCreator();
        }
        if (scope.hasService(ScreenScoper.SERVICE_NAME)) {
            ScreenScoper screenScoper = ScreenScoper.getService(scope);
            contextFactory = new MortarContextFactory(screenScoper);
        } else {
            contextFactory = new MortarContextFactory();
        }
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
        LibraryConfig config = getConfig(root.getUri().getAuthority());
        MortarFragment f = FoldersScreenFragment.ni(appContext, config, root);
        fm.replaceMainContent(f, true);
    }

    public LibraryConfig getConfig(String authority) {
        Bundle reply = appContext.getContentResolver()
                .call(LibraryUris.call(authority),
                        LibraryMethods.CONFIG, null, null);
        return LibraryConfig.materialize(reply);
    }

    public void startLoginActivity(LibraryConfig config) {
        Intent intent = new Intent().setComponent(config.getLoginComponent());
        activityResultsController.startActivityForResult(intent, ActivityRequestCodes.LIBRARY_PICKER, null);
        //todo handle result heere
    }
}
