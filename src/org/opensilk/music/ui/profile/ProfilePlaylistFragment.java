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

package org.opensilk.music.ui.profile;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.menu.RenamePlaylist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.MusicUtils;
import com.mobeta.android.dslv.DragSortListView;

import org.opensilk.music.adapters.SongListCardCursorAdapter;
import org.opensilk.music.loaders.PlaylistSongCursorLoader;
import org.opensilk.music.ui.cards.CardSongList;

/**
 * Created by drew on 2/24/14.
 */
public class ProfilePlaylistFragment extends ProfileBaseFragment<Playlist> implements
        DragSortListView.DropListener,
        DragSortListView.RemoveListener {

    private Playlist mPlaylist;

    public static ProfilePlaylistFragment newInstance(Bundle args) {
        ProfilePlaylistFragment f = new ProfilePlaylistFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlaylist = mBundleData;
        //We have to set this manually since we arent using CardListView
        ((SongListCardCursorAdapter) mAdapter).setRowLayoutId(R.layout.dragsort_card_list);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dragsort_listview_topmargin, container, false);
        mListView = (ListView) v.findViewById(android.R.id.list);
        if (isLastAdded()) {
            // last added arent sortable
            ((DragSortListView) mListView).setDragEnabled(false);
        } else {
            // Set the drop listener
            ((DragSortListView) mListView).setDropListener(this);
            // Set the swipe to remove listener
            ((DragSortListView) mListView).setRemoveListener(this);
        }
        // set the adapter
        mListView.setAdapter(mAdapter);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set actionbar title
        setTitle(mPlaylist.mPlaylistName);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.card_playlist, menu);
        if (mPlaylist.mPlaylistId == -2) {
            // Cant rename or delete lastadded
            menu.removeItem(R.id.card_menu_rename);
            menu.removeItem(R.id.card_menu_delete);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.card_menu_play:
                if (mPlaylist.mPlaylistId == -1) {
                    MusicUtils.playFavorites(getActivity(), false);
                } else if (mPlaylist.mPlaylistId == -2) {
                    MusicUtils.playLastAdded(getActivity(), false);
                } else {
                    MusicUtils.playPlaylist(getActivity(), mPlaylist.mPlaylistId, false);
                }
                return true;
            case R.id.card_menu_shuffle:
                if (mPlaylist.mPlaylistId == -1) {
                    MusicUtils.playFavorites(getActivity(), true);
                } else if (mPlaylist.mPlaylistId == -2) {
                    MusicUtils.playLastAdded(getActivity(), true);
                } else {
                    MusicUtils.playPlaylist(getActivity(), mPlaylist.mPlaylistId, true);
                }
                return true;
            case R.id.card_menu_add_queue:
                long[] list = null;
                if (mPlaylist.mPlaylistId == -1) {
                    list = MusicUtils.getSongListForFavorites(getActivity());
                } else if (mPlaylist.mPlaylistId == -2) {
                    list = MusicUtils.getSongListForLastAdded(getActivity());
                } else {
                    list = MusicUtils.getSongListForPlaylist(getActivity(),
                            mPlaylist.mPlaylistId);
                }
                MusicUtils.addToQueue(getActivity(), list);
                return true;
            case R.id.card_menu_rename:
                RenamePlaylist.getInstance(mPlaylist.mPlaylistId).show(
                        ((FragmentActivity) getActivity()).getSupportFragmentManager(),
                        "RenameDialog");
                return true;
            case R.id.card_menu_delete:
                new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getString(R.string.delete_dialog_title, mPlaylist.mPlaylistName))
                        .setPositiveButton(R.string.context_menu_delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                final Uri mUri = ContentUris.withAppendedId(
                                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                        mPlaylist.mPlaylistId);
                                getActivity().getContentResolver().delete(mUri, null, null);
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
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isFavorites() {
        return mPlaylist.mPlaylistId == -1;
    }

    private boolean isLastAdded() {
        return mPlaylist.mPlaylistId == -2;
    }

    /*
     * DragSort callbacks
     */

    @Override
    public void remove(final int which) {
        LocalSong song = ((CardSongList) mAdapter.getItem(which)).getData();
        if (!isFavorites()) {
            final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylist.mPlaylistId);
            getActivity().getContentResolver().delete(uri,
                    MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + song.songId,
                    null);
        }
    }

    @Override
    public void drop(final int from, final int to) {
        if (!isFavorites()) {
            MediaStore.Audio.Playlists.Members.moveItem(getActivity().getContentResolver(),
                    mPlaylist.mPlaylistId, from, to);
        }
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new PlaylistSongCursorLoader(getActivity(), args.getLong(Config.ID));
    }

    /*
     * Implement Abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        return new SongListCardCursorAdapter(getActivity(), false);
    }

    @Override
    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mBundleData.mPlaylistId);
        return b;
    }
}
