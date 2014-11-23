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
import android.os.Parcelable;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;
import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 10/21/14.
 */
public class GalleryPageView extends FrameLayout {

    @InjectView(R.id.list_container) View mListContainer;
    @InjectView(R.id.recyclerview) RecyclerView mList;
    @InjectView(R.id.empty_view) View mEmptyView;
    @InjectView(R.id.empty_text) TextView mEmptyText;
    @InjectView(R.id.loading_progress) ContentLoadingProgressBar mProgressContainer;

    boolean mLoadingShown;
    boolean mListShown;
    boolean mEmptyShown;

    @Inject ViewPresenter<GalleryPageView> presenter;

    public GalleryPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
//        Timber.v("onFinishInflate()");
        ButterKnife.inject(this);
        //note dont remove, needs to be called here for adapter savedstate
        presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
//        Timber.v("onAttachedToWindow()");
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//        Timber.v("onDetachedFromWindow");
        presenter.dropView(this);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
//        Timber.v("onSaveInstanceState");
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
//        Timber.v("onRestoreInstanceState");
        super.onRestoreInstanceState(state);
    }

    public ViewPresenter<GalleryPageView> getPresenter() {
        return presenter;
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
        mProgressContainer.setAnimation(AnimationUtils.loadAnimation(
                getContext(), shown ? android.R.anim.fade_in : android.R.anim.fade_out));
        if (shown) {
            mProgressContainer.show();
        } else {
            mProgressContainer.hide();
        }
    }

    public void setListShown(boolean shown, boolean animate) {
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
        setLoading(!shown);
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
