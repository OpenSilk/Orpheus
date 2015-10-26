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
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.apache.commons.io.FileUtils;
import org.opensilk.common.core.dagger2.ForApplication;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Note to the uninitiated (including you) invalid or 'bad' foreign keys
 * in *any* table will throw an exception no matter what table you access
 * (including unrelated ones) this can be very confusing and frustrating.
 * Just remember. everything in here is working as expected.
 * It is YOUR changes that fucked it up. If you don't believe me just
 * stash your changes and rerun the tests to confirm
 *
 * Created by drew on 8/25/15.
 */
@Singleton
public class IndexDatabaseHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 37;
    public static final String DB_NAME = "music.db";

    @Inject
    public IndexDatabaseHelper(@ForApplication Context context) {
        super(context, DB_NAME, null, DB_VERSION, new ErrorHandler());
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

        // STOPSHIP: 9/19/15 remove before release
        //cleanup mistakes prior to 3.0 release
        if (oldVersion < 32) {

            db.execSQL("DROP TRIGGER IF EXISTS tracks_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS albums_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS artists_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS genres_cleanup;");

            db.execSQL("DROP VIEW IF EXISTS album_info;");
            db.execSQL("DROP VIEW IF EXISTS artist_info;");
            db.execSQL("DROP VIEW IF EXISTS track_info;");
            db.execSQL("DROP VIEW IF EXISTS track_full");
            db.execSQL("DROP VIEW IF EXISTS artists_albums_map");
            db.execSQL("DROP VIEW IF EXISTS genre_info");
            db.execSQL("DROP VIEW IF EXISTS track_parent_map;");
            db.execSQL("DROP VIEW IF EXISTS genre_album_map;");
            db.execSQL("DROP VIEW IF EXISTS artist_album_map;");

            db.execSQL("DROP INDEX IF EXISTS artist_key_idx;");
            db.execSQL("DROP INDEX IF EXISTS album_key_idx;");
            db.execSQL("DROP INDEX IF EXISTS track_key_idx;");
            db.execSQL("DROP INDEX IF EXISTS genre_key_idx;");
            db.execSQL("DROP INDEX IF EXISTS artist_id_idx;");
            db.execSQL("DROP INDEX IF EXISTS album_id_idx;");
            db.execSQL("DROP INDEX IF EXISTS track_id_idx;");
            db.execSQL("DROP INDEX IF EXISTS track_res_id_idx;");
            db.execSQL("DROP INDEX IF EXISTS containers_uri_idx;");
            db.execSQL("DROP INDEX IF EXISTS playback_settings_key_idx;");

            db.execSQL("DROP TABLE IF EXISTS extracted_meta;");
            db.execSQL("DROP TABLE IF EXISTS track_resources;");
            db.execSQL("DROP TABLE IF EXISTS track_meta;");
            db.execSQL("DROP TABLE IF EXISTS album_meta;");
            db.execSQL("DROP TABLE IF EXISTS artist_meta;");
            db.execSQL("DROP TABLE IF EXISTS genre_meta;");
            db.execSQL("DROP TABLE IF EXISTS tracks_playlists_map;");
            db.execSQL("DROP TABLE IF EXISTS playlists;");
            db.execSQL("DROP TABLE IF EXISTS containers;");
            db.execSQL("DROP TABLE IF EXISTS tracks;");
            db.execSQL("DROP TABLE IF EXISTS scanner_meta;");
            db.execSQL("DROP TABLE IF EXISTS scanner_settings;");
            db.execSQL("DROP TABLE IF EXISTS playback_settings;");

        }
        if (oldVersion < 33) {
            db.execSQL("DROP VIEW IF EXISTS album_artist_info;");
        }
        if (oldVersion < 36) {
            db.execSQL("DROP VIEW IF EXISTS playlist_album_map;");
            db.execSQL("DROP VIEW IF EXISTS playlist_info;");
            db.execSQL("DROP VIEW IF EXISTS playlist_track_info;");
            db.execSQL("DROP TABLE IF EXISTS playlist_track;");
            db.execSQL("DROP TABLE IF EXISTS playlist_meta;");
            db.execSQL("DROP TABLE IF EXISTS playlist_track_meta;");
        }
        if (oldVersion < 37) {
            db.execSQL("DROP VIEW IF EXISTS genre_album_map;");
            db.execSQL("DROP VIEW IF EXISTS artist_album_map;");
        }
        //end mistakes cleanup

        if (oldVersion < DB_VERSION) {

            //Scanner meta
            db.execSQL("CREATE TABLE IF NOT EXISTS scanner_settings (" +
                    "rescan_count INTEGER DEFAULT 0" +
                    ");");
            //playback
            db.execSQL("CREATE TABLE IF NOT EXISTS playback_settings (" +
                    "key VARCHAR(32) NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                    "intVal INTEGER, " +
                    "textVal TEXT " +
                    ");");
            //Artist metadata
            db.execSQL("CREATE TABLE IF NOT EXISTS artist_meta (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "artist_name TEXT NOT NULL, " +
                    "artist_key TEXT NOT NULL, " +
                    "artist_bio_summary TEXT, " +
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
                    "album_bio_date_modified INTEGER, " +
                    "album_mbid TEXT COLLATE NOCASE, " +
                    "album_artist_id INTEGER REFERENCES artist_meta(_id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "UNIQUE(album_key,album_mbid)" +
                    ");");
            //
            db.execSQL("CREATE TABLE IF NOT EXISTS genre_meta (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "genre_name TEXT NOT NULL, " +
                    "genre_key TEXT NOT NULL UNIQUE " +
                    ");");
            //Containers
            db.execSQL("CREATE TABLE IF NOT EXISTS containers (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "uri TEXT NOT NULL UNIQUE, " +
                    "parent_uri TEXT NOT NULL, " +
                    "authority TEXT NOT NULL, " +
                    "in_error INTEGER DEFAULT 0 " +
                    ");");
            //Track resources
            db.execSQL("CREATE TABLE IF NOT EXISTS tracks (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "uri TEXT NOT NULL UNIQUE, " +
                    "container_id INTEGER NOT NULL REFERENCES containers(_id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "authority TEXT NOT NULL, " +
                    "track_name TEXT NOT NULL, " +
                    "track_key TEXT NOT NULL, " +
                    "artist_name TEXT, " +
                    "artist_key TEXT, " +
                    "album_name TEXT, " +
                    "album_key TEXT, " +
                    "album_artist_name TEXT, " +
                    "album_artist_key TEXT, " +
                    "track_number INTEGER, " +
                    "disc_number INTEGER, " +
                    "compilation INTEGER, " +
                    "genre TEXT, " +
                    "genre_key TEXT, " +
                    "artwork_uri TEXT, " +
                    "res_uri TEXT NOT NULL, " +
                    "res_headers TEXT, " +
                    "res_size INTEGER, " +
                    "res_mime_type TEXT, " +
                    "res_last_modified INTEGER, " + //opaque provided by library
                    "res_bitrate INTEGER, " +
                    "res_duration INTEGER, " +
                    "date_added INTEGER NOT NULL " +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS track_meta (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "track_id INTEGER REFERENCES tracks(_id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "artist_id INTEGER REFERENCES artist_meta(_id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "album_id INTEGER REFERENCES album_meta(_id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "genre_id INTEGER REFERENCES genre_meta(_id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "track_name TEXT, " +
                    "track_key TEXT, " +
                    "track_number INTEGER, " +
                    "disc_number INTEGER, " +
                    "compilation INTEGER, " +
                    "mime_type TEXT, " +
                    "bitrate INTEGER, " +
                    "duration INTEGER " +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS playlist_meta (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "playlist_name TEXT NOT NULL, " +
                    "date_added INTEGER, " +
                    "date_modified INTEGER" +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS playlist_track_meta (" +
                    "playlist_id INTEGER REFERENCES playlist_meta(_id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "track_id INTEGER REFERENCES track_meta(_id), " +//this has a trigger to reorder
                    "play_order INTEGER " +
                    ");");

            db.execSQL("CREATE VIEW IF NOT EXISTS artist_info AS SELECT " +
                    "a1._id, " +
                    "a1.artist_name as name, " +
                    "a1.artist_key, " +
                    "COUNT(DISTINCT a2._id) AS number_of_albums, " +
                    "COUNT(DISTINCT t1._id) AS number_of_tracks, "+
                    "artist_bio_summary as summary, " +
                    "artist_mbid as mbid " +
                    "FROM artist_meta a1 " +
                    "LEFT OUTER JOIN album_meta a2 ON a2.album_artist_id = a1._id " +
                    "LEFT OUTER JOIN track_meta t1 ON t1.artist_id = a1._id " +
                    "GROUP BY a1._id" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS album_artist_info AS SELECT " +
                    "a1._id, " +
                    "a1.name, " +
                    "a1.artist_key, " +
                    "a1.number_of_albums, " +
                    "a1.number_of_tracks, "+
                    "a1.summary, " +
                    "a1.mbid " +
                    "FROM artist_info a1 " +
                    "JOIN album_meta a2 ON a2.album_artist_id = a1._id " +
                    "GROUP BY a1._id" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS album_info AS SELECT " +
                    "a1._id, " +
                    "a1.album_name as name, " +
                    "a1.album_key, " +
                    "a2.artist_name as artist, " +
                    "album_artist_id as artist_id, " +
                    "a2.artist_key, " +
                    "COUNT(DISTINCT t1._id) as track_count, " +
                    "album_bio_summary as summary, " +
                    "album_mbid as mbid " +
                    "FROM album_meta a1 " +
                    "LEFT OUTER JOIN artist_meta a2 on a1.album_artist_id = a2._id " +
                    "LEFT OUTER JOIN track_meta t1 on a1._id = t1.album_id " +
                    "GROUP BY a1._id" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS genre_info AS SELECT " +
                    "g1._id, " +
                    "g1.genre_name as name, " +
                    "g1.genre_key, " +
                    "COUNT(t1._id) as number_of_tracks, " +
                    "COUNT(DISTINCT t1.album_id) as number_of_albums, " +
                    "COUNT(DISTINCT t1.artist_id) as number_of_artists " +
                    "FROM genre_meta g1 " +
                    "LEFT OUTER JOIN track_meta t1 ON g1._id = t1.genre_id " +
                    "GROUP BY g1._id" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS playlist_info AS SELECT " +
                    "p._id, " +
                    "p.playlist_name as name, " +
                    "p.date_added, " +
                    "p.date_modified, " +
                    "COUNT(DISTINCT t.artist_id) AS number_of_artists, " +
                    "COUNT(DISTINCT t.album_id) AS number_of_albums, " +
                    "COUNT(DISTINCT t.genre_id) AS number_of_genres, " +
                    "COUNT(DISTINCT t.track_id) AS number_of_tracks " +
                    "FROM playlist_meta p " +
                    "LEFT OUTER JOIN playlist_track_meta pt ON pt.playlist_id = p._id " +
                    "LEFT OUTER JOIN track_meta t ON pt.track_id = t._id " +
                    "GROUP BY p._id" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS track_parent_map as SELECT " +
                    "t1._id, " +
                    "c1.uri " +
                    "FROM tracks t1 " +
                    "INNER JOIN containers c1 ON t1.container_id = c1._id" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS track_info as SELECT " +
                    "t2.uri, " +
                    "tpm.uri as parent_uri, " +
                    "t1._id," +
                    "coalesce(t1.track_name, t2.track_name) as name, " +
                    "coalesce(t1.track_key, t2.track_key) as track_key, " +
                    "coalesce(a1.name, t2.artist_name) as artist," +
                    "t1.artist_id, " +
                    "coalesce(a2.name, t2.album_name) as album, " +
                    "t1.album_id, " +
                    "coalesce(a2.artist, t2.album_artist_name) as album_artist, " +
                    "a2.artist_id as album_artist_id, " +
                    "coalesce(t1.track_number, t2.track_number) as track, " +
                    "coalesce(t1.disc_number, t2.disc_number) as disc, " +
                    "coalesce(t1.compilation, t2.compilation) as compilation, " +
                    "coalesce(g1.genre_name, t2.genre) as genre, " +
                    "t1.genre_id, " +
                    "t2.artwork_uri, " +
                    "t2.res_uri, " +
                    "t2.res_headers, " +
                    "t2.res_size, " +
                    "coalesce(t1.mime_type, t2.res_mime_type) as res_mime_type, " +
                    "coalesce(t1.bitrate, t2.res_bitrate) as res_bitrate, " +
                    "coalesce(t1.duration, t2.res_duration) as res_duration, " +
                    "t2.date_added " +
                    "FROM track_meta t1 " +
                    "LEFT OUTER JOIN tracks t2 ON t1.track_id = t2._id " +
                    "LEFT OUTER JOIN track_parent_map tpm ON t1.track_id = tpm._id " +
                    "LEFT OUTER JOIN artist_info a1 ON t1.artist_id = a1._id " +
                    "LEFT OUTER JOIN album_info a2 ON t1.album_id = a2._id " +
                    "LEFT OUTER JOIN genre_meta g1 ON t1.genre_id = g1._id " +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS playlist_track_info AS SELECT " +
                    "* " +
                    "FROM track_info t1 " +
                    "JOIN playlist_track_meta t2 ON t1._id = t2.track_id " +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS genre_album_map AS SELECT " +
                    "g1._id, " +
                    "t1.album_id, " +
                    "t1.album as album_name, " +
                    "t1.album_artist, " +
                    "t1.artwork_uri " +
                    "FROM genre_meta g1 " +
                    "JOIN track_info t1 ON g1._id = t1.genre_id " +
                    "GROUP BY t1.album_id" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS artist_album_map AS SELECT " +
                    "a1._id, " +
                    "t1.album_id, " +
                    "t1.album as album_name, " +
                    "t1.album_artist, " +
                    "t1.artwork_uri " +
                    "FROM artist_meta a1 " +
                    "JOIN track_info t1 ON a1._id = t1.artist_id " +
                    "GROUP BY t1.album_id" +
                    ";");

            db.execSQL("CREATE VIEW IF NOT EXISTS playlist_album_map AS SELECT " +
                    "pt.playlist_id as _id, " +
                    "t1.album_id, " +
                    "t1.album as album_name, " +
                    "t1.album_artist, " +
                    "t1.artwork_uri " +
                    "FROM playlist_track_meta pt " +
                    "JOIN track_info t1 ON pt.track_id = t1._id " +
                    "GROUP BY t1.album_id" +
                    ";");

            // For a given artist_id, provides the album_id for albums on
            // which the artist appears.
//            db.execSQL("CREATE VIEW IF NOT EXISTS artists_albums_map AS " +
//                    "SELECT DISTINCT artist_id, album_id FROM track_meta" +
//                    ";");

            db.execSQL("CREATE INDEX IF NOT EXISTS artist_key_idx on artist_meta(artist_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS album_key_idx on album_meta(album_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS genre_key_idx on genre_meta(genre_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS artist_id_idx on artist_meta(_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS album_id_idx on album_meta(_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS track_id_idx on track_meta(_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS tracks_id_idx on tracks(_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS containers_uri_idx on containers(uri);");
            db.execSQL("CREATE INDEX IF NOT EXISTS playback_settings_key_idx on playback_settings(key);");


            //Cleanup albums when tracks are deleted
            db.execSQL("CREATE TRIGGER IF NOT EXISTS albums_cleanup AFTER DELETE ON track_meta " +
                    "FOR EACH ROW " +
                    "WHEN (SELECT COUNT(_id) FROM track_meta WHERE album_id=OLD.album_id) = 0 " +
                    "BEGIN " +
                    "DELETE FROM album_meta WHERE _id=OLD.album_id; " +
                    "END");
            //Cleanup artists when tracks are deleted
            db.execSQL("CREATE TRIGGER IF NOT EXISTS artists_cleanup AFTER DELETE ON track_meta " +
                    "FOR EACH ROW " +
                    "WHEN (SELECT COUNT(_id) FROM track_meta WHERE artist_id=OLD.artist_id) = 0 " +
                    "BEGIN " +
                    "DELETE FROM artist_meta WHERE _id=OLD.artist_id; " +
                    "END");
            //Cleanup genres when tracks are deleted
            db.execSQL("CREATE TRIGGER IF NOT EXISTS genres_cleanup AFTER DELETE ON track_meta " +
                    "FOR EACH ROW " +
                    "WHEN (SELECT COUNT(_id) FROM track_meta WHERE genre_id=OLD.genre_id) = 0 " +
                    "BEGIN " +
                    "DELETE FROM genre_meta WHERE _id=OLD.genre_id; " +
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
        db.execSQL("PRAGMA encoding = 'UTF-8';");
    }

    static class ErrorHandler implements DatabaseErrorHandler {
        @Override
        public void onCorruption(SQLiteDatabase dbObj) {
            //delete the db then kill the app
            FileUtils.deleteQuietly(new File(dbObj.getPath()));
            throw new RuntimeException("Corrupt database");
        }
    }

}
