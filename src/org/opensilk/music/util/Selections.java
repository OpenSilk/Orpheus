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

import android.provider.MediaStore;

/**
 * Created by drew on 6/24/14.
 */
public class Selections {

    public static final String SONG;
    public static final String ALBUM_SONGS;
    public static final String LAST_ADDED;

    static {
        SONG = new StringBuilder()
                .append(MediaStore.Audio.AudioColumns.IS_MUSIC + "=?")
                .append(" AND " + MediaStore.Audio.AudioColumns.TITLE + "!=?")
                .toString();
        ALBUM_SONGS = new StringBuilder()
                .append(MediaStore.Audio.AudioColumns.IS_MUSIC + "=?")
                .append(" AND " + MediaStore.Audio.AudioColumns.TITLE + "!=?")
                .append(" AND " + MediaStore.Audio.AudioColumns.ALBUM_ID + "=?")
                .toString();
        LAST_ADDED = new StringBuilder()
                .append(MediaStore.Audio.AudioColumns.IS_MUSIC + "=?")
                .append(" AND " + MediaStore.Audio.AudioColumns.TITLE + "!=?")
                .append(" AND " + MediaStore.Audio.Media.DATE_ADDED + ">?")
                .toString();
    }
}
