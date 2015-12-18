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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.common.ui.mortarfragment.MortarDialogFragment;
import org.opensilk.music.R;

import javax.inject.Inject;

/**
 * Created by drew on 12/17/15.
 */
public class PlaylistCreateScreenFragment extends MortarDialogFragment {

    public static PlaylistCreateScreenFragment ni(String authority) {
        PlaylistCreateScreenFragment f = new PlaylistCreateScreenFragment();
        f.setArguments(BundleHelper.b().putString(authority).get());
        return f;
    }

    @Override
    protected Screen newScreen() {
        return new PlaylistCreateScreen(BundleHelper.getString(getArguments()));
    }

    @Inject FragmentManagerOwner mFm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PlaylistCreateScreenComponent cmp = DaggerService.getDaggerComponent(getScope());
        cmp.inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.new_playlist);
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine(true);
        editText.setInputType(editText.getInputType()
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        b.setView(editText);
        b.setNegativeButton(R.string.cancel, null);
        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = editText.getText().toString();
                String authority = ((PlaylistCreateScreen)getScreen()).authority;
                if (!StringUtils.isEmpty(text)) {
                    mFm.showDialog(PlaylistProgressScreenFragment.create(text, authority));
                }
            }
        });
        return b.create();
    }

}
