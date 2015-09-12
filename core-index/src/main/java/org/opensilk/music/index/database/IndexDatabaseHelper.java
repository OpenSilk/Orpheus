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
import android.provider.MediaStore;

import org.opensilk.common.core.dagger2.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 8/25/15.
 */
@Singleton
public class IndexDatabaseHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 4;
    public static final String DB_NAME = "music.db";

    @Inject
    public IndexDatabaseHelper(@ForApplication Context context) {
        super(context, DB_NAME, null, DB_VERSION, null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS artist_images (" +
                "artist_id INTEGER PRIMARY KEY, " +
                "rel_path TEXT UNIQUE, " +
                "date_modified INTEGER" +
                ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS artist_thumbs (" +
                "artist_id INTEGER PRIMARY KEY, " +
                "rel_path TEXT UNIQUE, " +
                "date_modified INTEGER" +
                ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS album_images (" +
                "album_id INTEGER PRIMARY KEY, " +
                "rel_path TEXT UNIQUE, " +
                "date_modified INTEGER" +
                ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS album_thumbs (" +
                "album_id INTEGER PRIMARY KEY, " +
                "rel_path TEXT UNIQUE, " +
                "date_modified INTEGER" +
                ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS artist_meta (" +
                "artist_id INTEGER PRIMARY KEY, " +
                "artist_name TEXT NOT NULL, " +
                "artist_key TEXT NOT NULL COLLATE NOCASE, " +
                "artist_bio_summary TEXT, " +
                "artist_bio_content TEXT, " +
                "artist_bio_date_modified INTEGER, " +
                "artist_mbid TEXT, " +
                "UNIQUE(artist_key, artist_mbid) ON CONFLICT IGNORE" +
                ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS album_meta (" +
                "album_id INTEGER PRIMARY KEY, " +
                "album_name TEXT NOT NULL, " +
                "album_key TEXT NOT NULL COLLATE NOCASE, " +
                "album_bio_summary TEXT, " +
                "album_bio_content TEXT, " +
                "album_bio_date_modified INTEGER, " +
                "album_mbid TEXT NOT NULL COLLATE NOCASE, " +
                "UNIQUE(album_key, album_mbid) ON CONFLICT IGNORE" +
                ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS track_meta (" +
                "track_id INTEGER PRIMARY KEY, " +
                "track_name TEXT NOT NULL, " +
                "track_key TEXT NOT NULL COLLATE NOCASE, " +
                "duration INTEGER, " +
                "track_number INTEGER, " +
                "disc_number INTEGER DEFAULT 1, " +
                "genre TEXT, " +
                "artist_id INTEGER REFERENCES artist_meta(artist_id) ON DELETE CASCADE, " +
                "album_artist_id INTEGER REFERENCES artist_meta(artist_id) ON DELETE CASCADE, " +
                "album_id INTEGER REFERENCES album_meta(album_id) ON DELETE CASCADE," +
                "UNIQUE(artist_id,album_id,track_key,track_number,disc_number) ON CONFLICT IGNORE" +
                ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS locations (" +
                "location_id INTEGER PRIMARY KEY, " +
                "track_id INTEGER REFERENCES track_meta(track_id) ON DELETE CASCADE, " +
                "authority TEXT NOT NULL, " +
                "uri TEXT NOT NULL UNIQUE, " +
                "size INTEGER, " +
                "mime_type TEXT, " +
                "date_added INTEGER, " +
                "date_modified INTEGER, " +
                "bitrate INTEGER, " +
                ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS playlists (" +
                "playlist_id INTEGER PRIMARY KEY, " +
                "playlist_name TEXT UNIQUE," +
                ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS playlist_tracks (" +
                "playlist_id INTEGER REFERENCES playlists(playlist_id), " +
                "track_id INTEGER REFERENCES track_meta(track_id), " +
                "track_number INTEGER, " +
                "UNIQUE(playlist_id, track_id, track_number)" +
                ");");

        // Provides a unified track/artist/album info view.
        db.execSQL("CREATE VIEW IF NOT EXISTS tracks as SELECT * FROM track_meta " +
                "LEFT OUTER JOIN artist_meta ON track_meta.artist_id = artist_meta.artist_id " +
                "LEFT OUTER JOIN album_meta ON track_meta.album_id = album_meta.album_id;");

        // Provides some extra info about artists, like the number of tracks
        // and albums for this artist
        db.execSQL("CREATE VIEW IF NOT EXISTS artist_info AS " +
                "SELECT artist_id AS _id, artist, artist_key, " +
                "COUNT(DISTINCT album_key) AS number_of_albums, " +
                "COUNT(*) AS number_of_tracks FROM audio WHERE is_music=1 "+
                "GROUP BY artist_key;");

        // Provides extra info albums, such as the number of tracks
        db.execSQL("CREATE VIEW IF NOT EXISTS album_info AS " +
                "SELECT audio.album_id AS _id, album, album_key, " +
                "MIN(year) AS minyear, " +
                "MAX(year) AS maxyear, artist, artist_id, artist_key, " +
                "count(*) AS track_count" +
                ",album_art._data AS album_art" +
                " FROM audio LEFT OUTER JOIN album_art ON audio.album_id=album_art.album_id" +
                " WHERE is_music=1 GROUP BY audio.album_id;");

        // For a given artist_id, provides the album_id for albums on
        // which the artist appears.
        db.execSQL("CREATE VIEW IF NOT EXISTS artists_albums_map AS " +
                "SELECT DISTINCT artist_id, album_id FROM audio_meta;");
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

        // Contains audio playlist definitions
        db.execSQL("CREATE TABLE IF NOT EXISTS audio_playlists (" +
                "_id INTEGER PRIMARY KEY," +
                "_data TEXT," +  // _data is path for file based playlists, or null
                "name TEXT NOT NULL," +
                "date_added INTEGER," +
                "date_modified INTEGER" +
                ");");

        // Contains mappings between audio playlists and audio files
        db.execSQL("CREATE TABLE IF NOT EXISTS audio_playlists_map (" +
                "_id INTEGER PRIMARY KEY," +
                "audio_id INTEGER NOT NULL," +
                "playlist_id INTEGER NOT NULL," +
                "play_order INTEGER NOT NULL" +
                ");");

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
        // create the index that updates the database to schema version 65
        db.execSQL("CREATE INDEX IF NOT EXISTS titlekey_index on audio_meta(title_key);");
        db.execSQL("CREATE INDEX IF NOT EXISTS albumkey_index on albums(album_key);");
        db.execSQL("CREATE INDEX IF NOT EXISTS artistkey_index on artists(artist_key);");
        db.execSQL("CREATE INDEX IF NOT EXISTS album_id_idx on audio_meta(album_id);");
        db.execSQL("CREATE INDEX IF NOT EXISTS artist_id_idx on audio_meta(artist_id);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            //Drop tables from orpheus 0.x - 2.x
            db.execSQL("DROP TABLE IF EXISTS genres;");
            db.execSQL("DROP TABLE IF EXISTS playlists;");
            db.execSQL("DROP TABLE IF EXISTS recent;");
        }
        onCreate(db);
    }
}
