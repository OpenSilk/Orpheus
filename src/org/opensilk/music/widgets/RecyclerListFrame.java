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

package org.opensilk.music.widgets;

import android.content.Context;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.opensilk.music.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by drew on 11/24/14.
 */
public class RecyclerListFrame extends FrameLayout {

    @InjectView(R.id.list_container) protected View mListContainer;
    @InjectView(R.id.recyclerview) protected RecyclerView mList;
    @InjectView(R.id.empty_view) protected View mEmptyView;
    @InjectView(R.id.empty_text) protected TextView mEmptyText;
    @InjectView(R.id.loading_progress) protected ContentLoadingProgressBar mLoadingProgress;

    protected boolean mLoadingShown;
    protected boolean mListShown;
    protected boolean mEmptyShown;

    public RecyclerListFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
    }

    public RecyclerView getListView() {
        return mList;
    }

    public void setEmptyText(int stringRes) {
        mEmptyText.setText(getContext().getString(stringRes));
    }

    public void setLoading(boolean shown) {
        if (mLoadingShown == shown) {
            return;
        }
        mLoadingShown = shown;
        // this is on a delay so we always animate
        mLoadingProgress.setAnimation(AnimationUtils.loadAnimation(
                getContext(), shown ? android.R.anim.fade_in : android.R.anim.fade_out));
        if (shown) {
            mLoadingProgress.show();
        } else {
            mLoadingProgress.hide();
        }
    }

    public void setListShown(boolean shown, boolean animate) {
        setLoading(!shown);
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (animate) {
            mListContainer.startAnimation(AnimationUtils.loadAnimation(
                    getContext(), shown ? android.R.anim.fade_in : android.R.anim.fade_out));
        } else {
            mListContainer.clearAnimation();
        }
        if (shown) {
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            mListContainer.setVisibility(View.GONE);
        }
    }

    public void setListEmpty(boolean shown, boolean animate) {
        if (mEmptyShown == shown) {
            return;
        }
        mEmptyShown = shown;
        if (shown) {
            if (mListShown) {
                if (animate) {
                    mList.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
                    mEmptyView.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
                } else {
                    mList.clearAnimation();
                    mEmptyView.clearAnimation();
                }
                mList.setVisibility(GONE);
                mEmptyView.setVisibility(VISIBLE);
            } else {
                mList.setVisibility(GONE);
                mEmptyView.setVisibility(VISIBLE);
                setListShown(true, animate);
            }
        } else {
            if (mListShown) {
                if (animate) {
                    mEmptyView.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
                    mList.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
                } else {
                    mEmptyView.clearAnimation();
                    mList.clearAnimation();
                }
                mEmptyView.setVisibility(GONE);
                mList.setVisibility(VISIBLE);
            } else {
                mEmptyView.setVisibility(GONE);
                mList.setVisibility(VISIBLE);
                setListShown(true, animate);
            }
        }
    }
}
