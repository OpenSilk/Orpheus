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

package org.opensilk.music.ui3.delete;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import hugo.weaving.DebugLog;

/**
 * Created by drew on 5/14/15.
 */
public class DeleteScreenView extends FrameLayout {

    @Inject DeleteScreenPresenter mPresenter;

    @InjectView(R.id.title) TextView mTitle;
    @InjectView(R.id.message) TextView mMessage;
    @InjectView(R.id.message_container) ViewGroup mMessageContainer;
    @InjectView(R.id.loading_progress) ProgressBar mProgress;

    public DeleteScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        DeleteScreenComponent component = DaggerService.getDaggerComponent(getContext());
        component.inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        mPresenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
    }

    void setTitle(String title) {
        mTitle.setText(getResources().getString(R.string.delete_dialog_title, title));
    }

    void gotoLoading() {
        mMessageContainer.setVisibility(GONE);
        mProgress.setVisibility(VISIBLE);
    }

    void showSuccess() {
        Toast.makeText(getContext(), "Success", Toast.LENGTH_LONG).show();
        mPresenter.dismissSelf();
    }

    void showError(String msg) {
        if (msg == null) {
            Toast.makeText(getContext(), R.string.err_generic, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
        }
        mPresenter.dismissSelf();
    }

    @OnClick(R.id.btn_negative)
    public void onCancel() {
        mPresenter.dismissSelf();
    }

    @OnClick(R.id.btn_positive)
    public void onOk() {
        gotoLoading();
        mPresenter.doDelete();
    }
}
