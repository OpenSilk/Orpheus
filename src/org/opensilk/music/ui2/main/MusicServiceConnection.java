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
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.andrew.apollo.IApolloService;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.provider.MusicProviderUtil;
import com.andrew.apollo.utils.MusicUtils;
import com.squareup.otto.Bus;

import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
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
    private final EventBus eventBus;
    // protected by synchronized methods
    private Token serviceToken;

    @Inject
    public MusicServiceConnection(@ForApplication Context context, @Named("activity") EventBus eventBus) {
        Timber.v("new MusicServiceConnection");
        this.context = new ContextWrapper(context);
        this.eventBus = eventBus;
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
    }

    public synchronized boolean isBound() {
        return (serviceToken != null);
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
        return serviceToken.subject.asObservable().first().observeOn(Schedulers.io());
    }

    /*
        void openFile(String path);
    void open(in long [] list, int position);
    void stop();
    void pause();
    void play();
    //void prev();
    //void next();
    //void enqueue(in long [] list, int action);
    void setQueuePosition(int index);
    //void setShuffleMode(int shufflemode);
    void setRepeatMode(int repeatmode);
    void moveQueueItem(int from, int to);
    void toggleFavorite();
    void refresh();
    boolean isFavorite();
    boolean isPlaying();
    //long [] getQueue();
    //long duration();
    //long position();
    long seek(long pos);
    //long getAudioId();
    long getAlbumId();
    //String getArtistName();
    //String getTrackName();
    String getAlbumName();
    String getAlbumArtistName();
    Uri getDataUri();
    Uri getArtworkUri();
    int getQueuePosition();
    //int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(long id);
    //int getRepeatMode();
    int getMediaMountedCount();
    int getAudioSessionId();
    boolean isRemotePlayback();
    //ArtInfo getCurrentArtInfo();
    boolean isFromSDCard();
     */

    public void prev() {
        final Intent previous = new Intent(context, MusicPlaybackService.class);
        previous.setAction(MusicPlaybackService.PREVIOUS_ACTION);
        context.startService(previous);
    }

    public void next() {
        getObservable().subscribe(new Action1<IApolloService>() {
            @Override
            public void call(IApolloService iApolloService) {
                try {
                    iApolloService.next();
                } catch (RemoteException e) {
                    //TODO
                }
            }
        });
    }

    public void enqueueNext(final Func0<Song[]> func) {
        enqueueSongs(func, MusicPlaybackService.NEXT);
    }

    public void enqueueEnd(final Func0<Song[]> func) {
        enqueueSongs(func, MusicPlaybackService.LAST);
    }

    public void enqueueSongs(final Func0<Song[]> func, final int where) {
        getObservable().subscribe(new SimpleObserver<IApolloService>() {
            @Override
            public void onNext(IApolloService iApolloService) {
                try {
                    Song[] songs = func.call();
                    long[] providerIds = addSongsToMusicProvider(songs);
                    iApolloService.enqueue(providerIds, where);
                    eventBus.post(new MakeToast(MakeToast.Type.PLURALS, R.plurals.NNNtrackstoqueue, providerIds.length));
                } catch (RemoteException e) {
                    eventBus.post(new MakeToast(MakeToast.Type.NORMAL, R.string.err_addtoqueue));
                }
            }
        });
    }

    public void playOrPause() {
        getObservable().subscribe(new Action1<IApolloService>() {
            @Override
            public void call(IApolloService iApolloService) {
                Timber.v("playOrPause %s", Thread.currentThread().getName());
                try {
                    if (iApolloService.isPlaying()) {
                        iApolloService.pause();
                    } else {
                        iApolloService.play();
                    }
                } catch (RemoteException e) {
                    //TODO
                }
            }
        });
    }

    public void cycleShuffleMode() {
        getObservable().subscribe(new Action1<IApolloService>() {
            @Override
            public void call(IApolloService iApolloService) {
                try {
                    int shufflemode = iApolloService.getShuffleMode();
                    switch (shufflemode) {
                        case MusicPlaybackService.SHUFFLE_NONE:
                            iApolloService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
                            if (iApolloService.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
                                iApolloService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                            }
                            break;
                        case MusicPlaybackService.SHUFFLE_NORMAL:
                        case MusicPlaybackService.SHUFFLE_AUTO:
                            iApolloService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                            break;
                    }
                } catch (RemoteException e) {
                    //TODO
                }
            }
        });
    }

    public void cycleRepeatMode() {
        getObservable().subscribe(new Action1<IApolloService>() {
            @Override
            public void call(IApolloService iApolloService) {
                try {
                    int repeatmode = iApolloService.getRepeatMode();
                    switch (repeatmode) {
                        case MusicPlaybackService.REPEAT_NONE:
                            iApolloService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                            break;
                        case MusicPlaybackService.REPEAT_ALL:
                            iApolloService.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
                            if (iApolloService.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE) {
                                iApolloService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                            }
                            break;
                        default:
                            iApolloService.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
                    }
                } catch (RemoteException e) {
                    //TODO
                }
            }
        });
    }

    public Observable<Boolean> isPlaying() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(IApolloService iApolloService) {
                Timber.v("isPlaying %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.isPlaying());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<long[]> getQueue() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<long[]>>() {
            @Override
            public Observable<long[]> call(IApolloService iApolloService) {
                try {
                    return Observable.just(iApolloService.getQueue());
                } catch (Exception e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<Long> getDuration() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<Long>>() {
            @Override
            public Observable<Long> call(IApolloService iApolloService) {
                Timber.v("getDuration %s", Thread.currentThread().getName());
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
                Timber.v("getPosition %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.position());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<Long> getAudioId() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<Long>>() {
            @Override
            public Observable<Long> call(IApolloService iApolloService) {
                Timber.v("getAudioId %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.getAudioId());
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
                Timber.v("getArtistName %s", Thread.currentThread().getName());
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
                Timber.v("getTrackName %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.getTrackName());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<Integer> getShuffleMode() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(IApolloService iApolloService) {
                Timber.v("getShuffleMode %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.getShuffleMode());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<Integer> getRepeatMode() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(IApolloService iApolloService) {
                Timber.v("getRepeatMode %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.getRepeatMode());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<ArtInfo> getCurrentArtInfo() {
        return getObservable().flatMap(new Func1<IApolloService, Observable<ArtInfo>>() {
            @Override
            public Observable<ArtInfo> call(IApolloService iApolloService) {
                Timber.v("getCurrentArtInfo %s", Thread.currentThread().getName());
                try {
                    return Observable.just(iApolloService.getCurrentArtInfo());
                } catch (RemoteException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    /*
     * Helpers
     */

    public long[] addSongsToMusicProvider(Song[] songs) {
        long[] providerids = new long[songs.length];
        for (int ii=0; ii<songs.length; ii++) {
            providerids[ii] = MusicProviderUtil.insertSong(context, songs[ii]);
        }
        return providerids;
    }

    public void playAllSongs(final Func0<Song[]> func, final int startPos, final boolean shuffle) {
        getObservable().subscribe(new SimpleObserver<IApolloService>() {
            @Override
            public void onNext(IApolloService iApolloService) {
                try {
                    Song[] songs = func.call();
                    long[] providerids = addSongsToMusicProvider(songs);
                    MusicUtils.playAll(iApolloService, providerids, startPos, shuffle);
                } catch (Exception e) {
                    eventBus.post(new MakeToast(MakeToast.Type.NORMAL, R.string.err_addtoqueue));
                }
            }
        });
    }

    /**
     * In the event of RemoteExceptions we cant use Observable.retry() since
     * it retrys the same observable, we have to create a new observable
     * that will rebind us to the service
     */
    public static <T> Observable<T> wrapForRetry(final Func0<Observable<T>> func) {
        return func.call()
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends T>>() {
                    @Override
                    public Observable<? extends T> call(Throwable throwable) {
                        if (throwable instanceof RemoteException) {
                            // We retry once after delay
                            return Observable.timer(2, TimeUnit.SECONDS)
                                    .flatMap(new Func1<Long, Observable<T>>() {
                                        @Override
                                        public Observable<T> call(Long aLong) {
                                            return func.call();
                                        }
                                    });
                        } else {
                            return Observable.error(throwable);
                        }
                    }
                });
    }

}
