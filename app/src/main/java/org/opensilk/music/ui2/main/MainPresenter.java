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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.os.Bundle;

import com.andrew.apollo.MusicPlaybackService;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.common.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.mortar.PausesAndResumes;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.ui2.core.BroadcastObservables;
import org.opensilk.music.ui2.nowplaying.NowPlayingScreen;
import org.opensilk.music.ui2.queue.QueueScreen;

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
import rx.observers.Observers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;
import static org.opensilk.common.rx.RxUtils.notSubscribed;
import static org.opensilk.common.rx.RxUtils.observeOnMain;

/**
 * Created by drew on 4/20/15.
 */
@Singleton
public class MainPresenter extends ViewPresenter<MainView> implements PausesAndResumes {

    final Context appContext;
    final MusicServiceConnection musicService;
    final PauseAndResumeRegistrar pauseAndResumeRegistrar;
    final EventBus eventBus;
    final AppPreferences settings;

    @Inject
    protected MainPresenter(@ForApplication Context context,
                            MusicServiceConnection musicService,
                            PauseAndResumeRegistrar pauseAndResumeRegistrar,
                            @Named("activity") EventBus eventBus,
                            AppPreferences settings) {
        Timber.v("new MainViewBlueprint.Presenter()");
        this.appContext = context;
        this.musicService = musicService;
        this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
        this.eventBus = eventBus;
        this.settings = settings;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        Timber.v("onEnterScope(%s)", scope);
        super.onEnterScope(scope);
        pauseAndResumeRegistrar.register(scope, this);
        setupObservables();
        setupObservers();
    }

    @Override
    protected void onExitScope() {
        Timber.v("onExitScope()");
        super.onExitScope();
    }

    @Override
    public void onLoad(Bundle savedInstanceState) {
        Timber.v("onLoad(%s)", savedInstanceState);
        super.onLoad(savedInstanceState);
        if (pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onResume()");
            subscribeBroadcasts();
        }
    }

    @Override
    public void onSave(Bundle outState) {
        Timber.v("onSave(%s)", outState);
        super.onSave(outState);
        if (getView() == null
                && pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onPause()");
            unsubscribeBroadcasts();
        }
    }

    @Override
    public void onResume() {
        Timber.v("onResume()");
        subscribeBroadcasts();
    }

    @Override
    public void onPause() {
        Timber.v("onPause()");
        unsubscribeBroadcasts();
    }

    void updateFabPlay(boolean playing) {
        MainView v = getView();
        if (v == null) return;
        v.fabPlay.setChecked(playing);
    }

    void updateFabShuffle(int shufflemode) {
        MainView v = getView();
        if (v == null) return;
        v.fabShuffle.setImageLevel(shufflemode);
    }

    void updateFabRepeat(int repeatmode) {
        MainView v = getView();
        if (v == null) return;
        switch (repeatmode) {
            case MusicPlaybackService.REPEAT_NONE:
                v.fabRepeat.setImageLevel(0);
                break;
            case MusicPlaybackService.REPEAT_CURRENT:
                v.fabRepeat.setImageLevel(1);
                break;
            case MusicPlaybackService.REPEAT_ALL:
                v.fabRepeat.setImageLevel(2);
                break;
        }
    }

    void handlePrimaryAction(String event, String def) {
        String pref = settings.getString(event, def);
        switch (pref) {
            case AppPreferences.ACTION_PLAYPAUSE:
                musicService.playOrPause();
                break;
            case AppPreferences.ACTION_QUICK_CONTROLS:
                if (getView() != null) {
                    getView().toggleSecondaryFabs();
                }
                break;
            case AppPreferences.ACTION_OPEN_NOW_PLAYING:
                if (getView() != null) {
                    NowPlayingScreen.toggleNowPlaying(getView().getContext());
                }
                break;
            case AppPreferences.ACTION_OPEN_QUEUE:
                if (getView() != null) {
                    QueueScreen.toggleQueue(getView().getContext());
                }
                break;
            case AppPreferences.ACTION_NONE:
            default:
                break;
        }
    }

    void handlePrimaryClick() {
        handlePrimaryAction(AppPreferences.FAB_CLICK, AppPreferences.ACTION_PLAYPAUSE);
    }

    void handlePrimaryDoubleClick() {
        handlePrimaryAction(AppPreferences.FAB_DOUBLE_CLICK, AppPreferences.ACTION_QUICK_CONTROLS);
    }

    void handlePrimaryLongClick() {
        handlePrimaryAction(AppPreferences.FAB_LONG_CLICK, AppPreferences.ACTION_QUICK_CONTROLS);
    }

    void handlePrimaryFling() {
        handlePrimaryAction(AppPreferences.FAB_FLING, AppPreferences.ACTION_OPEN_NOW_PLAYING);
    }

    Observable<Boolean> playStateObservable;
    Observable<Integer> shuffleModeObservable;
    Observable<Integer> repeatModeObservable;

    void setupObservables() {
        playStateObservable = BroadcastObservables.playStateChanged(appContext);
        shuffleModeObservable = observeOnMain(BroadcastObservables.shuffleModeChanged(appContext, musicService));
        repeatModeObservable = observeOnMain(BroadcastObservables.repeatModeChanged(appContext, musicService));
    }

    Observer<Boolean> playStateObserver;
    Observer<Integer> shuffleModeObserver;
    Observer<Integer> repeatModeObserver;

    void setupObservers() {
        playStateObserver = Observers.create(
                new Action1<Boolean>() {
                    @Override
                    public void call(Boolean playing) {
                        updateFabPlay(playing);
                    }
                }
        );
        shuffleModeObserver = Observers.create(
                new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        updateFabShuffle(integer);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        unsubscribeShuffle();
                        subscribeShuffle();
                    }
                }
        );
        repeatModeObserver = Observers.create(
                new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        updateFabRepeat(integer);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        unsubscribeRepeat();
                        subscribeRepeat();
                    }
                }
        );
    }

    CompositeSubscription broadcastSubscriptions;
    Subscription shuffleModeSubscription;
    Subscription repeatModeSubscription;

    void subscribeBroadcasts() {
        if (notSubscribed(broadcastSubscriptions)) {
            broadcastSubscriptions = new CompositeSubscription(
                    playStateObservable.subscribe(playStateObserver)
            );
        }
        subscribeShuffle();
        subscribeRepeat();
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            broadcastSubscriptions.unsubscribe();
            broadcastSubscriptions = null;
        }
        unsubscribeShuffle();
        unsubscribeRepeat();
    }

    void subscribeShuffle() {
        if (isSubscribed(shuffleModeSubscription)) return;
        shuffleModeSubscription = shuffleModeObservable.subscribe(shuffleModeObserver);
    }

    void unsubscribeShuffle() {
        if (notSubscribed(shuffleModeSubscription)) return;
        shuffleModeSubscription.unsubscribe();
        shuffleModeSubscription = null;
    }

    void subscribeRepeat() {
        if (isSubscribed(repeatModeSubscription)) return;
        repeatModeSubscription = repeatModeObservable.subscribe(repeatModeObserver);
    }

    void unsubscribeRepeat() {
        if (notSubscribed(repeatModeSubscription)) return;
        repeatModeSubscription.unsubscribe();
        repeatModeSubscription = null;
    }

}
