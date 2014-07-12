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
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;

import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;

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

    private final Executor mBackgroundExecutor = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(new BackgroundRunnable(r));
                }
            }
    );

    static class BackgroundRunnable implements Runnable {
        private final Runnable r;
        BackgroundRunnable(Runnable r) {
            this.r = r;
        }
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            r.run();
        }
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
    public int bulkInsert(Uri uri, ContentValues[] values) {
        return super.bulkInsert(uri, values);
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor c = null;
        SQLiteDatabase db;
        switch (sUriMatcher.match(uri)) {
            case 1: // Recents
                db = mStore.getWritableDatabase();
                if (db != null) {
                    c = db.query(MusicStore.RECENT_TABLE,
                            projection, selection, selectionArgs, null, null, sortOrder);
//                    db.close();
                }
                break;
            case 2: // Genres
                db = mStore.getWritableDatabase();
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
                        mBackgroundExecutor.execute(r);
                        break;
                    }
                    if (c != null) {
                        c.close();
                    } else {
                        db.close();
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
                mBackgroundExecutor.execute(r1);

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
                db = mStore.getWritableDatabase();
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
                        mBackgroundExecutor.execute(r2);
                        break;
                    }
                    if (c != null) {
                        c.close();
                    } else {
                        db.close();
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
                mBackgroundExecutor.execute(r3);
                break;
        }
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
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
            if (lastAdded.moveToFirst()) {
                List<String> songs = new ArrayList<>(lastAdded.getCount());
                Set<String> albumidSet = new HashSet<>(lastAdded.getCount());
                do {
                    songs.add(lastAdded.getString(lastAdded.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                    albumidSet.add(lastAdded.getString(lastAdded.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)));
                } while (lastAdded.moveToNext());
                lastAddedSongNum = songs.size();
                lastAddedAlbumNum = albumidSet.size();
                if (lastAddedSongNum > 0) {
                    Collections.sort(songs);
                    StringBuilder songsIdsCsv = new StringBuilder();
                    int ii=0;
                    for (String s : songs) {
                        songsIdsCsv.append(s);
                        if (++ii < songs.size()) {
                            songsIdsCsv.append(",");
                        }
                    }
                    lastAddedSongids = songsIdsCsv.toString();
                }
                if (lastAddedAlbumNum > 0) {
                    List<String> albums = new ArrayList<>(albumidSet);
                    Collections.sort(albums);
                    StringBuilder albumIdsCsv = new StringBuilder();
                    int ii=0;
                    for (String s : albums) {
                        albumIdsCsv.append(s);
                        if (++ii < albums.size()) {
                            albumIdsCsv.append(",");
                        }
                    }
                    lastAddedAlbumids = albumIdsCsv.toString();
                }
            }
            lastAdded.close();
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
        Cursor playlists = getContext().getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                Projections.PLAYLIST,
                null, null, MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
        if (playlists != null && playlists.moveToFirst()) {
            do {
                // get playlist id
                final long id = playlists.getLong(playlists.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
                // get playlist name
                final String name = playlists.getString(playlists.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME));
                // We have to query for the song count
                final Cursor playlistSongs = getContext().getContentResolver().query(
                        MediaStore.Audio.Playlists.Members.getContentUri("external", id),
                        Projections.PLAYLIST_MEMBER,
                        Selections.PLAYLIST_MEMBER,
                        SelectionArgs.PLAYLIST_MEMBER,
                        MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
                int numSongs = 0;
                int numAlbums = 0;
                String songids = null;
                String albumids = null;
                if (playlistSongs != null) {
                    List<String> songs = new ArrayList<>(playlistSongs.getCount());
                    Set<String> albumsSet = new HashSet<>(playlistSongs.getCount());
                    if (playlistSongs.moveToFirst()) {
                        do {
                            songs.add(playlistSongs.getString(playlistSongs.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID)));
                            albumsSet.add(playlistSongs.getString(playlistSongs.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ALBUM_ID)));
                        } while (playlistSongs.moveToNext());
                    }
                    numSongs = songs.size();
                    numAlbums = albumsSet.size();
                    if (numSongs > 0) {
                        Collections.sort(songs);
                        StringBuilder songsIdsCsv = new StringBuilder();
                        int ii=0;
                        for (String s : songs) {
                            songsIdsCsv.append(s);
                            if (++ii < songs.size()) {
                                songsIdsCsv.append(",");
                            }
                        }
                        songids = songsIdsCsv.toString();
                    }
                    if (numAlbums > 0) {
                        List<String> albums = new ArrayList<>(albumsSet);
                        Collections.sort(albums);
                        StringBuilder albumIdsCsv = new StringBuilder();
                        int ii=0;
                        for (String s : albums) {
                            albumIdsCsv.append(s);
                            if (++ii < albums.size()) {
                                albumIdsCsv.append(",");
                            }
                        }
                        albumids = albumIdsCsv.toString();
                    }
                    playlistSongs.close();
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
    protected void updatePlaylistCache() {
        boolean wasupdated = false;
        // Add new
        Cursor c = makePlaylistMatrixCursor();
        if (c != null) {
            SQLiteDatabase db = mStore.getWritableDatabase();
            if (c.moveToFirst()) {
                do {
                    long id = c.getLong(c.getColumnIndexOrThrow(MusicStore.GroupCols._ID));
                    String name = c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.NAME));
                    int songCount = c.getInt(c.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_COUNT));
                    int albumCount = c.getInt(c.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_COUNT));
                    String songids = c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_IDS));
                    String albumids = c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_IDS));
                    // create content values
                    ContentValues values = new ContentValues(6);
                    values.put(MusicStore.GroupCols.NAME, name);
                    values.put(MusicStore.GroupCols.SONG_COUNT, songCount);
                    values.put(MusicStore.GroupCols.ALBUM_COUNT, albumCount);
                    values.put(MusicStore.GroupCols.SONG_IDS, songids);
                    values.put(MusicStore.GroupCols.ALBUM_IDS, albumids);
                    // check if its already there and update if needed
                    Cursor c2 = db.query(MusicStore.PLAYLIST_TABLE,
                            Projections.CACHED_GROUP,
                            MusicStore.GroupCols._ID + "=?",
                            new String[]{String.valueOf(id)},
                            null, null, null, null);
                    if (c2 != null) {
                        try {
                            if (c2.getCount() > 0) {
                                if (c2.moveToFirst()) {
                                    if (TextUtils.equals(name, c2.getString(c2.getColumnIndexOrThrow(MusicStore.GroupCols.NAME)))
                                            && songCount == c2.getInt(c2.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_COUNT))
                                            && albumCount == c2.getInt(c2.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_COUNT))
                                            && TextUtils.equals(songids, c2.getString(c2.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_IDS)))
                                            && TextUtils.equals(albumids, c2.getString(c2.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_IDS)))) {
                                        continue;
                                    }
                                }
                                db.update(MusicStore.PLAYLIST_TABLE, values,
                                        MusicStore.GroupCols._ID + "=?",
                                        new String[]{String.valueOf(id)});
                                Timber.v("Updated " +name+" playlist id="+id);
                                wasupdated = true;
                                continue;
                            }
                        } finally {
                            c2.close();
                        }
                    }
                    values.put(MusicStore.GroupCols._ID, id);
                    db.insert(MusicStore.PLAYLIST_TABLE, null, values);
                    wasupdated = true;
                } while (c.moveToNext());
            }
            // remove old
            Cursor c2 = db.query(MusicStore.PLAYLIST_TABLE,
                    Projections.CACHED_GROUP,
                    null, null, null, null, null, null);
            if (c2 != null) {
                if (c2.moveToFirst()) {
                    do {
                        boolean found = false;
                        long id = c2.getLong(c2.getColumnIndexOrThrow(BaseColumns._ID));
                        if (c.moveToFirst()) {
                            do {
                                long id2 = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
                                if (id == id2) {
                                    found = true;
                                    break;
                                }
                            } while (c.moveToNext());
                        }
                        if (!found) {
                            Timber.v("Removing playlist " + c2.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.NAME)));
                            db.delete(MusicStore.PLAYLIST_TABLE,
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
            getContext().getContentResolver().notifyChange(PLAYLIST_URI, null);
        }
    }

    @DebugLog
    protected void updateGenreCache() {
        boolean wasupdated = false;
        Cursor c = makeGenreMatrixCursor();
        if (c != null) {
            SQLiteDatabase db = mStore.getWritableDatabase();
            if (c.moveToFirst()) {
                do {
                    long id = c.getLong(c.getColumnIndexOrThrow(MusicStore.GroupCols._ID));
                    String name = c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.NAME));
                    int songCount = c.getInt(c.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_COUNT));
                    int albumCount = c.getInt(c.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_COUNT));
                    String songids = c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_IDS));
                    String albumids = c.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_IDS));
                    // create content values
                    ContentValues values = new ContentValues(6);
                    values.put(MusicStore.GroupCols.NAME, name);
                    values.put(MusicStore.GroupCols.SONG_COUNT, songCount);
                    values.put(MusicStore.GroupCols.ALBUM_COUNT, albumCount);
                    values.put(MusicStore.GroupCols.SONG_IDS, songids);
                    values.put(MusicStore.GroupCols.ALBUM_IDS, albumids);
                    // check if its already there and update if needed
                    Cursor c2 = db.query(MusicStore.GENRE_TABLE,
                            Projections.CACHED_GROUP,
                            MusicStore.GroupCols._ID + "=?",
                            new String[]{String.valueOf(id)},
                            null, null, null, null);
                    if (c2 != null) {
                        try {
                            if (c2.getCount() > 0) {
                                if (c2.moveToFirst()) {
                                    if (TextUtils.equals(name, c2.getString(c2.getColumnIndexOrThrow(MusicStore.GroupCols.NAME)))
                                            && songCount == c2.getInt(c2.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_COUNT))
                                            && albumCount == c2.getInt(c2.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_COUNT))
                                            && TextUtils.equals(songids, c2.getString(c2.getColumnIndexOrThrow(MusicStore.GroupCols.SONG_IDS)))
                                            && TextUtils.equals(albumids, c2.getString(c2.getColumnIndexOrThrow(MusicStore.GroupCols.ALBUM_IDS)))) {
                                        continue;
                                    }
                                }
                                db.update(MusicStore.GENRE_TABLE, values,
                                        MusicStore.GroupCols._ID + "=?",
                                        new String[]{String.valueOf(id)});
                                Timber.v("Updated " + name + " genre id=" + id);
                                wasupdated = true;
                                continue;
                            }
                        } finally {
                            c2.close();
                        }
                    }
                    values.put(MusicStore.GroupCols._ID, id);
                    db.insert(MusicStore.GENRE_TABLE, null, values);
                    wasupdated = true;
                } while (c.moveToNext());
            }
            // remove old
            Cursor c2 = db.query(MusicStore.GENRE_TABLE,
                    Projections.CACHED_GROUP,
                    null, null, null, null, null, null);
            if (c2 != null) {
                if (c2.moveToFirst()) {
                    do {
                        boolean found = false;
                        long id = c2.getLong(c2.getColumnIndexOrThrow(BaseColumns._ID));
                        if (c.moveToFirst()) {
                            do {
                                long id2 = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
                                if (id == id2) {
                                    found = true;
                                    break;
                                }
                            } while (c.moveToNext());
                        }
                        if (!found) {
                            Timber.v("Removing genre " + c2.getString(c.getColumnIndexOrThrow(MusicStore.GroupCols.NAME)));
                            db.delete(MusicStore.GENRE_TABLE,
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
            getContext().getContentResolver().notifyChange(GENRES_URI, null);
        }
    }

    @DebugLog
    protected MatrixCursor makeGenreMatrixCursor() {
        MatrixCursor c = null;
        // Pull list of all genres
        Cursor genres = getContext().getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                Projections.GENRE,
                Selections.GENRE,
                SelectionArgs.GENRE,
                MediaStore.Audio.Genres.DEFAULT_SORT_ORDER);

        // Build our custom cursor
        if (genres != null && genres.moveToFirst()) {
            SQLiteDatabase db = mStore.getWritableDatabase();
            c = new MatrixCursor(Projections.CACHED_GROUP);
            do {
                // Copy the genre id
                final long id = genres.getLong(genres.getColumnIndexOrThrow(BaseColumns._ID));
                // Copy the genre name
                final String name = genres.getString(genres.getColumnIndexOrThrow(MediaStore.Audio.GenresColumns.NAME));
                // Query for the members
                final Cursor genreSongs = getContext().getContentResolver().query(
                        MediaStore.Audio.Genres.Members.getContentUri("external", id),
                        Projections.GENRE_MEMBER,
                        Selections.GENRE_MEMBER,
                        SelectionArgs.GENRE_MEMBER,
                        MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER);

                // Don't add genres without any songs
                if (genreSongs == null) {
                    continue;
                } else if (genreSongs.getCount() <= 0) {
                    genreSongs.close();
                    continue;
                }

                // copy song count
                final int songNum = genreSongs.getCount();

                // loop the songs and filter all the unique album ids
                final List<String> songIds = new ArrayList<>(songNum);
                final HashSet<String> albumIdsSet = new HashSet<String>(songNum);
                if (genreSongs.moveToFirst()) {
                    do {
                        songIds.add(genreSongs.getString(genreSongs.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)));
                        albumIdsSet.add(genreSongs.getString(genreSongs.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.ALBUM_ID)));
                    } while (genreSongs.moveToNext());
                }
                // copy album count
                final int numAlbums = albumIdsSet.size();

                // close second cursor
                genreSongs.close();

                // build song ids csv
                Collections.sort(songIds);
                StringBuilder songsIdsCsv = new StringBuilder();
                int ii=0;
                for (String s : songIds) {
                    songsIdsCsv.append(s);
                    if (++ii < songIds.size()) {
                        songsIdsCsv.append(",");
                    }
                }

                // build album ids csv
                List<String> albumIds = new ArrayList<>(albumIdsSet);
                Collections.sort(albumIds);
                StringBuilder albumIdsCsv = new StringBuilder();
                ii=0;
                for (String s : albumIds) {
                    albumIdsCsv.append(s);
                    if (++ii < albumIds.size()) {
                        albumIdsCsv.append(",");
                    }
                }
                // add row to final cursor
                c.addRow(new Object[]{id, name, songNum, numAlbums, songsIdsCsv.toString(), albumIdsCsv.toString()});
            } while (genres.moveToNext());
            db.close();
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
                SQLiteDatabase db = mStore.getWritableDatabase();
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
                SQLiteDatabase db = mStore.getWritableDatabase();
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
                SQLiteDatabase db = mStore.getWritableDatabase();
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
