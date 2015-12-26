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
import android.provider.BaseColumns;
import android.provider.MediaStore;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxCursorLoader;
import org.opensilk.music.library.mediastore.provider.FoldersUris;
import org.opensilk.music.library.mediastore.util.Projections;
import org.opensilk.music.library.mediastore.util.SelectionArgs;
import org.opensilk.music.library.mediastore.util.Selections;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.model.Artist;

import javax.inject.Inject;
import javax.inject.Named;

import static org.opensilk.music.library.mediastore.util.CursorHelpers.getIntOrZero;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrEmpty;

/**
 * Created by drew on 10/24/14.
 */
@LoaderScope
public class ArtistsLoader extends RxCursorLoader<Artist> {

    final String authority;

    @Inject
    public ArtistsLoader(
            @ForApplication Context context,
            @Named("foldersLibraryAuthority") String authority
    ) {
        super(context);
        this.authority = authority;
        setUri(Uris.EXTERNAL_MEDIASTORE_ARTISTS);
        setProjection(Projections.LOCAL_ARTIST);
        setSelection(Selections.LOCAL_ARTIST);
        setSelectionArgs(SelectionArgs.LOCAL_ARTIST);
        //must set sort order
    }

    @Override
    protected Artist makeFromCursor(Cursor c) {
        // Copy the artist id
        final String id = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
        // Copy the artist name
        final String artistName = getStringOrEmpty(c, MediaStore.Audio.ArtistColumns.ARTIST);
        // Copy the number of albums
        final int albumCount = getIntOrZero(c, MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS);
        // Copy the number of songs
        final int songCount = getIntOrZero(c, MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS);
        // Create a new artist
        return Artist.builder()
                .setUri(FoldersUris.artist(authority, id))
                .setParentUri(FoldersUris.artists(authority))
                .setTracksUri(FoldersUris.artistTracks(authority, id))
                .setName(artistName)
                .setAlbumCount(albumCount)
                .setTrackCount(songCount)
                .build();
    }
}
