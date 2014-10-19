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

package org.opensilk.music.ui2.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action2;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/19/14.
 */
public abstract class MediaStoreLoader<T> {

    final Context context;

    Uri uri;
    String[] projection;
    String selection;
    String[] selectionArgs;
    String sortOrder;

    public MediaStoreLoader(Context context) {
        this.context = context;
    }

    public MediaStoreLoader(Context context,
                            Uri uri, String[] projection, String selection,
                            String[] selectionArgs, String sortOrder) {
        this.context = context;
        this.uri = uri;
        this.projection = projection;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.sortOrder = sortOrder;
    }

    protected abstract T makeFromCursor(Cursor c);

    protected void emmitError(Throwable t, Subscriber<? super T> subscriber) {
        if (subscriber.isUnsubscribed()) return;
        subscriber.onError(t);
    }

    public Observable<List<T>> getObservable() {
        return Observable.create(new Observable.OnSubscribe<T>() {
            // Querys the mediastore
            @Override
            public void call(Subscriber<? super T> subscriber) {
//                Timber.v("OnSubscribe %s", Thread.currentThread().getName());
                if (context == null) {
                    emmitError(new NullPointerException("Context must not be null"), subscriber);
                    return;
                }
                if (uri == null) {
                    emmitError(new NullPointerException("Uri must not be null"), subscriber);
                    return;
                }
                Cursor c = context.getContentResolver().query(
                        uri,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                );
                if (c == null) {
                    emmitError(new NullPointerException("Unable to obtain cursor"), subscriber);
                    return;
                }
                try {
                    c.moveToFirst();
                    do {
                        T item = makeFromCursor(c);
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        subscriber.onNext(item);
                    } while (c.moveToNext());
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    emmitError(e, subscriber);
                } finally {
                    if (c != null) c.close();
                }
            }
        })
        // collects the objects into a list and publishes the complete list as
        // a single onNext() call
        .collect(new ArrayList<T>(), new Action2<List<T>, T>() {
            @Override
            public void call(List<T> list, T item) {
//                Timber.v("collect %s", Thread.currentThread().getName());
                list.add(item);
            }
        })
        // want Query on an io thread
        .subscribeOn(Schedulers.io())
        // want the final List to be published on the main thread
        .observeOn(AndroidSchedulers.mainThread());
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public void setProjection(String[] projection) {
        this.projection = projection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public void setSelectionArgs(String[] selectionArgs) {
        this.selectionArgs = selectionArgs;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
