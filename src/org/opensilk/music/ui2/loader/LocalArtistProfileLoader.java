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

import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.LocalSongGroup;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.music.R;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import static org.opensilk.music.ui2.loader.AbsGenrePlaylistLoader.toArray;

/**
 * Created by drew on 11/19/14.
 */
public class LocalArtistProfileLoader extends RxCursorLoader<Object> {

    final LocalArtist artist;

    @Inject
    public LocalArtistProfileLoader(@ForApplication Context context, LocalArtist artist) {
        super(context);
        this.artist = artist;
        setUri(Uris.EXTERNAL_MEDIASTORE_ARTISTS_ALBUMS(artist.artistId));
        setProjection(Projections.LOCAL_ALBUM);
        setSelection(Selections.LOCAL_ALBUM);
        setSelectionArgs(SelectionArgs.LOCAL_ALBUM);
        setSortOrder(MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
    }

    @Override
    protected Object makeFromCursor(Cursor c) {
        return CursorHelpers.makeLocalAlbumFromCursor(c);
    }

    @Override
    public Observable<Object> getObservable() {
        Observable<Object> songGroupLoader = performSomeMagick(
                new RxCursorLoader<LocalSong>(
                    context,
                    Uris.EXTERNAL_MEDIASTORE_MEDIA,
                    Projections.LOCAL_SONG,
                    Selections.LOCAL_ARTIST_SONGS,
                    SelectionArgs.LOCAL_ARTIST_SONGS(artist.artistId),
                    SortOrder.LOCAL_ARTIST_SONGS
                ) {
                    @Override
                    protected LocalSong makeFromCursor(Cursor c) {
                        return CursorHelpers.makeLocalSongFromCursor(c);
                    }
                }.createObservable()
        );
        return songGroupLoader.concatWith(createObservable().subscribeOn(Schedulers.io()))
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public boolean hasCache() {
        return false; // ignores cache
    }

    public Observable<Object> performSomeMagick(Observable<LocalSong> observable) {
        Observable<LocalSong> o = observable.subscribeOn(Schedulers.io()).share();
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
                new Func2<List<Long>, List<Long>, Object>() {
                    @Override
                    public Object call(List<Long> songs, List<Long> albums) {
                        Collections.sort(albums);
                        return new LocalSongGroup(context.getString(R.string.title_all_songs),
                                artist.name, toArray(songs), toArray(albums));
                    }
                });
    }
}
