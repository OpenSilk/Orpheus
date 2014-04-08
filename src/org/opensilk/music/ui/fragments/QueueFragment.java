/*
 * Copyright (C) 2012 Andrew Neal
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

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;

import com.andrew.apollo.MusicStateListener;
import com.andrew.apollo.R;
import com.andrew.apollo.loaders.QueueLoader;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.MusicUtils;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.DragSortListView.RemoveListener;

import org.opensilk.music.ui.activities.BaseSlidingActivity;
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
        MusicStateListener,
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

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        // The View for the fragment's UI
        final ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.dragsort_listview, container, false);
        // Initialize the list
        mListView = (DragSortListView)rootView.findViewById(android.R.id.list);
        // Set the drop listener
        mListView.setDropListener(this);
        // Set the swipe to remove listener
        mListView.setRemoveListener(this);

        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(50);
        set.addAnimation(animation);

        animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f,Animation.RELATIVE_TO_SELF, 0.0f
        );
        animation.setDuration(140);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(set, 0.5f);
        mListView.setLayoutAnimation(controller);
        return rootView;
    }

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

    @Override
    public void onStart() {
        super.onStart();
        // Register the music status listener
        ((BaseSlidingActivity) getActivity()).addMusicStateListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        ((BaseSlidingActivity) getActivity()).removeMusicStateListener(this);
    }

    @Override
    public Loader<List<Song>> onCreateLoader(final int id, final Bundle args) {
        return new QueueLoader(getActivity());
    }

    @Override
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
        mAdapter.setRowLayoutId(R.layout.dragsort_card_list);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
        scrollToCurrentSong();
    }

    @Override
    public void onLoaderReset(Loader<List<Song>> listLoader) {

    }

    @Override
    public void remove(final int which) {
        Card c = mAdapter.getItem(which);
        mAdapter.remove(c);
        mAdapter.notifyDataSetChanged();
        MusicUtils.removeTrack(((CardQueueList) c).getData().mSongId);
    }

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

    public CardArrayAdapter getAdapter() {
        return mAdapter;
    }

    private void notifyDataSetChanged() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        }, 20);
    }

    @Override
    public void restartLoader() {
        if (isAdded()) {
            getLoaderManager().restartLoader(LOADER, null, this);
        }
    }

    @Override
    public void onMetaChanged() {
        notifyDataSetChanged();
        //mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPlaystateChanged() {
        notifyDataSetChanged();
        //mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onQueueChanged() {
        // Added this callback but now im thinking why?
        // Only way i can think of to change the queue while it is showing is
        // through the queue, so we already know about all the changes
//        if (isAdded()) {
//            getLoaderManager().restartLoader(LOADER, null, this);
//        }
    }
}
