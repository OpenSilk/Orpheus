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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.cards.event.AlbumCardClick;
import org.opensilk.music.ui.cards.event.GenreCardClick;
import org.opensilk.music.ui.cards.event.SongCardClick;
import org.opensilk.music.ui.home.adapter.SongAdapter;
import org.opensilk.music.ui.profile.adapter.ProfileArtistAdapter;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.profile.loader.GenreAlbumLoader;
import org.opensilk.music.ui.profile.loader.GenreSongLoader;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForFragment;
import org.opensilk.silkdagger.support.ScopedDaggerFragment;

import javax.inject.Inject;

/**
 * Created by drew on 2/28/14.
 */
public class ProfileGenreFragment extends ScopedDaggerFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    protected static int ALBUM_LOADER = 1;
    protected static int SONG_LOADER = 2;

    @Inject @ForFragment
    Bus mBus;

    private FragmentBusMonitor mBusMonitor;

    private ViewPager mViewPager;
    private ProfileGenrePagerAdapter mPagerAdapter;

    protected ProfileArtistAdapter mAlbumAdapter;
    protected SongAdapter mSongAdapter;

    private Genre mGenre;

    public static ProfileGenreFragment newInstance(Bundle args) {
        ProfileGenreFragment f = new ProfileGenreFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        mGenre = args.getParcelable(Config.EXTRA_DATA);
        mBusMonitor = new FragmentBusMonitor();
        mBus.register(mBusMonitor);
        // Initialize the adapter
        mPagerAdapter = new ProfileGenrePagerAdapter(getChildFragmentManager(), getActivity());
        // Init page adapters these are piggy backed on others
        mAlbumAdapter = new ProfileArtistAdapter(getActivity(), this);
        mSongAdapter = new SongAdapter(getActivity(), this);
        //start the loaders
        getLoaderManager().initLoader(ALBUM_LOADER, createLoaderArgs(), this);
        getLoaderManager().initLoader(SONG_LOADER, createLoaderArgs(), this);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        // The View for the fragment's UI
        final ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.pager_fragment, container, false);
        // Initialize the ViewPager
        mViewPager = (ViewPager)rootView.findViewById(R.id.pager);
        // Attach the adapter
        mViewPager.setAdapter(mPagerAdapter);
        return rootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        // set actionbar title
        ActionBarActivity activity = (ActionBarActivity) getActivity();
        activity.getSupportActionBar().setTitle(mGenre.mGenreName);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewPager = null;
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
        inflater.inflate(R.menu.popup_add_to_playlist, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.popup_play_all:
                mBus.post(new GenreCardClick(GenreCardClick.Event.PLAY_ALL, mGenre));
                return true;
            case R.id.popup_shuffle_all:
                mBus.post(new GenreCardClick(GenreCardClick.Event.SHUFFLE_ALL, mGenre));
                return true;
            case R.id.popup_add_to_queue:
                mBus.post(new GenreCardClick(GenreCardClick.Event.ADD_TO_QUEUE, mGenre));
                return true;
            case R.id.popup_add_to_playlist:
                mBus.post(new GenreCardClick(GenreCardClick.Event.ADD_TO_QUEUE, mGenre));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mGenre.mGenreId);
        return b;
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new ProfileModule(),
        };
    }

    @Override
    protected DaggerInjector getParentInjector(Activity activity) {
        return (DaggerInjector) activity;
    }

    /*
     * Implement LoaderCallbacks
     * NOTE: We handle the loaders here instead of in the pager fragments
     * because the child fragments somehow 'leak' the callbacks into
     * the fragment an the top of the backstack when we exit here
     * resetting its loader
     * TODO this probably doest apply anymore now that profiles are in another activity
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == ALBUM_LOADER) {
            return new GenreAlbumLoader(getActivity(), args.getLong(Config.ID));
        } else if (id == SONG_LOADER) {
            return new GenreSongLoader(getActivity(), args.getLong(Config.ID));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final int id = loader.getId();
        if (id == ALBUM_LOADER) {
            mAlbumAdapter.swapCursor(data);
        } else if (id == SONG_LOADER) {
            mSongAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        final int id = loader.getId();
        if (id == ALBUM_LOADER) {
            mAlbumAdapter.swapCursor(null);
        } else if (id == SONG_LOADER) {
            mSongAdapter.swapCursor(null);
        }
    }

    class FragmentBusMonitor {
        @Subscribe
        public void onGenreCardClick(GenreCardClick e) {
            final Genre genre = e.genre;
            Command command = null;
            switch (e.event) {
                case OPEN:
                    NavUtils.openGenreProfile(getActivity(), e.genre);
                    return;
                case PLAY_ALL:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForGenre(getActivity(), genre.mGenreId);
                            MusicUtils.playAllSongs(getActivity(), list, 0, false);
                            return null;
                        }
                    };
                    break;
                case SHUFFLE_ALL:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForGenre(getActivity(), genre.mGenreId);
                            MusicUtils.playAllSongs(getActivity(), list, 0, true);
                            return null;
                        }
                    };
                    break;
                case ADD_TO_QUEUE:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForGenre(getActivity(), genre.mGenreId);
                            MusicUtils.addSongsToQueueSilent(getActivity(), list);
                            return getResources().getQuantityString(R.plurals.NNNtrackstoqueue, list.length, list.length);
                        }
                    };
                    break;
                case ADD_TO_PLAYLIST:
                    long[] plist = MusicUtils.getSongListForGenre(getActivity(), genre.mGenreId);
                    AddToPlaylistDialog.newInstance(plist)
                            .show(getChildFragmentManager(), "AddToPlaylistDialog");
                    return;
            }
            if (command != null) {
                ApolloUtils.execute(false, new CommandRunner(getActivity(), command));
            }
        }
        @Subscribe
        public void onAlbumCardClick(AlbumCardClick e) {
            if (!(e.album instanceof LocalAlbum)) {
                return;
            }
            final LocalAlbum album = (LocalAlbum) e.album;
            Command command = null;
            switch (e.event) {
                case OPEN:
                    NavUtils.openAlbumProfile(getActivity(), album);
                    return;
                case PLAY_ALL:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForAlbum(getActivity(), album.albumId);
                            MusicUtils.playAllSongs(getActivity(), list, 0, false);
                            return null;
                        }
                    };
                    break;
                case SHUFFLE_ALL:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForAlbum(getActivity(), album.albumId);
                            MusicUtils.playAllSongs(getActivity(), list, 0, true);
                            return null;
                        }
                    };
                    break;
                case ADD_TO_QUEUE:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForAlbum(getActivity(), album.albumId);
                            MusicUtils.addSongsToQueueSilent(getActivity(), list);
                            return getResources().getQuantityString(R.plurals.NNNtrackstoqueue, list.length, list.length);
                        }
                    };
                    break;
                case ADD_TO_PLAYLIST:
                    long[] plist = MusicUtils.getSongListForAlbum(getActivity(), album.albumId);
                    AddToPlaylistDialog.newInstance(plist)
                            .show(getChildFragmentManager(), "AddToPlaylistDialog");
                    return;
                case MORE_BY_ARTIST:
                    NavUtils.openArtistProfile(getActivity(), MusicUtils.makeArtist(getActivity(), album.artistName));
                    return;
                case DELETE:
                    long[] dlist = MusicUtils.getSongListForAlbum(getActivity(), album.albumId);
                    DeleteDialog.newInstance(album.name, dlist, null) //TODO
                            .show(getChildFragmentManager(), "DeleteDialog");
                    return;
                default:
                    return;
            }
            if (command != null) {
                ApolloUtils.execute(false, new CommandRunner(getActivity(), command));
            }
        }
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
