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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.model.Song;
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
        ((SongListCardCursorAdapter) mAdapter).setRowLayoutId(R.layout.dragsort_card_list_thumb);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.profile_dragsort_listview, container, false);
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
        Song song = ((CardSongList) mAdapter.getItem(which)).getData();
        if (!isFavorites()) {
            final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylist.mPlaylistId);
            getActivity().getContentResolver().delete(uri,
                    MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + song.mSongId,
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
        return new SongListCardCursorAdapter(getActivity());
    }

    @Override
    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mBundleData.mPlaylistId);
        return b;
    }
}
