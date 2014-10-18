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
import com.andrew.apollo.utils.SortOrder;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.ui.home.adapter.SongAdapter;
import org.opensilk.music.ui.home.loader.SongLoader;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

/**
 * Created by drew on 6/24/14.
 */
public class SongFragment extends BasePagerFragment {

    @Inject @ForActivity
    DrawerHelper mDrawerHelper;

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
                mSettings.putString(AppPreferences.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z);
                refresh();
                return true;
            case R.id.menu_sort_by_za:
                mSettings.putString(AppPreferences.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_Z_A);
                refresh();
                return true;
            case R.id.menu_sort_by_artist:
                mSettings.putString(AppPreferences.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_ARTIST);
                refresh();
                return true;
            case R.id.menu_sort_by_album:
                mSettings.putString(AppPreferences.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_ALBUM);
                refresh();
                return true;
            case R.id.menu_sort_by_year:
                mSettings.putString(AppPreferences.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_YEAR);
                refresh();
                return true;
            case R.id.menu_sort_by_duration:
                mSettings.putString(AppPreferences.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_DURATION);
                refresh();
                return true;
            case R.id.menu_sort_by_filename:
                mSettings.putString(AppPreferences.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_FILENAME);
                refresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SongLoader(getActivity(), mSettings.getString(AppPreferences.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z));
    }

    @Override
    protected CursorAdapter createAdapter() {
        if (wantGridView()) {
            throw new UnsupportedOperationException("Cant to grid yet");
        } else {
            return new SongAdapter(getActivity(), mInjector);
        }
    }

    @Override
    public boolean wantGridView() {
        return false;
    }

}
