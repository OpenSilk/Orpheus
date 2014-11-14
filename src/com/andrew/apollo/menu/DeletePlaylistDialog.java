/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package com.andrew.apollo.menu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.andrew.apollo.Config;

import org.opensilk.music.R;
import org.opensilk.music.util.Uris;

/**
 * Created by drew on 11/14/14.
 */
public class DeletePlaylistDialog extends DialogFragment {

    public static DeletePlaylistDialog newInstance(String title, long playlist) {
        Bundle b = new Bundle();
        b.putString(Config.NAME, title);
        b.putLong("playlist", playlist);
        DeletePlaylistDialog f = new DeletePlaylistDialog();
        f.setArguments(b);
        return f;
    }

    String title;
    long playlist;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(Config.NAME);
            playlist = getArguments().getLong("playlist");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.delete_dialog_title, title))
                .setPositiveButton(R.string.context_menu_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        final Uri mUri = ContentUris.withAppendedId(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, playlist);
                        getActivity().getContentResolver().delete(mUri, null, null);
                        getActivity().getContentResolver().notifyChange(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, null);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setMessage(R.string.cannot_be_undone)
                .create();
    }
}
