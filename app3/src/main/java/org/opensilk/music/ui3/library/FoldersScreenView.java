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
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.common.ui.widget.FloatingActionButtonCheckable;
import org.opensilk.music.R;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.model.Container;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleableRecyclerAdapter;
import org.opensilk.music.ui3.common.BundleableRecyclerView2;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 9/17/15.
 */
public class FoldersScreenView extends CoordinatorLayout implements BundleableRecyclerView2 {

    @Inject BundleablePresenter mPresenter;
    @Inject BundleableRecyclerAdapter mAdapter;
    @Inject ToolbarOwner mToolbarOwner;
    @Inject @Named("folders_title") String mTitle;
    @Inject Container mThisContainer;
    @Inject IndexClient mIndexClient;

    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.recyclerview) RecyclerView mList;
    @InjectView(R.id.floating_action_button) FloatingActionButtonCheckable mFab;

    CompositeSubscription mSubscriptions = new CompositeSubscription();

    public FoldersScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        FoldersScreenComponent component = DaggerService.getDaggerComponent(getContext());
        component.inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        initRecyclerView();
        mPresenter.takeView(this);
        updateFab();
        subscribeClicks();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mToolbarOwner.attachToolbar(mToolbar);
            mToolbarOwner.setConfig(ActionBarConfig.builder().setTitle(mTitle)
                    .setMenuConfig(mPresenter.getMenuConfig()).build());
            mPresenter.takeView(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
        mToolbarOwner.detachToolbar(mToolbar);
        mSubscriptions.clear();
    }

    protected void initRecyclerView() {
        getListView().setHasFixedSize(true);
        getListView().setAdapter(mAdapter);
    }

    public void setupRecyclerView() {
        mAdapter.setGridStyle(mPresenter.isGrid());
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

    RecyclerView getListView() {
        return mList;
    }

    @Override
    public void setLoading(boolean loading) {

    }

    @Override
    public void setListShown(boolean show, boolean animate) {

    }

    @Override
    public void setListEmpty(boolean show, boolean animate) {

    }

    @Override
    public void setEmptyText(int resId) {
    }

    void updateFab() {
        mFab.setChecked(mIndexClient.isIndexed(mThisContainer));
    }

    void subscribeClicks() {
        mSubscriptions.add(RxView.clickEvents(mFab).subscribe(
                new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent onClickEvent) {
                        if (!mIndexClient.isIndexed(mThisContainer)) {
                            mIndexClient.add(mThisContainer);
                            mFab.setChecked(true);
                        } else {
                            mIndexClient.remove(mThisContainer);
                            //TODO show toast
                            mFab.setChecked(false);
                        }
                    }
                }
        ));
    }

}
