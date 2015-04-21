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

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.RecentSong;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui2.loader.OrderPreservingCursor;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 6/26/14.
 */
public class MusicProviderUtil {

    public static long insertSong(Context context, Song song) {
        //TODO compare and update if needed
        final Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{ BaseColumns._ID },
                // These are the only mandatory fields
                MusicStore.Cols.IDENTITY  + "=? AND " + MusicStore.Cols.NAME + "=? AND " + MusicStore.Cols.DATA_URI + "=?",
                new String[]{ song.identity, song.name, song.dataUri.toString() },
                null);
        if (c != null) {
            try {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    return c.getLong(0);
                }
            } finally {
                c.close();
            }
        }
        ContentValues values = makeSongContentValues(song);
        if (song instanceof LocalSong) {
            values.put(MusicStore.Cols.ALBUM_ARTIST_NAME, CursorHelpers.getAlbumArtist(context, ((LocalSong) song).albumId));
        }
        Uri uri = context.getContentResolver().insert(MusicProvider.RECENTS_URI, values);
        if (uri != null) {
            try {
                return Long.decode(uri.getLastPathSegment());
            } catch (NumberFormatException ignored) { }
        }
        return -1;
    }

    public static void updatePlaycount(Context context, long id) {
        final Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{ MusicStore.Cols.PLAYCOUNT },
                BaseColumns._ID + "=?",
                new String[]{ String.valueOf(id) },
                null);
        if (c != null) {
            int playcount = 0;
            try {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    playcount = c.getInt(0);
                }
            } finally {
                c.close();
            }
            ContentValues values = new ContentValues();
            values.put(MusicStore.Cols.PLAYCOUNT, ++playcount);
            values.put(MusicStore.Cols.LAST_PLAYED, System.currentTimeMillis());
            try {
                int count = context.getContentResolver().update(MusicProvider.RECENTS_URI,
                        values,
                        BaseColumns._ID + "=?",
                        new String[]{ String.valueOf(id) });
            } catch (Exception ignored) {} // This isnt that important so just dont crash
        }
    }

    @DebugLog @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static List<MediaSession.QueueItem> buildQueueList(Context context, long[] songs) {
        List<MediaSession.QueueItem> list = new ArrayList<>(15);
        if (songs.length == 0) return list;
        final OrderPreservingCursor c = new OrderPreservingCursor(context, songs,
                MusicProvider.RECENTS_URI, Projections.RECENT_SONGS, "", null);
        c.moveToFirst();
        int ii=0;
        do {
            list.add(new MediaSession.QueueItem(
                    new MediaDescription.Builder()
                        .setTitle(c.getString(c.getColumnIndex(MusicStore.Cols.NAME)))
                        .setSubtitle(c.getString(c.getColumnIndex(MusicStore.Cols.ARTIST_NAME)))
                        .setMediaId(c.getString(c.getColumnIndex(MusicStore.Cols.IDENTITY)))
                        .build(),
                    songs[ii]
            ));
        } while (c.moveToNext() && ++ii<15);
        c.close();
        return list;
    }

    public static long getIdForSong(Context context, Song song) {
        final Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{ BaseColumns._ID },
                // These are the only mandatory fields
                MusicStore.Cols.IDENTITY  + "=?" + MusicStore.Cols.NAME + "=?" + MusicStore.Cols.DATA_URI + "=?",
                new String[]{ song.identity, song.name, song.dataUri.toString() },
                null);
        if (c != null) {
            try {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    return c.getLong(0);
                }
            } finally {
                c.close();
            }
        }
        return -1;
    }

    public static long getIdForPath(Context context, String path) {
        final Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[] { BaseColumns._ID },
                MusicStore.Cols.DATA_URI + "=?",
                new String[] {path},
                null
        );
        if (c != null) {
            try {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    return c.getLong(0);
                }
            } finally {
                c.close();
            }
        }
        return -1;
    }

    public static RecentSong getRecentSong(Context context, long id) {
        final Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                Projections.RECENT_SONGS,
                BaseColumns._ID + "=?",
                new String[]{String.valueOf(id)},
                null);
        if (c != null) {
            try {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    return CursorHelpers.makeRecentSongFromRecentCursor(c);
                }
            } finally {
                c.close();
            }
        }
        return null;
    }

    public static long[] transformListToRealIds(Context context, long[] list) {
        final OrderPreservingCursor c = new OrderPreservingCursor(
                context,
                list,
                MusicProvider.RECENTS_URI,
                new String[]{
                        BaseColumns._ID,
                        MusicStore.Cols.IDENTITY,
                        MusicStore.Cols.ISLOCAL,
                },
                null,
                null
        );
        try {
            long[] newlist = new long[list.length];
            if (c.getCount() > 0 && c.moveToFirst()) {
                int ii = 0;
                do {
                    final boolean islocal = c.getInt(c.getColumnIndexOrThrow(MusicStore.Cols.ISLOCAL)) == 1;
                    if (islocal) {
                        try {
                            final long id = Long.decode(c.getString(c.getColumnIndexOrThrow(MusicStore.Cols.IDENTITY)));
                            newlist[ii++] = id;
                        } catch (NumberFormatException ex) {
                            //skip
                        }
                    }
                } while (c.moveToNext());
                if (ii != list.length) {
                    Timber.i("transformListToRealIds() Resizing list %d -> %d", list.length, ii);
                    long[] newlist2 = new long[ii];
                    System.arraycopy(newlist, 0, newlist2, 0, ii);
                    return newlist2;
                } else {
                    return newlist;
                }
            }
        } catch (IllegalArgumentException e) {
            //pass
        } finally {
            c.close();
        }
        return new long[0];
    }

    public static long getRealId(Context context, long id) {
        final Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{ MusicStore.Cols.IDENTITY },
                BaseColumns._ID + "=?",
                new String[]{ String.valueOf(id) }, null);
        if (c != null) {
            try {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    return Long.decode(c.getString(0));
                }
            } catch (NumberFormatException e) {
                //pass
            } finally {
                c.close();
            }
        }
        return -1;
    }

    public static long getAlbumId(Context context, long songId) {
        final Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{ MusicStore.Cols.ALBUM_IDENTITY },
                BaseColumns._ID + "=?",
                new String[]{ String.valueOf(songId) }, null);
        if (c != null) {
            try {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    return Long.decode(c.getString(0));
                }
            } catch (NumberFormatException e) {
                //pass
            } finally {
                c.close();
            }
        }
        return -1;
    }

    public static long getRecentId(Context context, long realSongId) {
        final Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{ MusicStore.Cols._ID },
                MusicStore.Cols.IDENTITY + "=? AND " + MusicStore.Cols.ISLOCAL + "=?",
                new String[]{String.valueOf(realSongId), "1"}, null
                );
        if (c != null) {
            try {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    return c.getLong(0);
                }
            } finally {
                c.close();
            }
        }
        return -1;
    }

    public static Uri getDataUri(Context context, long songId) {
        final Cursor c = context.getContentResolver().query(MusicProvider.RECENTS_URI,
                new String[]{ MusicStore.Cols.DATA_URI },
                BaseColumns._ID + "=?",
                new String[]{ String.valueOf(songId) }, null);
        if (c != null) {
            try {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    return Uri.parse(c.getString(0));
                }
            } catch (IllegalArgumentException e) {
                //pass
            } finally {
                c.close();
            }
        }
        return null;
    }

    public static void removeFromRecents(Context context, long recentsId) {
        context.getContentResolver().delete(MusicProvider.RECENTS_URI,
                MusicStore.Cols._ID + "=?",
                new String[] {String.valueOf(recentsId)}
        );
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
        values.put(MusicStore.Cols.ARTWORK_URI, song.artworkUri != null ? song.artworkUri.toString() : null);
        values.put(MusicStore.Cols.MIME_TYPE, song.mimeType);
        values.put(MusicStore.Cols.ISLOCAL, (song instanceof LocalSong) ? 1 : 0);
        values.put(MusicStore.Cols.PLAYCOUNT, 0);
        values.put(MusicStore.Cols.LAST_PLAYED, 0);
        return values;
    }
}
