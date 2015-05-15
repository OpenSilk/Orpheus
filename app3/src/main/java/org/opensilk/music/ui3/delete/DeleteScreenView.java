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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

/**
 * Created by drew on 5/14/15.
 */
public class DeleteScreenView extends FrameLayout {

    @Inject DeleteScreenPresenter mPresenter;

    Dialog mDialog;

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
        dismissDialog();
    }

    void gotoLoading() {
        showProgress();
    }

    void showDelete(String title) {
        dismissDialog();
        mDialog = new AlertDialog.Builder(getContext())
                .setTitle(getResources().getString(R.string.delete_dialog_title, title))
                .setMessage(R.string.cannot_be_undone)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    @DebugLog
                    public void onCancel(DialogInterface dialog) {
                        mPresenter.dismissSelf();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.dismissSelf();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.doDelete();
                        showProgress();
                    }
                })
                .show();
    }

    void showProgress() {
        dismissDialog();
        mDialog = new ProgressDialog(getContext());
        mDialog.setCancelable(false);
        mDialog.show();
    }

    void showError(String msg) {
        dismissDialog();
        mDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.err_generic)
                .setMessage(msg)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    @DebugLog
                    public void onCancel(DialogInterface dialog) {
                        mPresenter.dismissSelf();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.dismissSelf();
                    }
                })
                .show();
    }

    void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    void showSuccess() {
        dismissDialog();
        Toast.makeText(getContext(), "Success", Toast.LENGTH_LONG).show();
        mPresenter.dismissSelf();
    }
}
