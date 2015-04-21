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

package com.andrew.apollo.menu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import org.opensilk.music.R;

import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.util.CursorHelpers;

import java.util.List;

/**
 * Created by drew on 2/16/14.
 */
public class AddToPlaylistDialog extends DialogFragment implements DialogInterface.OnClickListener {

    protected List<Playlist> mUserPlaylists;
    protected long[] mSongIds;

    public static AddToPlaylistDialog newInstance(long[] playlist) {
        Bundle b = new Bundle();
        b.putLongArray("playlist_list", playlist);
        AddToPlaylistDialog f = new AddToPlaylistDialog();
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserPlaylists = getUserPlaylists();
        if (getArguments() != null) {
            mSongIds = getArguments().getLongArray("playlist_list");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<String> playlists = Lists.newArrayList();
        playlists.add(getActivity().getString(R.string.new_playlist));
        for (Playlist playlist: mUserPlaylists) {
            playlists.add(playlist.mPlaylistName);
        }
        CharSequence [] items = playlists.toArray(new CharSequence[playlists.size()]);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_to_playlist)
                .setItems(items, this)
                .create();
    }


    /**
     * This method will be invoked when a button in the dialog is clicked.
     *
     * @param dialog The dialog that received the click.
     * @param which  The button that was clicked (e.g.
     *               {@link android.content.DialogInterface#BUTTON1}) or the position
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case 0: //New playlist
                CreateNewPlaylist.getInstance(mSongIds).show(getFragmentManager(), "CreatePlaylist");
                break;
            default:
                MusicUtils.addToPlaylist(getActivity(), mSongIds, mUserPlaylists.get(which-1).mPlaylistId);
                break;
        }
        dialog.dismiss();
    }

    public List<Playlist> getUserPlaylists() {
        List<Playlist> usrPlaylists = Lists.newArrayList();
        // Create the Cursor
        Cursor c = CursorHelpers.makePlaylistCursor(getActivity());
        // Gather the data
        if (c != null && c.moveToFirst()) {
            do {
                // Copy the playlist id
                final long id = c.getLong(0);

                // Copy the playlist name
                final String name = c.getString(1);

                // Create a new playlist
                final Playlist playlist = new Playlist(id, name, 0, 0, null, null);

                // Add everything up
                usrPlaylists.add(playlist);
            } while (c.moveToNext());
        }
        // Close the cursor
        if (c != null) {
            c.close();
        }
        return usrPlaylists;
    }
}
