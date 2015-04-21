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

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.music.util.CursorHelpers;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Created by drew on 11/24/14.
 */
public class SearchLoader extends RxCursorLoader<Object> {

    @Inject
    public SearchLoader(@ForApplication Context context) {
        super(context);
    }

    @Override
    protected Object makeFromCursor(Cursor c) {
        // Get the MIME type
        final String mimetype = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
        if (mimetype.equals("artist")) {
            // get id
            final long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
            // Get the artist name
            final String name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            // Get the album count
            final int albumCount = c.getInt(c.getColumnIndexOrThrow("data1"));
            // Get the song count
            final int songCount = c.getInt(c.getColumnIndexOrThrow("data2"));
            // Build artist
            return new LocalArtist(id, name, songCount, albumCount);
        } else if (mimetype.equals("album")) {
            // Get the Id of the album
            final long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
            // Get the album name
            final String name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            // Get the artist nam
            final String artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            // generate artwork uri
            final Uri artworkUri = CursorHelpers.generateArtworkUri(id);
            Cursor c2 = CursorHelpers.getSingleLocalAlbumCursor(context, id);
            try {
                if (c2 != null && c2.moveToFirst()) {
                    return CursorHelpers.makeLocalAlbumFromCursor(c2);
                } else {
                    Timber.w("Unable to query for album %d", id);
                    // Build the album as best we can
                    return new LocalAlbum(id, name, artist, 0, null, artworkUri);
                }
            } finally {
                if (c2 != null) c2.close();
            }
        } else { // audio
            // get id
            final long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
            // Get the track name
            final String name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            // Get the album name
            final String album = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            // get artist name
            final String artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            Cursor c2 = CursorHelpers.getSingleLocalSongCursor(context, id);
            try {
                if (c2 != null && c2.moveToFirst()) {
                    return CursorHelpers.makeLocalSongFromCursor(c2);
                } else {
                    Timber.w("Unable to query for song %d", id);
                    // build the song as best we can
                    return new LocalSong(id, name, album, artist, null, 0, 0, CursorHelpers.generateDataUri(id), null, mimetype);
                }
            } finally {
                if (c2 != null) c2.close();
            }
        }
    }

    public SearchLoader setFilter(String filter) {
//        final Uri uri = Uri.parse("content://media/external/audio/search/search_suggest_query/" + Uri.encode(filter));
        final Uri uri = Uri.parse("content://media/external/audio/search/fancy/" + Uri.encode(filter));
        setUri(uri);
        reset();
        return this;
    }

}
