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
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.common.ui.recycler.RecyclerListCoordinator;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by drew on 10/24/15.
 */
public class BundleableRecyclerCoordinator extends RecyclerListCoordinator implements BundleableRecyclerView {

    @Inject protected BundleablePresenter mPresenter;
    @Inject protected BundleableRecyclerAdapter mAdapter;
    @Inject protected ToolbarOwner mToolbarOwner;

    @InjectView(R.id.toolbar) protected Toolbar mToolbar;

    public BundleableRecyclerCoordinator(Context context, AttributeSet attrs) {
        super(context, attrs);
        inject();
    }

    protected void inject() {
        BundleableComponent cmp = DaggerService.getDaggerComponent(getContext());
        cmp.inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        initRecyclerView();
        if (!isInEditMode()) {
            mPresenter.takeView(this);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mToolbarOwner.attachToolbar(mToolbar);
            mToolbarOwner.setConfig(ActionBarConfig.builder().setTitle(R.string.page_playlists)
                    .setMenuConfig(mPresenter.getMenuConfig()).build());
            mPresenter.takeView(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
        mToolbarOwner.detachToolbar(mToolbar);
    }

    protected void initRecyclerView() {
        getListView().setHasFixedSize(true);
        getListView().setAdapter(mAdapter);
    }

    public void setupRecyclerView() {
        mAdapter.setGridStyle(mPresenter.isGrid());
        mAdapter.setNumberTracks(mPresenter.wantsNumberedTracks());
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
