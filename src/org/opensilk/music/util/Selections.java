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

import android.provider.BaseColumns;
import android.provider.MediaStore;

/**
 * Created by drew on 6/24/14.
 */
public class Selections {

    public static final String LOCAL_SONG;
    public static final String LOCAL_ALBUM_SONGS;
    public static final String LAST_ADDED;
    public static final String LOCAL_ARTIST_SONGS;
    public static String LOCAL_ALBUMS(long[] albumIds) {
        final StringBuilder selection = new StringBuilder();
        selection.append(MediaStore.Audio.Albums._ID + " IN (");
        for (int i = 0; i < albumIds.length; i++) {
            selection.append(albumIds[i]);
            if (i < albumIds.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        return selection.toString();
    }
    public static final String GENRE;
    public static final String GENRE_MEMBER;
    public static final String PLAYLIST_MEMBER;

    static {
        LOCAL_SONG = MediaStore.Audio.AudioColumns.IS_MUSIC + "=? AND " + MediaStore.Audio.AudioColumns.TITLE + "!=?";
        LOCAL_ALBUM_SONGS = LOCAL_SONG + " AND " + MediaStore.Audio.AudioColumns.ALBUM_ID + "=?";
        LAST_ADDED = LOCAL_SONG + " AND " + MediaStore.Audio.Media.DATE_ADDED + ">?";
        LOCAL_ARTIST_SONGS = MediaStore.Audio.AudioColumns.IS_MUSIC + "=? AND " + MediaStore.Audio.AudioColumns.ARTIST_ID + "=?";
        GENRE = MediaStore.Audio.Genres.NAME + "!=?";
        GENRE_MEMBER = LOCAL_SONG;
        PLAYLIST_MEMBER = LOCAL_SONG;
    }
}
