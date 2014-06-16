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
import android.widget.FrameLayout;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.adapters.RecentGridCardCursorAdapter;
import org.opensilk.music.adapters.RecentListCardCursorAdapter;
import org.opensilk.music.loaders.RecentCursorLoader;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

import static com.andrew.apollo.utils.PreferenceUtils.RECENT_LAYOUT;

/**
 * Recents
 */
public class HomeRecentFragment extends HomePagerBaseCursorFragment {

    @Inject @ForActivity
    DrawerHelper mDrawerHelper;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mDrawerHelper.isDrawerOpen()) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.view_as, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_view_as_simple:
                mPreferences.setRecentLayout("simple");
                NavUtils.goHome(getActivity());
                return true;
            case R.id.menu_view_as_grid:
                mPreferences.setRecentLayout("grid");
                NavUtils.goHome(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new RecentCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
        if (data == null || data.isClosed() || data.getCount() <= 0) {
            // Set the empty text
            final FrameLayout emptyView = (FrameLayout) mRootView.findViewById(R.id.empty);
            final TextView emptyText = (TextView)mRootView.findViewById(R.id.empty_text);
            emptyText.setText(getString(R.string.empty_recent));
            if (isSimpleLayout()) {
                mListView.setEmptyView(emptyView);
            } else {
                mGridView.setEmptyView(emptyView);
            }
        }
    }

    /*
     * Implement abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        if (isSimpleLayout()) {
            return new RecentListCardCursorAdapter(getActivity());
        } else {
            return new RecentGridCardCursorAdapter(getActivity());
        }
    }

    protected boolean isSimpleLayout() {
        return PreferenceUtils.getInstance(getActivity()).isSimpleLayout(RECENT_LAYOUT,
                getActivity());
    }

}
