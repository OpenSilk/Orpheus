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

package org.opensilk.music.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.PreferenceUtils;

/**
 * Created by drew on 2/21/14.
 */
public class AlbumSongCursorLoader extends CursorLoader {

    public AlbumSongCursorLoader(Context context, long albumId) {
        super(context);
        setUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        setProjection(new String[]{
                        /* 0 */
                BaseColumns._ID,
                        /* 1 */
                MediaStore.Audio.AudioColumns.TITLE,
                        /* 2 */
                MediaStore.Audio.AudioColumns.ARTIST,
                        /* 3 */
                MediaStore.Audio.AudioColumns.ALBUM,
                        /* 4 */
                MediaStore.Audio.AudioColumns.DURATION
        });
        setSelection(new StringBuilder()
                .append(MediaStore.Audio.AudioColumns.IS_MUSIC + "=1")
                .append(" AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''")
                .append(" AND " + MediaStore.Audio.AudioColumns.ALBUM_ID + "=" + albumId)
                .toString());
        setSelectionArgs(null);
        setSortOrder(PreferenceUtils.getInstance(context).getAlbumSongSortOrder());
    }

    public static Song getSongFromCursor(Cursor c) {
        // Copy the song Id
        final long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));

        // Copy the song name
        final String songName = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));

        // Copy the artist name
        final String artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));

        // Copy the album name
        final String album = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));

        // Copy the duration
        final long duration = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION));

        // Make the duration label
        final int seconds = (int) (duration / 1000);

        // Create a new song
        return new Song(id, songName, artist, album, seconds);
    }

}
