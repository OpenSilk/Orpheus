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

package com.andrew.apollo.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import org.opensilk.music.BuildConfig;
import org.opensilk.music.R;

import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.PriorityAsyncTask;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dagger.ObjectGraph;
import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 2/24/14.
 */
public class MusicProvider extends ContentProvider {

    private static final String AUTHORITY = BuildConfig.MUSIC_AUTHORITY;
    private static final UriMatcher sUriMatcher;

    /** Uri for recents store */
    public static final Uri RECENTS_URI;
    /** Wrapper uri to query genres */
    public static final Uri GENRES_URI;
    /** Wrapper uri for playlists */
    public static final Uri PLAYLIST_URI;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        RECENTS_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("recents").build();
        sUriMatcher.addURI(AUTHORITY, "recents", 1);

        GENRES_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("genres").build();
        sUriMatcher.addURI(AUTHORITY, "genres", 2);

        // Genre albums
        sUriMatcher.addURI(AUTHORITY, "genre/#/albums", 3);

        PLAYLIST_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("playlists").build();
        sUriMatcher.addURI(AUTHORITY, "playlists", 4);
    }

    /** Generate a uri to query albums in a genre */
    public static Uri makeGenreAlbumsUri(long genreId) {
        return new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("genre")
                .appendPath(String.valueOf(genreId)).appendPath("albums").build();
    }

    private ObjectGraph mObjectGraph;
    private MusicStore mStore;

    @Override
    public boolean onCreate() {
        mObjectGraph = ObjectGraph.create(new ProviderModule(getContext()));
        mStore = mObjectGraph.get(MusicStore.class);
        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        //Background tasks will fail without
        PriorityAsyncTask.init();
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        return super.bulkInsert(uri, values);
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor c = null;
        SQLiteDatabase db;
        switch (sUriMatcher.match(uri)) {
            case 1: // Recents
                db = getMusicStoreDatabase(true);
                if (db != null) {
                    c = db.query(MusicStore.RECENT_TABLE,
                            projection, selection, selectionArgs, null, null, sortOrder);
//                    db.close();
                }
                break;
            case 2: // Genres
                db = getMusicStoreDatabase(true);
                if (db != null) {
                    c = db.query(MusicStore.GENRE_TABLE,
                            Projections.CACHED_GROUP, null, null,
                            null, null, MusicStore.GroupCols.NAME);
                    if (c != null && c.getCount() > 0) {
                        // cache hit
                        // schedule an update
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                updateGenreCache();
                            }
                        };
                        PriorityAsyncTask.execute(r, PriorityAsyncTask.Priority.LOW);
                        break;
                    }
                    if (c != null) {
                        c.close();
                    }
                }

                // pull genres, this query is very expensive
                c = makeGenreMatrixCursor();
                // to avoid blocking any further schedule
                // a cache update in the background
                // it will requery but that is preferable to
                // blocking while the cache is updated
                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        updateGenreCache();
                    }
                };
                PriorityAsyncTask.execute(r1, PriorityAsyncTask.Priority.LOW);;

                break;
            case 3: // Genre albums
                // Extract our genre id
                List<String> pathSegments = uri.getPathSegments();
                long genreId = Integer.valueOf(pathSegments.get(pathSegments.size() - 2));

                // pull song list for genres
                final Cursor genreSongs = getContext().getContentResolver().query(
                        MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                        new String[] {MediaStore.Audio.AudioColumns.ALBUM_ID },
                        Selections.LOCAL_SONG,
                        SelectionArgs.LOCAL_SONG,
                        MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER);

                // loop the songs and filter all the unique album ids
                final HashSet<String> albumIdsSet = new HashSet<String>();
                if (genreSongs != null && genreSongs.moveToFirst()) {
                    do {
                        albumIdsSet.add(genreSongs.getString(0));
                    } while (genreSongs.moveToNext());
                }
                if (genreSongs != null) {
                    genreSongs.close();
                }

                // Build our album selection
                final StringBuilder albumSelection = new StringBuilder();
                albumSelection.append(BaseColumns._ID + " IN (");
                int setSize = albumIdsSet.size();
                for (String albumId: albumIdsSet) {
                    albumSelection.append(albumId);
                    if (setSize-->1) {
                        albumSelection.append(",");
                    }
                }
                albumSelection.append(")");

                // And finally create the return cursor;
                c = getContext().getContentResolver().query(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        Projections.LOCAL_ALBUM,
                        albumSelection.toString(),
                        null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);

                if (c != null) {
                    // Set the notification uri on the albums uri not on our proxy uri
                    c.setNotificationUri(getContext().getContentResolver(),
                            MediaStore.Audio.Genres.Members.getContentUri("external", genreId));
                }
                return c;
            case 4: //Playlists
                db = getMusicStoreDatabase(true);
                if (db != null) {
                    c = db.query(MusicStore.PLAYLIST_TABLE,
                            Projections.CACHED_GROUP, null, null,
                            null, null, MusicStore.GroupCols.NAME);
                    if (c != null && c.getCount() > 0) {
                        // cache hit
                        // schedule an update
                        Runnable r2 = new Runnable() {
                            @Override
                            public void run() {
                                updatePlaylistCache();
                            }
                        };
                        PriorityAsyncTask.execute(r2, PriorityAsyncTask.Priority.LOW);
                        break;
                    }
                    if (c != null) {
                        c.close();
                    }
                }

                // pull playlists, this query is very expensive
                c = makePlaylistMatrixCursor();
                // to avoid blocking any further schedule
                // a cache update in the background
                // it will requery but that is preferable to
                // blocking while the cache is updated
                Runnable r3 = new Runnable() {
                    @Override
                    public void run() {
                        updatePlaylistCache();
                    }
                };
                PriorityAsyncTask.execute(r3, PriorityAsyncTask.Priority.LOW);
                break;
        }
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    protected SQLiteDatabase getMusicStoreDatabase(boolean tryReadonly) {
        SQLiteDatabase db = null;
        try {
            db = mStore.getWritableDatabase();
        } catch (SQLiteException e) {
            Timber.e(e, "Unable to get writable MusicStore database.");
            db = null;
            if (tryReadonly) {
                try {
                    db = mStore.getReadableDatabase();
                } catch (SQLiteException e1) {
                    Timber.e(e1, "Unable to get readonly MusicStore database");
                    db = null;
                }
            }
        }
        return db;
    }

    protected void updatePlaylistCache() {
        updateGroupCacheCache(makePlaylistMatrixCursor(), MusicStore.PLAYLIST_TABLE, PLAYLIST_URI);
    }

    @DebugLog
    protected MatrixCursor makePlaylistMatrixCursor() {
        MatrixCursor c = new MatrixCursor(Projections.CACHED_GROUP);
        //last added first
        // Get the song count
        final Cursor lastAdded = CursorHelpers.makeLastAddedCursor(getContext());
        int lastAddedSongNum = 0;
        int lastAddedAlbumNum = 0;
        String lastAddedSongids = null;
        String lastAddedAlbumids = null;
        if (lastAdded != null) {
            List<String> songs = new ArrayList<>(lastAdded.getCount());
            Set<String> albumidSet = new HashSet<>(lastAdded.getCount());
            if (lastAdded.getCount() > 0 && lastAdded.moveToFirst()) {
                do {
                    final String sId = CursorHelpers.getStringOrNull(lastAdded, BaseColumns._ID);
                    if (!TextUtils.isEmpty(sId)) {
                        songs.add(sId);
                    }
                    final String aId = CursorHelpers.getStringOrNull(lastAdded, MediaStore.Audio.AudioColumns.ALBUM_ID);
                    if (!TextUtils.isEmpty(aId)) {
                        albumidSet.add(aId);
                    }
                } while (lastAdded.moveToNext());
            }
            lastAdded.close();
            lastAddedSongNum = songs.size();
            if (lastAddedSongNum > 0) {
                Collections.sort(songs);
                lastAddedSongids = CursorHelpers.toCsv(songs);
            }
            lastAddedAlbumNum = albumidSet.size();
            if (lastAddedAlbumNum > 0) {
                List<String> albums = new ArrayList<>(albumidSet);
                Collections.sort(albums);
                lastAddedAlbumids = CursorHelpers.toCsv(albums);
            }
        }
        // if there are songs add the lastadded playlist
        if (lastAddedSongNum > 0) {
            c.addRow(new Object[]{
                    -2,
                    getContext().getResources().getString(R.string.playlist_last_added),
                    lastAddedSongNum,
                    lastAddedAlbumNum,
                    lastAddedSongids,
                    lastAddedAlbumids,
            });
        }
        // User playlists next
        // Pull all playlists
        Cursor playlists = CursorHelpers.makePlaylistCursor(getContext());
        if (playlists != null && playlists.getCount() > 0 && playlists.moveToFirst()) {
            do {
                // get playlist id
                final long id = CursorHelpers.getLongOrZero(playlists, BaseColumns._ID);
                // get playlist name
                final String name = CursorHelpers.getStringOrEmpty(playlists, MediaStore.Audio.Playlists.NAME);
                // We have to query for the song count
                final Cursor playlistSongs = CursorHelpers.makePlaylistMembersCursor(getContext(), id);
                int numSongs = 0;
                int numAlbums = 0;
                String songids = null;
                String albumids = null;
                if (playlistSongs != null) {
                    List<String> songs = new ArrayList<>(playlistSongs.getCount());
                    Set<String> albumsSet = new HashSet<>(playlistSongs.getCount());
                    if (playlistSongs.getCount() > 0 && playlistSongs.moveToFirst()) {
                        do {
                            final String sId = CursorHelpers.getStringOrNull(playlistSongs, MediaStore.Audio.Playlists.Members.AUDIO_ID);
                            if (!TextUtils.isEmpty(sId)) {
                                songs.add(sId);
                            }
                            final String aId = CursorHelpers.getStringOrNull(playlistSongs, MediaStore.Audio.Playlists.Members.ALBUM_ID);
                            if (!TextUtils.isEmpty(aId)) {
                                albumsSet.add(aId);
                            }
                        } while (playlistSongs.moveToNext());
                    }
                    playlistSongs.close();
                    numSongs = songs.size();
                    if (numSongs > 0) {
                        Collections.sort(songs);
                        songids = CursorHelpers.toCsv(songs);
                    }
                    numAlbums = albumsSet.size();
                    if (numAlbums > 0) {
                        List<String> albums = new ArrayList<>(albumsSet);
                        Collections.sort(albums);
                        albumids = CursorHelpers.toCsv(albums);
                    }
                }
                c.addRow(new Object[] {id, name, numSongs, numAlbums, songids, albumids});
            } while (playlists.moveToNext());
        }
        if (playlists != null) {
            playlists.close();
        }
        return c;
    }

    @DebugLog
    protected synchronized void updateGroupCacheCache(Cursor c, String tableName, Uri updateUri) {
        boolean wasupdated = false;
        // Add new
        if (c != null) {
            SQLiteDatabase db = getMusicStoreDatabase(false);
            if (db == null) {
                c.close();
                return;
            }
            if (c.moveToFirst()) {
                do {
                    long id = CursorHelpers.getLongOrZero(c, MusicStore.GroupCols._ID);
                    String name = CursorHelpers.getStringOrEmpty(c, MusicStore.GroupCols.NAME);
                    int songCount = CursorHelpers.getIntOrZero(c, MusicStore.GroupCols.SONG_COUNT);
                    int albumCount = CursorHelpers.getIntOrZero(c, MusicStore.GroupCols.ALBUM_COUNT);
                    String songids = CursorHelpers.getStringOrNull(c, MusicStore.GroupCols.SONG_IDS);
                    String albumids = CursorHelpers.getStringOrNull(c, MusicStore.GroupCols.ALBUM_IDS);
                    // create content values
                    ContentValues values = new ContentValues(6);
                    values.put(MusicStore.GroupCols.NAME, name);
                    values.put(MusicStore.GroupCols.SONG_COUNT, songCount);
                    values.put(MusicStore.GroupCols.ALBUM_COUNT, albumCount);
                    values.put(MusicStore.GroupCols.SONG_IDS, songids);
                    values.put(MusicStore.GroupCols.ALBUM_IDS, albumids);
                    // check if its already there and update if needed
                    Cursor c2 = db.query(tableName,
                            Projections.CACHED_GROUP,
                            MusicStore.GroupCols._ID + "=?",
                            new String[]{String.valueOf(id)},
                            null, null, null, null);
                    if (c2 != null) {
                        try {
                            if (c2.getCount() > 0) {
                                if (c2.moveToFirst()) {
                                    if (TextUtils.equals(name, CursorHelpers.getStringOrNull(c2, MusicStore.GroupCols.NAME))
                                            && songCount == CursorHelpers.getIntOrZero(c2, MusicStore.GroupCols.SONG_COUNT)
                                            && albumCount == CursorHelpers.getIntOrZero(c2, MusicStore.GroupCols.ALBUM_COUNT)
                                            && TextUtils.equals(songids, CursorHelpers.getStringOrNull(c2 ,MusicStore.GroupCols.SONG_IDS))
                                            && TextUtils.equals(albumids, CursorHelpers.getStringOrNull(c2, MusicStore.GroupCols.ALBUM_IDS))) {
                                        continue;
                                    }
                                }
                                db.update(tableName, values,
                                        MusicStore.GroupCols._ID + "=?",
                                        new String[]{String.valueOf(id)});
                                Timber.v("Updated " +name+" id="+id + " in table " + tableName);
                                wasupdated = true;
                                continue;
                            }
                        } finally {
                            c2.close();
                        }
                    }
                    values.put(MusicStore.GroupCols._ID, id);
                    db.insert(tableName, null, values);
                    wasupdated = true;
                } while (c.moveToNext());
            }
            // remove old
            Cursor c2 = db.query(tableName,
                    Projections.CACHED_GROUP,
                    null, null, null, null, MusicStore.GroupCols._ID, null);
            if (c2 != null) {
                if (c2.moveToFirst()) {
                    do {
                        boolean found = false;
                        long id = CursorHelpers.getLongOrZero(c2, BaseColumns._ID);
                        if (c.moveToFirst()) {
                            do {
                                long id2 = CursorHelpers.getLongOrZero(c, BaseColumns._ID);
                                if (id == id2) {
                                    found = true;
                                    break;
                                }
                            } while (c.moveToNext());
                        }
                        if (!found) {
                            Timber.v("Removing " + CursorHelpers.getStringOrNull(c2, MusicStore.GroupCols.NAME) + " from table " + tableName);
                            db.delete(tableName,
                                    BaseColumns._ID + "=?",
                                    new String[]{String.valueOf(id)});
                            wasupdated = true;
                        }
                    } while (c2.moveToNext());
                }
                c2.close();
            }
            db.close();
            c.close();
        }
        if (wasupdated) {
            getContext().getContentResolver().notifyChange(updateUri, null);
        }
    }

    @DebugLog
    protected void updateGenreCache() {
        updateGroupCacheCache(makeGenreMatrixCursor(), MusicStore.GENRE_TABLE, GENRES_URI);
    }

    @DebugLog
    protected MatrixCursor makeGenreMatrixCursor() {
        MatrixCursor c = null;
        // Pull list of all genres
        Cursor genres = CursorHelpers.makeGenreCursor(getContext());
        // Build our custom cursor
        if (genres != null && genres.getCount() > 0 && genres.moveToFirst()) {
            c = new MatrixCursor(Projections.CACHED_GROUP);
            do {
                // Copy the genre id
                final long id = CursorHelpers.getLongOrZero(genres, BaseColumns._ID);
                // Copy the genre name
                final String name = CursorHelpers.getStringOrEmpty(genres, MediaStore.Audio.GenresColumns.NAME);
                // Query for the members
                final Cursor genreSongs = CursorHelpers.makeGenreMembersCursor(getContext(), id);
                // Don't add genres without any songs
                if (genreSongs == null) {
                    continue;
                } else if (genreSongs.getCount() <= 0) {
                    genreSongs.close();
                    continue;
                }
                int songNum = 0;
                int albumNum = 0;
                String songs = null;
                String albums = null;
                // loop the songs and filter all the unique album ids
                final List<String> songIds = new ArrayList<>(genreSongs.getCount());
                final HashSet<String> albumIdsSet = new HashSet<String>(genreSongs.getCount());
                if (genreSongs.moveToFirst()) {
                    do {
                        final String sId = CursorHelpers.getStringOrNull(genreSongs, MediaStore.Audio.Genres.Members.AUDIO_ID);
                        if (!TextUtils.isEmpty(sId)) {
                            songIds.add(sId);
                        }
                        final String aId = CursorHelpers.getStringOrNull(genreSongs, MediaStore.Audio.Genres.Members.ALBUM_ID);
                        if (!TextUtils.isEmpty(aId)) {
                            albumIdsSet.add(aId);
                        }
                    } while (genreSongs.moveToNext());
                }
                // close second cursor
                genreSongs.close();
                // copy song count
                songNum = songIds.size();
                if (songNum > 0) {
                    // build song ids csv
                    Collections.sort(songIds);
                    songs = CursorHelpers.toCsv(songIds);
                }
                // copy album count
                albumNum = albumIdsSet.size();
                if (albumNum > 0) {
                    // build album ids csv
                    List<String> albumIds = new ArrayList<>(albumIdsSet);
                    Collections.sort(albumIds);
                    albums = CursorHelpers.toCsv(albumIds);
                }
                // add row to final cursor
                c.addRow(new Object[]{id, name, songNum, albumNum, songs, albums});
            } while (genres.moveToNext());
        }
        if (genres != null) {
            genres.close();
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        if (uri == null || values == null) {
            return null;
        }
        Uri ret = null;
        switch (sUriMatcher.match(uri)) {
            case 1:
                SQLiteDatabase db = getMusicStoreDatabase(false);
                if (db != null) {
                    long id = db.insert(MusicStore.RECENT_TABLE, null, values);
//                    db.close();
                    if (id >= 0) {
                        ret = ContentUris.withAppendedId(RECENTS_URI, id);
                    }
                }
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
        }
        if (ret != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ret;
    }

    @Override
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        int ret = 0;
        switch (sUriMatcher.match(uri)) {
            case 1:
                SQLiteDatabase db = getMusicStoreDatabase(false);
                if (db != null) {
                    ret = db.delete(MusicStore.RECENT_TABLE, selection, selectionArgs);
//                    db.close();
                }
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
        }
        if (ret != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ret;
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int ret = 0;
        switch (sUriMatcher.match(uri)) {
            case 1:
                SQLiteDatabase db = getMusicStoreDatabase(false);
                if (db != null) {
                    ret = db.update(MusicStore.RECENT_TABLE, values, selection, selectionArgs);
//                    db.close();
                }
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
        }
        if (ret != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ret;
    }
}
