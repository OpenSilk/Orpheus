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

package org.opensilk.music.library.mediastore.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxCursorLoader;
import org.opensilk.music.library.mediastore.provider.MediaStoreLibraryProvider;
import org.opensilk.music.library.mediastore.util.Projections;
import org.opensilk.music.library.mediastore.util.SelectionArgs;
import org.opensilk.music.library.mediastore.util.Selections;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Track;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func1;

import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateArtworkUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrEmpty;

/**
 * Created by drew on 5/5/15.
 */
public class GenresLoader extends RxCursorLoader<Genre> {

    final String myAuthority;
    final Provider<TracksLoader> tracksLoaderProvider;

    @Inject
    public GenresLoader(
            @ForApplication Context context,
            @Named("mediaStoreLibraryBaseAuthority") String baseAuthority,
            Provider<TracksLoader> tracksLoaderProvider
    ) {
        super(context);
        this.myAuthority = MediaStoreLibraryProvider.AUTHORITY_PFX+baseAuthority;
        this.tracksLoaderProvider = tracksLoaderProvider;
        setUri(Uris.EXTERNAL_MEDIASTORE_GENRES);
        setProjection(Projections.GENRE);
        setSelection(Selections.GENRE);
        setSelectionArgs(SelectionArgs.GENRE);
        // need set sortorder
    }

    @Override
    protected Genre makeFromCursor(Cursor c) throws Exception {
        final String id = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
        final String name = getStringOrEmpty(c, MediaStore.Audio.Genres.NAME);
        return Genre.builder()
                .setUri(LibraryUris.genre("FAKE", "0", id))
                .setName(name)
                .build();
    }

    @Override
    public Observable<Genre> createObservable() {
        return super.createObservable().flatMap(new Func1<Genre, Observable<Genre>>() {
            @Override
            public Observable<Genre> call(Genre genre) {
                //We have to pull all the member tracks so we can
                //set the extra info.
                TracksLoader l = tracksLoaderProvider.get();
                l.setUri(Uris.GENRE_MEMBERS(genre.getUri().getLastPathSegment()));
                l.setProjection(Projections.GENRE_SONGS);
                l.setSelection(Selections.GENRE_SONGS);
                l.setSelectionArgs(SelectionArgs.GENRE_SONGS);
//                return l.createObservable().collect(genre.buildUpon(), new Action2<Genre.Builder, Track>() {
//                    @Override
//                    public void call(Genre.Builder builder, Track track) {
//                        builder.addTrackUri(LibraryUris.track(myAuthority, "0", track.identity));
//                        builder.addAlbumUri(LibraryUris.album(myAuthority, "0", track.albumIdentity));
//                        builder.addArtInfo(track.albumArtistName, track.albumName, generateArtworkUri(track.albumIdentity));
//                    }
//                }).map(new Func1<Genre.Builder, Genre>() {
//                    @Override
//                    public Genre call(Genre.Builder builder) {
//                        return builder.build();
//                    }
//                });
                return Observable.empty();
            }
        }).filter(new Func1<Genre, Boolean>() {
            @Override
            public Boolean call(Genre genre) {
                // mediastore doesnt cleanup old genres so
                // we have to make sure not to add any that are empty
//                return genre.trackUris.size() > 0;
                return true;
            }
        });
    }
}
