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

package com.andrew.apollo.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.andrew.apollo.model.RecentSong;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;

/**
 * Created by drew on 6/26/14.
 */
public class MusicProviderUtil {

    public static long insertSong(Context context, Song song) {
        Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{ BaseColumns._ID },
                // These are the only mandatory fields
                MusicStore.Cols.IDENTITY  + "=? AND " + MusicStore.Cols.NAME + "=? AND " + MusicStore.Cols.DATA_URI + "=?",
                new String[]{ song.identity, song.name, song.dataUri.toString() },
                null);
        if (c != null) {
            long ret = -1;
            if (c.getCount() > 0 && c.moveToFirst()) {
                ret = c.getLong(0);
            }
            c.close();
            if (ret > 0) {
                return ret;
            }
        }
        ContentValues values = makeSongContentValues(song);
        Uri uri = context.getContentResolver().insert(MusicProvider.RECENTS_URI, values);
        if (uri != null) {
            try {
                return Long.decode(uri.getLastPathSegment());
            } catch (NumberFormatException ignored) { }
            return Long.decode(uri.getLastPathSegment());
        }
        return -1;
    }

    public static void updatePlaycount(Context context, long id) {
        Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{ MusicStore.Cols.PLAYCOUNT },
                BaseColumns._ID + "=?",
                new String[]{ String.valueOf(id) },
                null);
        if (c != null) {
            int playcount = 0;
            if (c.getCount() > 0 && c.moveToFirst()) {
                playcount = c.getInt(0);
            }
            ContentValues values = new ContentValues();
            values.put(MusicStore.Cols.PLAYCOUNT, ++playcount);
            values.put(MusicStore.Cols.LAST_PLAYED, System.currentTimeMillis());
            int count = context.getContentResolver().update(MusicProvider.RECENTS_URI,
                    values,
                    BaseColumns._ID + "=?",
                    new String[]{ String.valueOf(id) });
            c.close();
        }
    }

    public static long getIdforSong(Context context, Song song) {
        Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{ BaseColumns._ID },
                // These are the only mandatory fields
                MusicStore.Cols.IDENTITY  + "=?" + MusicStore.Cols.NAME + "=?" + MusicStore.Cols.DATA_URI + "=?",
                new String[]{ song.identity, song.name, song.dataUri.toString() },
                null);
        if (c != null) {
            long ret = -1;
            if (c.getCount() > 0 && c.moveToFirst()) {
                ret = c.getLong(0);
            }
            c.close();
            if (ret > 0) {
                return ret;
            }
        }
        return -1;
    }

    public static RecentSong getRecentSong(Context context, long id) {
        RecentSong s = null;
        Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                Projections.RECENT_SONGS,
                BaseColumns._ID + "=?",
                new String[]{String.valueOf(id)},
                null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                s = CursorHelpers.makeRecentSongFromCursor(c);
            }
            c.close();
        }
        return s;
    }

    public static long[] transformListToLocalIds(Context context, long[] list) {
        long[] newlist = new long[list.length];

        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < list.length; i++) {
            selection.append(list[i]);
            if (i < list.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");

        Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{
                        MusicStore.Cols.IDENTITY,
                        MusicStore.Cols.ISLOCAL,
                }, selection.toString(), null, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                int ii=0;
                do {
                    boolean islocal = c.getInt(c.getColumnIndexOrThrow(MusicStore.Cols.ISLOCAL)) == 1;
                    if (islocal) {
                        try {
                            long id = Long.decode(c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.IDENTITY)));
                            newlist[ii++] = id;
                        } catch (NumberFormatException ex) {
                            //pass
                        }
                    }
                } while (c.moveToNext());
            }
        }
        return newlist;
    }

    public static ContentValues makeSongContentValues(Song song) {
        ContentValues values = new ContentValues(15);
        values.put(MusicStore.Cols.IDENTITY, song.identity);
        values.put(MusicStore.Cols.NAME, song.name);
        values.put(MusicStore.Cols.ALBUM_NAME, song.albumName);
        values.put(MusicStore.Cols.ARTIST_NAME, song.artistName);
        values.put(MusicStore.Cols.ALBUM_ARTIST_NAME, song.albumArtistName);
        values.put(MusicStore.Cols.ALBUM_IDENTITY, song.albumIdentity);
        values.put(MusicStore.Cols.DURATION, song.duration);
        values.put(MusicStore.Cols.DATA_URI, song.dataUri.toString());
        values.put(MusicStore.Cols.ARTWORK_URI, song.artworkUri.toString());
        values.put(MusicStore.Cols.MIME_TYPE, song.mimeType);
        values.put(MusicStore.Cols.ISLOCAL, "media".equals(song.dataUri.getAuthority()) ? 1 : 0);
        values.put(MusicStore.Cols.PLAYCOUNT, 0);
        values.put(MusicStore.Cols.LAST_PLAYED, 0);
        return values;
    }
}
