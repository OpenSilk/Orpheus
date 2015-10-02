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

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import org.opensilk.common.core.dagger2.SubScreenScope;
import org.opensilk.common.core.rx.RxLoader;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.model.Container;
import org.opensilk.bundleable.Bundleable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 9/23/15.
 */
@SubScreenScope
public class LibraryRootScreenPresenter extends ViewPresenter<LibraryRootScreenView>
        implements RxLoader.ContentChangedListener {

    final LibraryProviderInfo providerInfo;
    final LibraryScreenPresenter parentPresenter;
    final BundleableLoader loader;
    final Uri rootUri;

    final CompositeSubscription subscriptions = new CompositeSubscription();

    @Inject
    public LibraryRootScreenPresenter(
            LibraryProviderInfo providerInfo,
            LibraryScreenPresenter parentPresenter,
            BundleableLoader loader
    ) {
        this.providerInfo = providerInfo;
        this.parentPresenter = parentPresenter;
        this.loader = loader;
        this.rootUri = LibraryUris.rootUri(providerInfo.getAuthority());
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        this.loader.setUri(rootUri).setMethod(LibraryMethods.ROOTS);
        this.loader.addContentChangedListener(this);
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        getView().title.setText(providerInfo.getTitle());
        Drawable d = providerInfo.getIcon();
        if (d == null) {
            d = ContextCompat.getDrawable(getView().getContext(), R.drawable.ic_extension_grey600_24dp);
        }
//            int bounds = (int) (24 * holder.itemView.getResources().getDisplayMetrics().density);
//            d.setBounds(0, 0, bounds, bounds);
        getView().avatar.setImageDrawable(d);
        getView().setloading();
        subscribeRoots();
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        subscriptions.clear();
    }

    @Override
    @DebugLog
    public void reload() {
        subscriptions.clear();
        subscribeRoots();
    }

    LibraryScreenPresenter getParent() {
        return parentPresenter;
    }

    void subscribeRoots() {
        subscriptions.add(loader.getListObservable()
                .map(new Func1<List<Bundleable>, List<Container>>() {
                    @Override
                    public List<Container> call(List<Bundleable> bundleables) {
                        List<Container> containers = new ArrayList<Container>(bundleables.size());
                        for (Bundleable b : bundleables) {
                            if (b instanceof Container) {
                                containers.add((Container) b);
                            }
                        }
                        return containers;
                    }
                })
                .subscribe(new Action1<List<Container>>() {
                    @Override
                    public void call(List<Container> roots) {
                        if (hasView()) {
                            if (roots.size() > 0) {
                                getView().addRoots(roots);
                            } else {
                                getView().setRetry();
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.w(throwable, "getRootListing(%s)", providerInfo.getAuthority());
                        if (throwable instanceof LibraryException) {
                            LibraryException e = (LibraryException) throwable;
                            if (e.getCode() == LibraryException.Kind.AUTH_FAILURE) {
                                if (hasView()) {
                                    getView().setNeedsAuth();
                                    return;
                                }
                            }
                        }
                        if (hasView()) {
                            getView().setRetry();
                        }
                    }
                }));
    }

}
