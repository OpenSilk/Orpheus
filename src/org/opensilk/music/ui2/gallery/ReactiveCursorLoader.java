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

package org.opensilk.music.ui2.gallery;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
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

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;

/**
 * Created by drew on 10/9/14.
 */
public class ReactiveCursorLoader {

    final Context context;

    final ForceLoadContentObserver observer;

    Uri uri;
    String[] projection;
    String selection;
    String[] selectionArgs;
    String sortOrder;

    public ReactiveCursorLoader(Context context) {
        this.context = context;
        this.observer = new ForceLoadContentObserver();
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

    public Context getContext() {
        return context;
    }

    public Observable<Cursor> getObservable() {
        return Observable.create(new Observable.OnSubscribe<Cursor>() {
            @Override
            public void call(Subscriber<? super Cursor> subscriber) {
                Cursor c = context.getContentResolver().query(
                        uri, projection, selection, selectionArgs, sortOrder);
                if (c == null) {
                    subscriber.onError(new NullPointerException("Unable to obtain cursor"));
                    return;
                }
                if (subscriber.isUnsubscribed()) {
                    c.close();
                    return;
                }
                c.registerContentObserver(observer);
                subscriber.onNext(c);
                subscriber.onCompleted();
            }
        });
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

        }
    }

}
