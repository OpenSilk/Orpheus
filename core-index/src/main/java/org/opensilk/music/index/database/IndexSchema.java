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
    public static final String UNKNOWN_STRING = "<unknown>";

    public interface ArtistInfo extends BaseColumns {
        String TABLE = "artist_info";
        String ARTIST = "name";
        String ARTIST_KEY = "artist_key";
        String NUMBER_OF_ALBUMS = "number_of_albums";
        String NUMBER_OF_TRACKS = "number_of_tracks";
        String BIO = "bio";
        String SUMMARY = "summary";
        String MBID = "mbid";
    }

    public interface AlbumInfo extends BaseColumns {
        String TABLE = "album_info";
        String ALBUM = "name";
        String ALBUM_KEY = "album_key";
        String ARTIST = "artist";
        String ARTIST_KEY = "artist_key";
        String ARTIST_ID = "artist_id";
        String TRACK_COUNT = "track_count";
        String BIO = "bio";
        String SUMMARY = "summary";
        String MBID = "mbid";
    }

    public interface TrackInfo extends BaseColumns {
        String TABLE = "track_info";
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
        String URI = "uri";
        String SIZE = "size";
        String MIME_TYPE = "mime_type";
        String DATE_ADDED = "date_added";
        String BITRATE = "bitrate";
        String DURATION = "duration";
    }

    public interface ArtistMeta extends BaseColumns {
        String TABLE = "artist_meta";
        String ARTIST_NAME = "artist_name";
        String ARTIST_KEY = "artist_key";
        String ARTIST_BIO_SUMMARY = "artist_bio_summary";
        String ARTIST_BIO_CONTENT = "artist_bio_content";
        String ARTIST_BIO_DATE_MOD = "artist_bio_date_modified";
        String ARTIST_MBID = "artist_mbid";
    }

    public interface AlbumMeta extends BaseColumns {
        String TABLE = "album_meta";
        String ALBUM_NAME = "album_name";
        String ALBUM_KEY = "album_key";
        String ALBUM_BIO_SUMMARY = "album_bio_summary";
        String ALBUM_BIO_CONTENT = "album_bio_content";
        String ALBUM_BIO_DATE_MOD = "album_bio_date_modified";
        String ALBUM_MBID = "album_mbid";
        String ALBUM_ARTIST_ID = "album_artist_id";
    }

    public interface TrackResMeta extends BaseColumns {
        String TABLE = "track_resources";
        String URI = "uri";
        String AUTHORITY = "authority";
        String TRACK_NAME = "track_name";
        String TRACK_KEY = "track_key";
        String SIZE = "size";
        String MIME_TYPE = "mime_type";
        String DATE_ADDED = "date_added";
        String LAST_MOD = "last_modified";
        String BITRATE = "bitrate";
        String DURATION = "duration";
        String TRACK_NUMBER = "track_number";
        String DISC_NUMBER = "disc_number";
        String GENRE = "genre";
        String CATEGORY = "category";
        String ARTIST_ID = "artist_id";
        String ALBUM_ID = "album_id";
        String CONTAINER_ID = "container_id";

        int CATEGORY_MUSIC = 1;
        int CATEGORY_PODCAST = 2;
        int CATEGORY_AUDIOBOOK = 3;
    }

    public interface Containers extends BaseColumns {
        String TABLE = "containers";
        String URI = "uri";
        String PARENT_URI = "parent_uri";
        String AUTHORITY = "authority";
        String IN_ERROR = "in_error";
    }

}
