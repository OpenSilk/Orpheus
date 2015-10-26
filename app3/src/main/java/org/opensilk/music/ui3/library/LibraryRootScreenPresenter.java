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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.dagger2.SubScreenScope;
import org.opensilk.common.core.rx.RxLoader;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.library.client.BundleableLoader;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Container;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Subscriber;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

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
    final ArrayList<Container> rootsList = new ArrayList<>();

    boolean clearAdapterOnload = true;
    boolean isLoading;

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
    @DebugLog
    protected void onExitScope() {
        super.onExitScope();
        subscriptions.clear();
        loader.removeContentChangedListener(this);
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        getView().title.setText(providerInfo.getTitle());
        Drawable d = providerInfo.getIcon();
        if (d == null) {
            d = ContextCompat.getDrawable(getView().getContext(), R.drawable.puzzle_grey600_36dp);
        }
        getView().avatar.setImageDrawable(d);
        if (!rootsList.isEmpty()) {
            getView().addRoots(rootsList, clearAdapterOnload);
            clearAdapterOnload = false;
        } else if (!isLoading) {
            subscribeRoots();
            getView().setloading();
        }//else isLoading
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
    }

    @Override
    @DebugLog
    public void reload() {
        subscriptions.clear();
        clearAdapterOnload = true;
        subscribeRoots();
        if (hasView()) {
            getView().setloading();
        }
    }

    void populateMenu(Context context, PopupMenu popupMenu) {
        LibraryConfig config = parentPresenter.getConfig(providerInfo.getAuthority());
        if (config.hasFlag(LibraryConfig.FLAG_REQUIRES_AUTH)) {
            popupMenu.inflate(R.menu.library_login);
        }
    }

    void handlePopupItemClick(Context context, MenuItem item) {
        LibraryConfig config = parentPresenter.getConfig(providerInfo.getAuthority());
        switch (item.getItemId()) {
            case R.id.library_login: {
                getParent().startLoginActivity(config);
                break;
            }
        }
    }

    LibraryScreenPresenter getParent() {
        return parentPresenter;
    }

    void subscribeRoots() {
        isLoading = true;
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
                .subscribe(new Subscriber<List<Container>>() {
                    @Override
                    public void onCompleted() {
                        isLoading = false;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        isLoading = false;
                        rootsList.clear();
                        clearAdapterOnload = true;
                        String msg = throwable.getMessage();
                        if (throwable instanceof LibraryException) {
                            LibraryException e = (LibraryException) throwable;
                            if (e.getCode() == LibraryException.Kind.AUTH_FAILURE) {
                                if (hasView()) {
                                    getView().setNeedsAuth();
                                    return;
                                }
                            }
                            msg = e.getCause().getMessage();
                        }
                        if (hasView()) {
                            getView().setError(msg);
                        }
                    }

                    @Override
                    public void onNext(List<Container> roots) {
                        rootsList.addAll(roots);
                        if (hasView()) {
                            getView().addRoots(roots, clearAdapterOnload);
                            clearAdapterOnload = false;
                        }
                    }
                }));
    }

}
