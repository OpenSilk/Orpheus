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

package org.opensilk.music.ui.fragments;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

import com.andrew.apollo.R;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.model.Song;

import org.opensilk.music.ui.cards.CardAlbumList;
import org.opensilk.music.ui.cards.CardArtistList;
import org.opensilk.music.ui.cards.CardSongList;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardCursorAdapter;
import it.gmariotti.cardslib.library.view.CardListView;

/**
 * Created by drew on 3/4/14.
 */
public class SearchFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener {

    private String mFilterString;

    private ListView mListView;

    private SearchAdapter mAdapter;

    private SearchView mSearchView;

    // From MediaProvider data1 and data2 only have values for artists
    // apparently, this is kind of annoying, we might look into
    // basic search in the future but its projection is beyond me.
    private String[] mSearchColsFancy = new String[] {
            android.provider.BaseColumns._ID,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Media.TITLE,
            "data1",
            "data2",
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the adapter
        mAdapter = new SearchAdapter(getActivity());
        if (savedInstanceState != null) {
            mFilterString = savedInstanceState.getString("query");
        }
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.card_listview_topmargin, container, false);
        mListView = (CardListView) v.findViewById(android.R.id.list);
        // set the adapter
        mListView.setAdapter(mAdapter);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        ActionBarActivity activity = (ActionBarActivity) getActivity();
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("query", mFilterString);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // make room for search
        menu.removeItem(R.id.media_route_menu_item);
        // remove old search item
        menu.removeItem(R.id.menu_search);

        // Search view
        inflater.inflate(R.menu.searchview, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_searchview);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setIconified(false);
        if (!TextUtils.isEmpty(mFilterString)) {
            mSearchView.setQuery(mFilterString, false);
        }
        // Add voice search
        final SearchManager searchManager = (SearchManager)getActivity().getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(getActivity().getComponentName());
        mSearchView.setSearchableInfo(searchableInfo);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu();
        mSearchView = null;
    }

    /**
     * Called when activity receives a search intent
     * from eg voice search
     * @param query
     */
    public void onNewQuery(String query) {
        mFilterString = query;
        mSearchView.setQuery(query, false);
        getLoaderManager().restartLoader(0, null, this);
        hideKeyboard();
    }

    private void hideKeyboard() {
        // When the search is "committed" by the user, then hide the keyboard so
        // the user can
        // more easily browse the list of results.
        if (mSearchView != null) {
            final InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
            mSearchView.clearFocus();
        }
    }

    /*
     * implement LoaderCallbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final Uri uri = Uri.parse("content://media/external/audio/search/fancy/"
                + Uri.encode(mFilterString));
        return new CursorLoader(getActivity(), uri, mSearchColsFancy, null, null, null);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        if (data == null || data.isClosed()) {
            // Set the empty text
//            final TextView empty = (TextView)findViewById(R.id.empty);
//            empty.setText(getString(R.string.empty_search));
//            mGridView.setEmptyView(empty);
            return;
        }
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    /*
     * implement QueryTextListener
     */

    @Override
    public boolean onQueryTextSubmit(final String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        hideKeyboard();
        return true;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        if (TextUtils.isEmpty(newText)) {
            return false;
        }
        // Called when the action bar search text has changed. Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        mFilterString = newText;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    private static final class SearchAdapter extends CardCursorAdapter {

        public SearchAdapter(Context context) {
            super(context, null, 0);
        }

        @Override
        protected Card getCardFromCursor(Cursor cursor) {
            // Get the MIME type
            final String mimetype = cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));

            if (mimetype.equals("artist")) {
                // get id
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
                // Get the artist name
                final String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
                // Get the album count
                final int albumCount = cursor.getInt(cursor.getColumnIndexOrThrow("data1"));
                // Get the song count
                final int songCount = cursor.getInt(cursor.getColumnIndexOrThrow("data2"));
                // Build artist
                final Artist artist = new Artist(id, name, songCount, albumCount);
                // return artist list card
                CardArtistList card = new CardArtistList(getContext(), artist);
                card.setThumbSize(getContext().getResources().getDimensionPixelSize(R.dimen.list_card_thumbnail_large),
                        getContext().getResources().getDimensionPixelSize(R.dimen.list_card_thumbnail_large));
                return card;
            } else if (mimetype.equals("album")) {
                // Get the Id of the album
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
                // Get the album name
                final String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
                // Get the artist nam
                final String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
                // Build the album as best we can
                final Album album = new Album(id, name, artist, 0, null);
                // return album list card
                CardAlbumList card = new CardAlbumList(getContext(), album);
                card.setThumbSize(getContext().getResources().getDimensionPixelSize(R.dimen.list_card_thumbnail_large),
                        getContext().getResources().getDimensionPixelSize(R.dimen.list_card_thumbnail_large));
                return card;
            } else { /* audio */
                // get id
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
                // Get the track name
                final String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                // Get the album name
                final String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                // get artist name
                final String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                // build the song as best we can
                final Song song = new Song(id, name, artist, album, 0, 0);
                // return song list card
                return new CardSongList(getContext(), song);
            }
        }
    }

}
