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

package org.opensilk.music.index.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.opensilk.common.core.dagger2.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 8/25/15.
 */
@Singleton
public class IndexDatabaseHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 8;
    public static final String DB_NAME = "music.db";

    @Inject
    public IndexDatabaseHelper(@ForApplication Context context) {
        super(context, DB_NAME, null, DB_VERSION, null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            //Drop tables from orpheus 0.x - 2.x
            db.execSQL("DROP TABLE IF EXISTS genres;");
            db.execSQL("DROP TABLE IF EXISTS playlists;");
            db.execSQL("DROP TABLE IF EXISTS recent;");
        }

        // STOPSHIP: 9/19/15  
        final int shipVer = DB_VERSION;

        if (oldVersion < shipVer) {
            //cleanup mistakes prior to 3.0 release
            db.execSQL("DROP TABLE IF EXISTS track_res_meta;");
            db.execSQL("DROP TABLE IF EXISTS track_meta;");
            db.execSQL("DROP TABLE IF EXISTS album_meta;");
            db.execSQL("DROP TABLE IF EXISTS artist_meta;");
            db.execSQL("DROP TABLE IF EXISTS container_uris;");
        }

        if (oldVersion < shipVer) {
            //Artist metadata
            db.execSQL("CREATE TABLE IF NOT EXISTS artist_meta (" +
                    "artist_id INTEGER PRIMARY KEY, " +
                    "artist_name TEXT NOT NULL, " +
                    "artist_key TEXT NOT NULL, " +
                    "artist_bio_summary TEXT, " +
                    "artist_bio_content TEXT, " +
                    "artist_bio_date_modified INTEGER, " +
                    "artist_mbid TEXT COLLATE NOCASE, " +
                    "UNIQUE(artist_key,artist_mbid) ON CONFLICT IGNORE" +
                    ");");
            //Album metadata
            db.execSQL("CREATE TABLE IF NOT EXISTS album_meta (" +
                    "album_id INTEGER PRIMARY KEY, " +
                    "album_name TEXT NOT NULL, " +
                    "album_key TEXT NOT NULL, " +
                    "album_bio_summary TEXT, " +
                    "album_bio_content TEXT, " +
                    "album_bio_date_modified INTEGER, " +
                    "album_mbid TEXT COLLATE NOCASE, " +
                    "album_artist_id INTEGER REFERENCES artist_meta(artist_id) ON DELETE CASCADE," +
                    "UNIQUE(album_key,album_mbid) ON CONFLICT IGNORE" +
                    ");");
            //Track metadata
            db.execSQL("CREATE TABLE IF NOT EXISTS track_meta (" +
                    "track_id INTEGER PRIMARY KEY, " +
                    "track_name TEXT NOT NULL, " +
                    "track_key TEXT NOT NULL, " +
                    "track_number INTEGER, " +
                    "disc_number INTEGER DEFAULT 1, " +
                    "genre TEXT, " +
                    "artist_id INTEGER REFERENCES artist_meta(artist_id) ON DELETE CASCADE, " +
                    "album_id INTEGER REFERENCES album_meta(album_id) ON DELETE CASCADE," +
                    "UNIQUE(artist_id,album_id,track_key,track_number,disc_number) ON CONFLICT IGNORE" +
                    ");");
            //Track resources
            db.execSQL("CREATE TABLE IF NOT EXISTS track_res_meta (" +
                    "res_id INTEGER PRIMARY KEY, " +
                    "track_id INTEGER REFERENCES track_meta(track_id) ON DELETE CASCADE, " +
                    "authority TEXT NOT NULL, " +
                    "uri TEXT NOT NULL UNIQUE, " +
                    "size INTEGER, " +
                    "mime_type TEXT, " +
                    "date_added INTEGER, " +
                    "last_modified INTEGER, " + //opaque provided by library
                    "bitrate INTEGER, " +
                    "duration INTEGER " +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS containers (" +
                    "uri TEXT NOT NULL UNIQUE ON CONFLICT IGNORE, " +
                    "authority TEXT NOT NULL" +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS playlists (" +
                    "playlist_id INTEGER PRIMARY KEY, " +
                    "playlist_name TEXT NOT NULL, " +
                    "date_added INTEGER, " +
                    "date_modified INTEGER" +
                    ");");
            db.execSQL("CREATE TABLE IF NOT EXISTS tracks_playlists_map (" +
                    "playlist_id INTEGER REFERENCES playlists(playlist_id), " +
                    "track_id INTEGER REFERENCES track_meta(track_id), " +
                    "track_number INTEGER, " +
                    "UNIQUE(playlist_id, track_id, track_number)" +
                    ");");

            // Provides a unified track/artist/album info view.
            db.execSQL("CREATE VIEW IF NOT EXISTS track_full as SELECT * FROM track_meta " +
                    "LEFT OUTER JOIN artist_meta ON track_meta.artist_id = artist_meta.artist_id " +
                    "LEFT OUTER JOIN album_meta ON track_meta.album_id = album_meta.album_id" +
                    ";");

            // Provides some extra info about artists, like the number of tracks and albums for this artist
            db.execSQL("CREATE VIEW IF NOT EXISTS artist_info AS " +
                    "SELECT artist_id AS _id, artist_name as name, artist_key, " +
                    "COUNT(DISTINCT album_id) AS number_of_albums, " +
                    "COUNT(*) AS number_of_tracks, "+
                    "artist_bio_content as bio, artist_bio_summary as summary, artist_mbid as mbid " +
                    "FROM track_full " +
                    "GROUP BY artist_id" +
                    ";");

            // Provides some extra info about tracks like album artist name and number of resources
            db.execSQL("CREATE VIEW IF NOT EXISTS track_info as SELECT " +
                    "t1.track_id as _id, track_name as name, track_key as title_key, duration, " +
                    "artist_name as artist, artist_id, album_name as album, album_id, " +
                    "album_artist_id, track_number as track, " +
                    "(select artist_name from artist_meta where artist_id = album_artist_id) as album_artist," +
                    "uri, size, mime_type, date_added, bitrate, duration " +
                    "FROM track_full t1 " +
                    "JOIN track_res_meta tm on t1.track_id = tm.track_id" +
                    ";");

            // Provides extra info albums, such as the number of tracks
//        db.execSQL("CREATE VIEW IF NOT EXISTS album_info AS " +
//                "SELECT album_id AS _id, album_name as album, album_key, " +
//                "artist_name as artist, artist_id, artist_key, album_artist_id," +
//                "count(*) AS track_count, " +
//                "album_bio_content as bio, album_bio_summary as summary, album_mbid as mbid " +
//                "FROM track_full WHERE hidden=0 GROUP BY album_id" +
//                ";");
            db.execSQL("CREATE VIEW IF NOT EXISTS album_info AS " +
                    "SELECT album_meta.album_id AS _id, album_name as name, album_key, " +
                    "artist_name as artist, album_artist_id as artist_id, artist_key," +
                    "count(track_id) AS track_count, " +
                    "album_bio_content as bio, album_bio_summary as summary, album_mbid as mbid " +
                    "FROM album_meta " +
                    "LEFT OUTER JOIN artist_meta on album_meta.album_artist_id = artist_meta.artist_id " +
                    "LEFT OUTER JOIN track_meta on album_meta.album_id = track_meta.album_id " +
                    "GROUP BY album_meta.album_id" +
                    ";");

            // For a given artist_id, provides the album_id for albums on
            // which the artist appears.
            db.execSQL("CREATE VIEW IF NOT EXISTS artists_albums_map AS " +
                    "SELECT DISTINCT artist_id, album_id FROM track_meta" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS genre_info AS " +
                    "SELECT DISTINCT genre, " +
                    "COUNT(track_id) as number_of_tracks, " +
                    "COUNT(DISTINCT album_id) as number_of_albums " +
                    "FROM track_meta GROUP BY genre" +
                    ";");

            db.execSQL("CREATE INDEX IF NOT EXISTS artistkey_idx on artist_meta(artist_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS albumkey_idx on album_meta(album_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS trackkey_idx on track_meta(track_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS artistid_idx on artist_meta(artist_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS albumid_idx on album_meta(album_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS trackid_idx on track_meta(track_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS trackresuri_idx on track_res_meta(uri);");
        }

        /*
        db.execSQL("CREATE TABLE IF NOT EXISTS artist_images (" +
                "artist_id INTEGER PRIMARY KEY, " +
                "_data TEXT UNIQUE, " +
                "date_modified INTEGER" +
                ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS artist_thumbs (" +
                "artist_id INTEGER PRIMARY KEY, " +
                "_data TEXT UNIQUE, " +
                "date_modified INTEGER" +
                ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS album_images (" +
                "album_id INTEGER PRIMARY KEY, " +
                "_data TEXT UNIQUE, " +
                "date_modified INTEGER" +
                ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS album_thumbs (" +
                "album_id INTEGER PRIMARY KEY, " +
                "_data TEXT UNIQUE, " +
                "date_modified INTEGER" +
                ");");

        db.execSQL("CREATE VIEW IF NOT EXISTS tracks_genres_map AS " +
                ";");

        // Cleans up when an audio file is deleted
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_meta_cleanup DELETE ON audio_meta " +
                "BEGIN " +
                "DELETE FROM audio_genres_map WHERE audio_id = old._id;" +
                "DELETE FROM audio_playlists_map WHERE audio_id = old._id;" +
                "END");

        // Contains audio genre definitions
        db.execSQL("CREATE TABLE IF NOT EXISTS audio_genres (" +
                "_id INTEGER PRIMARY KEY," +
                "name TEXT NOT NULL" +
                ");");

        // Contains mappings between audio genres and audio files
        db.execSQL("CREATE TABLE IF NOT EXISTS audio_genres_map (" +
                "_id INTEGER PRIMARY KEY," +
                "audio_id INTEGER NOT NULL," +
                "genre_id INTEGER NOT NULL" +
                ");");

        // Cleans up when an audio genre is delete
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_genres_cleanup DELETE ON audio_genres " +
                "BEGIN " +
                "DELETE FROM audio_genres_map WHERE genre_id = old._id;" +
                "END");

        // Cleans up when an audio playlist is deleted
        db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_playlists_cleanup DELETE ON audio_playlists " +
                "BEGIN " +
                "DELETE FROM audio_playlists_map WHERE playlist_id = old._id;" +
                "SELECT _DELETE_FILE(old._data);" +
                "END");

        // Cleans up album_art table entry when an album is deleted
        db.execSQL("CREATE TRIGGER IF NOT EXISTS albumart_cleanup1 DELETE ON albums " +
                "BEGIN " +
                "DELETE FROM album_art WHERE album_id = old.album_id;" +
                "END");

        // Cleans up album_art when an album is deleted
        db.execSQL("CREATE TRIGGER IF NOT EXISTS albumart_cleanup2 DELETE ON album_art " +
                "BEGIN " +
                "SELECT _DELETE_FILE(old._data);" +
                "END");
        */
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.execSQL("PRAGMA foreign_keys = ON;");
    }

}
