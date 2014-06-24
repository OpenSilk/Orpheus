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

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.SortOrder;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.adapters.SongListCardCursorAdapter;
import org.opensilk.music.loaders.SongCursorLoader;
import org.opensilk.music.ui.cards.old.CardShuffleList;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

import it.gmariotti.cardslib.library.view.CardListView;
import it.gmariotti.cardslib.library.view.CardView;

/**
 * Songs
 */
public class HomeSongFragment extends HomePagerBaseCursorFragment {

    @Inject @ForActivity
    DrawerHelper mDrawerHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The View for the fragment's UI
        mRootView = (ViewGroup)inflater.inflate(R.layout.card_listview_fastscroll, container, false);
        mLoadingEmpty = mRootView.findViewById(android.R.id.empty);

        mListView = (CardListView) mRootView.findViewById(android.R.id.list);
        mListView.setEmptyView(mLoadingEmpty);

        //Shuffle card header view
        CardView cardView = (CardView) inflater.inflate(R.layout.card_list_shuffle, null);
        // Theme the shuffle icon
        ImageView thumbnail = (ImageView) cardView.findViewById(R.id.card_thumbnail_image);
        thumbnail.setImageDrawable(ThemeHelper.getInstance(getActivity()).getShuffleButtonDrawable());
        // Set card (holds inner view)
        cardView.setCard(new CardShuffleList(getActivity()));
        // Add card to list
        mListView.addHeaderView(cardView);

        // Set the data behind the list
        mListView.setAdapter(mAdapter);

        return mRootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mDrawerHelper.isDrawerOpen()) {
            super.onCreateOptionsMenu(menu, inflater);
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

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SongCursorLoader(getActivity());
    }

    /*
     * Implement abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        return new SongListCardCursorAdapter(getActivity());
    }

    @Override
    protected boolean isSimpleLayout() {
        return true; // Were always list
    }

}
