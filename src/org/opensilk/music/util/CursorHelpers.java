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

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.model.RecentSong;
import com.andrew.apollo.provider.MusicProvider;
import com.andrew.apollo.provider.MusicStore;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.MusicApp;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.AppPreferences;
import org.opensilk.silkdagger.DaggerInjector;

import java.util.Arrays;

/**
 * Created by drew on 2/22/14.
 */
public class CursorHelpers {

    private CursorHelpers() {
        // static
    }

    /**
     * Creates a Song object from a cursor created with makeSongCursor
     * @param cursor cursor created with makeSongCursor
     * @return new Song object
     */
    public static LocalSong makeLocalSongFromCursor(Context context, Cursor cursor) {
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
        String albumArtist = null;// getAlbumArtist(context, albumId);
//        if (TextUtils.isEmpty(albumArtist)) albumArtist = artist;
        // Copy the duration
        final long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION));
        // Make the duration label
        final int seconds = (int) (duration / 1000);
        // get data uri
        final Uri dataUri = generateDataUri(id);
        // generate artwork uri
        final Uri artworkUri = generateArtworkUri(albumId);
        // mime
        final String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE));
        return new LocalSong(id, songName, album, artist, albumArtist, albumId, seconds, dataUri, artworkUri, mimeType);
    }

    public static RecentSong makeRecentSongFromRecentCursor(Cursor c) {
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
        if (TextUtils.isEmpty(artString)) {
            artworkUri = null;
        } else {
            artworkUri = Uri.parse(artString);
        }
        final String mimeType = c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.MIME_TYPE));
        final long recentid = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
        final boolean isLocal = c.getInt(c.getColumnIndexOrThrow(MusicStore.Cols.ISLOCAL)) == 1;
        final int playcount = c.getInt(c.getColumnIndexOrThrow(MusicStore.Cols.PLAYCOUNT));
        final long lastplayed = c.getLong(c.getColumnIndexOrThrow(MusicStore.Cols.LAST_PLAYED));
        return new RecentSong(identity, name, albumName, artistName, albumArtistName, albumIdentity, duration,
                dataUri, artworkUri, mimeType, recentid, isLocal, playcount, lastplayed);
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
        final Uri artworkUri = generateArtworkUri(Long.decode(id));
        // Create a new album
        return new Album(id, albumName, artist, songCount, year, artworkUri);
    }

    public static LocalAlbum makeLocalAlbumFromCursor(final Cursor c) {
        // Copy the album id
        final long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
        // Copy the album name
        final String albumName = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ALBUM));
        // Copy the artist name
        final String artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ARTIST));
        // Copy the number of songs
        final int songCount = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS));
        // Copy the release year
        String year = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.FIRST_YEAR));
        if (TextUtils.isEmpty(year)) {
            year = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.LAST_YEAR));
        }
        // generate artwork Uri
        final Uri artworkUri = generateArtworkUri(id);
        // Create a new album
        return new LocalAlbum(id, albumName, artist, songCount, year, artworkUri);
    }

    /**
     * Create artist from cusor
     * @param c cursor created with makeArtistCursor
     * @return new artist
     */
    public static LocalArtist makeLocalArtistFromCursor(final Cursor c) {
        // Copy the artist id
        final long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
        // Copy the artist name
        final String artistName = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.ARTIST));
        // Copy the number of albums
        final int albumCount = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS));
        // Copy the number of songs
        final int songCount = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS));
        // Create a new artist
        return new LocalArtist(id, artistName, albumCount, songCount);
    }

    public static Genre makeGenreFromCursor(final Cursor c) {
        final long id = c.getLong(c.getColumnIndexOrThrow(MusicStore.GroupCols._ID));
        final String name= c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.NAME));
        final int songNumber = c.getInt(c.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_COUNT));
        final int albumNumber = c.getInt(c.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_COUNT));
        final long[] songs = fromCsv(c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_IDS)));
        final long[] albums = fromCsv(c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_IDS)));
        return new Genre(id, name, songNumber, albumNumber, songs, albums);
    }

    public static Playlist makePlaylistFromCursor(final Cursor c) {
        final long id = c.getLong(c.getColumnIndexOrThrow(MusicStore.GroupCols._ID));
        final String name= c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.NAME));
        final int songNumber = c.getInt(c.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_COUNT));
        final int albumNumber = c.getInt(c.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_COUNT));
        final long[] songs = fromCsv(c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_IDS)));
        final long[] albums = fromCsv(c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_IDS)));
        return new Playlist(id, name, songNumber, albumNumber, songs, albums);
    }

    public static Cursor makeLastAddedCursor(final Context context) {
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Projections.LOCAL_SONG,
                Selections.LAST_ADDED,
                SelectionArgs.LAST_ADDED(),
                MediaStore.Audio.Media.DATE_ADDED + " DESC");
    }

    public static Cursor makeSongCursor(final Context context) {
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Projections.LOCAL_SONG,
                Selections.LOCAL_SONG,
                SelectionArgs.LOCAL_SONG,
                PreferenceUtils.getInstance(context).getSongSortOrder());
    }

    public static Cursor makeSingleLocalSongCursor(final Context context, long id) {
        String [] selectionArgs = Arrays.copyOf(SelectionArgs.LOCAL_SONG, SelectionArgs.LOCAL_SONG.length+1);
        selectionArgs[selectionArgs.length-1] = String.valueOf(id);
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Projections.LOCAL_SONG,
                Selections.LOCAL_SONG + " AND " + MediaStore.Audio.Media._ID + "=?",
                selectionArgs,
                null);
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
        return artist;
    }

    public static Cursor getCursorForAutoShuffle(Context context) {
        String selection = Selections.LOCAL_SONG;
        AppPreferences prefs = ((DaggerInjector) context.getApplicationContext()).getObjectGraph().get(AppPreferences.class);
        String deffldr = prefs.getString(AppPreferences.PREF_DEFAULT_MEDIA_FOLDER, null);
        if (!TextUtils.isEmpty(deffldr)) {
            selection += " AND " + MediaStore.Audio.Media.DATA + " like '" + deffldr + "%'";
        }
        return context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{BaseColumns._ID},
                selection,
                SelectionArgs.LOCAL_SONG,
                null);
    }

    public static Cursor makeArtistSongsCursor(Context context, long artistId) {
        return context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Projections.LOCAL_SONG,
                Selections.LOCAL_SONG + " AND " + MediaStore.Audio.Media.ARTIST_ID + "=?",
                new String[]{SelectionArgs.LOCAL_SONG[0], SelectionArgs.LOCAL_SONG[1], String.valueOf(artistId)},
                PreferenceUtils.getInstance(context).getArtistSongSortOrder());
    }

    public static Cursor makeLocalAlbumsCursor(Context context, long[] albumIds) {
        return context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                Projections.LOCAL_ALBUM,
                Selections.LOCAL_ALBUMS(albumIds),
                null,
                MediaStore.Audio.Albums.ALBUM_KEY);
    }

    public static ArtInfo makeArtInfoFromLocalAlbumCursor(Cursor c) {
        return new ArtInfo(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)),
                c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)),
                generateArtworkUri(c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))));
    }

    public static Cursor makeLocalArtistAlbumsCursor(Context context, long artistId) {
        return context.getContentResolver().query(MediaStore.Audio.Artists.Albums.getContentUri("external", artistId),
                Projections.LOCAL_ALBUM,
                null, null,
                MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
    }

    public static LocalSong[] makeLocalSongList(Context context, Uri uri, String[] projection,
                                                String selection, String[] selectionArgs, String sortOrder) {
        Cursor c = context.getContentResolver().query(uri,
                projection,
                selection,
                selectionArgs,
                sortOrder);
        if (c != null) {
            LocalSong[] list = new LocalSong[c.getCount()];
            if (c.moveToFirst()) {
                int ii=0;
                do {
                    list[ii++] = makeLocalSongFromCursor(context, c);
                } while (c.moveToNext());
            }
            c.close();
            return list;
        }
        return new LocalSong[0];
    }

    public static long[] getSongIdsForCursor(Cursor cursor) {
        if (cursor == null) {
            return new long[0];
        }
        final int len = cursor.getCount();
        final long[] list = new long[len];
        cursor.moveToFirst();
        int columnIndex = -1;
        try {
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
        } catch (final IllegalArgumentException notaplaylist) {
            columnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        }
        for (int i = 0; i < len; i++) {
            list[i] = cursor.getLong(columnIndex);
            cursor.moveToNext();
        }
        cursor.close();
        cursor = null;
        return list;
    }

    public static long[] getSongIdsForAlbum(Context context, long albumid) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Projections.ID_ONLY,
                Selections.LOCAL_ALBUM_SONGS,
                SelectionArgs.LOCAL_ALBUM_SONGS(albumid),
                SortOrder.LOCAL_ALBUM_SONGS);
        if (cursor != null) {
            final long[] mList = getSongIdsForCursor(cursor);
            cursor.close();
            cursor = null;
            return mList;
        }
        return new long[0];
    }

    public static LocalSong[] getLocalSongListForAlbum(final Context context, final long id) {
        Cursor cursor = context.getContentResolver().query(
                Uris.LOCAL_ALBUM_SONGS,
                Projections.LOCAL_SONG,
                Selections.LOCAL_ALBUM_SONGS,
                SelectionArgs.LOCAL_ALBUM_SONGS(id),
                SortOrder.LOCAL_ALBUM_SONGS);
        if (cursor != null) {
            LocalSong[] songs = new LocalSong[cursor.getCount()];
            if (cursor.moveToFirst()) {
                int ii=0;
                do {
                    songs[ii++] = CursorHelpers.makeLocalSongFromCursor(context, cursor);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return songs;
        }
        return new LocalSong[0];
    }

    public static Uri generateDataUri(long songId) {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
    }

    public static Uri generateArtworkUri(long albumId) {
        return ContentUris.withAppendedId(Uris.ARTWORK_URI, albumId);
    }

    static long[] fromCsv(String csv) {
        if (csv == null) {
            return new long[0];
        }
        final String[] strings = csv.split(",");
        if (strings == null || strings.length == 0) {
            return new long[0];
        }
        final long[] ids = new long[strings.length];
        for (int ii=0; ii< strings.length; ii++) {
            try {
                ids[ii] = Long.valueOf(strings[ii]);
            } catch (NumberFormatException e) {
                ids[ii] = -1;
            }
        }
        return ids;
    }

}
