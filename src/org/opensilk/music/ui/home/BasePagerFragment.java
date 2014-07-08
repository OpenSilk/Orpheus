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
import android.view.View;
import android.widget.CursorAdapter;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.silkdagger.DaggerInjector;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 6/28/14.
 */
public abstract class BasePagerFragment extends CardListGridFragment implements LoaderManager.LoaderCallbacks<Cursor>{

    protected static final int LOADER = 0;

    protected DaggerInjector mInjector;

    protected CursorAdapter mAdapter;
    protected PreferenceUtils mPreferences;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mInjector = (DaggerInjector) getParentFragment();
        mInjector.inject(this);
    }

    @Override
    //@DebugLog
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceUtils.getInstance(getActivity());
        mAdapter = createAdapter();
        // Start the loader
        getLoaderManager().initLoader(LOADER, null, this);
    }

    @Override
    //@DebugLog
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setEmptyText(getEmptyText());
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(mAdapter);
        setListShown(false);
//        if (!wantGridView()) {
//            // reset dividers for CardListView
//            ((ListView) getListView()).setDividerHeight((int)(1 * getResources().getDisplayMetrics().density));
//        }
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter = null;
        mPreferences = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mInjector = null;
    }

    /**
     * Restarts the loader. Called when user updates the sort by option
     */
    public void refresh() {
        // Wait a moment for the preference to change.
        SystemClock.sleep(10);
        getLoaderManager().restartLoader(LOADER, null, this);
    }

    protected CharSequence getEmptyText() {
        return getString(R.string.empty_music);
    }

    protected abstract CursorAdapter createAdapter();
    protected abstract boolean wantGridView();

    /*
     * Loader Callbacks
     */

    @Override
    @DebugLog
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        //seems to be required for staggeredgrid
        mAdapter.notifyDataSetChanged();
        setListShown(true);
    }

    @Override
    @DebugLog
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
        return wantGridView() ? R.layout.card_gridview : R.layout.card_listview;
    }

    @Override
    public int getEmptyViewLayout() {
        return R.layout.list_empty_view;
    }

}
