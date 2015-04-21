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
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.andrew.apollo.model.LocalSong;

import org.opensilk.music.util.CursorHelpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/23/14.
 */
public abstract class AbsGenrePlaylistLoader<T> implements RxLoader<T> {

    class UriObserver extends ContentObserver {
        UriObserver(Handler handler) {
            super(handler);
        }
        @Override
        @DebugLog
        public void onChange(boolean selfChange) {
            reset();
            for (ContentChangedListener l : contentChangedListeners) {
                l.reload();
            }
        }
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            onChange(selfChange);
        }
    }

    protected final List<ContentChangedListener> contentChangedListeners;
    protected final Context context;

    Uri uri;
    String[] projection;
    String selection;
    String[] selectionArgs;
    String sortOrder;

    String[] projection2;
    String selection2;
    String[] selectionArgs2;
    String sortOrder2;

    private UriObserver uriObserver;

    protected Observable<T> cachedObservable;

    public AbsGenrePlaylistLoader(Context context) {
        contentChangedListeners = new ArrayList<>();
        this.context = context;
    }

    protected abstract int getIdColumnIdx(Cursor c);
    protected abstract int getNameColumnIdx(Cursor c);
    protected abstract Uri getUriForId(long id);
    protected abstract T createItem(long id, String name, int songCount, int albumCount, long[] songIds, long[] albumIds);

    //@DebugLog
    public Observable<T> getObservable() {
        if (uriObserver == null) {
            uriObserver = new UriObserver(new Handler(Looper.getMainLooper()));
            context.getContentResolver().registerContentObserver(uri, true, uriObserver);
        }
        RxCursorLoader<T> collectionLoader = new RxCursorLoader<T>(context,
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder) {
            @Override
            protected T makeFromCursor(Cursor c) {
                long id = c.getInt(getIdColumnIdx(c));
                String name = c.getString(getNameColumnIdx(c));
                RxCursorLoader<LocalSong> songsLoader = new RxCursorLoader<LocalSong>(context,
                        getUriForId(id),
                        projection2,
                        selection2,
                        selectionArgs2,
                        sortOrder2) {
                    @Override
                    protected LocalSong makeFromCursor(Cursor c) {
                        return CursorHelpers.makeLocalSongFromCursor(c);
                    }
                };
                Observable<T> item = performSomeMagick(songsLoader.createObservable(), id, name);
                return item.toBlocking().first();
            }
        };
        return collectionLoader.createObservable().subscribeOn(Schedulers.io());
    }

    //@DebugLog
    public Observable<T> performSomeMagick(Observable<LocalSong> observable,
                                           final long itemId,
                                           final String itemName) {
        Observable<LocalSong> o = observable.subscribeOn(Schedulers.io()).replay().refCount();
        //songs
        Observable<List<Long>> songs = o.map(new Func1<LocalSong, Long>() {
            @Override
            public Long call(LocalSong localSong) {
                return localSong.songId;
            }
            // collect output into list
        }).toList();
        //albums
        Observable<List<Long>> albums = o.map(new Func1<LocalSong, Long>() {
            @Override
            public Long call(LocalSong localSong) {
                return localSong.albumId;
            }
            //only want unique albums, //collect output into list
        }).distinct().toList();
        // zip the songs and albums into a playlist
        return Observable.zip(songs, albums,
                new Func2<List<Long>, List<Long>, T>() {
                    @Override
                    public T call(List<Long> songs, List<Long> albums) {
                        Collections.sort(albums);
                        return createItem(itemId, itemName, songs.size(), albums.size(), toArray(songs), toArray(albums));
                    }
                });
    }

    public void reset() {
        cachedObservable = null;
    }

    @DebugLog
    public void addContentChangedListener(ContentChangedListener l) {
        contentChangedListeners.add(l);
    }

    @DebugLog
    public void removeContentChangedListener(ContentChangedListener l) {
        contentChangedListeners.remove(l);
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

    public void setProjection2(String[] projection) {
        this.projection2 = projection;
    }

    public void setSelection2(String selection) {
        this.selection2 = selection;
    }

    public void setSelectionArgs2(String[] selectionArgs) {
        this.selectionArgs2 = selectionArgs;
    }

    public void setSortOrder2(String sortOrder) {
        this.sortOrder2 = sortOrder;
    }

    // Modified Longs.toArray from Guava
    public static long[] toArray(Collection<Long> collection) {
        Object[] boxedArray = collection.toArray();
        int len = boxedArray.length;
        long[] array = new long[len];
        for (int i = 0; i < len; i++) {
            array[i] = ((Long) boxedArray[i]).longValue();
        }
        return array;
    }

    protected void dump(Throwable throwable) {
        Timber.e(throwable, "AbsGenrePlaylistLoader(uri=%s\nprojection=%s\nselection=%s\n" +
                        "selectionArgs=%s\nsortOrder=%s\nprojection2=%s\nselection2=%s\n" +
                        "selectionArgs2=%s\nsortOrder2=%s",
                uri, projection, selection, selectionArgs, sortOrder,
                projection2, selection2, selectionArgs2, sortOrder2);
    }

}
