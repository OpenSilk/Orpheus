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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.recycler.RecyclerListFrame;
import org.opensilk.music.R;

import javax.inject.Inject;

/**
 * Created by drew on 5/2/15.
 */
public class BundleableRecyclerList extends RecyclerListFrame implements BundleableRecyclerView {

    @Inject protected BundleablePresenter mPresenter;
    @Inject protected BundleableRecyclerAdapter mAdapter;

    public BundleableRecyclerList(Context context, AttributeSet attrs) {
        super(context, attrs);
        doInject();
    }

    protected void doInject() {
        BundleableComponent component = DaggerService.getDaggerComponent(getContext());
        component.inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initRecyclerView();
        mPresenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
    }

    protected void initRecyclerView() {
        getListView().setHasFixedSize(true);
        getListView().setAdapter(mAdapter);
    }

    public void setupRecyclerView() {
        mAdapter.setGridStyle(mPresenter.isGrid());
        mAdapter.setNumberTracks(mPresenter.wantsNumberedTracks());
        mAdapter.setAllowSelectionMode(mPresenter.isAllowLongPressSelection());
        getListView().getRecycledViewPool().clear();
        getListView().setLayoutManager(getLayoutManager());
    }

    public void notifyAdapterResetIncoming() {

    }

    protected RecyclerView.LayoutManager getLayoutManager() {
        if (mPresenter.isGrid()) {
            return makeGridLayoutManager();
        } else {
            return makeListLayoutManager();
        }
    }

    protected RecyclerView.LayoutManager makeGridLayoutManager() {
        int numCols = getContext().getResources().getInteger(R.integer.grid_columns);
        return new GridLayoutManager(getContext(), numCols, GridLayoutManager.VERTICAL, false);
    }

    protected RecyclerView.LayoutManager makeListLayoutManager() {
        return new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    }

    public BundleableRecyclerAdapter getAdapter() {
        return mAdapter;
    }

    public BundleablePresenter getPresenter() {
        return mPresenter;
    }

}
