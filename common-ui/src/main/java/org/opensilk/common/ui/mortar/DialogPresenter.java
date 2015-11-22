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

package org.opensilk.common.ui.mortar;

import android.app.Dialog;
import android.os.Bundle;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.common.core.mortar.MortarActivity;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;

/**
 * Dialogs *must* be dismissed on configuration changes
 * presenters cant do this so their views must manage the dialogs
 * this gets rather annoying so this class tries to abstract
 * that functionality to the activity.
 * Created by drew on 10/15/15.
 */
@ActivityScope
public class DialogPresenter extends Presenter<MortarActivity> {

    //what we need to restore the dialog on configuration change
    private WeakReference<DialogFactory> mLastFactory;
    private WeakReference<Dialog> mDialog;
    private Bundle mSavedInstance;


    @Inject
    public DialogPresenter() {
    }

    @Override
    protected BundleService extractBundleService(MortarActivity view) {
        return BundleService.getBundleService(view);
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (mSavedInstance != null) {
            DialogFactory lastFactory = mLastFactory != null ? mLastFactory.get() : null;
            if (lastFactory != null) {
                doShowDialog(lastFactory, mSavedInstance);
            }
        }
    }

    @Override
    public void dropView(MortarActivity view) {
        super.dropView(view);
        mSavedInstance = doDismissDialog();
    }

    public void showDialog(DialogFactory factory) {
        dismissDialog();
        mLastFactory = new WeakReference<>(factory);
        doShowDialog(factory, null);
    }

    public void dismissDialog() {
        doDismissDialog();
        mSavedInstance = null;
        if (mLastFactory != null) {
            mLastFactory.clear();
        }
    }

    private void doShowDialog(DialogFactory factory, Bundle savedInstance) {
        if (hasView()) {
            Dialog dialog = factory.call(getView());
            if (savedInstance != null) {
                dialog.onRestoreInstanceState(savedInstance);
            }
            dialog.show();
            mDialog = new WeakReference<Dialog>(dialog);
        }
    }

    private Bundle doDismissDialog() {
        Bundle savedInstance = null;
        Dialog dialog = mDialog != null ? mDialog.get() : null;
        if (dialog != null) {
            if (dialog.isShowing()) {
                savedInstance = dialog.onSaveInstanceState();
            }
            dialog.dismiss();
            mDialog.clear();
        }
        return savedInstance;
    }

}
