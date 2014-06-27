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
import android.text.TextUtils;

import com.andrew.apollo.model.Artist;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.model.RecentSong;
import com.andrew.apollo.provider.MusicStore;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.artwork.ArtworkProvider;
import org.opensilk.music.util.Projections;
import org.opensilk.music.loaders.SongCursorLoader;

import hugo.weaving.DebugLog;

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
    public static com.andrew.apollo.model.Song makeSongFromCursorOld(Cursor cursor) {
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
        return new com.andrew.apollo.model.Song(id, songName, artist, album, albumId, seconds);
    }

    public static Song makeSongFromCursor(Context context, Cursor cursor) {
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
        // find the album artist
        String albumArtist = getAlbumArtist(context, albumId);
        if (TextUtils.isEmpty(albumArtist)) albumArtist = artist;
        // Copy the duration
        final long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION));
        // Make the duration label
        final int seconds = (int) (duration / 1000);
        // get data uri
        final Uri dataUri = generateDataUri(id);
        // generate artwork uri
        final Uri artworkUri = generateArtworkUri(albumId);
        return new Song(String.valueOf(id), songName, album, artist, albumArtist, String.valueOf(albumId), seconds, dataUri, artworkUri);
    }

    public static Song makeSongFromRecentCursor(Cursor c) {
        final String identity = c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.IDENTITY));
        final String name = c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.NAME));
        final String albumName = c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.ALBUM_NAME));
        final String artistName = c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.ARTIST_NAME));
        final String albumArtistName = c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.ALBUM_ARTIST_NAME));
        final String albumIdentity = c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.ALBUM_IDENTITY));
        final int duration = c.getInt(c.getColumnIndexOrThrow(MusicStore.Cols.DURATION));
        final Uri dataUri = Uri.parse(c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.DATA_URI)));
        String artString = c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.ARTWORK_URI));
        final Uri artworkUri;
        if (artString == null) {
            String albumartist = albumArtistName != null ? albumArtistName : artistName;
            if (albumartist != null && albumName != null) {
                artworkUri = ArtworkProvider.createArtworkUri(albumartist, albumName);
            } else {
                artworkUri = null;
            }
        } else {
            artworkUri = Uri.parse(artString);
        }
        return new Song.Builder()
                .setIdentity(identity)
                .setName(name)
                .setAlbumName(albumName)
                .setArtistName(artistName)
                .setAlbumArtistName(albumArtistName)
                .setAlbumIdentity(albumIdentity)
                .setDuration(duration)
                .setDataUri(dataUri)
                .setArtworkUri(artworkUri)
                //TODO mime
                .build();
    }

    public static RecentSong makeRecentSongFromCursor(Cursor c) {
        final Song song = makeSongFromRecentCursor(c);
        final long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
        final boolean isLocal = c.getInt(c.getColumnIndexOrThrow(MusicStore.Cols.ISLOCAL)) == 1;
        final int playcount = c.getInt(c.getColumnIndexOrThrow(MusicStore.Cols.PLAYCOUNT));
        final long lastplayed = c.getLong(c.getColumnIndexOrThrow(MusicStore.Cols.LAST_PLAYED));
        return new RecentSong(song, id, isLocal, playcount, lastplayed);
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

    public static String getAlbumArtist(Context context, long albumId) {
        Cursor c = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{ MediaStore.Audio.Albums.ARTIST },
                MediaStore.Audio.Albums._ID + "=?",
                new String[]{ String.valueOf(albumId) },
                null);
        String artist = null;
        if (c != null) {
            if (c.moveToFirst()) {
                artist = c.getString(0);
            }
            c.close();
        }
        return  artist;
    }

    public static Uri generateDataUri(long songId) {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
    }

    public static Uri generateArtworkUri(long albumId) {
        return ContentUris.withAppendedId(ARTWORK_URI, albumId);
    }

}
