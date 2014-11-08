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
import android.os.Bundle;

import org.opensilk.common.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.mortar.PausesAndResumes;
import org.opensilk.common.rx.HoldsSubscription;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.Observers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;
import static org.opensilk.common.rx.RxUtils.notSubscribed;
import static org.opensilk.common.rx.RxUtils.observeOnMain;

/**
 * Created by drew on 10/15/14.
 */
public class FooterBlueprint {

    @Singleton
    public static class Presenter extends ViewPresenter<FooterView> implements PausesAndResumes {

        final Context appContext;
        final PauseAndResumeRegistrar pauseAndResumeRegistrar;
        final MusicServiceConnection musicService;
        final ArtworkRequestManager artworkReqestor;

        CompositeSubscription broadcastSubscriptions;
        //These are separate since they query the service
        //and need error handling
        Subscription artworkSubscription;
        Subscription progressSubscription;

        Observable<Boolean> playStateObservable;
        Observable<String[]> metaObservable;
        Observable<ArtInfo> artworkObservable;
        Observable<Long> currentPositionObservable;
        Observable<Long> progressObservable;

        Observer<Boolean> playStateObserver;
        Observer<String[]> metaObserver;
        Observer<ArtInfo> artworkObserver;
        Observer<Long> progressObserver;

        PaletteObserver paletteObserver;

        @Inject
        public Presenter(@ForApplication Context context,
                         PauseAndResumeRegistrar pauseAndResumeRegistrar,
                         MusicServiceConnection musicService,
                         ArtworkRequestManager artworkReqestor) {
            Timber.v("new FooterViewBlueprint.Presenter");
            this.appContext = context;
            this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
            this.musicService = musicService;
            this.artworkReqestor = artworkReqestor;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope()");
            super.onEnterScope(scope);
            pauseAndResumeRegistrar.register(scope, this);
            setupObserables();
            setupObservers();
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad()");
            super.onLoad(savedInstanceState);
            //just for safety we should always receive a call to onResume()
            subscribeBroadcasts();
            //playstate will kick off progress subscription
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
            if (getView() == null) {
                //just for safety we should always receive a call to onPause()
                unsubscribeBroadcasts();
                unsubscribeProgress();
            }
        }

        @Override
        public void onResume() {
            Timber.v("onResume()");
            if (getView() == null) return;
            subscribeBroadcasts();
            //playstate will kick off progress subscription
        }

        @Override
        public void onPause() {
            Timber.v("onPause");
            unsubscribeBroadcasts();
            unsubscribeProgress();
        }

        void setTrackName(String s) {
            FooterView v = getView();
            if (v == null) return;
            v.trackTitle.setText(s);
        }

        void setArtistName(String s) {
            FooterView v = getView();
            if (v == null) return;
            v.artistName.setText(s);
        }

        void setProgress(int progress) {
            FooterView v = getView();
            if (v == null) return;
            v.progressBar.setProgress(progress);
        }

        void updateArtwork(ArtInfo artInfo) {
            FooterView v = getView();
            if (v == null) return;
            HoldsSubscription sh = (HoldsSubscription) v.artworkThumbnail;
            AnimatedImageView av = (AnimatedImageView) v.artworkThumbnail;
            sh.addSubscription(artworkReqestor.newAlbumRequest(av,
                    /*paletteObserver*/ null, artInfo, ArtworkType.THUMBNAIL));
        }

        void updateBackground(PaletteResponse paletteResponse) {
            FooterView v = getView();
            if (v == null) return;
            v.updateBackground(paletteResponse);
        }

        void setupObserables() {
            playStateObservable = observeOnMain(BroadcastObservables.playStateChanged(appContext));
            metaObservable = observeOnMain(Observable.zip(
                    BroadcastObservables.trackChanged(appContext),
                    BroadcastObservables.artistChanged(appContext),
                    new Func2<String, String, String[]>() {
                        @Override
                        public String[] call(String trackName, String artistName) {
                            Timber.v("metaObservable(zip) called on %s", Thread.currentThread().getName());
                            return new String[] {trackName,artistName};
                        }
                    }
            ));
            artworkObservable = observeOnMain(BroadcastObservables.metaChanged(appContext)
                    .flatMap(new Func1<Intent, Observable<ArtInfo>>() {
                        @Override
                        public Observable<ArtInfo> call(Intent intent) {
                            return musicService.getCurrentArtInfo();
                        }
                    })
            );
            currentPositionObservable = Observable.zip(
                    musicService.getPosition(),
                    musicService.getDuration(),
                    new Func2<Long, Long, Long>() {
                        @Override
                        public Long call(Long position, Long duration) {
                            Timber.v("currentPositionObservable(%d, %d) %s", position, duration, Thread.currentThread().getName());
                            if (position > 0 && duration > 0) {
                                return (1000 * position / duration);
                            } else {
                                return (long) 1000;
                            }
                        }
                    }
            );
            progressObservable = observeOnMain(
                    // construct an Observable than repeats every .5s,
                    Observable.interval(500, TimeUnit.MILLISECONDS)
                    // we then fetch the progress as a percentage,
                    .flatMap(new Func1<Long, Observable<Long>>() {
                        @Override
                        public Observable<Long> call(Long aLong) {
//                            Timber.v("progressObservable(flatMap) %s", Thread.currentThread().getName());
                            return currentPositionObservable;
                        }
                    })
            );
        }

        void setupObservers() {
            playStateObserver = Observers.create(
                    new Action1<Boolean>() {
                        @Override
                        public void call(Boolean playing) {
                            Timber.v("playStateObserver(result) %s", Thread.currentThread().getName());
                            if (playing) {
                                subscribeProgress();
                            } else {
                                unsubscribeProgress();
                                // update the current position
                                currentPositionObservable.observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(progressObserver);
                            }
                        }
                    }
            );
            metaObserver = Observers.create(
                    new Action1<String[]>() {
                        @Override
                        public void call(String[] strings) {
                            Timber.v("metaObserver(result) %s", Thread.currentThread().getName());
                            if (strings.length == 2) {
                                setTrackName(strings[0]);
                                setArtistName(strings[1]);
                            }
                        }
                    }
            );
            artworkObserver = Observers.create(
                    new Action1<ArtInfo>() {
                        @Override
                        public void call(ArtInfo artInfo) {
                            updateArtwork(artInfo);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            //TODO resubscribe
                        }
                    }
            );
            progressObserver = Observers.create(
                    new Action1<Long>() {
                        @Override
                        public void call(Long progress) {
//                    Timber.d("progressObserver(result) %s", Thread.currentThread().getName());
                            setProgress(progress.intValue());
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            unsubscribeProgress();
                            subscribeProgress();
                        }
                    }
            );
            paletteObserver = new PaletteObserver() {
                @Override
                public void onNext(PaletteResponse paletteResponse) {
                    updateBackground(paletteResponse);
                }
            };
        }

        void subscribeBroadcasts() {
            if (notSubscribed(broadcastSubscriptions)) {
                broadcastSubscriptions = new CompositeSubscription(
                        playStateObservable.subscribe(playStateObserver),
                        metaObservable.subscribe(metaObserver)
                );
            }
            subscribeArtwork();
        }

        void unsubscribeBroadcasts() {
            if (isSubscribed(broadcastSubscriptions)) {
                broadcastSubscriptions.unsubscribe();
                broadcastSubscriptions = null;
            }
            unsubscribeArtwork();
        }

        void subscribeArtwork() {
            if (notSubscribed(artworkSubscription)) {
                artworkSubscription = artworkObservable.subscribe(artworkObserver);
            }
        }

        void unsubscribeArtwork() {
            if (isSubscribed(artworkSubscription)) {
                artworkSubscription.unsubscribe();
                artworkSubscription = null;
            }
        }

        void subscribeProgress() {
            if (notSubscribed(progressSubscription)) {
                progressSubscription = progressObservable.subscribe(progressObserver);
            }
        }

        void unsubscribeProgress() {
            if (isSubscribed(progressSubscription)) {
                progressSubscription.unsubscribe();
                progressSubscription = null;
            }
        }

    }
}
