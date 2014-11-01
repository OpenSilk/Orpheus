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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.model.RecentSong;

import org.opensilk.common.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.mortar.PausesAndResumes;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.event.ConfirmDelete;
import org.opensilk.music.ui2.event.OpenAddToPlaylist;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.NowPlayingCursor;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.Observers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.music.util.RxUtil.isSubscribed;
import static org.opensilk.music.util.RxUtil.notSubscribed;

/**
 * Created by drew on 10/15/14.
 */
public class QueueBlueprint {

    @Singleton
    public static class Presenter extends ViewPresenter<QueueView> implements PausesAndResumes {

        final Context appContext;
        final MusicServiceConnection musicService;
        final EventBus bus;
        final PauseAndResumeRegistrar pauseAndResumeRegistrar;

        @Inject
        public Presenter(@ForApplication Context context,
                         MusicServiceConnection musicService,
                         @Named("activity") EventBus bus,
                        PauseAndResumeRegistrar pauseAndResumeRegistrar) {
            this.appContext = context;
            this.musicService = musicService;
            this.bus = bus;
            this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            super.onEnterScope(scope);
            pauseAndResumeRegistrar.register(scope, this);
            setupObservables();
            setupObservers();
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            getView().setup();
            subscribeBroadcasts();
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
            unsubscribeBroadcasts();
        }

        @Override
        public void onResume() {
            subscribeBroadcasts();
        }

        @Override
        public void onPause() {
            unsubscribeBroadcasts();
        }

        public void setQueuePosition(int position) {

        }

        public void removeQueueItem(long recentId) {

        }

        public void moveQueueItem(int from, int to) {

        }

        public boolean handleItemOverflowClick(OverflowAction action, RecentSong song) {
            switch (action) {
                case PLAY_NEXT:
                    removeQueueItem(song.recentId);
                    //MusicUtils.playNext(new long[]{song.recentId});
                    return true;
                case ADD_TO_PLAYLIST:
                    if (song.isLocal) {
                        try {
                            long id = Long.decode(song.identity);
                            bus.post(new OpenAddToPlaylist(new long[]{id}));
                        } catch (NumberFormatException ex) {
                            //TODO
                        }
                    } // else unsupported
                    return true;
                case MORE_BY_ARTIST:
                    if (song.isLocal) {
//                        NavUtils.openArtistProfile(getActivity(), MusicUtils.makeArtist(getActivity(), song.artistName));
                    } // else TODO
                    return true;
                case SET_RINGTONE:
                    if (song.isLocal) {
//                        try {
//                            long id = Long.decode(song.identity);
//                            MusicUtils.setRingtone(getActivity(), id);
//                        } catch (NumberFormatException ex) {
//                            //TODO
//                        }
                    } // else unsupported
                    return true;
                case DELETE:
                    if (song.isLocal) {
                        try {
                            long id = Long.decode(song.identity);
                            bus.post(new ConfirmDelete(new long[]{id}, song.name));
                        } catch (NumberFormatException ex) {
                            //TODO
                        }
                    } // else unsupported
                    return true;
                default:
                    return false;
            }
        }

        List<RecentSong> getQueue() {
            Cursor c = new NowPlayingCursor(appContext, musicService);
            List<RecentSong> songs = new ArrayList<>(c.getCount());
            if (c.moveToFirst()) {
                do {
                    final RecentSong s = CursorHelpers.makeRecentSongFromRecentCursor(c);
                    songs.add(s);
                } while (c.moveToNext());
            }
            c.close();
            return songs;
        }

        void updateQueue(List<RecentSong> newQueue) {
            QueueView v = getView();
            if (v == null) return;
            v.adapter.setNotifyOnChange(false);
            v.adapter.clear();
            v.adapter.addAll(newQueue);
            v.adapter.setNotifyOnChange(true);
            v.adapter.notifyDataSetChanged();
        }

        Observable<Boolean> playStateObservable;
        Observable<Long> metaChangedObservable;
        Observable<List<RecentSong>> queueChangedObservable;

        void setupObservables() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
            intentFilter.addAction(MusicPlaybackService.META_CHANGED);
            intentFilter.addAction(MusicPlaybackService.QUEUE_CHANGED);
            Scheduler scheduler = Schedulers.computation();
            // obr will call onNext on the main thread so we observeOn computation
            // so our chained operators will be called on computation instead of main.
            Observable<Intent> intentObservable = AndroidObservable.fromBroadcast(appContext, intentFilter);
            playStateObservable = intentObservable.observeOn(scheduler)
                    // Filter for only PLAYSTATE_CHANGED actions
                    .filter(new Func1<Intent, Boolean>() {
                        // called on computation
                        @Override
                        public Boolean call(Intent intent) {
                            Timber.v("playstateSubscripion filter called on %s", Thread.currentThread().getName());
                            return MusicPlaybackService.PLAYSTATE_CHANGED.equals(intent.getAction());
                        }
                    })
                            // filter out repeats only taking most recent
//                    .debounce(20, TimeUnit.MILLISECONDS, scheduler)
                    // XXX the intent contains the playstate as an extra but it could be out of date
//                    .map(new Func1<Intent, Boolean>() {
//                        @Override
//                        public Boolean call(Intent intent) {
//                            return intent.getBooleanExtra("playing", false);
//                        }
//                    })
                    // flatMap the intent into a boolean by requesting the playstate
                    .flatMap(new Func1<Intent, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(Intent intent) {
                            Timber.v("playstateSubscription flatMap called on %s", Thread.currentThread().getName());
                            return musicService.isPlaying();
                        }
                    })
                            // observe final result on main thread
                    .observeOn(AndroidSchedulers.mainThread());
            metaChangedObservable = intentObservable.observeOn(scheduler)
                    .filter(new Func1<Intent, Boolean>() {
                        @Override
                        public Boolean call(Intent intent) {
                            return MusicPlaybackService.META_CHANGED.equals(intent.getAction());
                        }
                    })
//                    .map(new Func1<Intent, Long>() {
//                        @Override
//                        public Long call(Intent intent) {
//                            return intent.getLongExtra("id", -1);
//                        }
//                    })
                    .flatMap(new Func1<Intent, Observable<Long>>() {
                        @Override
                        public Observable<Long> call(Intent intent) {
                            return musicService.getAudioId();
                        }
                    });
            queueChangedObservable = intentObservable.observeOn(Schedulers.io())
                    .filter(new Func1<Intent, Boolean>() {
                        @Override
                        public Boolean call(Intent intent) {
                            return MusicPlaybackService.QUEUE_CHANGED.equals(intent.getAction());
                        }
                    })
                    .map(new Func1<Intent, List<RecentSong>>() {
                        @Override
                        public List<RecentSong> call(Intent intent) {
                            return getQueue();
                        }
                    });
        }

        Observer<Boolean> playStateObserver;
        Observer<Long> metaChangedObserver;
        Observer<List<RecentSong>> queueChangedObserver;

        void setupObservers() {
            playStateObserver = Observers.create(new Action1<Boolean>() {
                @Override
                public void call(Boolean playing) {
                    QueueView v = getView();
                    if (v == null) return;
                    v.onPlaystateChanged(playing);
                }
            });
            metaChangedObserver = Observers.create(new Action1<Long>() {
                @Override
                public void call(Long audioId) {
                    QueueView v = getView();
                    if (v == null) return;
                    v.onCurrentSongChanged(audioId);
                }
            });
            queueChangedObserver = Observers.create(new Action1<List<RecentSong>>() {
                @Override
                public void call(List<RecentSong> recentSongs) {
                    updateQueue(recentSongs);
                }
            });
        }

        CompositeSubscription broadcastSubscriptions;

        void subscribeBroadcasts() {
            if (isSubscribed(broadcastSubscriptions)) return;
            broadcastSubscriptions = new CompositeSubscription(
                    playStateObservable.subscribe(playStateObserver),
                    metaChangedObservable.subscribe(metaChangedObserver),
                    queueChangedObservable.subscribe(queueChangedObserver)
            );
        }

        void unsubscribeBroadcasts() {
            if (notSubscribed(broadcastSubscriptions)) return;
            broadcastSubscriptions.unsubscribe();
            broadcastSubscriptions = null;
        }
    }


}
