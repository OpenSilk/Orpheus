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

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.andrew.apollo.IApolloService;
import com.andrew.apollo.MusicPlaybackService;

import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.operators.OperatorBroadcastRegister;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import rx.subjects.ReplaySubject;
import timber.log.Timber;

/**
 * Created by drew on 10/15/14.
 */
@Singleton
public class MusicServiceConnection {

    private class Token implements ServiceConnection {
        final AsyncSubject<IApolloService> subject;

        private Token(AsyncSubject<IApolloService> subject) {
            this.subject = subject;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IApolloService connection = IApolloService.Stub.asInterface(service);
            subject.onNext(connection);
            subject.onCompleted();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unbind();
        }
    }

    private final Context context;
    // protected by synchronized methods
    private Token serviceToken;

    private Observable<Intent> broadcastObservable;

    @Inject
    public MusicServiceConnection(@ForApplication Context context) {
        this.context = new ContextWrapper(context);
    }

    public synchronized void bind() {
        if (serviceToken == null) {
            Timber.d("binding MusicService");
            AsyncSubject<IApolloService> subject = AsyncSubject.create();
            serviceToken = new Token(subject);
            context.startService(new Intent(context, MusicPlaybackService.class));
            context.bindService(new Intent(context, MusicPlaybackService.class), serviceToken, 0);
        }
    }

    public synchronized void unbind() {
        if (serviceToken == null) return;
        Timber.d("unbinding MusicService");
        context.unbindService(serviceToken);
        serviceToken = null;
        if (broadcastObservable != null) broadcastObservable = null;
    }

    public synchronized boolean isBound() {
        return (serviceToken != null);
    }

    public synchronized Observable<Intent> getBroadcastObservable() {
        if (broadcastObservable != null) return broadcastObservable;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        intentFilter.addAction(MusicPlaybackService.META_CHANGED);
        intentFilter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        intentFilter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        intentFilter.addAction(MusicPlaybackService.REFRESH);
        intentFilter.addAction(MusicPlaybackService.QUEUE_CHANGED);
        OperatorBroadcastRegister obr = new OperatorBroadcastRegister(context, intentFilter, null, null);
        ReplaySubject<Intent> subject = ReplaySubject.createWithTime(2, TimeUnit.MINUTES, AndroidSchedulers.mainThread());
        Observable<Intent> obs = Observable.create(obr);
        obs.subscribe(subject);
        broadcastObservable = subject.asObservable();
        return broadcastObservable;
    }

    synchronized void ensureConnection() {
        if (serviceToken == null) bind();
    }

    void onError(Exception e) {
        Timber.e(e, "MusicServiceConnection");
        unbind();
    }

    Observable<IApolloService> getObservable() {
        ensureConnection();
        // NOTE: onServiceConnected() is called from main thread
        // hence the onNext() in the subject is called from main thread
        // for this reason we 'observe' the onNextCall on an IO thread.
        // so when the functions will receive the Func1.call() in the flatMap
        // on the IO thread not the main thread.
        return serviceToken.subject.asObservable().observeOn(Schedulers.io());
    }

    /*
        void openFile(String path);
    void open(in long [] list, int position);
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    void enqueue(in long [] list, int action);
    void setQueuePosition(int index);
    void setShuffleMode(int shufflemode);
    void setRepeatMode(int repeatmode);
    void moveQueueItem(int from, int to);
    void toggleFavorite();
    void refresh();
    boolean isFavorite();
    boolean isPlaying();
    long [] getQueue();
    //long duration();
    //long position();
    long seek(long pos);
    long getAudioId();
    long getAlbumId();
    //String getArtistName();
    //String getTrackName();
    String getAlbumName();
    String getAlbumArtistName();
    Uri getDataUri();
    Uri getArtworkUri();
    int getQueuePosition();
    int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(long id);
    int getRepeatMode();
    int getMediaMountedCount();
    int getAudioSessionId();
    boolean isRemotePlayback();
    ArtInfo getCurrentArtInfo();
    boolean isFromSDCard();
     */

    public Observable<Long> getDuration() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<Long>>() {
            @Override
            public Observable<Long> call(IApolloService iApolloService) {
                Timber.v("getDuration called on %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.duration());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<Long> getPosition() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<Long>>() {
            @Override
            public Observable<Long> call(IApolloService iApolloService) {
                Timber.v("getPosition called on %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.position());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<Boolean> isPlaying() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(IApolloService iApolloService) {
                Timber.v("isPlaying called on %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.isPlaying());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<Boolean> playOrPause() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(IApolloService iApolloService) {
                Timber.v("playOrPause called on %s", Thread.currentThread().getName());
                try {
                    if (iApolloService.isPlaying()) {
                        iApolloService.pause();
                        return Observable.just(false);
                    } else {
                        iApolloService.play();
                        return Observable.just(true);
                    }
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<String> getArtistName() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<String>>() {
            @Override
            public Observable<String> call(IApolloService iApolloService) {
                Timber.v("getArtistName called on %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.getArtistName());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<String> getTrackName() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<String>>() {
            @Override
            public Observable<String> call(IApolloService iApolloService) {
                Timber.v("getTrackName called on %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.getTrackName());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

}
