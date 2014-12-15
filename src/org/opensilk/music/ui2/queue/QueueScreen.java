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

package org.opensilk.music.ui2.queue;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;

import com.andrew.apollo.menu.AddToPlaylistDialog;
import com.andrew.apollo.model.RecentSong;
import com.andrew.apollo.provider.MusicProviderUtil;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.mortar.PausesAndResumes;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.BaseSwitcherToolbarActivity;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.BroadcastObservables;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.OpenDialog;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.ui2.loader.NowPlayingCursor;
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import flow.Flow;
import flow.Layout;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.Observers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

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
            addsTo = BaseSwitcherToolbarActivity.Module.class,
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
        final ActionBarOwner actionBarOwner;
        final OverflowHandlers.RecentSongs overflowHandler;

        @Inject
        public Presenter(@ForApplication Context context,
                         MusicServiceConnection musicService,
                         @Named("activity") EventBus bus,
                        PauseAndResumeRegistrar pauseAndResumeRegistrar,
                        ArtworkRequestManager requestor,
                        ActionBarOwner actionBarOwner,
                        OverflowHandlers.RecentSongs overflowHandler) {
            this.appContext = context;
            this.musicService = musicService;
            this.bus = bus;
            this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
            this.requestor = requestor;
            this.actionBarOwner = actionBarOwner;
            this.overflowHandler = overflowHandler;
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
            setupActionBar();
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

        void setupActionBar() {
            actionBarOwner.setConfig(new ActionBarOwner.Config.Builder()
                    .setTitle(R.string.title_queue)
                    .setUpButtonEnabled(true)
                    .setMenuConfig(new ActionBarOwner.MenuConfig.Builder()
                            .withMenus(R.menu.popup_save_queue, R.menu.popup_clear_queue)
                            .setActionHandler(new Func1<Integer, Boolean>() {
                                @Override
                                public Boolean call(Integer integer) {
                                    switch (integer) {
                                        case R.id.popup_menu_save_queue:
                                            musicService.getQueue().subscribe(new Action1<long[]>() {
                                                @Override
                                                public void call(long[] queue) {
                                                    if (queue != null && queue.length > 0) {
                                                        long[] playlist = MusicProviderUtil.transformListToRealIds(appContext, queue);
                                                        if (playlist.length > 0) {
                                                            bus.post(new OpenDialog(AddToPlaylistDialog.newInstance(playlist)));
                                                            return;
                                                        }
                                                    }
                                                    //TODO toast
                                                }
                                            });
                                            return true;
                                        case R.id.popup_menu_clear_queue:
                                            musicService.clearQueue();
                                            if (getView() != null)
                                                AppFlow.get(getView().getContext()).goBack();
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                            }).build())
                    .build());
        }
    }

    public static void toggleQueue(Context context) {
        if (context == null) return;
        Flow flow = AppFlow.get(context);
        if (flow == null) return;
        if (flow.getBackstack().current().getScreen() instanceof QueueScreen) {
            flow.goBack();
        } else {
            flow.goTo(new QueueScreen());
        }
    }

    public static final Creator<QueueScreen> CREATOR = new Creator<QueueScreen>() {
        @Override
        public QueueScreen createFromParcel(Parcel source) {
            QueueScreen s = new QueueScreen();
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public QueueScreen[] newArray(int size) {
            return new QueueScreen[size];
        }
    };

}
