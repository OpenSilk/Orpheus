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
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortarfragment.MortarDialogFragment;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.model.spi.Bundleable;

import javax.inject.Inject;

/**
 * Created by drew on 5/14/15.
 */
public class DeleteScreenFragment extends MortarDialogFragment {
    public static final String NAME = DeleteScreenFragment.class.getName();

    public static DeleteScreenFragment ni(Context context, LibraryConfig libraryConfig,
                                    LibraryInfo libraryInfo, Bundleable bundleable) {
        DeleteScreen s = new DeleteScreen(libraryConfig, libraryInfo, bundleable);
        Bundle args = new Bundle();
        args.putParcelable("screen", s);
        return factory(context, NAME, args);
    }

    @Override
    protected Screen newScreen() {
        getArguments().setClassLoader(getClass().getClassLoader());
        return getArguments().<DeleteScreen>getParcelable("screen");
    }

    public DeleteScreenFragment() {
//        setStyle(STYLE_NO_TITLE, 0);
//        setStyle(STYLE_NORMAL, R.style.Theme_AppCompat_Light_Dialog_Alert);
    }

    @Inject DeleteScreenFragmentPresenter presenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DeleteScreenComponent component = DaggerService.getDaggerComponent(getScope());
        component.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.takeView(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.dropView(this);
    }
}
