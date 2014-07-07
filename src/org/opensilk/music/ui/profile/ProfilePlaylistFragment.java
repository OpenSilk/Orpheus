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
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.menu.RenamePlaylist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.mobeta.android.dslv.DragSortListView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.cards.SongCard;
import org.opensilk.music.ui.cards.SongPlaylistCard;
import org.opensilk.music.ui.cards.event.PlaylistCardClick;
import org.opensilk.music.ui.cards.event.SongCardClick;
import org.opensilk.music.ui.cards.handler.PlaylistCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongCardClickHandler;
import org.opensilk.music.ui.profile.adapter.ProfilePlaylistAdapter;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.profile.loader.PlaylistSongLoader;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

/**
 * Created by drew on 2/24/14.
 */
public class ProfilePlaylistFragment extends ProfileBaseFragment<Playlist> implements
        DragSortListView.DropListener,
        DragSortListView.RemoveListener {

    @Inject @ForFragment
    protected Bus mBus;

    protected SongCardClickHandler mSongClickHandler;
    protected PlaylistCardClickHandler mPlaylistClickHandler;

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
        registerHandlers();
        //We have to set this manually since we arent using CardListView
        ((ProfilePlaylistAdapter) mAdapter).setRowLayoutId(R.layout.list_dragsort_card_layout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dragsort_listview_topmargin, container, false);
        mListView = (ListView) v.findViewById(android.R.id.list);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // enable options menu
        setHasOptionsMenu(true);
        // Set actionbar title
        setTitle(mPlaylist.mPlaylistName);
    }

    @Override
    public void onDestroy() {
        unregisterHandlers();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.popup_play_all, menu);
        inflater.inflate(R.menu.popup_shuffle_all, menu);
        inflater.inflate(R.menu.popup_add_to_queue, menu);
        if (!isLastAdded()) {
            // cant rename or delete last added
            inflater.inflate(R.menu.popup_rename, menu);
            inflater.inflate(R.menu.popup_delete, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.popup_play_all:
                mBus.post(new PlaylistCardClick(PlaylistCardClick.Event.PLAY_ALL, mPlaylist));
                return true;
            case R.id.popup_shuffle_all:
                mBus.post(new PlaylistCardClick(PlaylistCardClick.Event.SHUFFLE_ALL, mPlaylist));
                return true;
            case R.id.popup_add_to_queue:
                mBus.post(new PlaylistCardClick(PlaylistCardClick.Event.ADD_TO_QUEUE, mPlaylist));
                return true;
            case R.id.popup_rename:
                mBus.post(new PlaylistCardClick(PlaylistCardClick.Event.RENAME, mPlaylist));
                return true;
            case R.id.popup_delete:
                mBus.post(new PlaylistCardClick(PlaylistCardClick.Event.DELETE, mPlaylist));
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
        Song song = ((SongPlaylistCard) mAdapter.getItem(which)).getData();
        if (!isFavorites()) {
            final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylist.mPlaylistId);
            getActivity().getContentResolver().delete(uri,
                    MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?",
                    new String[]{song.identity});
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
        return new PlaylistSongLoader(getActivity(), args.getLong(Config.ID));
    }

    /*
     * Implement Abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        return new ProfilePlaylistAdapter(getActivity(), this);
    }

    @Override
    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mBundleData.mPlaylistId);
        return b;
    }

    private void registerHandlers() {
        mSongClickHandler = getObjectGraph().get(SongCardClickHandler.class);
        mPlaylistClickHandler = getObjectGraph().get(PlaylistCardClickHandler.class);
        mBus.register(mSongClickHandler);
        mBus.register(mPlaylistClickHandler);
    }

    private void unregisterHandlers() {
        mBus.unregister(mSongClickHandler);
        mBus.unregister(mPlaylistClickHandler);
    }
}
