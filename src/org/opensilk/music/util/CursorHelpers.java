/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.andrew.apollo.model.Artist;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.api.model.Album;
import org.opensilk.music.util.Projections;
import org.opensilk.music.loaders.SongCursorLoader;

/**
 * Created by drew on 2/22/14.
 */
public class CursorHelpers {

    public static final Uri ARTWORK_URI;

    static {
        ARTWORK_URI = Uri.parse("content://media/external/audio/albumart");
    }

    private CursorHelpers() {
        // static
    }

    /**
     * Creates a Song object from a cursor created with makeSongCursor
     * @param cursor cursor created with makeSongCursor
     * @return new Song object
     */
    public static Song makeSongFromCursor(Cursor cursor) {
        // Copy the song Id
        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        // Copy the song name
        final String songName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
        // Copy the artist name
        final String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
        // Copy the album name
        final String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
        // Copy the album id
        final long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
        // Copy the duration
        final long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION));
        // Make the duration label
        final int seconds = (int) (duration / 1000);
        // Create a new song
        return new Song(id, songName, artist, album, albumId, seconds);
    }

    /**
     * Creates album from given cursor
     *
     * @param c cursor created with makeAlbumCursor
     * @return new Album
     */
    public static Album makeAlbumFromCursor(final Cursor c) {
        // Copy the album id
        final String id = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
        // Copy the album name
        final String albumName = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ALBUM));
        // Copy the artist name
        final String artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ARTIST));
        // Copy the number of songs
        final int songCount = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS));
        // Copy the release year
        final String year = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.FIRST_YEAR));
        // generate artwork Uri
        final Uri artworkUri = ContentUris.withAppendedId(ARTWORK_URI, Long.decode(id));
        // Create a new album
        return new Album(id, albumName, artist, songCount, year, artworkUri);
    }

    /**
     * Create artist from cusor
     * @param c cursor created with makeArtistCursor
     * @return new artist
     */
    public static Artist makeArtistFromCursor(final Cursor c) {
        // Copy the artist id
        final long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
        // Copy the artist name
        final String artistName = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.ARTIST));
        // Copy the number of albums
        final int albumCount = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS));
        // Copy the number of songs
        final int songCount = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS));
        // Create a new artist
        return new Artist(id, artistName, songCount, albumCount);
    }

    public static Genre makeGenreFromCursor(final Cursor c) {
        final long id = c.getLong(c.getColumnIndexOrThrow("_id"));
        final String name= c.getString(c.getColumnIndexOrThrow("name"));
        final int songNumber = c.getInt(c.getColumnIndexOrThrow("song_number"));
        final int albumNumber = c.getInt(c.getColumnIndexOrThrow("album_number"));
        return new Genre(id, name, songNumber, albumNumber);
    }

    public static Playlist makePlaylistFromCursor(final Cursor c) {
        final long id = c.getLong(c.getColumnIndexOrThrow("_id"));
        final String name = c.getString(c.getColumnIndexOrThrow("name"));
        final int songNumber = c.getInt(c.getColumnIndexOrThrow("song_number"));
        return new Playlist(id, name, songNumber);
    }

    public static Cursor makeLastAddedCursor(final Context context) {
        final int fourWeeks = 4 * 3600 * 24 * 7;
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Projections.SONG,
                MediaStore.Audio.AudioColumns.IS_MUSIC + "=? AND " + MediaStore.Audio.AudioColumns.TITLE
                        + "!=? AND " + MediaStore.Audio.Media.DATE_ADDED + ">?",
                new String[] {"1", "''", String.valueOf(System.currentTimeMillis() / 1000 - fourWeeks)},
                MediaStore.Audio.Media.DATE_ADDED + " DESC");
    }

    public static Cursor makeSongCursor(final Context context) {
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Projections.SONG,
                SongCursorLoader.SELECTION,
                SongCursorLoader.SELECTION_ARGS,
                PreferenceUtils.getInstance(context).getSongSortOrder());
    }

}
