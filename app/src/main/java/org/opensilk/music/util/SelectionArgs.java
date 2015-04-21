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

package org.opensilk.music.util;

/**
 * Created by drew on 6/24/14.
 */
public class SelectionArgs {

    private static final String[] ONE_AND_EMPTY;
    private static final String[] EMPTY;

    public static final String[] LOCAL_SONG;
    public static String[] LOCAL_ALBUM_SONGS(final long albumId) {
        return new String[] {"1", "''", String.valueOf(albumId)};
    }
    public static String[] LAST_ADDED() {
        final int fourWeeks = 4 * 3600 * 24 * 7;
        return new String[] {"1", "''", String.valueOf(System.currentTimeMillis() / 1000 - fourWeeks)};
    }
    public static String[] LOCAL_ARTIST_SONGS(final long artistId) {
        return new String[] {"1", String.valueOf(artistId)};
    }
    public static final String[] GENRE;
    public static final String[] GENRE_MEMBER;
    public static final String[] PLAYLIST_MEMBER;
    public static final String[] PLAYLIST_SONGS;
    public static final String[] SONG_GROUP;
    public static final String[] LOCAL_ALBUM;
    public static final String[] LOCAL_ARTIST;
    public static final String[] PLAYLIST;
    public static final String[] GENRE_SONGS;

    static {
        ONE_AND_EMPTY = new String[]{"1","''"};
        EMPTY = new String[]{"''"};

        LOCAL_SONG = ONE_AND_EMPTY;
        GENRE = EMPTY;
        GENRE_MEMBER = ONE_AND_EMPTY;
        PLAYLIST_MEMBER = ONE_AND_EMPTY;
        PLAYLIST_SONGS = ONE_AND_EMPTY;
        SONG_GROUP = ONE_AND_EMPTY;
        LOCAL_ALBUM = EMPTY;
        LOCAL_ARTIST = EMPTY;
        PLAYLIST = EMPTY;
        GENRE_SONGS = ONE_AND_EMPTY;
    }
}
