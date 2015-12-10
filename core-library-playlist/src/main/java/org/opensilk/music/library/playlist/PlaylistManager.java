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

package org.opensilk.music.library.playlist;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import org.opensilk.music.library.client.LibraryClient;
import org.opensilk.music.library.playlist.provider.PlaylistMethods;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import static org.opensilk.music.library.playlist.PlaylistExtras.RESULT_ERROR;
import static org.opensilk.music.library.playlist.PlaylistExtras.RESULT_SUCCESS;

/**
 * Created by drew on 12/10/15.
 */
public class PlaylistManager {

    final Handler mCallbackHandler;
    final Context mContext;
    final String mAuthority;

    public PlaylistManager(Context context, String authority) {
        mCallbackHandler = new Handler(Looper.getMainLooper());
        mContext = context;
        mAuthority = authority;
    }

    public Observable<Uri> create(final String name) {
        return wrapObservable(new Func1<LibraryClient, Observable<? extends Uri>>() {
            @Override
            public Observable<? extends Uri> call(final LibraryClient libraryClient) {
                return Observable.create(new Observable.OnSubscribe<Uri>() {
                    @Override
                    public void call(final Subscriber<? super Uri> subscriber) {
                        final ResultReceiver resultReceiver = new ResultReceiver(mCallbackHandler) {
                            final AtomicBoolean resultReceived = new AtomicBoolean(false);

                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                if (!resultReceived.compareAndSet(false, true)) {
                                    return;
                                }
                                switch (resultCode) {
                                    case RESULT_SUCCESS:
                                        subscriber.onNext(PlaylistExtras.getUri(resultData));
                                        subscriber.onCompleted();
                                        break;
                                    case RESULT_ERROR:
                                        subscriber.onError(new Exception(PlaylistExtras.getError(resultData)));
                                        break;
                                }
                            }
                        };
                        PlaylistExtras.Builder extras = PlaylistExtras.b()
                                .putName(name)
                                .putResultReceiver(resultReceiver)
                                ;
                        Bundle reply = libraryClient.makeCall(PlaylistMethods.CREATE, extras.get());
                        if (!PlaylistExtras.getOk(reply)) {
                            subscriber.onError(new IllegalArgumentException(PlaylistExtras.getError(reply)));
                        }
                    }
                });
            }
        });
    }

    public Observable<Integer> addTo(final Uri playlist, final List<Uri> tracks) {
        return wrapObservable(new Func1<LibraryClient, Observable<? extends Integer>>() {
            @Override
            public Observable<? extends Integer> call(final LibraryClient libraryClient) {
                return Observable.create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(final Subscriber<? super Integer> subscriber) {
                        final ResultReceiver resultReceiver = new ResultReceiver(mCallbackHandler) {
                            final AtomicBoolean resultReceived = new AtomicBoolean(false);

                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                if (!resultReceived.compareAndSet(false, true)) {
                                    return;
                                }
                                switch (resultCode) {
                                    case RESULT_SUCCESS:
                                        subscriber.onNext(PlaylistExtras.getInt(resultData));
                                        subscriber.onCompleted();
                                        break;
                                    case RESULT_ERROR:
                                        subscriber.onError(new Exception(PlaylistExtras.getError(resultData)));
                                        break;
                                }
                            }
                        };
                        PlaylistExtras.Builder extras = PlaylistExtras.b()
                                .putUri(playlist)
                                .putUriList(tracks)
                                .putResultReceiver(resultReceiver)
                                ;
                        Bundle reply = libraryClient.makeCall(PlaylistMethods.ADD_TO, extras.get());
                        if (!PlaylistExtras.getOk(reply)) {
                            subscriber.onError(new IllegalArgumentException(PlaylistExtras.getError(reply)));
                        }
                    }
                });
            }
        });
    }

    public Observable<Integer> removeFrom(final Uri playlist, final List<Uri> tracks) {
        return wrapObservable(new Func1<LibraryClient, Observable<? extends Integer>>() {
            @Override
            public Observable<? extends Integer> call(final LibraryClient libraryClient) {
                return Observable.create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(final Subscriber<? super Integer> subscriber) {
                        final ResultReceiver resultReceiver = new ResultReceiver(mCallbackHandler) {
                            final AtomicBoolean resultReceived = new AtomicBoolean(false);

                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                if (!resultReceived.compareAndSet(false, true)) {
                                    return;
                                }
                                switch (resultCode) {
                                    case RESULT_SUCCESS:
                                        subscriber.onNext(PlaylistExtras.getInt(resultData));
                                        subscriber.onCompleted();
                                        break;
                                    case RESULT_ERROR:
                                        subscriber.onError(new Exception(PlaylistExtras.getError(resultData)));
                                        break;
                                }
                            }
                        };
                        PlaylistExtras.Builder extras = PlaylistExtras.b()
                                .putUri(playlist)
                                .putUriList(tracks)
                                .putResultReceiver(resultReceiver);
                        Bundle reply = libraryClient.makeCall(PlaylistMethods.REMOVE_FROM, extras.get());
                        if (!PlaylistExtras.getOk(reply)) {
                            subscriber.onError(new IllegalArgumentException(PlaylistExtras.getError(reply)));
                        }
                    }
                });
            }
        });
    }

    public Observable<Integer> update(final Uri playlist, final List<Uri> tracks) {
        return wrapObservable(new Func1<LibraryClient, Observable<? extends Integer>>() {
            @Override
            public Observable<? extends Integer> call(final LibraryClient libraryClient) {
                return Observable.create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(final Subscriber<? super Integer> subscriber) {
                        final ResultReceiver resultReceiver = new ResultReceiver(mCallbackHandler) {
                            final AtomicBoolean resultReceived = new AtomicBoolean(false);

                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                if (!resultReceived.compareAndSet(false, true)) {
                                    return;
                                }
                                switch (resultCode) {
                                    case RESULT_SUCCESS:
                                        subscriber.onNext(PlaylistExtras.getInt(resultData));
                                        subscriber.onCompleted();
                                        break;
                                    case RESULT_ERROR:
                                        subscriber.onError(new Exception(PlaylistExtras.getError(resultData)));
                                        break;
                                }
                            }
                        };
                        PlaylistExtras.Builder extras = PlaylistExtras.b()
                                .putUri(playlist)
                                .putUriList(tracks)
                                .putResultReceiver(resultReceiver);
                        Bundle reply = libraryClient.makeCall(PlaylistMethods.UPDATE, extras.get());
                        if (!PlaylistExtras.getOk(reply)) {
                            subscriber.onError(new IllegalArgumentException(PlaylistExtras.getError(reply)));
                        }
                    }
                });
            }
        });
    }

    public Observable<Integer> delete(final List<Uri> playlists) {
        return wrapObservable(new Func1<LibraryClient, Observable<? extends Integer>>() {
            @Override
            public Observable<? extends Integer> call(final LibraryClient libraryClient) {
                return Observable.create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(final Subscriber<? super Integer> subscriber) {
                        final ResultReceiver resultReceiver = new ResultReceiver(mCallbackHandler) {
                            final AtomicBoolean resultReceived = new AtomicBoolean(false);

                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                if (!resultReceived.compareAndSet(false, true)) {
                                    return;
                                }
                                switch (resultCode) {
                                    case RESULT_SUCCESS:
                                        subscriber.onNext(PlaylistExtras.getInt(resultData));
                                        subscriber.onCompleted();
                                        break;
                                    case RESULT_ERROR:
                                        subscriber.onError(new Exception(PlaylistExtras.getError(resultData)));
                                        break;
                                }
                            }
                        };
                        PlaylistExtras.Builder extras = PlaylistExtras.b()
                                .putUriList(playlists)
                                .putResultReceiver(resultReceiver);
                        Bundle reply = libraryClient.makeCall(PlaylistMethods.DELETE, extras.get());
                        if (!PlaylistExtras.getOk(reply)) {
                            subscriber.onError(new IllegalArgumentException(PlaylistExtras.getError(reply)));
                        }
                    }
                });
            }
        });
    }

    private <T> Observable<T> wrapObservable(Func1<LibraryClient, Observable<? extends T>> observableFunc1) {
        return Observable.using(
                new Func0<LibraryClient>() {
                    @Override
                    public LibraryClient call() {
                        return new LibraryClient(mContext, mAuthority);
                    }
                },
                observableFunc1,
                new Action1<LibraryClient>() {
                    @Override
                    public void call(LibraryClient libraryClient) {
                        libraryClient.release();
                    }
                },
                true //release eagerly
        );
    }

}
