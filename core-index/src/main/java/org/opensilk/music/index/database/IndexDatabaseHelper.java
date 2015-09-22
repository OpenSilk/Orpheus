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

    public static final int DB_VERSION = 22;
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
        if (oldVersion < 5) {
            //Drop tables from orpheus 0.x - 2.x
            db.execSQL("DROP TABLE IF EXISTS genres;");
            db.execSQL("DROP TABLE IF EXISTS playlists;");
            db.execSQL("DROP TABLE IF EXISTS recent;");
        }

        if (oldVersion < DB_VERSION) {
            // STOPSHIP: 9/19/15 remove before release
            //cleanup mistakes prior to 3.0 release
            db.execSQL("DROP TRIGGER IF EXISTS tracks_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS albums_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS artists_cleanup;");

            db.execSQL("DROP VIEW IF EXISTS album_info;");
            db.execSQL("DROP VIEW IF EXISTS artist_info;");
            db.execSQL("DROP VIEW IF EXISTS track_info;");
            db.execSQL("DROP VIEW IF EXISTS track_full");
            db.execSQL("DROP VIEW IF EXISTS artists_albums_map");
            db.execSQL("DROP VIEW IF EXISTS genre_info");

            db.execSQL("DROP INDEX IF EXISTS artistkey_idx;");
            db.execSQL("DROP INDEX IF EXISTS albumkey_idx;");
            db.execSQL("DROP INDEX IF EXISTS trackkey_idx;");
            db.execSQL("DROP INDEX IF EXISTS artistid_idx;");
            db.execSQL("DROP INDEX IF EXISTS albumid_idx;");
            db.execSQL("DROP INDEX IF EXISTS trackid_idx;");
            db.execSQL("DROP INDEX IF EXISTS trackresuri_idx;");

            db.execSQL("DROP TABLE IF EXISTS track_meta;");
            db.execSQL("DROP TABLE IF EXISTS track_resources;");
            db.execSQL("DROP TABLE IF EXISTS album_meta;");
            db.execSQL("DROP TABLE IF EXISTS artist_meta;");
            db.execSQL("DROP TABLE IF EXISTS tracks_playlists_map;");
            db.execSQL("DROP TABLE IF EXISTS playlists;");
            db.execSQL("DROP TABLE IF EXISTS containers;");
            //end mistakes cleanup

            //Artist metadata
            db.execSQL("CREATE TABLE IF NOT EXISTS artist_meta (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "artist_name TEXT NOT NULL, " +
                    "artist_key TEXT NOT NULL, " +
                    "artist_bio_summary TEXT, " +
                    "artist_bio_content TEXT, " +
                    "artist_bio_date_modified INTEGER, " +
                    "artist_mbid TEXT COLLATE NOCASE, " +
                    "UNIQUE(artist_key,artist_mbid)" +
                    ");");
            //Album metadata
            db.execSQL("CREATE TABLE IF NOT EXISTS album_meta (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "album_name TEXT NOT NULL, " +
                    "album_key TEXT NOT NULL, " +
                    "album_bio_summary TEXT, " +
                    "album_bio_content TEXT, " +
                    "album_bio_date_modified INTEGER, " +
                    "album_mbid TEXT COLLATE NOCASE, " +
                    "album_artist_id INTEGER REFERENCES artist_meta(_id) ON DELETE CASCADE," +
                    "UNIQUE(album_key,album_mbid)" +
                    ");");
            //Containers
            db.execSQL("CREATE TABLE IF NOT EXISTS containers (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "uri TEXT NOT NULL UNIQUE, " +
                    "parent_uri TEXT NOT NULL, " +
                    "authority TEXT NOT NULL " +
                    ");");
            //Track resources
            db.execSQL("CREATE TABLE IF NOT EXISTS track_resources (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "uri TEXT NOT NULL UNIQUE, " +
                    "authority TEXT NOT NULL, " +
                    "track_name TEXT NOT NULL, " +
                    "track_key TEXT NOT NULL, " +
                    "size INTEGER, " +
                    "mime_type TEXT, " +
                    "date_added INTEGER, " +
                    "last_modified INTEGER, " + //opaque provided by library
                    "bitrate INTEGER, " +
                    "duration INTEGER, " +
                    "track_number INTEGER, " +
                    "disc_number INTEGER DEFAULT 1, " +
                    "genre TEXT, " +
                    "album_name TEXT, " +
                    "artist_name TEXT, " +
                    "album_artist_name TEXT, " +
                    "category INTEGER NOT NULL DEFAULT 1, " + //Music 1, Podcast 2, AudioBook 3
                    "artist_id INTEGER REFERENCES artist_meta(_id) ON DELETE CASCADE, " +
                    "album_id INTEGER REFERENCES album_meta(_id) ON DELETE CASCADE, " +
                    "container_id INTEGER REFERENCES containers(_id) ON DELETE CASCADE " +
                    ");");

//            db.execSQL("CREATE TABLE IF NOT EXISTS playlists (" +
//                    "playlist_id INTEGER PRIMARY KEY, " +
//                    "playlist_name TEXT NOT NULL, " +
//                    "date_added INTEGER, " +
//                    "date_modified INTEGER" +
//                    ");");
//            db.execSQL("CREATE TABLE IF NOT EXISTS tracks_playlists_map (" +
//                    "playlist_id INTEGER REFERENCES playlists(playlist_id), " +
//                    "track_id INTEGER REFERENCES track_meta(track_id), " +
//                    "track_number INTEGER, " +
//                    "UNIQUE(playlist_id, track_id, track_number)" +
//                    ");");

            // Provides a unified track/artist/album info view.
//            db.execSQL("CREATE VIEW IF NOT EXISTS track_full as SELECT * FROM track_meta " +
//                    "LEFT OUTER JOIN artist_meta ON track_meta.artist_id = artist_meta.artist_id " +
//                    "LEFT OUTER JOIN album_meta ON track_meta.album_id = album_meta.album_id" +
//                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS artist_info AS " +
                    "SELECT a1._id, a1.artist_name as name, artist_key, " +
                    "COUNT(DISTINCT a2._id) AS number_of_albums, " +
                    "COUNT(DISTINCT t1.track_key) AS number_of_tracks, "+
                    "artist_bio_content as bio, artist_bio_summary as summary, artist_mbid as mbid " +
                    "FROM artist_meta a1 " +
                    "LEFT OUTER JOIN album_meta a2 ON a2.album_artist_id = a1._id " +
                    "LEFT OUTER JOIN track_resources t1 ON t1.artist_id = a1._id " +
                    "GROUP BY a1._id" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS album_info AS " +
                    "SELECT a1._id, a1.album_name as name, album_key, " +
                    "a2.artist_name as artist, album_artist_id as artist_id, artist_key, " +
                    "COUNT(DISTINCT t1.track_key) as track_count, " +
                    "album_bio_content as bio, album_bio_summary as summary, album_mbid as mbid " +
                    "FROM album_meta a1 " +
                    "LEFT OUTER JOIN artist_meta a2 on a1.album_artist_id = a2._id " +
                    "LEFT OUTER JOIN track_resources t1 on a1._id = t1.album_id " +
                    "GROUP BY a1._id" +
                    ";");

            // Provides some extra info about tracks like album artist name and number of resources
            db.execSQL("CREATE VIEW IF NOT EXISTS track_info as SELECT " +
                    "t1._id, track_name as name, track_key, " +
                    "a1.artist_name as artist, artist_id, a2.album_name as album, album_id, " +
                    "a2.album_artist_id, track_number as track, disc_number as disc, " +
                    "(SELECT artist_name from artist_meta where _id = album_artist_id) as album_artist," +
                    "uri, size, mime_type, date_added, bitrate, duration " +
                    "FROM track_resources t1 " +
                    "LEFT OUTER JOIN artist_meta a1 ON t1.artist_id = a1._id " +
                    "LEFT OUTER JOIN album_meta a2 ON t1.album_id = a2._id " +
                    "GROUP BY t1._id" +
                    ";");


            // For a given artist_id, provides the album_id for albums on
            // which the artist appears.
            db.execSQL("CREATE VIEW IF NOT EXISTS artists_albums_map AS " +
                    "SELECT DISTINCT artist_id, album_id FROM track_resources" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS genre_info AS " +
                    "SELECT DISTINCT genre, " +
                    "COUNT(_id) as number_of_tracks, " +
                    "COUNT(DISTINCT album_id) as number_of_albums " +
                    "FROM track_resources GROUP BY genre" +
                    ";");

            db.execSQL("CREATE INDEX IF NOT EXISTS artist_key_idx on artist_meta(artist_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS album_key_idx on album_meta(album_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS track_key_idx on track_resources(track_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS artist_id_idx on artist_meta(_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS album_id_idx on album_meta(_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS track_id_idx on track_resources(_id);");

            //Cleanup albums when tracks are deleted
            db.execSQL("CREATE TRIGGER IF NOT EXISTS albums_cleanup AFTER DELETE ON track_resources " +
                    "FOR EACH ROW " +
                    "WHEN (SELECT COUNT(_id) FROM track_resources WHERE album_id=OLD.album_id) = 0 " +
                    "BEGIN " +
                    "DELETE FROM album_meta WHERE _id=OLD.album_id; " +
                    "END");
            //Cleanup artists when tracks are deleted
            db.execSQL("CREATE TRIGGER IF NOT EXISTS artists_cleanup AFTER DELETE ON track_resources " +
                    "FOR EACH ROW " +
                    "WHEN (SELECT COUNT(_id) FROM track_resources WHERE artist_id=OLD.artist_id) = 0 " +
                    "BEGIN " +
                    "DELETE FROM artist_meta WHERE _id=OLD.artist_id; " +
                    "END");
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
