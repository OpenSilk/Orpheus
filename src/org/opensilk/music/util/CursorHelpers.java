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

import org.opensilk.music.GraphHolder;
import org.opensilk.music.MusicApp;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.AppPreferences;
import org.opensilk.silkdagger.DaggerInjector;

import java.util.Arrays;
import java.util.Collection;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 2/22/14.
 */
public class CursorHelpers {

    private static final long[] sEmptyList;
    private static final LocalSong[] sEmptySongList;
    private static final String sEmptyString;

    static {
        sEmptyList = new long[0];
        sEmptySongList = new LocalSong[0];
        sEmptyString = new String("");
    }

    private CursorHelpers() {
        // static
    }

    public static LocalSong makeLocalSongFromCursor(Context context, final Cursor c) {
        // Copy the song Id
        final long id = getLongOrZero(c, BaseColumns._ID);
        // Copy the song name
        final String songName = getStringOrEmpty(c, MediaStore.Audio.AudioColumns.TITLE);
        // Copy the artist name
        final String artist = getStringOrNull(c, MediaStore.Audio.AudioColumns.ARTIST);
        // Copy the album name
        final String album = getStringOrNull(c, MediaStore.Audio.AudioColumns.ALBUM);
        // Copy the album id
        final long albumId = getLongOrZero(c, MediaStore.Audio.AudioColumns.ALBUM_ID);
        // find the album artist
//        String albumArtist = null;// getAlbumArtist(context, albumId);
//        if (TextUtils.isEmpty(albumArtist)) albumArtist = artist;
        // Copy the duration
        final long duration = getLongOrZero(c, MediaStore.Audio.AudioColumns.DURATION);
        // Make the duration label
        final int seconds = (int) (duration > 0 ? (duration / 1000) : 0);
        // get data uri
        final Uri dataUri = generateDataUri(id);
        // generate artwork uri
        final Uri artworkUri = albumId > 0 ? generateArtworkUri(albumId) : null;
        // mime
        final String mimeType = getStringOrNull(c, MediaStore.Audio.AudioColumns.MIME_TYPE);
        return new LocalSong(id, songName, album, artist, /*albumArtist*/ null, albumId, seconds, dataUri, artworkUri, mimeType);
    }

    public static RecentSong makeRecentSongFromRecentCursor(final Cursor c) {
        final String identity = getStringOrEmpty(c, MusicStore.Cols.IDENTITY);
        final String name = getStringOrEmpty(c, MusicStore.Cols.NAME);
        final String albumName = getStringOrNull(c, MusicStore.Cols.ALBUM_NAME);
        final String artistName = getStringOrNull(c, MusicStore.Cols.ARTIST_NAME);
        final String albumArtistName = getStringOrNull(c, MusicStore.Cols.ALBUM_ARTIST_NAME);
        final String albumIdentity = getStringOrNull(c, MusicStore.Cols.ALBUM_IDENTITY);
        final int duration = getIntOrZero(c, MusicStore.Cols.DURATION);
        final Uri dataUri = Uri.parse(getStringOrEmpty(c, MusicStore.Cols.DATA_URI));
        String artString = getStringOrNull(c, MusicStore.Cols.ARTWORK_URI);
        final Uri artworkUri;
        if (TextUtils.isEmpty(artString)) {
            artworkUri = null;
        } else {
            artworkUri = Uri.parse(artString);
        }
        final String mimeType = getStringOrNull(c, MusicStore.Cols.MIME_TYPE);
        final long recentid = getLongOrZero(c, MusicStore.Cols._ID);
        final boolean isLocal = getIntOrZero(c, MusicStore.Cols.ISLOCAL) == 1;
        final int playcount = getIntOrZero(c, MusicStore.Cols.PLAYCOUNT);
        final long lastplayed = getLongOrZero(c, MusicStore.Cols.LAST_PLAYED);
        return new RecentSong(identity, name, albumName, artistName, albumArtistName, albumIdentity, duration,
                dataUri, artworkUri, mimeType, recentid, isLocal, playcount, lastplayed);
    }

    public static LocalAlbum makeLocalAlbumFromCursor(final Cursor c) {
        // Copy the album id
        final long id = getLongOrZero(c, BaseColumns._ID);
        // Copy the album name
        final String albumName = getStringOrEmpty(c, MediaStore.Audio.AlbumColumns.ALBUM);
        // Copy the artist name
        final String artist = getStringOrNull(c, MediaStore.Audio.AlbumColumns.ARTIST);
        // Copy the number of songs
        final int songCount = getIntOrZero(c, MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS);
        // Copy the release year
        String year = getStringOrNull(c, MediaStore.Audio.AlbumColumns.FIRST_YEAR);
        if (TextUtils.isEmpty(year)) {
            year = getStringOrNull(c, MediaStore.Audio.AlbumColumns.LAST_YEAR);
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
        final long id = getLongOrZero(c, BaseColumns._ID);
        // Copy the artist name
        final String artistName = getStringOrEmpty(c, MediaStore.Audio.ArtistColumns.ARTIST);
        // Copy the number of albums
        final int albumCount = getIntOrZero(c, MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS);
        // Copy the number of songs
        final int songCount = getIntOrZero(c, MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS);
        // Create a new artist
        return new LocalArtist(id, artistName, albumCount, songCount);
    }

    public static Genre makeGenreFromCursor(final Cursor c) {
        final long id = getLongOrZero(c, MusicStore.GroupCols._ID);
        final String name= getStringOrEmpty(c, MusicStore.GroupCols.NAME);
        final int songNumber = getIntOrZero(c, MusicStore.GroupCols.SONG_COUNT);
        final int albumNumber = getIntOrZero(c, MusicStore.GroupCols.ALBUM_COUNT);
        final long[] songs = fromCsv(getStringOrNull(c, MusicStore.GroupCols.SONG_IDS));
        final long[] albums = fromCsv(getStringOrNull(c, MusicStore.GroupCols.ALBUM_IDS));
        return new Genre(id, name, songNumber, albumNumber, songs, albums);
    }

    public static Playlist makePlaylistFromCursor(final Cursor c) {
        final long id = getLongOrZero(c, MusicStore.GroupCols._ID);
        final String name= getStringOrEmpty(c, MusicStore.GroupCols.NAME);
        final int songNumber = getIntOrZero(c, MusicStore.GroupCols.SONG_COUNT);
        final int albumNumber = getIntOrZero(c, MusicStore.GroupCols.ALBUM_COUNT);
        final long[] songs = fromCsv(getStringOrNull(c, MusicStore.GroupCols.SONG_IDS));
        final long[] albums = fromCsv(getStringOrNull(c, MusicStore.GroupCols.ALBUM_IDS));
        return new Playlist(id, name, songNumber, albumNumber, songs, albums);
    }

    public static Cursor makePlaylistCursor(Context context) {
        return context.getContentResolver().query(
                Uris.EXTERNAL_MEDIASTORE_PLAYLISTS,
                Projections.PLAYLIST,
                Selections.PLAYLIST,
                SelectionArgs.PLAYLIST,
                MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
    }

    public static Cursor makePlaylistMembersCursor(Context context, long playlistid) {
        return context.getContentResolver().query(
                Uris.PLAYLIST(playlistid),
                Projections.PLAYLIST_MEMBER,
                Selections.PLAYLIST_MEMBER,
                SelectionArgs.PLAYLIST_MEMBER,
                SortOrder.PLAYLIST_SONGS);
    }

    public static Cursor makeLastAddedCursor(final Context context) {
        return context.getContentResolver().query(Uris.EXTERNAL_MEDIASTORE_MEDIA,
                Projections.LOCAL_SONG,
                Selections.LAST_ADDED,
                SelectionArgs.LAST_ADDED(),
                SortOrder.LAST_ADDED);
    }

    public static Cursor makeGenreCursor(Context context) {
        return context.getContentResolver().query(
                Uris.EXTERNAL_MEDIASTORE_GENRES,
                Projections.GENRE,
                Selections.GENRE,
                SelectionArgs.GENRE,
                MediaStore.Audio.Genres.DEFAULT_SORT_ORDER);
    }

    public static Cursor makeGenreMembersCursor(Context context, long genereId) {
        return context.getContentResolver().query(
                Uris.GENRE(genereId),
                Projections.GENRE_MEMBER,
                Selections.GENRE_MEMBER,
                SelectionArgs.GENRE_MEMBER,
                MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER);
    }

    public static Cursor makeSongCursor(final Context context) {
        AppPreferences settings = GraphHolder.get(context).getObj(AppPreferences.class);
        final String sortOrder = settings.getString(AppPreferences.SONG_SORT_ORDER,
                com.andrew.apollo.utils.SortOrder.SongSortOrder.SONG_A_Z);
        return context.getContentResolver().query(Uris.EXTERNAL_MEDIASTORE_MEDIA,
                Projections.LOCAL_SONG,
                Selections.LOCAL_SONG,
                SelectionArgs.LOCAL_SONG,
                sortOrder);
    }

    public static Cursor makeSingleLocalSongCursor(final Context context, long id) {
        return context.getContentResolver().query(Uris.EXTERNAL_MEDIASTORE_MEDIA,
                Projections.LOCAL_SONG,
                Selections.LOCAL_SONG + " AND " + BaseColumns._ID + "=" + String.valueOf(id),
                SelectionArgs.LOCAL_SONG,
                null);
    }

    public static String getAlbumArtist(Context context, long albumId) {
        Cursor c = context.getContentResolver().query(Uris.EXTERNAL_MEDIASTORE_ALBUMS,
                new String[]{ MediaStore.Audio.AlbumColumns.ARTIST },
                BaseColumns._ID + "=?",
                new String[]{ String.valueOf(albumId) },
                null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    return getStringOrNull(c, MediaStore.Audio.AlbumColumns.ARTIST);
                }
            } finally {
                c.close();
            }
        }
        return null;
    }

    public static Cursor getCursorForAutoShuffle(Context context) {
        String selection = Selections.LOCAL_SONG;
        AppPreferences settings = GraphHolder.get(context).getObj(AppPreferences.class);
        String deffldr = settings.getString(AppPreferences.PREF_DEFAULT_MEDIA_FOLDER, null);
        if (!TextUtils.isEmpty(deffldr)) {
            selection += " AND " + MediaStore.Audio.AudioColumns.DATA + " like '" + deffldr + "%'";
        }
        return context.getContentResolver().query(
                Uris.EXTERNAL_MEDIASTORE_MEDIA,
                Projections.ID_ONLY,
                selection,
                SelectionArgs.LOCAL_SONG,
                BaseColumns._ID);
    }

    public static Cursor makeArtistSongsCursor(Context context, long artistId) {
        return context.getContentResolver().query(
                Uris.EXTERNAL_MEDIASTORE_MEDIA,
                Projections.LOCAL_SONG,
                Selections.LOCAL_SONG + " AND " + MediaStore.Audio.AudioColumns.ARTIST_ID + "=" + String.valueOf(artistId),
                SelectionArgs.LOCAL_SONG,
                SortOrder.LOCAL_ARTIST_SONGS);
    }

    public static Cursor makeLocalAlbumsCursor(Context context, long[] albumIds) {
        return context.getContentResolver().query(
                Uris.EXTERNAL_MEDIASTORE_ALBUMS,
                Projections.LOCAL_ALBUM,
                Selections.LOCAL_ALBUM + " AND " + Selections.LOCAL_ALBUMS(albumIds),
                SelectionArgs.LOCAL_ALBUM,
                MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
    }

    public static ArtInfo makeArtInfoFromLocalAlbumCursor(final Cursor c) {
        return new ArtInfo(getStringOrNull(c, MediaStore.Audio.AlbumColumns.ARTIST),
                getStringOrNull(c, MediaStore.Audio.AlbumColumns.ALBUM),
                generateArtworkUri(getLongOrZero(c, BaseColumns._ID)));
    }

    public static Cursor makeLocalArtistAlbumsCursor(Context context, long artistId) {
        return context.getContentResolver().query(
                Uris.EXTERNAL_MEDIASTORE_ARTISTS_ALBUMS(artistId),
                Projections.LOCAL_ALBUM,
                Selections.LOCAL_ALBUM,
                SelectionArgs.LOCAL_ALBUM,
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
            try {
                if (c.moveToFirst()) {
                    int ii=0;
                    do {
                        list[ii++] = makeLocalSongFromCursor(context, c);
                    } while (c.moveToNext());
                }
                return list;
            } finally {
                c.close();
            }
        }
        return sEmptySongList;
    }

    public static long[] getSongIdsForCursor(final Cursor c) {
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int colidx = -1;
                    try {
                        colidx = c.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
                    } catch (IllegalArgumentException e) {
                        try {
                            colidx = c.getColumnIndexOrThrow(BaseColumns._ID);
                        } catch (IllegalArgumentException e1) {
                            return sEmptyList;
                        }
                    }
                    long[] list = new long[c.getCount()];
                    int ii = 0;
                    do {
                       list[ii++] = c.getLong(colidx);
                    } while (c.moveToNext());
                    return list;
                }
            } finally {
                c.close();
            }
        }
        return sEmptyList;
    }

    public static long[] getSongIdsForAlbum(Context context, long albumid) {
        Cursor cursor = context.getContentResolver().query(
                Uris.EXTERNAL_MEDIASTORE_MEDIA,
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
        return sEmptyList;
    }

    public static LocalSong[] getLocalSongListForAlbum(final Context context, final long id) {
        Cursor cursor = context.getContentResolver().query(
                Uris.LOCAL_ALBUM_SONGS,
                Projections.LOCAL_SONG,
                Selections.LOCAL_ALBUM_SONGS,
                SelectionArgs.LOCAL_ALBUM_SONGS(id),
                SortOrder.LOCAL_ALBUM_SONGS);
        if (cursor != null) {
            try {
                LocalSong[] songs = new LocalSong[cursor.getCount()];
                if (cursor.moveToFirst()) {
                    int ii=0;
                    do {
                        songs[ii++] = makeLocalSongFromCursor(context, cursor);
                    } while (cursor.moveToNext());
                }
                return songs;
            } finally {
                cursor.close();
            }
        }
        return sEmptySongList;
    }

    public static LocalSong[] getLocalSongListForPlaylist(Context context, long playlistId) {
        Cursor cursor = context.getContentResolver().query(
                Uris.PLAYLIST(playlistId),
                Projections.PLAYLIST_SONGS,
                Selections.PLAYLIST_SONGS,
                SelectionArgs.PLAYLIST_SONGS,
                SortOrder.PLAYLIST_SONGS);
        if (cursor != null) {
            try {
                final LocalSong[] list = new LocalSong[cursor.getCount()];
                if (cursor.moveToFirst()) {
                    int ii=0;
                    do {
                        list[ii++] = makeLocalSongFromCursor(context, cursor);
                    } while (cursor.moveToNext());
                }
                return list;
            } finally {
                cursor.close();
            }
        }
        return sEmptySongList;
    }

    public static LocalSong[] getLocalSongListForLastAdded(Context context) {
        final Cursor cursor = makeLastAddedCursor(context);
        if (cursor != null) {
            try {
                final LocalSong[] list = new LocalSong[cursor.getCount()];
                if (cursor.moveToFirst()) {
                    int ii=0;
                    do {
                        list[ii++] = makeLocalSongFromCursor(context, cursor);
                    } while (cursor.moveToNext());
                }
                return list;
            } finally {
                cursor.close();
            }
        }
        return sEmptySongList;
    }

    public static Uri generateDataUri(long songId) {
        return ContentUris.withAppendedId(Uris.EXTERNAL_MEDIASTORE_MEDIA, songId);
    }

    public static Uri generateArtworkUri(long albumId) {
        return ContentUris.withAppendedId(Uris.ARTWORK_URI, albumId);
    }

    public static String getStringOrEmpty(Cursor c, String col) {
        try {
            return c.getString(c.getColumnIndexOrThrow(col));
        } catch (IllegalArgumentException e) {
            Timber.e(e, "getStringOrEmpty("+col+")");
            return sEmptyString;
        }
    }

    public static String getStringOrNull(Cursor c, String col) {
        try {
            return c.getString(c.getColumnIndexOrThrow(col));
        } catch (IllegalArgumentException e) {
            Timber.e(e, "getStringOrNull("+col+")");
            return null;
        }
    }

    public static long getLongOrZero(Cursor c, String col) {
        try {
            return c.getLong(c.getColumnIndexOrThrow(col));
        } catch (IllegalArgumentException e) {
            Timber.e(e, "getLongOrZero("+col+")");
            return 0;
        }
    }

    public static int getIntOrZero(Cursor c, String col) {
        try {
            return c.getInt(c.getColumnIndexOrThrow(col));
        } catch (IllegalArgumentException e) {
            Timber.e(e, "getIntOrZero("+col+")");
            return 0;
        }
    }

    static long[] fromCsv(String csv) {
        if (TextUtils.isEmpty(csv)) {
            return sEmptyList;
        }
        final String[] strings = csv.split(",");
        if (strings == null || strings.length == 0) {
            return sEmptyList;
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

    public static String toCsv(Collection<String> collection) {
        if (collection == null || collection.size() == 0) {
            return sEmptyString;
        }
        StringBuilder songsIdsCsv = new StringBuilder();
        int ii = collection.size();
        for (String s : collection) {
            songsIdsCsv.append(s);
            if (ii --> 1) {
                songsIdsCsv.append(",");
            }
        }
        return songsIdsCsv.toString();
    }

}
