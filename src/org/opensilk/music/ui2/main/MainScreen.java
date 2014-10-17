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

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import com.andrew.apollo.MusicPlaybackService;

import org.opensilk.music.ui2.ActivityModule;
import org.opensilk.music.ui2.core.FlowOwner;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui3.theme.Themer;
import org.opensilk.music.widgets.FloatingActionButton;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Parcer;
import mortar.Blueprint;
import mortar.MortarScope;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.events.OnClickEvent;
import rx.android.observables.AndroidObservable;
import rx.android.observables.ViewObservable;
import rx.android.operators.OperatorBroadcastRegister;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.subscriptions.AndroidSubscriptions;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.Observers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.music.util.RxUtil.isSubscribed;
import static org.opensilk.music.util.RxUtil.notSubscribed;

/**
 * Created by drew on 10/5/14.
 */
public class MainScreen implements Blueprint {

    /**
     * Required for a race condition cause by Android when a new scope is created
     * before the old one is destroyed
     * <p/>
     * https://github.com/square/mortar/issues/87#issuecomment-43849264
     */
    final String scopename;

    public MainScreen(String scopename) {
        Timber.v("new MainScreen(%s)", scopename);
        this.scopename = scopename;
    }

    @Override
    public String getMortarScopeName() {
        return scopename;
    }

    @Override
    public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module(
            includes = ActivityModule.class,
            injects = {
                    MainView.class,
                    FooterView.class,
            },
            library = true
    )
    public static class Module {

        @Provides @Singleton
        public Flow provideFlow(Presenter presenter) {
            return presenter.getFlow();
        }

    }

    @Singleton
    public static class Presenter extends FlowOwner<Blueprint, MainView> {

        final MusicServiceConnection musicService;

        @Inject
        protected Presenter(Parcer<Object> parcer, MusicServiceConnection musicService) {
            super(parcer);
            Timber.v("new MainScreen.Presenter()");
            this.musicService = musicService;
        }

        @Override
        protected Blueprint getFirstScreen() {
            return new GalleryScreen();
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope(%s)", scope);
            super.onEnterScope(scope);
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
            setupObservables();
            setupObservers();
            subscribeFabClicks();
            subscribeBroadcasts();
        }

        @Override
        public void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
        }

        @Override
        public void dropView(MainView view) {
            super.dropView(view);
            unsubscribeFabClicks();
            unsubscribeBroadcasts();
        }

        void updateFabPlay(boolean playing) {
            MainView v = getView();
            if (v == null) return;
            v.fabPlay.setIcon(playing ? Themer.getPauseIcon(v.getContext(), true)
                    : Themer.getPlayIcon(v.getContext(), true));
        }

        void openQueue() {
            Flow flow = getFlow();
            if (flow.getBackstack().current().getScreen() instanceof QueueScreen) return;
            flow.goTo(new QueueScreen());
        }

        void closeQueue() {
            Flow flow = getFlow();
            if (flow.getBackstack().current().getScreen() instanceof QueueScreen) flow.goBack();
        }

        Subscription fabPlaySubscription;
        Subscription fabNextSubscription;
        Subscription fabPrevSubscription;

        void subscribeFabClicks() {
            MainView v = getView();
            if (v == null) return;
            fabPlaySubscription = ViewObservable.clicks(v.fabPlay).subscribe(new Action1<OnClickEvent>() {
                @Override
                public void call(OnClickEvent onClickEvent) {
                    musicService.playOrPause()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Action1<Boolean>() {
                                @Override
                                public void call(Boolean playing) {
                                    updateFabPlay(playing);
                                }
                            });
                }
            });
            fabNextSubscription = ViewObservable.clicks(v.fabNext).subscribe(new Action1<OnClickEvent>() {
                @Override
                public void call(OnClickEvent onClickEvent) {
                    musicService.next();
                }
            });
            fabPrevSubscription = ViewObservable.clicks(v.fabPrev).subscribe(new Action1<OnClickEvent>() {
                @Override
                public void call(OnClickEvent onClickEvent) {
                    musicService.prev();
                }
            });
        }

        void unsubscribeFabClicks() {
            if (!notSubscribed(fabPlaySubscription)) {
                fabPlaySubscription.unsubscribe();
                fabPlaySubscription = null;
            }
            if (!notSubscribed(fabNextSubscription)) {
                fabNextSubscription.unsubscribe();
                fabNextSubscription = null;
            }
            if (!notSubscribed(fabPrevSubscription)) {
                fabPrevSubscription.unsubscribe();
                fabPrevSubscription = null;
            }
        }

        Observable<Boolean> playStateObservable;
        Observable<Integer> repeatModeObservable;
        Observable<Intent> shuffleModeObservable;

        void setupObservables() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
            intentFilter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
            intentFilter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
            Scheduler scheduler = Schedulers.computation();
            // obr will call onNext on the main thread so we observeOn computation
            // so our chained operators will be called on computation instead of main.
            Observable<Intent> intentObservable = AndroidObservable.fromBroadcast(getView().getContext(), intentFilter).observeOn(scheduler);
            playStateObservable = intentObservable
                    // Filter for only PLAYSTATE_CHANGED actions
                    .filter(new Func1<Intent, Boolean>() {
                        // called on computation
                        @Override
                        public Boolean call(Intent intent) {
                            Timber.v("playstateSubscripion filter called on %s", Thread.currentThread().getName());
                            return intent.getAction() != null && intent.getAction().equals(MusicPlaybackService.PLAYSTATE_CHANGED);
                        }
                    })
                            // filter out repeats only taking most recent
                    .debounce(20, TimeUnit.MILLISECONDS, scheduler)
                            // flatMap the intent into a boolean by requesting the playstate
                            // XXX the intent contains the playstate as an extra but
                            //     it could be out of date
                    .flatMap(new Func1<Intent, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(Intent intent) {
                            Timber.v("playstateSubscription flatMap called on %s", Thread.currentThread().getName());
                            return musicService.isPlaying();
                        }
                    })
                            // observe final result on main thread
                    .observeOn(AndroidSchedulers.mainThread());
        }

        Observer<Boolean> playStateObserver;
        Observer<Integer> repeatModeObserver;
        Observer<Integer> shuffleModeObserver;

        void setupObservers() {
            playStateObserver = Observers.create(new Action1<Boolean>() {
                @Override
                public void call(Boolean playing) {
                    updateFabPlay(playing);
                }
            });
        }

        CompositeSubscription broadcastSubscriptions;

        void subscribeBroadcasts() {
            if (notSubscribed(broadcastSubscriptions)) {
                broadcastSubscriptions = new CompositeSubscription(
                        playStateObservable.subscribe(playStateObserver)
                );
            }
        }

        void unsubscribeBroadcasts() {
            if (notSubscribed(broadcastSubscriptions)) return;
            broadcastSubscriptions.unsubscribe();
            broadcastSubscriptions = null;
        }

        static boolean notSubscribed(Subscription subscription) {
            return subscription == null || subscription.isUnsubscribed();
        }

    }

}
