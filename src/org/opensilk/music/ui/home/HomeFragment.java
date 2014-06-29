/*
 * Copyright (C) 2012 Andrew Neal
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

package org.opensilk.music.ui.home;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.cards.event.AlbumCardClick;
import org.opensilk.music.ui.cards.event.ArtistCardClick;
import org.opensilk.music.ui.cards.event.GenreCardClick;
import org.opensilk.music.ui.cards.event.SongCardClick;
import org.opensilk.music.ui.home.adapter.HomePagerAdapter;
import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.qualifier.ForFragment;
import org.opensilk.silkdagger.support.ScopedDaggerFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * This class is used to hold the {@link ViewPager} used for swiping between the
 * playlists, recent, artists, albums, songs, and genre {@link Fragment}
 */
public class HomeFragment extends ScopedDaggerFragment {

    @Inject @ForActivity
    DrawerHelper mDrawerHelper;
    @Inject @ForActivity
    ActionBarController mActionBarHelper;
    @Inject @ForFragment
    Bus mFragmentBus;

    @InjectView(R.id.pager)
    ViewPager mViewPager;

    private HomePagerAdapter mPagerAdapter;
    private PreferenceUtils mPreferences;

    private FragmentBusMonitor mFragmentMonitor;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the preferences
        mPreferences = PreferenceUtils.getInstance(getActivity());

        // Initialize the adapter
        mPagerAdapter = new HomePagerAdapter(getActivity(), getChildFragmentManager());

        List<MusicFragment> pages = mPreferences.getHomePages();

        if (pages == null || pages.size() < 1) {
            final MusicFragment[] mFragments = MusicFragment.values();
            for (final MusicFragment mFragment : mFragments) {
                mPagerAdapter.add(mFragment, null);
            }
        } else {
            for (MusicFragment page : pages) {
                mPagerAdapter.add(page, null);
            }
        }

        mFragmentMonitor = new FragmentBusMonitor();
        mFragmentBus.register(mFragmentMonitor);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup)inflater.inflate(
                R.layout.pager_fragment, container, false);
        ButterKnife.inject(this, rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // Offscreen pager loading limit
//        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        // Start on the last page the user was on
        mViewPager.setCurrentItem(mPreferences.getStartPage());
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        //Set title
        mActionBarHelper.setTitle(getString(R.string.drawer_device));
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save the last page the use was on
        mPreferences.setStartPage(mViewPager.getCurrentItem());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public void onDestroy() {
        mFragmentBus.unregister(mFragmentMonitor);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!mDrawerHelper.isDrawerOpen()) {
            // Party shuffle
            inflater.inflate(R.menu.party_shuffle, menu);
            // Shuffle all
//        inflater.inflate(R.menu.shuffle, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_party_shuffle:
                // Starts autoshuffle mode
                MusicUtils.startPartyShuffle();
                return true;
            case R.id.menu_shuffle:
                // Shuffle all the songs
                MusicUtils.shuffleAll(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Abstract methods
     */

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new HomeModule(),
        };
    }

    @Override
    protected DaggerInjector getParentInjector(Activity activity) {
        return (DaggerInjector) activity;
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
        public void onArtistCardClick(ArtistCardClick e) {
            if (!(e.artist instanceof LocalArtist)) {
                return;
            }
            final LocalArtist artist = (LocalArtist) e.artist;
            Command command = null;
            switch (e.event) {
                case OPEN:
                    NavUtils.openArtistProfile(getActivity(), artist);
                    return;
                case PLAY_ALL:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForArtist(getActivity(), artist.artistId);
                            MusicUtils.playAllSongs(getActivity(), list, 0, false);
                            return null;
                        }
                    };
                    break;
                case SHUFFLE_ALL:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForArtist(getActivity(), artist.artistId);
                            MusicUtils.playAllSongs(getActivity(), list, 0, true);
                            return null;
                        }
                    };
                    break;
                case ADD_TO_QUEUE:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForArtist(getActivity(), artist.artistId);
                            MusicUtils.addSongsToQueueSilent(getActivity(), list);
                            return getResources().getQuantityString(R.plurals.NNNtrackstoqueue, list.length, list.length);
                        }
                    };
                    break;
                case ADD_TO_PLAYLIST:
                    long[] plist = MusicUtils.getSongListForArtist(getActivity(), artist.artistId);
                    AddToPlaylistDialog.newInstance(plist)
                            .show(getChildFragmentManager(), "AddToPlaylistDialog");
                    return;
                case DELETE:
                    long[] dlist = MusicUtils.getSongListForArtist(getActivity(), artist.artistId);
                    DeleteDialog.newInstance(artist.name, dlist, null) //TODO
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
    }

}