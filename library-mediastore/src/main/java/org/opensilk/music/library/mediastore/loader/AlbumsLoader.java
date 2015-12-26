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

package org.opensilk.music.library.mediastore.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxCursorLoader;
import org.opensilk.music.library.mediastore.provider.FoldersUris;
import org.opensilk.music.library.mediastore.util.Projections;
import org.opensilk.music.library.mediastore.util.SelectionArgs;
import org.opensilk.music.library.mediastore.util.Selections;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.model.Album;

import javax.inject.Inject;
import javax.inject.Named;

import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateArtworkUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getIntOrZero;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrEmpty;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrNull;

/**
 * Created by drew on 10/24/14.
 */
@LoaderScope
public class AlbumsLoader extends RxCursorLoader<Album> {

    final String authority;

    @Inject
    public AlbumsLoader(
            @ForApplication Context context,
            @Named("foldersLibraryAuthority") String authority
    ) {
        super(context);
        this.authority = authority;
        setUri(Uris.EXTERNAL_MEDIASTORE_ALBUMS);
        setProjection(Projections.LOCAL_ALBUM);
        setSelection(Selections.LOCAL_ALBUM);
        setSelectionArgs(SelectionArgs.LOCAL_ALBUM);
        // need set sortorder
    }

    @Override
    protected Album makeFromCursor(Cursor c) {
       return makeFromCursor(c, authority);
    }

    public static Album makeFromCursor(Cursor c, String authority) {
        // Copy the album id
        final String id = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
        // Copy the album name
        final String albumName = getStringOrEmpty(c, MediaStore.Audio.AlbumColumns.ALBUM);
        // Copy the artist name
        final String artist = getStringOrNull(c, MediaStore.Audio.AlbumColumns.ARTIST);
        // Copy the number of songs
        final int songCount = getIntOrZero(c, MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS);
        // Copy the release year
//        int year = getIntOrZero(c, MediaStore.Audio.AlbumColumns.FIRST_YEAR);
//        if (year == 0) {
//            year = getIntOrZero(c, MediaStore.Audio.AlbumColumns.LAST_YEAR);
//        }
        // generate artwork Uri
        final Uri artworkUri = generateArtworkUri(id);
        return Album.builder()
                .setUri(FoldersUris.album(authority, id))
                .setParentUri(FoldersUris.albums(authority))
                .setTracksUri(FoldersUris.albumTracks(authority, id))
//                .setDetailsUri(FoldersUris.albumDetails(authority, id))
                .setName(albumName)
                .setArtistName(artist)
                .setTrackCount(songCount)
                .setArtworkUri(artworkUri)
                .build();
    }

}
