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
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.MenuItem;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.andrew.apollo.Config;

/**
 * Created by drew on 2/23/14.
 */
public abstract class ProfileBaseFragment<D extends Parcelable> extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // Loader identifier
    protected static final int LOADER = 2;

    // main content
    protected ListView mListView;
    protected CursorAdapter mAdapter;

    // object passed in bundle
    protected D mBundleData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            // We're fucked;
            getActivity().onBackPressed();
        }
        mBundleData = getArguments().getParcelable(Config.EXTRA_DATA);
        // init the adapter
        mAdapter = createAdapter();
        // start the loader
        getLoaderManager().initLoader(LOADER, createLoaderArgs(), this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set home as up
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        // Enable the options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Loader Callbacks
     */

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    protected void setTitle(String title) {
        getActivity().getActionBar().setTitle(title);
    }

    /*
     * Abstract methods
     */

    protected abstract CursorAdapter createAdapter();
    protected abstract Bundle createLoaderArgs();

}
