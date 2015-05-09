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

package org.opensilk.music.ui3.common;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.rx.RxLoader;
import org.opensilk.common.core.rx.SimpleObserver;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.common.ui.mortar.HasOptionsMenu;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.model.util.BundleableUtil;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Subscription;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static org.opensilk.common.core.rx.RxUtils.notSubscribed;

/**
 * Created by drew on 5/2/15.
 */
@ScreenScope
public class BundleablePresenter extends ViewPresenter<BundleableRecyclerView>
        implements RxLoader.ContentChangedListener, HasOptionsMenu {

    protected final AppPreferences preferences;
    protected final ArtworkRequestManager requestor;
    protected final BundleableLoader loader;
    protected final FragmentManagerOwner fm;
    protected final ItemClickListener itemClickListener;
    protected final OverflowClickListener overflowClickListener;
    protected final ActionBarMenuConfig menuConfig;

    protected Boolean wantGrid;

    protected Subscription subscription;
    protected boolean adapterIsDirty;

    @Inject
    public BundleablePresenter(
            AppPreferences preferences,
            ArtworkRequestManager requestor,
            BundleableLoader loader,
            FragmentManagerOwner fm,
            BundleablePresenterConfig config
    ) {
        this.preferences = preferences;
        this.requestor = requestor;
        this.loader = loader;
        this.fm = fm;
        this.wantGrid = config.wantsGrid;
        this.itemClickListener = config.itemClickListener;
        this.overflowClickListener = config.overflowClickListener;
        this.menuConfig = config.menuConfig;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        loader.addContentChangedListener(this);
    }

    @Override
    @DebugLog
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        setupRecyclerView(false);
//        if (savedInstanceState != null) {
//            addAll(BundleableUtil.unflatten(savedInstanceState));
//            getView().setListShown(true, false);
//            adapterIsDirty = true;
//        } else {
            getView().setLoading(true);
//        }
        if (notSubscribed(subscription)) {
            load();
        }
    }

    @Override
    @DebugLog
    protected void onSave(Bundle outState) {
        super.onSave(outState);
//        if (hasView()) {
//            BundleableUtil.flatten(outState, getView().getAdapter().getItems());
//        }
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (subscription != null) subscription.unsubscribe();
        loader.removeContentChangedListener(this);
    }

    protected void setupRecyclerView(boolean clear) {
        if (hasView()) {
            getView().setupRecyclerView();
            if (clear) {
                if (isSubscribed(subscription)) {
                    subscription.unsubscribe();
                }
                adapterIsDirty = true;
                load();
            }
        }
    }

    // reset the recyclerview for eg layoutmanager change
    public void resetRecyclerView() {
        setupRecyclerView(true);
    }

    protected void showRecyclerView() {
        if (hasView()) {
            getView().setListShown(true, true);
        }
    }

    protected void showEmptyView() {
        if (hasView()) {
            setEmptyText();
            getView().setListEmpty(true, true);
        }
    }

    protected void setEmptyText() {
        if (hasView()) {
            getView().setEmptyText(R.string.empty_music);
        }
    }

    // start the loader
    @DebugLog
    protected void load() {
        subscription = loader.getListObservable().subscribe(new SimpleObserver<List<Bundleable>>() {
            @Override
            @DebugLog
            public void onNext(List<Bundleable> bundleables) {
                addAll(bundleables);
            }

            @Override
            public void onCompleted() {
                if (hasView() && getView().getAdapter().isEmpty()){
                    showEmptyView();
                }
            }
        });
    }

    // cancels any ongoing load and starts a new one
    @DebugLog
    public void reload() {
        if (isSubscribed(subscription)) {
            subscription.unsubscribe();
        }
        adapterIsDirty = true;
        loader.reset();
        load();
    }

    protected void addAll(Collection<Bundleable> collection) {
        if (hasView()) {
            if (adapterIsDirty) {
                adapterIsDirty = false;
                getView().notifyAdapterResetIncoming();
                getView().getAdapter().replaceAll(collection);
            } else {
                getView().getAdapter().addAll(collection);
            }
            showRecyclerView();
        }
    }

    protected void addItem(Bundleable item) {
        if (hasView()) {
            if (adapterIsDirty) {
                adapterIsDirty = false;
                getView().notifyAdapterResetIncoming();
                getView().getAdapter().clear();
            }
            getView().getAdapter().addItem(item);
            showRecyclerView();
        }
    }

    public List<Bundleable> getItems() {
        if (hasView()) {
            return getView().getAdapter().getItems();
        }
        return null;
    }

    public void setWantsGrid(boolean yes) {
        wantGrid = yes;
    }

    public boolean isGrid() {
        return wantGrid;
    }

    public BundleableLoader getLoader() {
        return loader;
    }

    public void onItemClicked(Context context, Bundleable item) {
        itemClickListener.onItemClicked(this, context, item);
    }

    public void onOverflowClicked(Context context, PopupMenu m, Bundleable item) {
        overflowClickListener.onBuildMenu(context, m, item);
    }

    public boolean onOverflowActionClicked(Context context, OverflowAction action, Bundleable item) {
        return overflowClickListener.onItemClicked(context, action, item);
    }

    @Override
    public ActionBarMenuConfig getMenuConfig() {
        return menuConfig;
    }

    public ArtworkRequestManager getRequestor() {
        return requestor;
    }

    public FragmentManagerOwner getFm() {
        return fm;
    }
}
