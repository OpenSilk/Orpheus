/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui2.library;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.HandlesBack;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;
import timber.log.Timber;

/**
 * Created by drew on 10/5/14.
 */
public class LibraryView extends FrameLayout implements HandlesBack {

    @Inject LibraryScreen.Presenter presenter;

    @InjectView(R.id.list_container) View mListContainer;
    @InjectView(R.id.recyclerview) RecyclerView mList;
    @InjectView(R.id.empty_view) View mEmptyView;
    @InjectView(R.id.empty_text) TextView mEmptyText;
    @InjectView(R.id.loading_progress) ContentLoadingProgressBar mLoadingProgress;
    @InjectView(R.id.more_loading_progress) ContentLoadingProgressBar mMoreLoadingProgress;

    final LibraryAdapter adapter;

    ProgressDialog mProgressDialog;

    boolean mLoadingShown;
    boolean mListShown;
    boolean mEmptyShown;

    public LibraryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
        adapter = new LibraryAdapter(presenter);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        mList.setAdapter(adapter);
        mList.setHasFixedSize(true);
        mList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
        dismissProgressDialog();
    }

    public void setEmptyText(int stringRes) {
        mEmptyText.setText(getContext().getString(stringRes));
    }

    public void setMoreLoading(boolean show) {
        if (show) {
            mMoreLoadingProgress.show();
        } else {
            mMoreLoadingProgress.hide();
        }
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

    void showProgressDialog() {
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getContext().getString(R.string.fetching_song_list));
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Timber.v("ProgressDialog Canceled");
                presenter.cancelWork();
            }
        });
        mProgressDialog.show();
    }

    void dismissProgressDialog() {
        if (isProgressDialogShowing()) mProgressDialog.dismiss();
    }

    void updateProgressDialog(int newCount) {
        String msg = getResources().getString(R.string.fetching_song_list)
                + MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, newCount);
        if (isProgressDialogShowing()) mProgressDialog.setMessage(msg);
    }

    boolean isProgressDialogShowing() {
        return (mProgressDialog != null && mProgressDialog.isShowing());
    }

    @Override
    public boolean onBackPressed() {
        if (isProgressDialogShowing()) {
            mProgressDialog.cancel();
            return true;
        }
        return false;
    }
}
