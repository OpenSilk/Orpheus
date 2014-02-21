/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.opensilk.music.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import com.andrew.apollo.loaders.QueueLoader;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.recycler.RecycleHolder;
import com.andrew.apollo.utils.MusicUtils;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.DragSortListView.RemoveListener;

import org.opensilk.music.ui.cards.CardQueueList;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;

/**
 * This class is used to display all of the songs in the queue.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class QueueFragment extends Fragment implements
        LoaderCallbacks<List<Song>>,
        DropListener,
        RemoveListener {

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER = 0;

    /**
     * The adapter for the list
     */
    private CardArrayAdapter mAdapter;

    /**
     * The list view
     */
    private DragSortListView mListView;

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
        final ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.dragsort_listview, null);
        // Initialize the list
        mListView = (DragSortListView)rootView.findViewById(android.R.id.list);
        // Release any references to the recycled Views
        mListView.setRecyclerListener(new RecycleHolder());
        // Set the drop listener
        mListView.setDropListener(this);
        // Set the swipe to remove listener
        mListView.setRemoveListener(this);
        return rootView;
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

    @Override
    public void onResume() {
        super.onResume();
        scrollToCurrentSong();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @DebugLog
    public Loader<List<Song>> onCreateLoader(final int id, final Bundle args) {
        return new QueueLoader(getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @DebugLog
    public void onLoadFinished(final Loader<List<Song>> loader, final List<Song> data) {
        // Check for any errors
        if (data.isEmpty()) {
            return;
        }

        ArrayList<Card> cards = new ArrayList<Card>();
        for (int ii=0; ii<data.size(); ii++) {
            CardQueueList card = new CardQueueList(getActivity(), data.get(ii));
            card.setId(String.valueOf(ii));
            cards.add(card);
        }
        mAdapter = new CardArrayAdapter(getActivity(), cards);
        //We have to set this manually since we arent using CardListView
        mAdapter.setRowLayoutId(R.layout.dragsort_card_list_thumb);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
        scrollToCurrentSong();
    }

    @Override
    @DebugLog
    public void onLoaderReset(Loader<List<Song>> listLoader) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @DebugLog
    public void remove(final int which) {
        Card c = mAdapter.getItem(which);
        mAdapter.remove(c);
        mAdapter.notifyDataSetChanged();
        MusicUtils.removeTrack(((CardQueueList) c).getData().mSongId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final int from, final int to) {
        Card c = mAdapter.getItem(from);
        mAdapter.remove(c);
        mAdapter.insert(c, to);
        mAdapter.notifyDataSetChanged();
        MusicUtils.moveQueueItem(from, to);
    }

    /**
     * Scrolls the list to the currently playing song when the user touches the
     * header in the {@link TitlePageIndicator}.
     */
    @DebugLog
    public void scrollToCurrentSong() {
        final int currentSongPosition = getItemPositionBySong();

        if (currentSongPosition != 0) {
            mListView.setSelection(currentSongPosition);
        }
    }

    /**
     * @return The position of an item in the list based on the name of the
     *         currently playing song.
     */
    @DebugLog
    private int getItemPositionBySong() {
        final long trackId = MusicUtils.getCurrentAudioId();
        if (mAdapter == null) {
            return 0;
        }
        for (int i = 0; i < mAdapter.getCount(); i++) {
            if (((CardQueueList) mAdapter.getItem(i)).getData().mSongId == trackId) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Called to restart the loader callbacks
     */
    @DebugLog
    public void refreshQueue() {
        if (isAdded()) {
            getLoaderManager().restartLoader(LOADER, null, this);
        }
    }

    public CardArrayAdapter getAdapter() {
        return mAdapter;
    }
}
