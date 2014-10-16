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
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.ui2.core.lifecycle.PauseAndResumeRegistrar;
import org.opensilk.music.ui2.core.lifecycle.PausesAndResumes;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.operators.OperatorBroadcastRegister;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.andrew.apollo.utils.MusicUtils.sService;

/**
 * Created by drew on 10/15/14.
 */
public class FooterScreen {

    @Singleton
    public static class Presenter extends ViewPresenter<FooterView> implements PausesAndResumes {

        final PauseAndResumeRegistrar pauseAndResumeRegistrar;
        final MusicServiceConnection musicService;

        @Inject
        public Presenter(PauseAndResumeRegistrar pauseAndResumeRegistrar,
                         MusicServiceConnection musicService) {
            this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
            this.musicService = musicService;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope()");
            super.onEnterScope(scope);
            pauseAndResumeRegistrar.register(scope, this);
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad()");
            super.onLoad(savedInstanceState);
            musicService.getTrackName().subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    setTrackName(s);
                }
            });
            musicService.getArtistName().subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    setArtistName(s);
                }
            });
            setUpProgress();
        }

        @Override
        protected void onSave(Bundle outState) {
            Timber.v("onSave()");
            super.onSave(outState);
        }

        @Override
        public void onResume() {
            Timber.v("onResume()");
            setUpProgress();
        }

        @Override
        public void onPause() {
            Timber.v("onPause");
            if (progressSubscription != null) progressSubscription.unsubscribe();
        }

        Subscription playStateSubscription;
        Subscription metaSubscription;
        Subscription progressSubscription;

        @Override
        public void dropView(FooterView view) {
            Timber.v("dropView()");
            super.dropView(view);
            if (playStateSubscription != null) metaSubscription.unsubscribe();
            if (metaSubscription != null) metaSubscription.unsubscribe();
            if (progressSubscription != null) progressSubscription.unsubscribe();
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

        void initBroadcastObserver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
            intentFilter.addAction(MusicPlaybackService.META_CHANGED);
            OperatorBroadcastRegister obr = new OperatorBroadcastRegister(getView().getContext(), intentFilter, null, null);
            Observable<Intent> o = Observable.create(obr);
            if (playStateSubscription != null && !playStateSubscription.isUnsubscribed()) playStateSubscription.unsubscribe();
            playStateSubscription = o.filter(new Func1<Intent, Boolean>() {
                @Override
                public Boolean call(Intent intent) {
                    return intent.getAction() != null && intent.getAction().equals(MusicPlaybackService.PLAYSTATE_CHANGED);
                }
            }).map(new Func1<Intent, Void>() {
                @Override
                public Void call(Intent intent) {

                    return null;
                }
            }).subscribe();
        }

        void setUpProgress() {
            if (progressSubscription != null && !progressSubscription.isUnsubscribed()) progressSubscription.unsubscribe();
            progressSubscription = Observable.interval(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .flatMap(new Func1<Long, Observable<Long>>() {
                @Override
                public Observable<Long> call(Long aLong) {
                    Timber.d("FlatMap Called on %s", Thread.currentThread().getName());
                    return Observable.zip(musicService.getPosition(), musicService.getDuration(), new Func2<Long, Long, Long>() {
                        @Override
                        public Long call(Long position, Long duration) {
                            Timber.d("Zip called on %s", Thread.currentThread().getName());
                            if (position > 0 && duration > 0) {
                                return (1000 * position / duration);
                            } else {
                                return (long) 1000;
                            }
                        }
                    });
                }
            }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                @Override
                public void call(Long progress) {
                    Timber.d("Result returned on %s", Thread.currentThread().getName());
                    setProgress(progress.intValue());
                }
            });
        }

    }
}
