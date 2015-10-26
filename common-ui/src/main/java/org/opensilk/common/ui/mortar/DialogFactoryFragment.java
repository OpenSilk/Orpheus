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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;

import timber.log.Timber;

/**
 * Created by drew on 10/25/15.
 */
public final class DialogFactoryFragment extends AppCompatDialogFragment {
    private DialogFactory factory;

    public static DialogFactoryFragment ni(DialogFactory factory) {
        DialogFactoryFragment f = new DialogFactoryFragment();
        f.factory = factory;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (factory == null) {
            Timber.w("Dismissing fragment no factory");
            dismiss();
        }
    }

    @NonNull @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (factory == null) {
            throw new IllegalArgumentException("Null DialogFactory");
        }
        return factory.call(getActivity());
    }
}
