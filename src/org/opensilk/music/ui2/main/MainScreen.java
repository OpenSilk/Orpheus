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
import android.os.Bundle;

import com.andrew.apollo.MusicPlaybackService;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.mortar.PausesAndResumes;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.R;
import org.opensilk.music.ui2.BaseSwitcherActivityBlueprint;
import org.opensilk.common.dagger.qualifier.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

import flow.Flow;
import flow.Layout;
import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.events.OnClickEvent;
import rx.android.observables.ViewObservable;
import rx.functions.Action1;
import rx.observers.Observers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;
import static org.opensilk.common.rx.RxUtils.notSubscribed;
import static org.opensilk.common.rx.RxUtils.observeOnMain;

/**
 * Created by drew on 10/5/14.
 */
@Layout(R.layout.main)
@WithModule(MainScreen.Module.class)
public class MainScreen extends Screen {

    @dagger.Module(
            addsTo = BaseSwitcherActivityBlueprint.Module.class,
            injects = {
                    MainView.class,
                    FooterView.class,
            }
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<MainView> implements PausesAndResumes {

        final Context appContext;
        final MusicServiceConnection musicService;
        final PauseAndResumeRegistrar pauseAndResumeRegistrar;

        @Inject
        protected Presenter(@ForApplication Context context,
                            MusicServiceConnection musicService,
                            PauseAndResumeRegistrar pauseAndResumeRegistrar) {
            Timber.v("new MainViewBlueprint.Presenter()");
            this.appContext = context;
            this.musicService = musicService;
            this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
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
                subscribeFabClicks();
                subscribeBroadcasts();
            }
        }

        @Override
        public void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
            if (getView() == null) {
                if (pauseAndResumeRegistrar.isRunning()) {
                    unsubscribeFabClicks();
                    unsubscribeBroadcasts();
                }
            }
        }

        @Override
        public void onResume() {
            Timber.v("onResume()");
            subscribeFabClicks();
            subscribeBroadcasts();
        }

        @Override
        public void onPause() {
            Timber.v("onPause()");
            unsubscribeFabClicks();
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

        CompositeSubscription fabClicksSubscription;

        void subscribeFabClicks() {
            if (isSubscribed(fabClicksSubscription)) return;
            MainView v = getView();
            if (v == null) return;
            fabClicksSubscription = new CompositeSubscription(
                    ViewObservable.clicks(v.fabPlay).subscribe(new Action1<OnClickEvent>() {
                        @Override
                        public void call(OnClickEvent onClickEvent) {
                            musicService.playOrPause();
                        }
                    }),
                    ViewObservable.clicks(v.fabNext).subscribe(new Action1<OnClickEvent>() {
                        @Override
                        public void call(OnClickEvent onClickEvent) {
                            musicService.next();
                        }
                    }),
                    ViewObservable.clicks(v.fabPrev).subscribe(new Action1<OnClickEvent>() {
                        @Override
                        public void call(OnClickEvent onClickEvent) {
                            musicService.prev();
                        }
                    }),
                    ViewObservable.clicks(v.fabShuffle).subscribe(new Action1<OnClickEvent>() {
                        @Override
                        public void call(OnClickEvent onClickEvent) {
                            musicService.cycleShuffleMode();
                        }
                    }),
                    ViewObservable.clicks(v.fabRepeat).subscribe(new Action1<OnClickEvent>() {
                        @Override
                        public void call(OnClickEvent onClickEvent) {
                            musicService.cycleRepeatMode();
                        }
                    })
            );
        }

        void unsubscribeFabClicks() {
            if (notSubscribed(fabClicksSubscription)) return;
            fabClicksSubscription.unsubscribe();
            fabClicksSubscription = null;
        }

        Observable<Boolean> playStateObservable;
        Observable<Integer> shuffleModeObservable;
        Observable<Integer> repeatModeObservable;

        void setupObservables() {
            playStateObservable = observeOnMain(BroadcastObservables.playStateChanged(appContext));
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

}
