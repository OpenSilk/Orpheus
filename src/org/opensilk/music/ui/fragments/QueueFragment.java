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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
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

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.RecentSong;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.DragSortListView.RemoveListener;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.bus.EventBus;
import org.opensilk.music.bus.events.MetaChanged;
import org.opensilk.music.bus.events.MusicServiceConnectionChanged;
import org.opensilk.music.bus.events.PlaystateChanged;
import org.opensilk.music.bus.events.QueueChanged;
import org.opensilk.music.bus.events.Refresh;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.cards.SongQueueCard;
import org.opensilk.music.ui.cards.event.SongQueueCardClick;
import org.opensilk.music.ui.fragments.adapter.QueueAdapter;
import org.opensilk.music.ui.fragments.loader.QueueLoader;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForFragment;
import org.opensilk.silkdagger.support.ScopedDaggerFragment;

import java.util.List;

import javax.inject.Inject;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * This class is used to display all of the songs in the queue.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class QueueFragment extends ScopedDaggerFragment implements
        LoaderCallbacks<List<RecentSong>>,
        DropListener,
        RemoveListener {

    @Inject @ForFragment
    Bus mFragmentBus;

    private QueueAdapter mAdapter;
    private DragSortListView mListView;

    private GlobalBusMonitor mGlobalMonitor;
    private FragmentBusMonitor mFragmentMonitor;

    /** Track loader state */
    private boolean isFirstLoad = true;

    /** Stores list state when loader is restarted */
    private ScrollPosition mLastPosition = new ScrollPosition();
    private static class ScrollPosition {
        private int prevActiveIndex;
        private int index;
        private int top;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register the music status listener
        mGlobalMonitor = new GlobalBusMonitor();
        EventBus.getInstance().register(mGlobalMonitor);
        // register with localbus
        mFragmentMonitor = new FragmentBusMonitor();
        mFragmentBus.register(mFragmentMonitor);
        // init adapter
        mAdapter = new QueueAdapter(getActivity(), this);
        //We have to set this manually since we arent using CardListView
        mAdapter.setRowLayoutId(R.layout.list_card_dragsort_layout);
        // Start the loader
        getLoaderManager().initLoader(0, null, this);
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

        if (savedInstanceState == null) {
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
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onDestroy() {
        // Unregister the busses;
        EventBus.getInstance().unregister(mGlobalMonitor);
        mFragmentBus.unregister(mFragmentMonitor);
        super.onDestroy();
    }

    /*
     * Loader callbacks
     */

    @Override
    public Loader<List<RecentSong>> onCreateLoader(final int id, final Bundle args) {
        return new QueueLoader(getActivity());
    }

    @Override
    //@DebugLog
    public void onLoadFinished(final Loader<List<RecentSong>> loader, final List<RecentSong> data) {
        mAdapter.clear();
        // Check for any errors
        if (data == null || data.isEmpty()) {
            return;
        }
        mAdapter.addSongs(data);
        // On first load go to current song else restore the previous scroll position
        if (isFirstLoad) {
            isFirstLoad = false;
            scrollToCurrentSong();
        } else {
            restoreScrollPosition();
        }
    }

    @Override
    public void onLoaderReset(Loader<List<RecentSong>> listLoader) {
        mAdapter.clear();
    }

    /*
     * RemoveListener
     */

    @Override
    public void remove(final int which) {
        final Card c = mAdapter.getItem(which);
        mAdapter.remove(c);
        ApolloUtils.execute(false, new CommandRunner(getActivity(), new Command() {
            @Override
            public CharSequence execute() {
                MusicUtils.removeQueueItem(((SongQueueCard) c).getData().recentId);
                return null;
            }
        }));
    }

    /*
     * Droplistener
     */

    @Override
    public void drop(final int from, final int to) {
        Card c = mAdapter.getItem(from);
        mAdapter.setNotifyOnChange(false);
        mAdapter.remove(c);
        mAdapter.insert(c, to);
        mAdapter.setNotifyOnChange(true);
        mAdapter.notifyDataSetChanged();
        ApolloUtils.execute(false, new CommandRunner(getActivity(), new Command() {
            @Override
            public CharSequence execute() {
                MusicUtils.moveQueueItem(from, to);
                return null;
            }
        }));
    }

    /**
     * Scrolls the list to the currently playing song
     */
    //@DebugLog
    public void scrollToCurrentSong() {
        final int currentSongPosition = getCurrentTrackPosition();

        if (currentSongPosition != 0) {
            mListView.setSelection(currentSongPosition);
        }
    }

    private int getCurrentTrackPosition() {
        final long trackId = MusicUtils.getCurrentAudioId();
        return getTrackPosition(trackId);
    }

    private int getTrackPosition(long trackId) {
        if (mAdapter == null || mAdapter.getCount() == 0) {
            return 0;
        }
        for (int i = 0; i < mAdapter.getCount(); i++) {
            if (trackId == ((SongQueueCard) mAdapter.getItem(i)).getData().recentId) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Restores list position after loader restart
     */
    private void restoreScrollPosition() {
        if (MusicUtils.getShuffleMode() == MusicPlaybackService.SHUFFLE_AUTO) {
            // when the service adjustss the queue our saved state is no longer
            // valid so we must compensate for the new offset so we dont
            // jump to the wrong position
            int activeIndex = getCurrentTrackPosition();
            if (activeIndex != mLastPosition.prevActiveIndex) {
                mLastPosition.index += (activeIndex - mLastPosition.prevActiveIndex);
                if (mLastPosition.index < 0) {
                    mLastPosition.index = 0;
                }
            }
        }
        mListView.setSelectionFromTop(mLastPosition.index, mLastPosition.top);
    }

    /*
     * Abstract methods
     */

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new QueueModule(),
        };
    }

    @Override
    protected DaggerInjector getParentInjector(Activity activity) {
        return (DaggerInjector) activity;
    }

    class GlobalBusMonitor {

        private final Handler mHandler;
        private final Runnable mLoaderRestartRunnable;

        GlobalBusMonitor() {
            mHandler = new Handler();
            mLoaderRestartRunnable = new Runnable() {
                @Override
                public void run() {
                    restartLoader(null);
                }
            };
        }

        @Subscribe
        public void restartLoader(Refresh e) {
            if (isAdded()) {
                // This is a slight hack to give us a refrence if the
                // items in the queue are moved
                mLastPosition.prevActiveIndex = getCurrentTrackPosition();
                // store top most index
                mLastPosition.index = mListView.getFirstVisiblePosition();
                View v = mListView.getChildAt(0);
                // store offset of top item
                mLastPosition.top = v == null ? 0 : v.getTop();
                getLoaderManager().restartLoader(0, null, QueueFragment.this);
            }
        }

        @Subscribe
        public void onMetaChanged(MetaChanged e) {
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Subscribe
        public void onPlaystateChanged(PlaystateChanged e) {
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Subscribe
        //@DebugLog
        public void onQueueChanged(QueueChanged e) {
            // For auto shuffle when the queue gets adjusted we
            // can receive several QUEUE_CHANGED updates in quick
            // succession so we batch the restart calls so our
            // saved state doesnt alter unexpectedly while
            // we wait on the loader
            scheduleLoaderRestart();
        }

        @Subscribe
        public void onMusicServiceConnectionChanged(MusicServiceConnectionChanged e) {
            if (e.isConnected()) {
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        private void scheduleLoaderRestart() {
            mHandler.removeCallbacks(mLoaderRestartRunnable);
            mHandler.postDelayed(mLoaderRestartRunnable, 40);
        }

    }

    class FragmentBusMonitor {
        @Subscribe
        public void onCardItemClicked(SongQueueCardClick e) {
            final RecentSong song = e.song;
            Command c = null;
            switch (e.event) {
                case OPEN:
                    c = new Command() {
                        @Override
                        public CharSequence execute() {
                            final int pos = getTrackPosition(song.recentId);
                            // When selecting a track from the queue, just jump there instead of
                            // reloading the queue. This is both faster, and prevents accidentally
                            // dropping out of party shuffle.
                            MusicUtils.setQueuePosition(pos);
                            return null;
                        }
                    };
                    break;
                case PLAY_NEXT:
                    c = new Command() {
                        @Override
                        public CharSequence execute() {
                            MusicUtils.removeQueueItem(song.recentId);
                            MusicUtils.playNext(new long[]{song.recentId});
                            return null;
                        }
                    };
                    break;
                case ADD_TO_PLAYLIST:
                    if (song.isLocal) {
                        try {
                            long id = Long.decode(song.identity);
                            AddToPlaylistDialog.newInstance(new long[]{id})
                                    .show(getActivity().getSupportFragmentManager(), "AddToPlaylistDialog");
                        } catch (NumberFormatException ex) {
                            //TODO
                        }
                    } // else unsupported
                    return;
                case MORE_BY_ARTIST:
                    if (song.isLocal) {
                        NavUtils.openArtistProfile(getActivity(), MusicUtils.makeArtist(getActivity(), song.artistName));
                    } // else TODO
                    return;
                case SET_RINGTONE:
                    if (song.isLocal) {
                        c = new Command() {
                            @Override
                            public CharSequence execute() {
                                try {
                                    long id = Long.decode(song.identity);
                                    MusicUtils.setRingtone(getActivity(), id);
                                } catch (NumberFormatException ex) {
                                    //TODO
                                }
                                return null;
                            }
                        };
                    } // else unsupported
                    break;
                case DELETE:
                    if (song.isLocal) {
                        try {
                            long id = Long.decode(song.identity);
                            DeleteDialog.newInstance(song.name, new long[]{id}, null)
                                    .show(getActivity().getSupportFragmentManager(), "DeleteDialog");
                        } catch (NumberFormatException ex) {
                            //TODO
                        }
                    } // else unsupported
                    return;
                default:
                    return;
            }
            if (c != null) {
                ApolloUtils.execute(false, new CommandRunner(getActivity(), c));
            }
        }

    }

}
