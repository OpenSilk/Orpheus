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

import android.provider.BaseColumns;

/**
 * Created by drew on 8/26/15.
 */
public class IndexSchema {

    /**
     * The string that is used when a media attribute is not known. For example,
     * if an audio file does not have any meta data, the artist and album columns
     * will be set to this value.
     *
     * from MediaStore.java
     */
    public static final String UNKNOWN_STRING = "[unknown]";

    public interface Info {

        interface Artist extends BaseColumns {
            String TABLE = "artist_info";
            String ALBUM_ARSTIST_TABLE = "album_artist_info";
            String TITLE = "name";
            String ARTIST_KEY = "artist_key";
            String NUMBER_OF_ALBUMS = "number_of_albums";
            String NUMBER_OF_TRACKS = "number_of_tracks";
            String SUMMARY = "summary";
            String MBID = "mbid";
            String AUTHORITY = "authority";
        }

        interface Album extends BaseColumns {
            String TABLE = "album_info";
            String TITLE = "name";
            String ALBUM_KEY = "album_key";
            String ARTIST = "artist";
            String ARTIST_KEY = "artist_key";
            String ARTIST_ID = "artist_id";
            String TRACK_COUNT = "track_count";
            String SUMMARY = "summary";
            String MBID = "mbid";
            String AUTHORITY = "authority";
        }

        interface Genre extends BaseColumns {
            String TABLE = "genre_info";
            String TITLE = "name";
            String GENRE_KEY = "genre_key";
            String NUMBER_OF_ARTISTS = "number_of_artists";
            String NUMBER_OF_ALBUMS = "number_of_albums";
            String NUMBER_OF_TRACKS = "number_of_tracks";
            String AUTHORITY = "authority";
        }

        interface Track extends BaseColumns {
            String TABLE = "track_info";
            String URI = "uri";
            String PARENT_URI = "parent_uri";
            String AUTHORITY = "authority";
            String TITLE = "name";
            String TRACK_KEY = "track_key";
            String ARTIST = "artist";
            String ARTIST_ID = "artist_id";
            String ALBUM = "album";
            String ALBUM_ID = "album_id";
            String ALBUM_ARTIST = "album_artist";
            String ALBUM_ARTIST_ID = "album_artist_id";
            String TRACK = "track";
            String DISC = "disc";
            String COMPILATION = "compilation";
            String GENRE = "genre";
            String GENRE_ID = "genre_id";
            String ARTWORK_URI = "artwork_uri";
            String RES_URI = "res_uri";
            String RES_HEADERS = "res_headers";
            String RES_SIZE = "res_size";
            String RES_MIME_TYPE = "res_mime_type";
            String RES_BITRATE = "res_bitrate";
            String RES_DURATION = "res_duration";
            String RES_LAST_MOD = "res_last_modified";
            String DATE_ADDED = "date_added";
        }

        interface Playlist extends BaseColumns {
            String TABLE = "playlist_info";
            String NAME = "name";
            String NUMBER_OF_ARTISTS = "number_of_artists";
            String NUMBER_OF_ALBUMS = "number_of_albums";
            String NUMBER_OF_GENRES = "number_of_genres";
            String NUMBER_OF_TRACKS = "number_of_tracks";
        }

        interface PlaylistTrack extends Track {
            String TABLE = "playlist_track_info";
            String TRACK_ID = "track_id";
            String PLAYLIST_ID = "playlist_id";
            String PLAY_ORDER = "play_order";
        }

    }

    public interface Meta {

        interface Artist extends BaseColumns {
            String TABLE = "artist_meta";
            String ARTIST_NAME = "artist_name";
            String ARTIST_KEY = "artist_key";
            String ARTIST_BIO_SUMMARY = "artist_bio_summary";
            String ARTIST_BIO_DATE_MOD = "artist_bio_date_modified";
            String ARTIST_MBID = "artist_mbid";
            String AUTHORITY = "authority";
        }

        interface Album extends BaseColumns {
            String TABLE = "album_meta";
            String ALBUM_NAME = "album_name";
            String ALBUM_KEY = "album_key";
            String ALBUM_BIO_SUMMARY = "album_bio_summary";
            String ALBUM_BIO_DATE_MOD = "album_bio_date_modified";
            String ALBUM_MBID = "album_mbid";
            String AUTHORITY = "authority";
            String ALBUM_ARTIST_ID = "album_artist_id";
        }

        interface Genre extends BaseColumns {
            String TABLE = "genre_meta";
            String GENRE_NAME = "genre_name";
            String GENRE_KEY = "genre_key";
            String AUTHORITY = "authority";
        }

        interface Track extends BaseColumns {
            String TABLE = "track_meta";
            String TRACK_ID = "track_id";
            String ARTIST_ID = "artist_id";
            String ALBUM_ID = "album_id";
            String GENRE_ID = "genre_id";
            String TRACK_NAME = "track_name";
            String TRACK_KEY = "track_key";
            String TRACK_NUMBER = "track_number";
            String DISC_NUMBER = "disc_number";
            String COMPILATION = "compilation";
            String MIME_TYPE = "mime_type";
            String BITRATE = "bitrate";
            String DURATION = "duration";
        }

        interface Playlist extends BaseColumns {
            String TABLE = "playlist_meta";
            String NAME = "playlist_name";
            String DATE_ADDED = "date_added";
            String DATE_MODIFIED = "date_modified";
        }

        interface PlaylistTrack {
            String TABLE = "playlist_track_meta";
            String TRACK_ID = "track_id";
            String PLAYLIST_ID = "playlist_id";
            String PLAY_ORDER = "play_order";
        }

    }

    public interface Misc {

        interface AlbumMap extends BaseColumns {
            String ALBUM_ID = "album_id";
            String ALBUM_NAME = "album_name";
            String ALBUM_ARTIST = "album_artist";
            String ARTWORK_URI = "artwork_uri";
        }

        interface GenreAlbumMap extends AlbumMap {
            String TABLE = "genre_album_map";
        }

        interface ArtistAlbumMap extends AlbumMap {
            String TABLE = "artist_album_map";
        }

        interface PlaylistAlbumMap extends AlbumMap {
            String TABLE = "playlist_album_map";
        }
    }

    public interface Containers extends BaseColumns {
        String TABLE = "containers";
        String URI = "uri";
        String PARENT_URI = "parent_uri";
        String AUTHORITY = "authority";
        String IN_ERROR = "in_error";
    }

    public interface Tracks extends BaseColumns {
        String TABLE = "tracks";
        String CONTAINER_ID = "container_id";
        String URI = "uri";
        String AUTHORITY = "authority";
        String TRACK_NAME = "track_name";
        String TRACK_KEY = "track_key";
        String ARTIST_NAME = "artist_name";
        String ARTIST_KEY = "artist_KEY";
        String ALBUM_NAME = "album_name";
        String ALBUM_KEY = "album_key";
        String ALBUM_ARTIST_NAME = "album_artist_name";
        String ALBUM_ARTIST_KEY = "album_artist_key";
        String TRACK_NUMBER = "track_number";
        String DISC_NUMBER = "disc_number";
        String COMPILATION = "compilation";
        String GENRE = "genre";
        String GENRE_KEY = "genre_key";
        String ARTWORK_URI = "artwork_uri";
        String RES_URI = "res_uri";
        String RES_HEADERS = "res_headers";
        String RES_SIZE = "res_size";
        String RES_MIME_TYPE = "res_mime_type";
        String RES_LAST_MOD = "res_last_modified";
        String RES_BITRATE = "res_bitrate";
        String RES_DURATION = "res_duration";
        String DATE_ADDED = "date_added";
    }

    public interface PlaybackSettings {
        String TABLE = "playback_settings";
        String KEY = "key";
        String INT_VALUE = "intVal";
        String TEXT_VALUE = "textVal";
        //TEXT
        String KEY_LAST_QUEUE_LIST = "last_queue_list";
        //INT
        String KEY_LAST_QUEUE_POS = "last_queue_pos";
        //INT
        String KEY_LAST_QUEUE_REPEAT = "last_queue_repeat";
        //INT
        String KEY_LAST_QUEUE_SHUFFLE = "last_queue_shuffle";
        //LONG
        String KEY_LAST_SEEK_POS = "last_seek_pos";
        //INT
        String BROADCAST_META = "broadcast_meta";
    }

}
