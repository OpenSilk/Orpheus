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
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.andrew.apollo.MusicStateListener;
import com.andrew.apollo.R;

import org.opensilk.music.ui.activities.BaseSlidingActivity;

import it.gmariotti.cardslib.library.view.CardGridView;
import it.gmariotti.cardslib.library.view.CardListView;

/**
 * All the common elements used in fragments backed by cursor loaders
 *
 * Created by drew on 2/22/14.
 */
public abstract class HomePagerBaseCursorFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        MusicStateListener {

    /**
     * LoaderCallbacks identifier
     */
    protected static final int LOADER = 0;

    /**
     * Fragment UI
     */
    protected ViewGroup mRootView;

    /**
     * The grid view
     */
    protected CardGridView mGridView;

    /**
     * The list view
     */
    protected CardListView mListView;

    /**
     * Loading progress
     */
    protected View mLoadingEmpty;

    /**
     * Our cursor adapter
     */
    protected CursorAdapter mAdapter;


    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        // Register the music status listener //TODO dont want this anymore
        ((BaseSlidingActivity)activity).setMusicStateListenerListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = createAdapter();
        // Start the loader
        getLoaderManager().initLoader(LOADER, null, this);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // The View for the fragment's UI
        if (isSimpleLayout()) {
            mRootView = (ViewGroup)inflater.inflate(R.layout.card_listview_fastscroll, null);
            mLoadingEmpty = mRootView.findViewById(android.R.id.empty);
            mListView = (CardListView) mRootView.findViewById(android.R.id.list);
            mListView.setEmptyView(mLoadingEmpty);
            // Set the data behind the list
            mListView.setAdapter(mAdapter);
        } else {
            mRootView = (ViewGroup)inflater.inflate(R.layout.card_gridview, null);
            mLoadingEmpty = mRootView.findViewById(android.R.id.empty);
            mGridView = (CardGridView) mRootView.findViewById(R.id.card_grid);
            mGridView.setEmptyView(mLoadingEmpty);
            // Set the data behind the grid
            mGridView.setAdapter(mAdapter);
        }
        return mRootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRootView = null;
        mListView = null;
        mGridView = null;
    }

    /**
     * Restarts the loader. Called when user updates the sort by option
     */
    public void refresh() {
        // Wait a moment for the preference to change.
        SystemClock.sleep(10);
        getLoaderManager().restartLoader(LOADER, null, this);
    }

    /*
     * Loader Callbacks
     */

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.isClosed() || data.getCount() <= 0) {
            // hide the progress
            mLoadingEmpty.setVisibility(View.GONE);
            // Set the empty text
            final FrameLayout emptyView = (FrameLayout) mRootView.findViewById(R.id.empty);
            final TextView emptyText = (TextView)mRootView.findViewById(R.id.empty_text);
            emptyText.setText(getString(R.string.empty_music));
            if (isSimpleLayout()) {
                mListView.setEmptyView(emptyView);
            } else {
                mGridView.setEmptyView(emptyView);
            }
        }
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    /*
     * MusicState Listener
     */

    @Override
    public void restartLoader() {

    }

    @Override
    public void onMetaChanged() {

    }

    /*
     * Abstract methods
     */

    protected abstract CursorAdapter createAdapter();
    protected abstract boolean isSimpleLayout();
}
