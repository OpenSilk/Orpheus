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

package org.opensilk.music.ui3.main;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.recycler.DividerItemDecoration;
import org.opensilk.common.ui.recycler.HeaderRecyclerAdapter;
import org.opensilk.common.ui.util.ViewUtils;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by drew on 5/1/15.
 */
public class DrawerScreenView extends LinearLayout {

    @Inject DrawerScreenPresenter mPresenter;
    @Inject DrawerScreenViewAdapter mAdapter;

    @InjectView(android.R.id.list) RecyclerView mRecyclerView;
    @InjectView(R.id.car_mode) View mCarMode;

    public DrawerScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        DaggerService.<DrawerScreenComponent>getDaggerComponent(context).inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        HeaderRecyclerAdapter<DrawerScreenViewAdapter.ViewHolder> headerAdapter =
                new HeaderRecyclerAdapter<>(mAdapter);
        headerAdapter.addHeader(ViewUtils.inflate(getContext(), R.layout.drawer_banner, this, false));
        mRecyclerView.setAdapter(headerAdapter);
        mRecyclerView.setHasFixedSize(false);
        mPresenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
    }

    public DrawerScreenViewAdapter getAdapter() {
        return mAdapter;
    }

    @OnClick(R.id.app_settings)
    void openSettings() {
        mPresenter.openSettings(getContext());
    }

    @OnClick(R.id.car_mode)
    void opencarMode() {
        mPresenter.openCarMode(getContext());
    }
}
