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
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.rx.RxLoader;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.ui.mortar.ActionBarMenuHandler;
import org.opensilk.common.ui.mortar.ActionModePresenter;
import org.opensilk.common.ui.mortar.HasOptionsMenu;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.library.client.BundleableLoader;
import org.opensilk.music.model.Model;
import org.opensilk.music.playback.control.PlaybackController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.Presenter;
import mortar.bundler.BundleService;
import rx.Subscriber;
import rx.Subscription;

/**
 * Created by drew on 5/2/15.
 */
@ScreenScope
public class BundleablePresenter extends Presenter<BundleableRecyclerView>
        implements RxLoader.ContentChangedListener, HasOptionsMenu, ActionBarMenuHandler {

    protected final AppPreferences preferences;
    protected final ArtworkRequestManager requestor;
    protected final BundleableLoader loader;
    protected final FragmentManagerOwner fm;
    protected final ItemClickListener itemClickListener;
    protected final MenuHandler menuConfig;
    protected final List<Bundleable> loaderSeed;
    protected final ActionModePresenter actionModePresenter;
    protected final PlaybackController playbackController;
    protected final IndexClient indexClient;
    protected final BundleablePresenterConfig presenterConfig;

    protected boolean wantGrid;
    protected Subscription subscription;
    protected boolean adapterIsDirty;
    private boolean isLoading;

    @Inject
    public BundleablePresenter(
            AppPreferences preferences,
            ArtworkRequestManager requestor,
            BundleableLoader loader,
            FragmentManagerOwner fm,
            BundleablePresenterConfig config,
            @Named("loader_uri") Uri uri,
            ActionModePresenter actionModePresenter,
            PlaybackController playbackController,
            IndexClient indexClient
    ) {
        this.preferences = preferences;
        this.requestor = requestor;
        this.loader = loader.setUri(uri).setSortOrder(preferences.getSortOrder(uri, config.defaultSortOrder));
        this.fm = fm;
        this.wantGrid = StringUtils.equals(preferences.getLayout(uri, config.wantsGrid), AppPreferences.GRID);
        this.itemClickListener = config.itemClickListener;
        this.menuConfig = config.menuConfig;
        this.loaderSeed = config.loaderSeed;
        this.actionModePresenter = actionModePresenter;
        this.playbackController = playbackController;
        this.indexClient = indexClient;
        this.presenterConfig = config;
    }

    @Override
    protected BundleService extractBundleService(BundleableRecyclerView view) {
        return BundleService.getBundleService(view.getContext());
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        loader.addContentChangedListener(this);
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        RxUtils.unsubscribe(subscription);
        loader.removeContentChangedListener(this);
    }

    @Override
    @DebugLog
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        setupRecyclerView(false);
        if (!isLoading) {
            getView().showLoading();
            load();
        }
    }

    @Override
    @DebugLog
    protected void onSave(Bundle outState) {
        super.onSave(outState);
    }

    protected void setupRecyclerView(boolean clear) {
        if (hasView()) {
            getView().setupRecyclerView();
            if (clear) {
                adapterIsDirty = true;
                RxUtils.unsubscribe(subscription);
                load();
            }
        }
    }

    // reset the recyclerview for eg layoutmanager change
    public void resetRecyclerView() {
        setupRecyclerView(true);
    }

    // start the loader
    @DebugLog
    protected void load() {
        isLoading = true;
        subscription = loader.getListObservable().subscribe(new Subscriber<List<Bundleable>>() {
            @Override
            @DebugLog
            public void onNext(List<Bundleable> bundleables) {
                addAll(bundleables);
            }

            @Override
            public void onCompleted() {
                isLoading = false;
                if (hasView() && getView().getAdapter().isEmpty()) {
                    getView().setEmptyText(R.string.empty_music);
                    getView().showEmpty(true);
                }
            }

            @Override
            public void onError(Throwable e) {
                isLoading = false;
                if (hasView()) {
                    getView().setEmptyText(R.string.error_loading_list);
                    getView().showEmpty(true);
                }
            }
        });
    }

    // cancels any ongoing load and starts a new one
    @DebugLog
    public void reload() {
        RxUtils.unsubscribe(subscription);
        adapterIsDirty = true;
        loader.reset();
        load();
    }

    protected void addAll(Collection<Bundleable> collection) {
        if (hasView()) {
            if (adapterIsDirty) {
                adapterIsDirty = false;
                getView().notifyAdapterResetIncoming();
                if (!loaderSeed.isEmpty()) {
                    List<Bundleable> toAdd = new ArrayList<>(loaderSeed.size() + collection.size());
                    toAdd.addAll(loaderSeed);
                    toAdd.addAll(collection);
                    getView().getAdapter().replaceAll(toAdd);
                } else {
                    getView().getAdapter().replaceAll(collection);
                }
            } else {
                getView().getAdapter().addAll(collection);
            }
            getView().showList(true);
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
            getView().showList(true);
        }
    }

    public List<Bundleable> getItems() {
        if (hasView()) {
            return getView().getAdapter().getItems();
        }
        return Collections.emptyList();
    }

    public List<Bundleable> getSelectedItems() {
        if (hasView()) {
            return getView().getAdapter().getSelectedItems();
        }
        return Collections.emptyList();
    }

    public void setWantsGrid(boolean yes) {

        wantGrid = yes;
    }

    public boolean isGrid() {
        return wantGrid;
    }

    public boolean wantsNumberedTracks() {
        return presenterConfig.wantsNumberedTracks;
    }

    public boolean isAllowLongPressSelection() {
        return presenterConfig.allowLongPressSelection;
    }

    public BundleableLoader getLoader() {
        return loader;
    }

    public void onItemClicked(Context context, Bundleable item) {
        if (itemClickListener != null) itemClickListener.onItemClicked(this, context, (Model) item);
    }

    @Override
    public boolean onBuildMenu(MenuInflater menuInflater, Menu menu) {
        return menuConfig.onBuildMenu(this, menuInflater, menu);
    }

    @Override
    public boolean onMenuItemClicked(Context context, MenuItem menuItem) {
        return menuConfig.onMenuItemClicked(this, context, menuItem);
    }

    @Override
    public ActionBarMenuHandler getMenuConfig() {
        return this;
    }

    public ArtworkRequestManager getRequestor() {
        return requestor;
    }

    public FragmentManagerOwner getFm() {
        return fm;
    }

    public AppPreferences getSettings() {
        return preferences;
    }

    public PlaybackController getPlaybackController() {
        return playbackController;
    }

    public IndexClient getIndexClient() {
        return indexClient;
    }

    public String getToolbarTitle() {
        return presenterConfig.toolbarTitle;
    }

    public void onFabClicked(View view) {
        if (presenterConfig.fabClickAction != null) {
            presenterConfig.fabClickAction.call(view.getContext(), this);
        } else {
            List<Uri> toPlay = UtilsCommon.filterTracks(getItems());
            if (toPlay.isEmpty()) {
                return; //TODO toast?
            }
            getPlaybackController().playAll(toPlay, 0);
        }
    }

    private ActionMode actionMode;

    public void onItemClicked(BundleableRecyclerAdapter.ViewHolder viewHolder, Bundleable item) {
        if (itemClickListener != null) itemClickListener.onItemClicked(this, viewHolder.itemView.getContext(), (Model)item);
    }

    public void onItemSelected() {
        if (actionMode != null) actionMode.invalidate();
    }

    public void onItemUnselected() {
        if (actionMode != null) {
            if (getView().getAdapter().getSelectedItems().isEmpty()) {
                actionMode.finish();
            } else {
                actionMode.invalidate();
            }
        }
    }

    public void onStartSelectionMode(BundleableRecyclerAdapter.OnSelectionModeEnded callback) {
        actionMode = actionModePresenter.startActionMode(new ActionModeCallback(callback));
    }

    class ActionModeCallback implements ActionMode.Callback {
        final BundleableRecyclerAdapter.OnSelectionModeEnded callback;

        public ActionModeCallback(BundleableRecyclerAdapter.OnSelectionModeEnded callback) {
            this.callback = callback;
        }

        @Override
        @DebugLog
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitleOptionalHint(true);
            boolean started = menuConfig.onBuildActionMenu(BundleablePresenter.this,
                    mode.getMenuInflater(), menu);
            if (!started) {
                mode.finish();
            }
            return started;
        }

        @Override
        @DebugLog
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean handled = menuConfig.onActionMenuItemClicked(BundleablePresenter.this,
                    getView().getContext(), item);
            if (handled) {
                mode.finish();
            }
            return handled;
        }

        @Override
        @DebugLog
        public void onDestroyActionMode(ActionMode mode) {
            callback.onEndSelectionMode();
            actionMode = null;
        }
    }
}
