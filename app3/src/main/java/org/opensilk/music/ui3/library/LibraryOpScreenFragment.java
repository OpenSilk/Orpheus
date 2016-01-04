/*
 * Copyright (c) 2016 OpenSilk Productions LLC
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

package org.opensilk.music.ui3.library;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortarfragment.MortarDialogFragment;
import org.opensilk.music.R;

import javax.inject.Inject;

/**
 * Created by drew on 1/4/16.
 */
public class LibraryOpScreenFragment extends MortarDialogFragment {

    public static LibraryOpScreenFragment ni(LibraryOpScreen screen) {
        LibraryOpScreenFragment f = new LibraryOpScreenFragment();
        f.setArguments(BundleHelper.b().putParcleable(screen).get());
        return f;
    }

    @Override
    protected Screen newScreen() {
        return (LibraryOpScreen) BundleHelper.getParcelable(getArguments());
    }

    @Inject LibraryOpScreenPresenter mPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LibraryOpScreenComponent cmp = DaggerService.getDaggerComponent(getScope());
        cmp.inject(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        mPresenter.takeView(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPresenter.dropView(this);
    }

    @NonNull @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = createDialogContext();
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(getString(R.string.processing));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        return dialog;
    }
}
