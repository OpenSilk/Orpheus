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

package org.opensilk.music.ui.home;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.SortOrder;

import org.opensilk.music.loaders.SongCursorLoader;
import org.opensilk.music.ui.cards.CardShuffle;
import org.opensilk.music.ui.cards.views.ThemedCardView;
import org.opensilk.music.ui.home.adapter.SongCardCursorAdapter;
import org.opensilk.music.ui.library.CardListFragment;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

/**
 * Created by drew on 6/24/14.
 */
public class SongFragment extends CardListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    @Inject @ForActivity
    DrawerHelper mDrawerHelper;

    protected PreferenceUtils mPreferences;

    private SongCardCursorAdapter mAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DaggerInjector) getParentFragment()).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new SongCardCursorAdapter(getActivity(), (DaggerInjector) getParentFragment());
        // Start the loader
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Shuffle card header view
        ThemedCardView cardView = (ThemedCardView) getLayoutInflater(null).inflate(R.layout.list_card_layout, null);
        // Set card (holds inner view)
        cardView.setCard(new CardShuffle(getActivity()));
        // Add card to list
        getListView().addHeaderView(cardView);
        // set the adapter
        setListAdapter(mAdapter);
        // set empty view
        setEmptyText(getString(R.string.empty_music));
        // hide list until loaded
        setListShown(false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mDrawerHelper.isDrawerOpen()) {
            inflater.inflate(R.menu.song_sort_by, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_by_az:
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
                refresh();
                return true;
            case R.id.menu_sort_by_za:
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_Z_A);
                refresh();
                return true;
            case R.id.menu_sort_by_artist:
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
                refresh();
                return true;
            case R.id.menu_sort_by_album:
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
                refresh();
                return true;
            case R.id.menu_sort_by_year:
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
                refresh();
                return true;
            case R.id.menu_sort_by_duration:
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_DURATION);
                refresh();
                return true;
            case R.id.menu_sort_by_filename:
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_FILENAME);
                refresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refresh() {
        // Wait a moment for the preference to change.
        SystemClock.sleep(10);
        getLoaderManager().restartLoader(0, null, this);
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SongCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        setListShown(true);
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (isResumed()) {
            setListShown(false);
        }
        mAdapter.swapCursor(null);
    }

    /*
     * Abstract methods
     */

    @Override
    public int getListViewLayout() {
        return R.layout.card_listview_fastscroll2;
    }

}
