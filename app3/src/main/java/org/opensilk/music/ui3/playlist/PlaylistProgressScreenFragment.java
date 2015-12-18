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

package org.opensilk.music.ui3.playlist;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortarfragment.MortarDialogFragment;
import org.opensilk.music.R;

import java.util.List;

import javax.inject.Inject;

/**
 * Created by drew on 12/17/15.
 */
public class PlaylistProgressScreenFragment extends MortarDialogFragment {

    public static PlaylistProgressScreenFragment create(String name, String authority) {
        PlaylistProgressScreenFragment f = new PlaylistProgressScreenFragment();
        f.setArguments(BundleHelper.b().putInt(1).putString(authority).putString2(name).get());
        return f;
    }

    public static PlaylistProgressScreenFragment addTo(Uri playlist, int kind, List<Uri> uris) {
        PlaylistProgressScreenFragment f = new PlaylistProgressScreenFragment();
        f.setArguments(BundleHelper.b().putInt(2).putUri(playlist).putInt2(kind).putList(uris)
                .putString(playlist.getAuthority()).get());
        return f;
    }

    public static PlaylistProgressScreenFragment delete(List<Uri> uris, String authority) {
        PlaylistProgressScreenFragment f = new PlaylistProgressScreenFragment();
        f.setArguments(BundleHelper.b().putInt(3).putList(uris).putString(authority).get());
        return f;
    }

    public static PlaylistProgressScreenFragment update(Uri playlist, List<Uri> uris) {
        PlaylistProgressScreenFragment f = new PlaylistProgressScreenFragment();
        f.setArguments(BundleHelper.b().putInt(4).putUri(playlist).putList(uris)
                .putString(playlist.getAuthority()).get());
        return f;
    }

    @Override
    protected Screen newScreen() {
        int kind = BundleHelper.getInt(getArguments());
        switch (kind) {
            case 1:
                return new PlaylistProgressScreen(PlaylistProgressScreen.Operation.CREATE, getArguments());
            case 2:
                return new PlaylistProgressScreen(PlaylistProgressScreen.Operation.ADDTO, getArguments());
            case 3:
                return new PlaylistProgressScreen(PlaylistProgressScreen.Operation.DELETE, getArguments());
            case 4:
                return new PlaylistProgressScreen(PlaylistProgressScreen.Operation.UPDATE, getArguments());
            default:
                throw new IllegalArgumentException("Unknown playlist operation");
        }
    }

    @Inject PlaylistProgressScreenPresenter mPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PlaylistProgressScreenComponent cmp = DaggerService.getDaggerComponent(getScope());
        cmp.inject(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.processing));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        return dialog;
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
}
