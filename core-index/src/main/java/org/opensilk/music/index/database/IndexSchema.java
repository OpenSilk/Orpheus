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

import android.database.DatabaseUtils;
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

    public interface ContainerMeta extends BaseColumns {
        String AUTHORITY = "authority";
        String LIBRARY_IDENT = "library_ident";
        String CONTAINER_IDENT = "container_ident";
    }

    public interface ContainerChildren extends BaseColumns {
        String PARENT_IDENT = "parent_ident";
        String CONTAINER_IDENT = "container_ident";
    }

    public interface TrackMeta extends BaseColumns {
        String TRACK_NAME = "track_name";
        String TRACK_NAME_KEY = "track_name_key";
        String DURATION = "duration";
        String ARTIST_ID = "artist_id";
        String ALBUM_ID = "album_id";
        /**
         * Position in album
         */
        String ALBUM_INDEX = "album_index";
        String GENRE_ID = "genre_id";
    }

    public interface Track {
        String TRACK_ID = "track_id";
        String TITLE = "title";
        String ALBUM = "album";
        String ARTIST = "artist";
        String ALBUM_ARTIST = "album_artist";
        String DURATION = "duration";

    }

    public interface ArtistMeta extends BaseColumns {
        String ARTIST_NAME = "artist_name";
        String ARTIST_NAME_KEY = "artist_name_key";
        String MBID = "mbid";
        String BIO = "bio";
        String URL = "url";
        String IMAGE_ID = "image_id";
        String THUMB_ID = "thumb_id";
    }

    public interface AlbumMeta extends BaseColumns {
        String ALBUM_NAME = "album_name";
        String ALBUM_NAME_KEY = "album_name_key";
        String ALBUM_ARTIST_ID = "album_artist_id";
        String YEAR = "year";
        String MBID = "mbid";
        String IMAGE_ID = "image_id";
        String THUMB_ID = "thumb_id";
    }

    public interface GenreMeta extends BaseColumns {
        String GENRE = "genre";
        String GENRE_KEY = "genre_key";
    }

    public interface PlaylistMeta extends BaseColumns {
        String PLAYLIST = "playlist";
        String PLAYLIST_KEY = "playlist_key";
    }

    public interface PlayistTracks {
        String PLAYLIST_ID = "playlist_id";
        String TRACK_ID = "track_id";
        /**
         * Position in playlist
         */
        String PLAYLIST_INDEX = "playlist_index";
    }

    public interface Image extends BaseColumns {
        String DATA = "_data";
    }

    public interface Thumb extends BaseColumns {
        String DATA = "_data";
    }

    public interface TrackLocation extends BaseColumns {
        String TRACK_ID = "track_id";
        String SIZE = "size";
        String MIME_TYPE = "mime_type";
        String DATE_ADDED = "date_added";
        String DATE_MODIFIED = "date_modified";
        String BITRATE = "bitrate";
        String AUTHORITY = "authority";
        String LIBRARY_IDENT = "library_ident";
        String DATA = "_data";
    }

    /**
     * Converts a name to a "key" that can be used for grouping, sorting
     * and searching.
     * The rules that govern this conversion are:
     * - remove 'special' characters like ()[]'!?.,
     * - remove leading/trailing spaces
     * - convert everything to lowercase
     * - remove leading "the ", "an " and "a "
     * - remove trailing ", the|an|a"
     * - remove accents. This step leaves us with CollationKey data,
     *   which is not human readable
     *
     * from MediaStore.java
     *
     * @param name The artist or album name to convert
     * @return The "key" for the given name.
     */
    public static String keyFor(String name) {
        if (name != null)  {
            boolean sortfirst = false;
            if (name.equals(UNKNOWN_STRING)) {
                return "\001";
            }
            // Check if the first character is \001. We use this to
            // force sorting of certain special files, like the silent ringtone.
            if (name.startsWith("\001")) {
                sortfirst = true;
            }
            name = name.trim().toLowerCase();
            if (name.startsWith("the ")) {
                name = name.substring(4);
            }
            if (name.startsWith("an ")) {
                name = name.substring(3);
            }
            if (name.startsWith("a ")) {
                name = name.substring(2);
            }
            if (name.endsWith(", the") || name.endsWith(",the") ||
                    name.endsWith(", an") || name.endsWith(",an") ||
                    name.endsWith(", a") || name.endsWith(",a")) {
                name = name.substring(0, name.lastIndexOf(','));
            }
            name = name.replaceAll("[\\[\\]\\(\\)\"'.,?!]", "").trim();
            if (name.length() > 0) {
                // Insert a separator between the characters to avoid
                // matches on a partial character. If we ever change
                // to start-of-word-only matches, this can be removed.
                StringBuilder b = new StringBuilder();
                b.append('.');
                int nl = name.length();
                for (int i = 0; i < nl; i++) {
                    b.append(name.charAt(i));
                    b.append('.');
                }
                name = b.toString();
                String key = DatabaseUtils.getCollationKey(name);
                if (sortfirst) {
                    key = "\001" + key;
                }
                return key;
            } else {
                return "";
            }
        }
        return null;
    }

}
