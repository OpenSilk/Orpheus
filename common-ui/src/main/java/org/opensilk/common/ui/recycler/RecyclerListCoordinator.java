/*
 * Copyright (C) 2015 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.ui.recycler;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.opensilk.common.ui.R;

import butterknife.ButterKnife;

/**
 * Created by drew on 11/24/14.
 */
public class RecyclerListCoordinator extends CoordinatorLayout {

    protected RecyclerView mList;
    protected View mEmptyView;
    protected TextView mEmptyText;
    protected ProgressBar mLoadingProgress;

    protected boolean mLoadingShown;
    protected boolean mListShown;
    protected boolean mEmptyShown;

    public RecyclerListCoordinator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mList = ButterKnife.findById(this, R.id.recyclerview);
        mEmptyView = ButterKnife.findById(this, R.id.empty_view);
        mEmptyText = ButterKnife.findById(this, R.id.empty_text);
        mLoadingProgress = ButterKnife.findById(this, R.id.loading_progress);
    }

    public RecyclerView getListView() {
        return mList;
    }

    public void setEmptyText(int stringRes) {
        mEmptyText.setText(getContext().getString(stringRes));
    }

    public void showLoading() {
        _setLoading(true);
        _setListEmpty(false, false);
        _setListShown(false, false);
    }

    public void showList(boolean animate) {
        _setLoading(false);
        _setListEmpty(false, animate);
        _setListShown(true, animate);
    }

    public void showEmpty(boolean animate) {
        _setLoading(false);
        _setListEmpty(true, animate);
        _setListShown(false, animate);
    }

    protected void _setLoading(boolean shown) {
        if (mLoadingShown == shown) {
            return;
        }
        mLoadingShown = shown;
        // we always animate
        mLoadingProgress.setAnimation(AnimationUtils.loadAnimation(
                getContext(), shown ? android.R.anim.fade_in : android.R.anim.fade_out));
        if (shown) {
            mLoadingProgress.setVisibility(VISIBLE);
        } else {
            mLoadingProgress.setVisibility(GONE);
        }
    }

    protected void _setListShown(boolean shown, boolean animate) {
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (animate) {
            mList.startAnimation(AnimationUtils.loadAnimation(
                    getContext(), shown ? android.R.anim.fade_in : android.R.anim.fade_out));
        } else {
            mList.clearAnimation();
        }
        if (shown) {
            mList.setVisibility(View.VISIBLE);
        } else {
            mList.setVisibility(View.GONE);
        }
    }

    protected void _setListEmpty(boolean shown, boolean animate) {
        if (mEmptyShown == shown) {
            return;
        }
        mEmptyShown = shown;
        if (animate) {
            mEmptyView.startAnimation(AnimationUtils.loadAnimation(
                    getContext(), shown ? android.R.anim.fade_in : android.R.anim.fade_out));
        } else {
            mEmptyView.clearAnimation();
        }
        if (shown) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }
}