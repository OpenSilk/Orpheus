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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CursorAdapter;

import org.opensilk.music.R;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.SortOrder;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.ui.home.adapter.ArtistGridAdapter;
import org.opensilk.music.ui.home.adapter.ArtistListAdapter;
import org.opensilk.music.ui.home.loader.ArtistLoader;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

/**
 * Created by drew on 6/28/14.
 */
public class ArtistFragment extends BasePagerFragment {

    @Inject @ForActivity
    DrawerHelper mDrawerHelper;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mDrawerHelper.isDrawerOpen()) {
            inflater.inflate(R.menu.artist_sort_by, menu);
            inflater.inflate(R.menu.view_as, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_by_az:
                mSettings.putString(AppPreferences.ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_A_Z);
                refresh();
                return true;
            case R.id.menu_sort_by_za:
                mSettings.putString(AppPreferences.ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_Z_A);
                refresh();
                return true;
            case R.id.menu_sort_by_number_of_songs:
                mSettings.putString(AppPreferences.ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
                refresh();
                return true;
            case R.id.menu_sort_by_number_of_albums:
                mSettings.putString(AppPreferences.ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS);
                refresh();
                return true;
            case R.id.menu_view_as_simple:
                mSettings.putString(AppPreferences.ARTIST_LAYOUT, AppPreferences.SIMPLE);
                NavUtils.goHome(getActivity());
                return true;
            case R.id.menu_view_as_grid:
                mSettings.putString(AppPreferences.ARTIST_LAYOUT, AppPreferences.GRID);
                NavUtils.goHome(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new ArtistLoader(getActivity(), mSettings.getString(AppPreferences.ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_A_Z));
    }

    @Override
    protected CursorAdapter createAdapter() {
        if (wantGridView()) {
            return new ArtistGridAdapter(getActivity(), mInjector);
        } else {
            return new ArtistListAdapter(getActivity(), mInjector);
        }
    }

    @Override
    public boolean wantGridView() {
        return mSettings.getString(AppPreferences.ARTIST_LAYOUT, AppPreferences.GRID).equals(AppPreferences.GRID);
    }

}
