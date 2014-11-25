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
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

import org.opensilk.music.R;
import org.opensilk.music.widgets.RecyclerListFrame;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;
import timber.log.Timber;

/**
 * Created by drew on 10/5/14.
 */
public class LibraryView extends RecyclerListFrame {

    @Inject LibraryScreen.Presenter presenter;

    @InjectView(R.id.more_loading_progress) ContentLoadingProgressBar mMoreLoadingProgress;

    final LibraryAdapter adapter;

    ProgressDialog mProgressDialog;

    public LibraryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
        adapter = new LibraryAdapter(presenter);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mList.setAdapter(adapter);
        mList.setHasFixedSize(true);
        mList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
        dismissProgressDialog();
    }

    public void setMoreLoading(boolean show) {
        if (show) {
            mMoreLoadingProgress.show();
        } else {
            mMoreLoadingProgress.hide();
        }
    }

    void showProgressDialog() {
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getContext().getString(R.string.msg_fetching_song_list));
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

    boolean isProgressDialogShowing() {
        return (mProgressDialog != null && mProgressDialog.isShowing());
    }

}
