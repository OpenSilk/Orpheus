/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2.library;

import android.os.Bundle;
import android.os.RemoteException;

import com.andrew.apollo.model.RecentSong;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.event.MakeToast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.umass.lastfm.Result;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/9/14.
 */
public class BackgroundWork {

    final LibraryScreen.Presenter presenter;

    Subscription subscription;

    public BackgroundWork(LibraryScreen.Presenter presenter) {
        this.presenter = presenter;
    }

    public void startWork(final OverflowAction action, final Bundleable item) {
        subscription = getAll(item).subscribeOn(Schedulers.io()).subscribe(new Subscriber<Observable<List<Song>>>() {
            List<Song> allSongs = new ArrayList<>();

            @Override
            public void onCompleted() {
                Timber.v("onCompleted(outer) %s", Thread.currentThread().getName());
                onListFetched(action, allSongs);
            }

            @Override
            public void onError(Throwable e) {
                Timber.v("onError(outer) %s", Thread.currentThread().getName());
            }

            @Override
            public void onNext(Observable<List<Song>> listObservable) {
                Timber.v("onNext(outer) %s", Thread.currentThread().getName());
                //add the inner to the outer so it is canceled when outer is canceled
                add(listObservable.subscribe(new Subscriber<List<Song>>() {
                    @Override
                    public void onCompleted() {
                        Timber.v("onCompleted(inner) %s", Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.v("onError(inner) %s", Thread.currentThread().getName());
                    }

                    @Override
                    public void onNext(List<Song> songs) {
                        Timber.v("onNext(inner) %s", Thread.currentThread().getName());
                        allSongs.addAll(songs);
                    }
                }));
            }
        });
    }

    public void cancelWork() {
        if (isSubscribed(subscription)) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    public Observable<Observable<List<Song>>> getAll(final Bundleable item) {
        return Observable.create(new Observable.OnSubscribe<Observable<List<Song>>>() {
            @Override
            public void call(Subscriber<? super Observable<List<Song>>> subscriber) {
                Timber.v("create(outer) %s", Thread.currentThread().getName());
                subscriber.onNext(getSome(subscriber, item, null));
            }
        });
    }

    public Observable<List<Song>> getSome(final Subscriber<? super Observable<List<Song>>> outerSubscriber,
                                          final Bundleable item,
                                          final Bundle token) {
        return Observable.create(new Observable.OnSubscribe<List<Song>>() {
            @Override
            public void call(Subscriber<? super List<Song>> subscriber) {
                Timber.v("create(inner) %s", Thread.currentThread().getName());
                try {
                    LibraryConnection.Result result =
                            presenter.connection.listSongsInFolder(
                                    presenter.pluginInfo,
                                    presenter.libraryInfo.buildUpon(item.getIdentity(), item.getName()),
                                    token
                            ).toBlocking().single();
                    List<Song> songs = new ArrayList<>(result.items.size());
                    for (Bundleable b : result.items) {
                        if (b instanceof Song) songs.add((Song)b);
                    }
                    if (subscriber.isUnsubscribed()) return;
                    subscriber.onNext(songs);
                    subscriber.onCompleted();
                    if (result.token != null) {
                        if (!outerSubscriber.isUnsubscribed())
                            outerSubscriber.onNext(getSome(outerSubscriber, item, result.token));
                    } else {
                        if (!outerSubscriber.isUnsubscribed())
                            outerSubscriber.onCompleted();
                    }
                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed())
                        subscriber.onError(e);
                }
            }
        });
    }

    public void onListFetched(OverflowAction action, final List<Song> songs) {
        switch (action) {
            case ADD_TO_QUEUE:
                presenter.musicService.enqueueEnd(new Func0<Song[]>() {
                    @Override
                    public Song[] call() {
                        return songs.toArray(new Song[songs.size()]);
                    }
                });
                break;
            case PLAY_ALL:
                presenter.musicService.playAllSongs(new Func0<Song[]>() {
                    @Override
                    public Song[] call() {
                        return songs.toArray(new Song[songs.size()]);
                    }
                }, 0, false);
                break;
            case SHUFFLE_ALL:
                presenter.musicService.playAllSongs(new Func0<Song[]>() {
                    @Override
                    public Song[] call() {
                        return songs.toArray(new Song[songs.size()]);
                    }
                }, 0, true);
                break;
        }
        presenter.dismissProgressDialog();
    }

}
