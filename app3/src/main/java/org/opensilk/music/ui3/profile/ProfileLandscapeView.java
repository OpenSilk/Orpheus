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

package org.opensilk.music.ui3.profile;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.ui3.common.BundleableRecyclerAdapter;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import mortar.MortarScope;

/**
 * Created by drew on 11/21/14.
 */
public class ProfileLandscapeView extends LinearLayout implements ProfileView {

//    @Inject ProfilePresenter presenter;

    @InjectView(android.R.id.list) RecyclerView mList;

    boolean mLightTheme;

    BundleableRecyclerAdapter mAdapter;

    public ProfileLandscapeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLightTheme = ThemeUtils.isLightTheme(getContext());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
//        LayoutInflater.from(getContext()).inflate(
//                (presenter.getNumArtwork() >= 2) ?  R.layout.profile_hero4 : R.layout.profile_hero,
//                ButterKnife.<ViewGroup>findById(this, R.id.hero_holder),
//                true
//        );
        ButterKnife.inject(this);
//        mAdapter = presenter.makeAdapter(getContext());
        mList.setAdapter(mAdapter);
        mList.setLayoutManager(getLayoutManager(getContext()));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
//        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//        presenter.takeView(this);
    }

    @Override
    public MortarScope getScope() {
        return MortarScope.getScope(getContext());
    }

    @Override
    public BundleableRecyclerAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public boolean isLandscape() {
        return true;
    }

    @Override
    public void prepareRefresh() {

    }

    RecyclerView.LayoutManager getLayoutManager(Context context) {
//        if (presenter.isGrid()) {
            return makeGridLayoutManager(context);
//        } else {
//            return makeListLayoutManager(context);
//        }
    }

    RecyclerView.LayoutManager makeGridLayoutManager(Context context) {
        final int numCols = context.getResources().getInteger(R.integer.profile_grid_cols_horizontal);
        return new GridLayoutManager(context, numCols, GridLayoutManager.VERTICAL, false);
    }

    RecyclerView.LayoutManager makeListLayoutManager(Context context) {
        return new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
    }

}
