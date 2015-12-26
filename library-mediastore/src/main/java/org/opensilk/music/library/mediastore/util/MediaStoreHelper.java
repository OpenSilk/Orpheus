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

package org.opensilk.music.library.mediastore.util;

import android.content.Context;
import android.database.Cursor;

import org.opensilk.music.library.mediastore.loader.AlbumsLoader;
import org.opensilk.music.library.mediastore.loader.ArtistsLoader;
import org.opensilk.music.library.mediastore.loader.GenresLoader;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Genre;

/**
 * Created by drew on 12/26/15.
 */
public class MediaStoreHelper {

    public static Artist getArtist(Context context, String authority, String id) {
        Cursor c = context.getContentResolver().query(
                CursorHelpers.appendId(Uris.EXTERNAL_MEDIASTORE_ARTISTS, id),
                Projections.LOCAL_ARTIST, null, null, null);
        try {
            if (c != null && c.moveToFirst()) {
                return ArtistsLoader.makeFromCursor(c, authority);
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public static Album getAlbum(Context context, String authority, String id) {
        Cursor c = context.getContentResolver().query(
                CursorHelpers.appendId(Uris.EXTERNAL_MEDIASTORE_ALBUMS, id),
                Projections.LOCAL_ALBUM, null, null, null);
        try {
            if (c != null && c.moveToFirst()) {
                return AlbumsLoader.makeFromCursor(c, authority);
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public static Genre getGenre(Context context, String authority, String id) {
        Cursor c = context.getContentResolver().query(
                CursorHelpers.appendId(Uris.EXTERNAL_MEDIASTORE_GENRES, id),
                Projections.GENRE, null, null, null);
        try {
            if (c != null && c.moveToFirst()) {
                return GenresLoader.makeFromCursor(c, authority);
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

}
