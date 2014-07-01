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
import org.opensilk.music.ui.cards.event.SongCardClick;
import org.opensilk.music.ui.profile.adapter.ProfilePlaylistAdapter;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.profile.loader.PlaylistSongLoader;
import org.opensilk.music.ui.cards.CardSongList;
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
    Bus mBus;
    private FragmentBusMonitor mBusMonitor;

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
        mBusMonitor = new FragmentBusMonitor();
        mBus.register(mBusMonitor);
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
        mBus.unregister(mBusMonitor);
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
        Command command = null;
        switch (item.getItemId()) {
            case R.id.popup_play_all:
                command = new Command() {
                    @Override
                    public CharSequence execute() {
                        if (isLastAdded()) {
                            MusicUtils.playLastAdded(getActivity(), false);
                        } else {
                            MusicUtils.playPlaylist(getActivity(), mPlaylist.mPlaylistId, false);
                        }
                        return null;
                    }
                };
                break;
            case R.id.popup_shuffle_all:
                command = new Command() {
                    @Override
                    public CharSequence execute() {
                        if (isLastAdded()) {
                            MusicUtils.playLastAdded(getActivity(), true);
                        } else {
                            MusicUtils.playPlaylist(getActivity(), mPlaylist.mPlaylistId, true);
                        }
                        return null;
                    }
                };
                break;
            case R.id.popup_add_to_queue:
                command = new Command() {
                    @Override
                    public CharSequence execute() {
                        LocalSong[] list;
                        if (isLastAdded()) {
                            list = MusicUtils.getLocalSongListForLastAdded(getActivity());
                        } else {
                            list = MusicUtils.getLocalSongListForPlaylist(getActivity(), mPlaylist.mPlaylistId);
                        }
                        MusicUtils.addSongsToQueueSilent(getActivity(), list);
                        return getResources().getQuantityString(R.plurals.NNNtrackstoqueue, list.length, list.length);
                    }
                };
                break;
            case R.id.popup_rename:
                RenamePlaylist.getInstance(mPlaylist.mPlaylistId)
                        .show(getActivity().getSupportFragmentManager(), "RenameDialog");
                return true;
            case R.id.popup_delete:
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.delete_dialog_title, mPlaylist.mPlaylistName))
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
        if (command != null) {
            ApolloUtils.execute(false, new CommandRunner(getActivity(), command));
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

    class FragmentBusMonitor {
        @Subscribe
        public void onSongCardClick(SongCardClick e) {
            if (!(e.song instanceof LocalSong)) {
                return;
            }
            final LocalSong song = (LocalSong) e.song;
            switch (e.event) {
                case PLAY:
                    MusicUtils.playAllSongs(getActivity(), new Song[]{song}, 0, false);
                    break;
                case PLAY_NEXT:
                    MusicUtils.playNext(getActivity(), new Song[]{song});
                    break;
                case ADD_TO_QUEUE:
                    MusicUtils.addSongsToQueue(getActivity(), new Song[]{song});
                    break;
                case ADD_TO_PLAYLIST:
                    AddToPlaylistDialog.newInstance(new long[]{song.songId})
                            .show(getChildFragmentManager(), "AddToPlaylistDialog");
                    break;
                case MORE_BY_ARTIST:
                    NavUtils.openArtistProfile(getActivity(), MusicUtils.makeArtist(getActivity(), song.artistName));
                    break;
                case SET_RINGTONE:
                    MusicUtils.setRingtone(getActivity(), song.songId);
                    break;
                case DELETE:
                    DeleteDialog.newInstance(song.name, new long[]{song.songId}, null)
                            .show(getChildFragmentManager(), "DeleteDialog");
                    break;
            }
        }
    }
}
