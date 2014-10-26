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
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.andrew.apollo.model.Genre;

import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by drew on 10/24/14.
 */
@Singleton
public class LocalGenresLoader extends AbsGenrePlaylistLoader<Genre> {
    @Inject
    public LocalGenresLoader(@ForApplication Context context) {
        super(context);
        setUri(Uris.EXTERNAL_MEDIASTORE_GENRES);
        setProjection(Projections.GENRE);
        setSelection(Selections.GENRE);
        setSelectionArgs(SelectionArgs.GENRE);
        setSortOrder(MediaStore.Audio.Genres.DEFAULT_SORT_ORDER);

        setProjection2(Projections.GENRE_SONGS);
        setSelection2(Selections.GENRE_SONGS);
        setSelectionArgs2(SelectionArgs.GENRE_SONGS);
        setSortOrder2(SortOrder.GENRE_SONGS);
    }

    @Override
    protected int getIdColumnIdx(Cursor c) {
        return c.getColumnIndexOrThrow(BaseColumns._ID);
    }

    @Override
    protected int getNameColumnIdx(Cursor c) {
        return c.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME);
    }

    @Override
    protected Uri getUriForId(long id) {
        return Uris.GENRE(id);
    }

    @Override
    protected Genre createItem(long id, String name, int songCount, int albumCount, long[] songIds, long[] albumIds) {
        return new Genre(id, name, songCount, albumCount, songIds, albumIds);
    }

    @Override
    public Observable<Genre> getObservable() {
        cache.clear();
        cachePopulated = false;
        return super.getObservable().filter(new Func1<Genre, Boolean>() {
            @Override
            public Boolean call(Genre genre) {
                // mediastore doesnt cleanup old genres so
                // we have to make sure not to add any that are empty
                return genre.mSongNumber > 0;
            }
        }).doOnNext(new Action1<Genre>() {
            @Override
            public void call(Genre genre) {
                cache.add(genre);
            }
        }).doOnCompleted(new Action0() {
            @Override
            public void call() {
                cachePopulated = true;
            }
        }).doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                cache.clear();
                cachePopulated = false;
                Timber.e(throwable, "Couldn't get geners");
            }
        }).onErrorResumeNext(Observable.<Genre>empty()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<Genre>> getListObservable() {
        return getObservable().toList();
    }
}
