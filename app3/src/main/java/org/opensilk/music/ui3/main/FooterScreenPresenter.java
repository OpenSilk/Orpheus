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

package org.opensilk.music.ui3.main;

import android.content.Context;
import android.os.Bundle;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.ui.mortar.PausesAndResumes;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.playback.control.PlaybackController;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
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

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static org.opensilk.common.core.rx.RxUtils.notSubscribed;
import static org.opensilk.common.core.rx.RxUtils.observeOnMain;

/**
 * Created by drew on 4/20/15.
 */
@ScreenScope
public class FooterScreenPresenter extends ViewPresenter<FooterScreenView>
        implements PausesAndResumes {

    final Context appContext;
    final PlaybackController playbackController;
    final ArtworkRequestManager artworkReqestor;
    final PauseAndResumeRegistrar pauseAndResumeRegistrar;
    final AppPreferences settings;

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
    public FooterScreenPresenter(
            @ForApplication Context context,
            PlaybackController playbackController,
            ArtworkRequestManager artworkReqestor,
            PauseAndResumeRegistrar pauseAndResumeRegistrar,
            AppPreferences settings
    ) {
        this.appContext = context;
        this.playbackController = playbackController;
        this.artworkReqestor = artworkReqestor;
        this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
        this.settings = settings;
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
        if (pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onResume()");
            subscribeBroadcasts();
            //playstate will kick off progress subscription
        }
    }

    @Override
    protected void onSave(Bundle outState) {
        Timber.v("onSave()");
        super.onSave(outState);
        if (getView() == null
                && pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onPause()");
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
        if (hasView()) {
            getView().trackTitle.setText(s);
        }
    }

    void setArtistName(String s) {
        if (hasView()) {
            getView().artistName.setText(s);
        }
    }

    void setProgress(int progress) {
        if (hasView()) {
            getView().progressBar.setProgress(progress);
        }
    }

    void updateArtwork(ArtInfo artInfo) {
        if (hasView()) {
            AnimatedImageView av = getView().artworkThumbnail;
            av.addSubscription(artworkReqestor.newRequest(av,
                /*paletteObserver*/ null, artInfo, ArtworkType.THUMBNAIL));
        }
    }

    void updateBackground(PaletteResponse paletteResponse) {
        if (hasView()) {
            getView().updateBackground(paletteResponse);
        }
    }

    void onClick(Context context) {
        handleClick(settings.getString(AppPreferences.FOOTER_CLICK, AppPreferences.ACTION_OPEN_QUEUE), context);
    }

    boolean onLongClick(Context context) {
        return handleClick(settings.getString(AppPreferences.FOOTER_LONG_CLICK, AppPreferences.ACTION_NONE), context);
    }

    void onThumbClick(Context context) {
        handleClick(settings.getString(AppPreferences.FOOTER_THUMB_CLICK, AppPreferences.ACTION_OPEN_NOW_PLAYING), context);
    }

    boolean onThumbLongClick(Context context) {
        return handleClick(settings.getString(AppPreferences.FOOTER_THUMB_LONG_CLICK, AppPreferences.ACTION_NONE), context);
    }

    @DebugLog
    boolean handleClick(String action, Context context) {
        switch (action) {
            case AppPreferences.ACTION_OPEN_QUEUE:
                //QueueScreen.toggleQueue(context);
                return true;
            case AppPreferences.ACTION_OPEN_NOW_PLAYING:
                //NowPlayingScreen.toggleNowPlaying(context);
                return true;
            case AppPreferences.ACTION_NONE:
            default:
                return false;
        }
    }

    void setupObserables() {
        /*
        playStateObservable = BroadcastObservables.playStateChanged(appContext);
        metaObservable = observeOnMain(Observable.zip(
                BroadcastObservables.trackChanged(appContext),
                BroadcastObservables.artistChanged(appContext),
                new Func2<String, String, String[]>() {
                    @Override
                    public String[] call(String trackName, String artistName) {
                        Timber.v("metaObservable(zip) called on %s", Thread.currentThread().getName());
                        return new String[]{trackName, artistName};
                    }
                }
        ));
        artworkObservable = observeOnMain(BroadcastObservables.artworkChanged(appContext, musicService));
        currentPositionObservable = Observable.zip(
                musicService.getPosition(),
                musicService.getDuration(),
                new Func2<Long, Long, Long>() {
                    @Override
                    public Long call(Long position, Long duration) {
//                            Timber.v("currentPositionObservable(%d, %d) %s", position, duration, Thread.currentThread().getName());
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
        */
    }

    void setupObservers() {
        /*
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
                            currentPositionObservable
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Action1<Long>() {
                                        @Override
                                        public void call(Long progress) {
                                            setProgress(progress.intValue());
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable throwable) {
                                            //ignore
                                        }
                                    });
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
                        unsubscribeArtwork();
                        subscribeArtwork();
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
                        Timber.i("progressObserver(error) resubscribing");
                        unsubscribeProgress();
                        Observable.timer(2, TimeUnit.SECONDS).subscribe(new Action1<Long>() {
                            @Override
                            public void call(Long aLong) {
                                subscribeProgress();
                            }
                        });
                    }
                }
        );
        paletteObserver = new PaletteObserver() {
            @Override
            public void onNext(PaletteResponse paletteResponse) {
                updateBackground(paletteResponse);
            }
        };
        */
    }

    void subscribeBroadcasts() {
        /*
        if (notSubscribed(broadcastSubscriptions)) {
            broadcastSubscriptions = new CompositeSubscription(
                    playStateObservable.subscribe(playStateObserver),
                    metaObservable.subscribe(metaObserver)
            );
        }
        subscribeArtwork();
        */
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            broadcastSubscriptions.unsubscribe();
            broadcastSubscriptions = null;
        }
        unsubscribeArtwork();
    }

    void subscribeArtwork() {
        /*
        if (notSubscribed(artworkSubscription)) {
            artworkSubscription = artworkObservable.subscribe(artworkObserver);
        }
        */
    }

    void unsubscribeArtwork() {
        if (isSubscribed(artworkSubscription)) {
            artworkSubscription.unsubscribe();
            artworkSubscription = null;
        }
    }

    void subscribeProgress() {
        /*
        if (notSubscribed(progressSubscription)) {
            progressSubscription = progressObservable.subscribe(progressObserver);
        }
        */
    }

    void unsubscribeProgress() {
        if (isSubscribed(progressSubscription)) {
            progressSubscription.unsubscribe();
            progressSubscription = null;
        }
    }

}
