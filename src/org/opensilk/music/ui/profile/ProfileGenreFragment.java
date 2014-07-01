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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.ui.profile.adapter.ProfileArtistAdapter;
import org.opensilk.music.ui.profile.adapter.ProfilePlaylistAdapter;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.loaders.GenreAlbumCursorLoader;
import org.opensilk.music.loaders.GenreSongCursorLoader;

/**
 * Created by drew on 2/28/14.
 */
public class ProfileGenreFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    protected static int ALBUM_LOADER = 1;
    protected static int SONG_LOADER = 2;

    private ViewPager mViewPager;
    private ProfileGenrePagerAdapter mPagerAdapter;

    protected ProfileArtistAdapter mAlbumAdapter;
    protected ProfilePlaylistAdapter mSongAdapter;

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
        // Initialize the adapter
        mPagerAdapter = new ProfileGenrePagerAdapter(getChildFragmentManager(), getActivity());
        // Init page adapters
        mAlbumAdapter = new ProfileArtistAdapter(getActivity(), null);
        mSongAdapter = new ProfilePlaylistAdapter(getActivity(), null);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.card_genre, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case R.id.card_menu_play:
                MusicUtils.playAll(getActivity(), MusicUtils.getSongListForGenre(getActivity(), mGenre.mGenreId), 0, false);
                return true;
            case R.id.card_menu_shuffle:
                MusicUtils.playAll(getActivity(), MusicUtils.getSongListForGenre(getActivity(), mGenre.mGenreId), 0, true);
                return true;
            case R.id.card_menu_add_queue:
                MusicUtils.addToQueue(getActivity(), MusicUtils.getSongListForGenre(getActivity(), mGenre.mGenreId));
                return true;
            case R.id.card_menu_add_playlist:
                AddToPlaylistDialog.newInstance(MusicUtils.getSongListForGenre(getActivity(), mGenre.mGenreId))
                        .show(((FragmentActivity) getActivity()).getSupportFragmentManager(), "AddToPlaylistDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mGenre.mGenreId);
        return b;
    }

    /*
     * Implement LoaderCallbacks
     * NOTE: We handle the loaders here instead of in the pager fragments
     * because the child fragments somehow 'leak' the callbacks into
     * the fragment an the top of the backstack when we exit here
     * resetting its loader
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == ALBUM_LOADER) {
            return new GenreAlbumCursorLoader(getActivity(), args.getLong(Config.ID));
        } else if (id == SONG_LOADER) {
            return new GenreSongCursorLoader(getActivity(), args.getLong(Config.ID));
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
}
