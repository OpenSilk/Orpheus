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

package org.opensilk.music.ui.cards.handler;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;

import org.opensilk.music.R;
import com.andrew.apollo.menu.RenamePlaylist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.provider.MusicProvider;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.squareup.otto.Subscribe;

import org.opensilk.music.ui.cards.event.PlaylistCardClick;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

/**
 * Created by drew on 7/1/14.
 */
public class PlaylistCardClickHandler {
    private final FragmentActivity activity;

    @Inject
    public PlaylistCardClickHandler(@ForActivity FragmentActivity activity) {
        this.activity = activity;
    }

    public FragmentActivity getActivity() {
        return activity;
    }

    @Subscribe
    public void onPlaylistCardClick(PlaylistCardClick e) {
        final Playlist playlist = e.playlist;
        Command command = null;
        switch (e.event) {
            case OPEN:
                NavUtils.openPlaylistProfile(getActivity(), playlist);
                return;
            case PLAY_ALL:
                command = new Command() {
                    @Override
                    public CharSequence execute() {
                        if (playlist.mPlaylistId == -2) {
                            MusicUtils.playLastAdded(getActivity(), false);
                        } else {
                            MusicUtils.playPlaylist(getActivity(), playlist.mPlaylistId, false);
                        }
                        return null;
                    }
                };
                break;
            case SHUFFLE_ALL:
                command = new Command() {
                    @Override
                    public CharSequence execute() {
                        if (playlist.mPlaylistId == -2) {
                            MusicUtils.playLastAdded(getActivity(), true);
                        } else {
                            MusicUtils.playPlaylist(getActivity(), playlist.mPlaylistId, true);
                        }
                        return null;
                    }
                };
                break;
            case ADD_TO_QUEUE:
                command = new Command() {
                    @Override
                    public CharSequence execute() {
                        LocalSong[] list;
                        if (playlist.mPlaylistId == -2) {
                            list = CursorHelpers.getLocalSongListForLastAdded(getActivity());
                        } else {
                            list = CursorHelpers.getLocalSongListForPlaylist(getActivity(), playlist.mPlaylistId);
                        }
                        return MusicUtils.addSongsToQueueSilent(getActivity(), list);
                    }
                };
                break;
            case RENAME:
                RenamePlaylist.getInstance(playlist.mPlaylistId)
                        .show(getActivity().getSupportFragmentManager(), "RenameDialog");
                return;
            case DELETE:
                new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getString(R.string.delete_dialog_title, playlist.mPlaylistName))
                        .setPositiveButton(R.string.context_menu_delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                final Uri mUri = ContentUris.withAppendedId(
                                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                        playlist.mPlaylistId);
                                getActivity().getContentResolver().delete(mUri, null, null);
                                getActivity().getContentResolver().notifyChange(MusicProvider.PLAYLIST_URI, null);
                                MusicUtils.refresh();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                dialog.dismiss();
                            }
                        })
                        .setMessage(R.string.cannot_be_undone)
                        .create()
                        .show();
                return;
        }
        if (command != null) {
            new CommandRunner(getActivity(), command).execute();
        }
    }

}
