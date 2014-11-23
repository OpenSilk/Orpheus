/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2.gallery;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.widget.PopupMenu;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandler;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.RxLoader;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Subscription;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;
import static org.opensilk.common.rx.RxUtils.notSubscribed;

/**
 * Created by drew on 10/19/14.
 */
public abstract class BasePresenter<T> extends ViewPresenter<GalleryPageView> implements HasOptionsMenu, RxLoader.ContentChangedListener {

    protected final AppPreferences preferences;
    protected final ArtworkRequestManager artworkRequestor;
    protected final RxLoader<T> loader;
    protected final OverflowHandler<T> popupHandler;

    protected Subscription subscription;
    protected ActionBarOwner.MenuConfig actionBarMenu;

    public BasePresenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor,
                         RxLoader<T> loader, OverflowHandler<T> popupHandler) {
        this.preferences = preferences;
        this.artworkRequestor = artworkRequestor;
        this.loader = loader;
        this.popupHandler = popupHandler;
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
        if (notSubscribed(subscription)) {
            getView().setLoading(true);
            load();
        }
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (subscription != null) subscription.unsubscribe();
        loader.removeContentChangedListener(this);
    }

    @Override
    public ActionBarOwner.MenuConfig getMenuConfig() {
        ensureMenu();
        return actionBarMenu;
    }

    // Init the recyclerview
    protected void setupRecyclerView(boolean clear) {
        if (!viewNotNull()) return;
        BaseAdapter<T> adapter = newAdapter();
        adapter.setGridStyle(isGrid());
        RecyclerView v = getView().getListView();
        v.setHasFixedSize(true);
        v.setLayoutManager(getLayoutManager(v.getContext()));
        v.swapAdapter(adapter, clear);
        if (clear) load();
    }

    // reset the recyclerview for eg layoutmanager change
    protected void resetRecyclerView() {
        setupRecyclerView(true);
    }

    protected void showRecyclerView() {
        if (viewNotNull()) getView().setListShown(true, true);
    }

    protected void showEmptyView() {
        if (viewNotNull()) {
            setEmptyText();
            getView().setListEmpty(true, true);
        }
    }

    protected void setEmptyText() {
        if (viewNotNull()) getView().setEmptyText(R.string.empty_music);
    }

    protected BaseAdapter<T> getAdapter() {
        if (!viewNotNull()) throw new NullPointerException("You didn't check if list was null");
        return (BaseAdapter<T>) getView().getListView().getAdapter();
    }

    protected boolean viewNotNull() {
        return getView() != null;
    }

    //handle item clicks
    protected abstract void onItemClicked(BaseAdapter.ViewHolder holder, T item);
    // make a new adapter
    protected abstract BaseAdapter<T> newAdapter();
    // start the loader
    protected abstract void load();
    // init overflow menu
    protected abstract void ensureMenu();

    // cancels any ongoing load and starts a new one
    @DebugLog
    public void reload() {
        if (isSubscribed(subscription)) subscription.unsubscribe();
        if (viewNotNull()) getAdapter().clear();
        loader.reset();
        load();
    }

    protected void onCreateOverflowMenu(PopupMenu m, T item) {
        popupHandler.populateMenu(m, item);
    }

    protected boolean onOverflowItemClicked(OverflowAction action, T item) {
        return popupHandler.handleClick(action, item);
    }

    protected boolean isGrid() {
        return false;
    }

    protected RecyclerView.LayoutManager getLayoutManager(Context context) {
        if (isGrid()) {
            return makeGridLayoutManager(context);
        } else {
            return makeListLayoutManager(context);
        }
    }

    protected RecyclerView.LayoutManager makeGridLayoutManager(Context context) {
        int numCols = context.getResources().getInteger(R.integer.grid_columns);
        return new GridLayoutManager(context, numCols, GridLayoutManager.VERTICAL, false);
    }

    protected RecyclerView.LayoutManager makeListLayoutManager(Context context) {
        return new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
    }

}
