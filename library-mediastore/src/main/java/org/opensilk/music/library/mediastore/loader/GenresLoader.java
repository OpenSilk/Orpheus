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
import android.util.SparseBooleanArray;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxCursorLoader;
import org.opensilk.music.library.mediastore.provider.FoldersUris;
import org.opensilk.music.library.mediastore.util.Projections;
import org.opensilk.music.library.mediastore.util.SelectionArgs;
import org.opensilk.music.library.mediastore.util.Selections;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Genre;

import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateArtworkUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getLongOrZero;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrEmpty;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrNull;

/**
 * Created by drew on 5/5/15.
 */
@LoaderScope
public class GenresLoader extends RxCursorLoader<Genre> {

    final String authority;

    @Inject
    public GenresLoader(
            @ForApplication Context context,
            @Named("foldersLibraryAuthority") String authority
    ) {
        super(context);
        this.authority = authority;
        setUri(Uris.EXTERNAL_MEDIASTORE_GENRES);
        setProjection(Projections.GENRE);
        setSelection(Selections.GENRE);
        setSelectionArgs(SelectionArgs.GENRE);
        // need set sortorder
    }

    @Override
    protected Genre makeFromCursor(Cursor c) throws Exception {
        return makeFromCursor(c, authority);
    }

    public static Genre makeFromCursor(Cursor c, String authority) {
        final String id = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
        final String name = getStringOrEmpty(c, MediaStore.Audio.Genres.NAME);
        return Genre.builder()
                .setUri(FoldersUris.genre(authority, id))
                .setParentUri(FoldersUris.genres(authority))
                .setTracksUri(FoldersUris.genreTracks(authority, id))
//                .setDetailsUri(FoldersUris.genreDetails(authority, id))
                .setName(name)
                .build();
    }

    static final String[] artInfoProj = new String[]{
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            "album_artist",
    };

    @Override
    public Observable<Genre> createObservable() {
        return super.createObservable().map(new Func1<Genre, Genre>() {
            @Override
            public Genre call(Genre genre) {
                Cursor c = context.getContentResolver().query(
                        Uris.GENRE_MEMBERS(genre.getUri().getLastPathSegment()),
                        artInfoProj, null, null, null);
                try {
                    if (c != null && c.moveToFirst()) {
                        Genre.Builder gb = Genre.builder();
                        SparseBooleanArray addedIds = new SparseBooleanArray();
                        int addedCount = 0;
                        do {
                            String albumId = c.getString(0);
                            String album = c.getString(1);
                            String albumArtist = c.getString(2);
                            if (!StringUtils.isEmpty(albumId) && StringUtils.isNumeric(albumId)) {
                                //Don't add same one twice (to save allocations)
                                long aid = Long.valueOf(albumId);
                                if (aid < Integer.MAX_VALUE) {
                                    if (addedIds.get((int) aid, false)) {
                                        continue;
                                    } else {
                                        addedIds.put((int) aid, true);
                                    }
                                }
                                if (!StringUtils.isEmpty(album) && !StringUtils.isEmpty(albumArtist)) {
                                    gb.addArtInfo(ArtInfo.forAlbum(albumArtist, album, generateArtworkUri(albumId)));
                                    addedCount++;
                                } else {
                                    gb.addArtInfo(ArtInfo.forAlbum(null, null, generateArtworkUri(albumId)));
                                    addedCount++;
                                }
                            }
                        } while (c.moveToNext() && addedCount < 4);
                        int trackCount = c.getCount();
                        genre = gb
                                .setUri(genre.getUri())
                                .setParentUri(genre.getParentUri())
                                .setTracksUri(genre.getTracksUri())
                                .setName(genre.getName())
                                        //new stuff
                                .setTrackCount(trackCount)
                                .build();
                    }
                } catch (Exception e) {
                    Timber.w(e, "Add track count to genre, %s", genre.getName());
                } finally {
                    if (c != null) c.close();
                }
                return genre;
            }
        }).filter(new Func1<Genre, Boolean>() {
            @Override
            public Boolean call(Genre genre) {
                // mediastore doesnt cleanup old genres so
                // we have to make sure not to add any that are empty
                return genre.getTracksCount() > 0;
            }
        });
    }

}
