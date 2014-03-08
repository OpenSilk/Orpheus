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
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.andrew.apollo.BuildConfig;

import org.opensilk.music.loaders.Projections;

import java.util.HashSet;
import java.util.List;

/**
 * Created by drew on 2/24/14.
 */
public class MusicProvider extends ContentProvider {

    private static final String AUTHORITY = BuildConfig.FLAVORED_AUTHORITY;
    private static final UriMatcher sUriMatcher;

    /** Uri for recents store */
    public static final Uri RECENTS_URI;
    /** Wrapper uri to query genres */
    public static final Uri GENRES_URI;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        RECENTS_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("recents").build();
        sUriMatcher.addURI(AUTHORITY, "recents", 1);

        GENRES_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("genres").build();
        sUriMatcher.addURI(AUTHORITY, "genres", 2);

        // Genre albums
        sUriMatcher.addURI(AUTHORITY, "genre/#/albums", 3);
    }

    /** Generate a uri to query albums in a genre */
    public static Uri makeGenreAlbumsUri(long genreId) {
        return new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("genre")
                .appendPath(String.valueOf(genreId)).appendPath("albums").build();
    }

    /** Reference to our recents store instance */
    private static RecentStore sRecents;

    @Override
    public boolean onCreate() {
        sRecents = RecentStore.getInstance(getContext());
        return true;
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor c = null;
        switch (sUriMatcher.match(uri)) {
            case 1: // Recents
                c = sRecents.getReadableDatabase().query(RecentStore.RecentStoreColumns.NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case 2: // Genres
                // Pull list of all genres
                Cursor genres = getContext().getContentResolver().query(
                        MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                        new String[] { BaseColumns._ID, MediaStore.Audio.GenresColumns.NAME },
                        MediaStore.Audio.Genres.NAME + " !=?",
                        new String[] {"''"}, MediaStore.Audio.Genres.DEFAULT_SORT_ORDER);

                // Build our custom cursor
                if (genres != null && genres.moveToFirst()) {
                    c = new MatrixCursor(new String[] {"_id", "name", "song_number"});
                    do {
                        // Copy the genre id
                        final long id = genres.getLong(genres.getColumnIndexOrThrow(BaseColumns._ID));

                        // Copy the genre name
                        final String name = genres.getString(genres.getColumnIndexOrThrow(MediaStore.Audio.GenresColumns.NAME));

                        // This double query really sucks but we have to do in otherwise we can end up
                        // with genres without any songs
                        final Cursor genreSongs = getContext().getContentResolver().query(
                                MediaStore.Audio.Genres.Members.getContentUri("external", id),
                                new String[] { BaseColumns._ID },
                                MediaStore.Audio.Genres.Members.IS_MUSIC + "=? AND " + MediaStore.Audio.Genres.Members.TITLE + "!=?",
                                new String[] {"1", "''"}, MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER);

                        // Don't add genres without any songs
                        if (genreSongs == null) {
                            continue;
                        } else if (genreSongs.getCount() <= 0) {
                            genreSongs.close();
                            continue;
                        }

                        // copy song count
                        final int songNum = genreSongs.getCount();

                        // close second cursor
                        genreSongs.close();

                        ((MatrixCursor) c).addRow(new Object[] {id, name, songNum});
                    } while (genres.moveToNext());
                }
                if (genres != null) {
                    genres.close();
                }
                if (c != null) {
                    // Set the notification uri on the genres uri not on our proxy uri
                    c.setNotificationUri(getContext().getContentResolver(), MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI);
                }
                return c;
            case 3: // Genre songs
                // Extract our genre id
                List<String> pathSegments = uri.getPathSegments();
                long genreId = Integer.valueOf(pathSegments.get(pathSegments.size() - 2));

                // pull song list for genres
                final Cursor genreSongs = getContext().getContentResolver().query(
                        MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                        new String[] {MediaStore.Audio.AudioColumns.ALBUM_ID },
                        MediaStore.Audio.Genres.Members.IS_MUSIC + "=? AND " + MediaStore.Audio.Genres.Members.TITLE + "!=?",
                        new String[] {"1", "''"}, MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER);

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
                        Projections.ALBUM,
                        albumSelection.toString(),
                        null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);

                if (c != null) {
                    // Set the notification uri on the albums uri not on our proxy uri
                    c.setNotificationUri(getContext().getContentResolver(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI);
                }
                return c;
        }
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
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
                SQLiteDatabase db = sRecents.getWritableDatabase();
                db.beginTransaction();
                // Todo update playcount instead
                db.delete(RecentStore.RecentStoreColumns.NAME,
                        BaseColumns._ID + " = ?",
                        new String[] {
                            String.valueOf(values.get(BaseColumns._ID))
                        }
                );
                db.insert(RecentStore.RecentStoreColumns.NAME, null, values);
                db.setTransactionSuccessful();
                db.endTransaction();
                ret = RECENTS_URI.buildUpon().appendPath(values.getAsString(BaseColumns._ID)).build();
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
                ret = sRecents.getWritableDatabase().delete(RecentStore.RecentStoreColumns.NAME, selection, selectionArgs);
                break;
        }
        if (ret != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ret;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
