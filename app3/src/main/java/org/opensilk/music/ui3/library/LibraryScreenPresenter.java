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
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.view.Gravity;
import android.view.View;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.loader.LibraryProviderInfoLoader;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.ViewPresenter;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.music.library.provider.LibraryMethods.CONFIG;
import static org.opensilk.music.library.provider.LibraryMethods.LIST;

/**
 * Created by drew on 9/6/15.
 */
public class LibraryScreenPresenter extends ViewPresenter<LibraryScreenView> {

    final Context appContext;
    final LibraryProviderInfoLoader loader;
    final AppPreferences settings;
    final FragmentManagerOwner fm;
    final ActivityResultsController activityResultsController;

    CompositeSubscription subscriptions = new CompositeSubscription();

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
                    final ProviderInfoItem item = new ProviderInfoItem(info);
                    getView().getAdapter().addItem(item);
                    subscriptions.add(getRootListing(item));
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Timber.e(throwable, "onLoad");
            }
        });
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        subscriptions.unsubscribe();
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

    Subscription getRootListing(final ProviderInfoItem item) {
        BundleableLoader l = new BundleableLoader(
                appContext,
                LibraryUris.rootUri(item.getInfo().getAuthority()),
                null
        ).setMethod(LibraryMethods.ROOTS);
        return l.createObservable()
                .map(new Func1<List<Bundleable>, List<Container>>() {
                    @Override
                    public List<Container> call(List<Bundleable> bundleables) {
                        List<Container> containers = new ArrayList<Container>(bundleables.size());
                        for (Bundleable b : bundleables) {
                            //This cast is safe since library filters for us
                            containers.add((Container) b);
                        }
                        return containers;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Container>>() {
                    @Override
                    public void call(List<Container> roots) {
                        if (hasView()) {
                            LibraryScreenViewAdapter adapter = getView().getAdapter();
                            int index = adapter.indexOf(item);
                            item.getRoots().addAll(roots);
                            item.setLoading(false);
                            adapter.notifyItemChanged(index);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.w(throwable, "getRootListing(%s)", item.getInfo().getAuthority());
                        if (throwable instanceof LibraryException) {
                            LibraryException e = (LibraryException) throwable;
                            if (e.getCode() == LibraryException.Kind.AUTH_FAILURE) {
                                item.setLoading(false);
                                item.setNeedsLogin(true);
                            }
                        }
                        if (hasView()) {
                            item.setLoading(false);
                            item.setError(true);
                        }
                        if (hasView()) {
                            LibraryScreenViewAdapter adapter = getView().getAdapter();
                            int index = adapter.indexOf(item);
                            adapter.notifyItemChanged(index);
                        }
                    }
                });
    }

}
