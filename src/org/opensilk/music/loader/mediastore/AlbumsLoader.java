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

package org.opensilk.music.loader.mediastore;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.utils.SortOrder;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.Uris;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.internal.operators.OnSubscribeCache;
import rx.observers.Observers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/8/14.
 */
@Singleton
public class AlbumsLoader {

    final Context context;
    final AppPreferences prefs;

    final ForceLoadContentObserver observer;
    final Set<Action0> changeListeners;

    @Inject
    public AlbumsLoader(@ForApplication Context context, AppPreferences prefs) {
        this.context = context;
        this.prefs = prefs;
        this.observer = new ForceLoadContentObserver();
        this.changeListeners = new HashSet<>();
    }

    public Observable<List<Album>> getObservable() {
        return Observable.create(new Observable.OnSubscribe<Album>() {
            // Querys the mediastore and publishes the Albums
            @Override
            public void call(Subscriber<? super Album> subscriber) {
//                Timber.v("Album Observable: called on: %s", Thread.currentThread().getName());
                Cursor c = context.getContentResolver().query(
                        Uris.EXTERNAL_MEDIASTORE_ALBUMS,
                        Projections.LOCAL_ALBUM,
                        Selections.LOCAL_ALBUM,
                        SelectionArgs.LOCAL_ALBUM,
                        prefs.getString(AppPreferences.ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z)
                );
                if (c == null) {
                    subscriber.onError(new NullPointerException("Unable to obtain cursor"));
                    return;
                }
                c.moveToFirst();
                do {
                    if (subscriber.isUnsubscribed()) {
                        c.close();
                        return;
                    }
                    LocalAlbum a = CursorHelpers.makeLocalAlbumFromCursor(c);
                    subscriber.onNext(a);
                } while (c.moveToNext());
                c.close();
                subscriber.onCompleted();
            }
        })
        // collects the album objects into a list and publishes the complete list as
        // a single onNext() call
        .collect(new ArrayList<Album>(), new Action2<List<Album>, Album>() {
            @Override
            public void call(List<Album> albums, Album album) {
//                Timber.v("Albums Collector called on: %s", Thread.currentThread().getName());
                albums.add(album);
            }
        })
        // want Query on an io thread
        .subscribeOn(Schedulers.io())
        // want the final List to be published on the main thread
        .observeOn(AndroidSchedulers.mainThread());
    }

    public void registerChangeListener(Action0 l) {
        changeListeners.add(l);
    }

    public void unregisterChangeListener(Action0 l) {
        changeListeners.remove(l);
    }

    public final class ForceLoadContentObserver extends ContentObserver {
        public ForceLoadContentObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            for (Action0 l : changeListeners) {
                l.call();
            }
        }
    }

}
