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

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;

import com.andrew.apollo.MusicStateListener;
import com.andrew.apollo.R;

import org.opensilk.music.ui.activities.BaseSlidingActivity;

import java.util.List;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 2/11/14.
 */
public abstract class HomePagerBaseFragment<D> extends Fragment implements
        LoaderManager.LoaderCallbacks<List<D>>,
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
    protected GridView mGridView;

    /**
     * The list view
     */
    protected ListView mListView;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        // Register the music status listener
        ((BaseSlidingActivity)activity).setMusicStateListenerListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // The View for the fragment's UI
        if (isSimpleLayout()) {
            mRootView = (ViewGroup)inflater.inflate(R.layout.card_listview_thumb, null);
            mListView = (ListView) mRootView.findViewById(android.R.id.list);
        } else {
            mRootView = (ViewGroup)inflater.inflate(R.layout.card_gridview, null);
            mGridView = (GridView) mRootView.findViewById(R.id.card_grid);
        }
        return mRootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        // Start the loader
        getLoaderManager().initLoader(LOADER, null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @DebugLog
    public void onLoaderReset(final Loader<List<D>> loader) {

    }

    /**
     * Restarts the loader.
     */
    public void refresh() {
        // Wait a moment for the preference to change.
        SystemClock.sleep(10);
        getLoaderManager().restartLoader(LOADER, null, this);
    }

    /**
     * Implement MusicStateListener
     */
    @Override
    public void restartLoader() {
        // Update the list when the user deletes any items
        getLoaderManager().restartLoader(LOADER, null, this);
    }

    /**
     * ImplementMusicStateListener
     */
    @Override
    public void onMetaChanged() {
        // Nothing to do
    }

    protected abstract boolean isSimpleLayout();

    protected abstract boolean isDetailedLayout();

}
