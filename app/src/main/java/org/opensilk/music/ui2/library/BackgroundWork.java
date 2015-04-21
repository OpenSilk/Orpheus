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

import org.opensilk.music.R;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.event.MakeToast;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/9/14.
 */
public class BackgroundWork {

    final LibraryScreenPresenter presenter;

    Subscription subscription;

    public BackgroundWork(LibraryScreenPresenter presenter) {
        this.presenter = presenter;
    }

    public void startWork(final OverflowAction action, final Bundleable item) {
        subscription = getAll(item).subscribeOn(Schedulers.io()).subscribe(new Subscriber<Observable<List<Song>>>() {
            List<Song> allSongs = new ArrayList<>();

            @Override
            public void onCompleted() {
                Timber.v("onCompleted(outer) %s", Thread.currentThread().getName());
                if (!allSongs.isEmpty()) {
                    onListFetched(action, allSongs);
                } else {
                    presenter.bus.post(new MakeToast(R.string.err_unable_to_fetch_songs));
                }
                presenter.dismissProgressDialog();
            }

            @Override
            public void onError(Throwable e) {
                Timber.w(e, "onError(outer) %s ", Thread.currentThread().getName());
            }

            @Override
            public void onNext(Observable<List<Song>> listObservable) {
                Timber.v("onNext(outer) %s", Thread.currentThread().getName());
                //add the inner to the outer so it is canceled when outer is canceled
                add(listObservable.subscribe(new Subscriber<List<Song>>() {
                    @Override
                    public void onCompleted() {
                        Timber.v("onCompleted(inner) %s", Thread.currentThread().getName());
                        //unused observable resubscibes itself
                    }

                    @Override
                    public void onError(Throwable e) {
                        //TODO why does canceling cause this to be called?
                        Timber.w(e, "onError(inner) %s", Thread.currentThread().getName());
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

    Observable<Observable<List<Song>>> getAll(final Bundleable item) {
        return Observable.create(new Observable.OnSubscribe<Observable<List<Song>>>() {
            @Override
            public void call(Subscriber<? super Observable<List<Song>>> subscriber) {
                Timber.v("create(outer) %s", Thread.currentThread().getName());
                subscriber.onNext(getSome(subscriber, item, null));
            }
        });
    }

    Observable<List<Song>> getSome(final Subscriber<? super Observable<List<Song>>> outerSubscriber,
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
                    if (result.token != null && !songs.isEmpty()) {
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

    void onListFetched(OverflowAction action, final List<Song> songs) {
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
    }

}
