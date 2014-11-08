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

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;

import org.opensilk.music.R;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 10/24/14.
 */
@Singleton
public class LocalPlaylistsLoader extends AbsGenrePlaylistLoader<Playlist> {

    @Inject
    public LocalPlaylistsLoader(@ForApplication Context context) {
        super(context);
        setUri(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS);
        setProjection(Projections.PLAYLIST);
        setSelection(Selections.PLAYLIST);
        setSelectionArgs(SelectionArgs.PLAYLIST);
        setSortOrder(MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);

        setProjection2(Projections.PLAYLIST_SONGS);
        setSelection2(Selections.PLAYLIST_SONGS);
        setSelectionArgs2(SelectionArgs.PLAYLIST_SONGS);
        setSortOrder2(SortOrder.PLAYLIST_SONGS);
    }

    @Override
    protected int getIdColumnIdx(Cursor c) {
        return c.getColumnIndexOrThrow(BaseColumns._ID);
    }

    @Override
    protected int getNameColumnIdx(Cursor c) {
        return c.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME);
    }

    @Override
    protected Uri getUriForId(long id) {
        return Uris.PLAYLIST(id);
    }

    @Override
    protected Playlist createItem(long id, String name, int songCount, int albumCount, long[] songIds, long[] albumIds) {
        return new Playlist(id, name, songCount, albumCount, songIds, albumIds);
    }

    @Override
    public Observable<Playlist> getObservable() {
        cache.clear();
        cachePopulated = false;
        RxCursorLoader<LocalSong> lastAddedLoader = new RxCursorLoader<LocalSong>(context,
                Uris.EXTERNAL_MEDIASTORE_MEDIA,
                Projections.LOCAL_SONG,
                Selections.LAST_ADDED,
                SelectionArgs.LAST_ADDED(),
                SortOrder.LAST_ADDED) {
            @Override
            protected LocalSong makeFromCursor(Cursor c) {
                return CursorHelpers.makeLocalSongFromCursor(c);
            }
        };

        Observable<Playlist> lastAddedObservable = performSomeMagick(
                // performSomeMagick subscribes us on io so we dont need to do that here
                lastAddedLoader.createObservable(),
                -1, context.getResources().getString(R.string.playlist_last_added));

        // we want last added first so concat them together
        return Observable.concat(lastAddedObservable, super.getObservable())
                .doOnNext(new Action1<Playlist>() {
                    @Override
                    public void call(Playlist playlist) {
                        cache.add(playlist);
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        cachePopulated = true;
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        cache.clear();
                        cachePopulated = false;
                        Timber.e(throwable, "Unable to obtain playlists");
                    }
                })
                .onErrorResumeNext(Observable.<Playlist>empty())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<Playlist>> getListObservable() {
        return getObservable().toList();
    }
}
