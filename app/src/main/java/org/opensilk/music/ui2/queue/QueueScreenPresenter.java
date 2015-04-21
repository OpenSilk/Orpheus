/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui2.queue;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import com.andrew.apollo.menu.AddToPlaylistDialog;
import com.andrew.apollo.model.RecentSong;
import com.andrew.apollo.provider.MusicProviderUtil;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.mortar.PausesAndResumes;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.BroadcastObservables;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.OpenDialog;
import org.opensilk.music.ui2.loader.NowPlayingCursor;
import org.opensilk.music.util.CursorHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
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
 * Created by drew on 4/20/15.
 */
@Singleton
public class QueueScreenPresenter extends ViewPresenter<QueueScreenView> implements PausesAndResumes {

    final Context appContext;
    final MusicServiceConnection musicService;
    final EventBus bus;
    final PauseAndResumeRegistrar pauseAndResumeRegistrar;
    final ArtworkRequestManager requestor;
    final ActionBarOwner actionBarOwner;
    final OverflowHandlers.RecentSongs overflowHandler;

    @Inject
    public QueueScreenPresenter(@ForApplication Context context,
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

    //@DebugLog
    List<RecentSong> getQueue() {
        final long[] queue = musicService.getQueue().toBlocking().first();
        Cursor c = new NowPlayingCursor(appContext, queue);
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
        QueueScreenView v = getView();
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
        playStateObservable = BroadcastObservables.playStateChanged(appContext);
        metaChangedObservable = BroadcastObservables.trackIdChanged(appContext);
        queueChangedObservable = observeOnMain(
                BroadcastObservables.queueChanged(appContext)
                        .debounce(150, TimeUnit.MILLISECONDS)
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
                QueueScreenView v = getView();
                if (v == null) return;
                v.onPlaystateChanged(playing);
            }
        });
        metaChangedObserver = Observers.create(new Action1<Long>() {
            @Override
            public void call(Long audioId) {
                QueueScreenView v = getView();
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
                                                    } else {
                                                        bus.post(new MakeToast(R.string.err_unsupported_for_library));
                                                    }
                                                } else {
                                                    bus.post(new MakeToast(R.string.err_generic));
                                                }
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
