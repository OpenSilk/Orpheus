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
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.mortar.PausesAndResumes;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.music.R;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.BaseSwitcherActivity;
import org.opensilk.music.ui2.LauncherActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.event.ConfirmDelete;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.OpenAddToPlaylist;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.NowPlayingCursor;
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import flow.Layout;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observers.Observers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;
import static org.opensilk.common.rx.RxUtils.notSubscribed;
import static org.opensilk.common.rx.RxUtils.observeOnMain;

/**
 * Created by drew on 10/15/14.
 */
@Layout(R.layout.queue)
@WithModule(QueueScreen.Module.class)
@WithTransitions(
        forward = { R.anim.shrink_fade_out, R.anim.slide_in_child_bottom },
        backward = { R.anim.slide_out_child_bottom, R.anim.grow_fade_in }
)
public class QueueScreen extends Screen {

    @dagger.Module(
            addsTo = BaseSwitcherActivity.Module.class,
            injects = QueueView.class
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<QueueView> implements PausesAndResumes {

        final Context appContext;
        final MusicServiceConnection musicService;
        final EventBus bus;
        final PauseAndResumeRegistrar pauseAndResumeRegistrar;
        final ArtworkRequestManager requestor;

        @Inject
        public Presenter(@ForApplication Context context,
                         MusicServiceConnection musicService,
                         @Named("activity") EventBus bus,
                        PauseAndResumeRegistrar pauseAndResumeRegistrar,
                        ArtworkRequestManager requestor) {
            this.appContext = context;
            this.musicService = musicService;
            this.bus = bus;
            this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
            this.requestor = requestor;
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
            if (pauseAndResumeRegistrar.isRunning()) {
                subscribeBroadcasts();
            }
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
            if (getView() == null && pauseAndResumeRegistrar.isRunning()) {
                unsubscribeBroadcasts();
            }
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
            musicService.setQueuePosition(position);
        }

        public void removeQueueItem(long recentId) {
            musicService.removeTrack(recentId);
        }

        public void moveQueueItem(int from, int to) {
            musicService.moveQueueItem(from, to);
        }

        public boolean handleItemOverflowClick(OverflowAction action, RecentSong song) {
            switch (action) {
                case PLAY_NEXT:
                    removeQueueItem(song.recentId);
                    musicService.enqueueNext(new long[]{song.recentId});
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
                        NavUtils.openArtistProfile(appContext, MusicUtils.makeArtist(appContext, song.artistName));
                    } // else TODO
                    return true;
                case SET_RINGTONE:
                    //TODO
                    bus.post(new MakeToast(R.string.err_unimplemented));
//                    if (song.isLocal) {
//                        try {
//                            long id = Long.decode(song.identity);
//                            String s = MusicUtils.setRingtone(appContext, id);
//                        } catch (NumberFormatException ex) {
//                            //TODO
//                        }
//                    } // else unsupported
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
            playStateObservable = observeOnMain(BroadcastObservables.playStateChanged(appContext));
            metaChangedObservable = observeOnMain(BroadcastObservables.trackIdChanged(appContext));
            queueChangedObservable = observeOnMain(
                    BroadcastObservables.queueChanged(appContext)
                            .debounce(500, TimeUnit.MILLISECONDS)
                            .observeOn(Schedulers.io())
                            .map(new Func1<Intent, List<RecentSong>>() {
                                @Override
                                public List<RecentSong> call(Intent intent) {
                                    return getQueue();
                                }
                            })
            );
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
            queueChangedObserver = Observers.create(
                    new Action1<List<RecentSong>>() {
                        @Override
                        public void call(List<RecentSong> recentSongs) {
                            updateQueue(recentSongs);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            unsubscribeQueue();
                            subscribeQueue();
                        }
                    });
        }

        CompositeSubscription broadcastSubscriptions;
        Subscription queueChangedSubscription;

        void subscribeBroadcasts() {
            if (notSubscribed(broadcastSubscriptions)) {
                broadcastSubscriptions = new CompositeSubscription(
                        playStateObservable.subscribe(playStateObserver),
                        metaChangedObservable.subscribe(metaChangedObserver)
                );
            }
            subscribeQueue();
        }

        void unsubscribeBroadcasts() {
            if (isSubscribed(broadcastSubscriptions)) {
                broadcastSubscriptions.unsubscribe();
                broadcastSubscriptions = null;
            }
            unsubscribeQueue();
        }

        void subscribeQueue() {
            if (isSubscribed(queueChangedSubscription)) return;
            queueChangedSubscription = queueChangedObservable.subscribe(queueChangedObserver);
        }

        void unsubscribeQueue() {
            if (notSubscribed(queueChangedSubscription)) return;
            queueChangedSubscription.unsubscribe();
            queueChangedSubscription = null;
        }
    }


}
