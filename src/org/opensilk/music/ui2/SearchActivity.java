/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui2;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;

import org.opensilk.common.flow.Screen;
import org.opensilk.music.R;
import org.opensilk.music.ui2.main.Main;


import static android.app.SearchManager.QUERY;

/**
 * Created by drew on 11/9/14.
 */
public class SearchActivity extends BaseSwitcherActivity implements SearchView.OnQueryTextListener {

    public static class Blueprint extends BaseMortarActivity.Blueprint {

        public Blueprint(String scopeName) {
            super(scopeName);
        }

        @Override
        public Object getDaggerModule() {
            return new Module();
        }

    }

    @dagger.Module (
            includes = {
                    BaseSwitcherActivity.Module.class,
                    Main.Module.class,
            },
            injects = SearchActivity.class
    )
    public static class Module {

    }

    SearchView mSearchView;
    String mFilterString;

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
    protected Blueprint getBlueprint(String scopeName) {
        return new Blueprint(scopeName);
    }

    @Override
    public Screen getDefaultScreen() {
        return new Screen() {
        };
    }

    @Override
    protected void setupView() {
        setContentView(R.layout.main_profile);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpButtonEnabled(true);
        getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        if (savedInstanceState != null) {
            mFilterString = savedInstanceState.getString("query");
        } else {
            onNewIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mFilterString = intent.getStringExtra(QUERY);
            if (!TextUtils.isEmpty(mFilterString)) {
                if (mSearchView != null) {
                    mSearchView.setQuery(mFilterString, false);
                }
                restartLoaders();
                hideKeyboard();
            }
        }
//        setIntent(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("query", mFilterString);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Search view
        getMenuInflater().inflate(R.menu.searchview, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_searchview);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setIconified(false);
        if (!TextUtils.isEmpty(mFilterString)) {
            mSearchView.setQuery(mFilterString, false);
        }
        // Add voice search
        final SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
        mSearchView.setSearchableInfo(searchableInfo);
        return true;
    }

    void restartLoaders() {
//        final Uri uri = Uri.parse("content://media/external/audio/search/fancy/"
//                + Uri.encode(mFilterString));
//        return new CursorLoader(getActivity(), uri, mSearchColsFancy, null, null, null);
    }

    private void hideKeyboard() {
        // When the search is "committed" by the user, then hide the keyboard so
        // the user can more easily browse the list of results.
        if (mSearchView != null) {
            final InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
            mSearchView.clearFocus();
        }
    }

    /*
     * implement QueryTextListener
     */

    @Override
    public boolean onQueryTextSubmit(final String query) {
        if (TextUtils.isEmpty(query)) return false;
        hideKeyboard();
        return true;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        if (TextUtils.isEmpty(newText)) return false;
        // Called when the action bar search text has changed. Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        mFilterString = newText;
        restartLoaders();
        return true;
    }

    protected Object getCardFromCursor(Cursor cursor) {
        // Get the MIME type
        final String mimetype = cursor.getString(cursor
                .getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
    /*
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
            final LocalArtist artist = new LocalArtist(id, name, songCount, albumCount);
            final ArtistCard card = new ArtistCard(getContext(), artist);
            card.useListLayout();
            mInjector.inject(card);
            return card;
        } else if (mimetype.equals("album")) {
            // Get the Id of the album
            final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            // Get the album name
            final String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            // Get the artist nam
            final String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
            // generate artwork uri
            final Uri artworkUri = CursorHelpers.generateArtworkUri(id);
            // Build the album as best we can
            final LocalAlbum album = new LocalAlbum(id, name, artist, 0, null, artworkUri);
            final AlbumCard card = new AlbumCard(getContext(), album);
            card.useListLayout();
            mInjector.inject(card);
            return card;
        } else { // audio
            // get id
            final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            // Get the track name
            final String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            // Get the album name
            final String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            // get artist name
            final String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            // build the song as best we can
            final LocalSong song = new LocalSong(id, name, album, artist, null, 0, 0, CursorHelpers.generateDataUri(id), null, mimetype);
            final SongCard card = new SongCard(getContext(), song);
            card.useSimpleLayout();
//            mInjector.inject(card);
            return card;
        }
        */
        return null;
    }

}
