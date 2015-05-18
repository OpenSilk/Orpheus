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

package org.opensilk.music.library.mediastore.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

/**
 * Created by drew on 2/22/14.
 */
public class CursorHelpers {

    private static final long[] sEmptyList;
    private static final String sEmptyString;

    static {
        sEmptyList = new long[0];
        sEmptyString = new String("");
    }

    private CursorHelpers() {
        // static
    }

    public static Uri appendId(Uri base, String id) {
        return ContentUris.withAppendedId(base, Long.parseLong(id));
    }

    public static Uri generateDataUri(long songId) {
        return ContentUris.withAppendedId(Uris.EXTERNAL_MEDIASTORE_MEDIA, songId);
    }

    public static Uri generateDataUri(String songId) {
        return appendId(Uris.EXTERNAL_MEDIASTORE_MEDIA, songId);
    }

    public static Uri generateArtworkUri(long albumId) {
        return ContentUris.withAppendedId(Uris.ARTWORK_URI, albumId);
    }

    public static Uri generateArtworkUri(String albumId) {
        return appendId(Uris.ARTWORK_URI, albumId);
    }

    public static String getStringOrEmpty(Cursor c, String col) {
        try {
            return c.getString(c.getColumnIndexOrThrow(col));
        } catch (IllegalArgumentException|NullPointerException e) {
            Timber.w(e, "getStringOrEmpty(" + col + ")");
            return sEmptyString;
        }
    }

    public static String getStringOrNull(Cursor c, String col) {
        try {
            return c.getString(c.getColumnIndexOrThrow(col));
        } catch (IllegalArgumentException|NullPointerException e) {
            Timber.w(e, "getStringOrNull("+col+")");
            return null;
        }
    }

    public static long getLongOrZero(Cursor c, String col) {
        try {
            return c.getLong(c.getColumnIndexOrThrow(col));
        } catch (IllegalArgumentException|NullPointerException e) {
            Timber.w(e, "getLongOrZero("+col+")");
            return 0;
        }
    }

    public static int getIntOrZero(Cursor c, String col) {
        try {
            return c.getInt(c.getColumnIndexOrThrow(col));
        } catch (IllegalArgumentException|NullPointerException e) {
            Timber.w(e, "getIntOrZero("+col+")");
            return 0;
        }
    }

    public static long[] getSongIdsForAlbum(Context context, String albumid) {
        Cursor cursor = context.getContentResolver().query(
                Uris.EXTERNAL_MEDIASTORE_MEDIA,
                Projections.ID_ONLY,
                Selections.LOCAL_ALBUM_SONGS,
                SelectionArgs.LOCAL_ALBUM_SONGS(albumid),
                null);
        if (cursor != null) {
            try {
                return getSongIdsForCursor(cursor);
            } finally {
                cursor.close();
            }
        }
        return sEmptyList;
    }

    public static long[] getSongIdsForArtist(Context context, String artistId) {
        Cursor cursor = context.getContentResolver().query(
                Uris.EXTERNAL_MEDIASTORE_MEDIA,
                Projections.ID_ONLY,
                Selections.LOCAL_ARTIST_SONGS,
                SelectionArgs.LOCAL_ARTIST_SONGS(artistId),
                null);
        if (cursor != null) {
            try {
                return getSongIdsForCursor(cursor);
            } finally {
                cursor.close();
            }
        }
        return sEmptyList;
    }

    public static long[] getSongIdsForCursor(final Cursor c) {
        if (c != null && c.moveToFirst()) {
            int colidx = c.getColumnIndexOrThrow(BaseColumns._ID);
            long[] list = new long[c.getCount()];
            int ii = 0;
            do {
                list[ii++] = c.getLong(colidx);
            } while (c.moveToNext());
            return list;
        } else {
            return sEmptyList;
        }
    }

    public static int deleteTracks(final Context context, List<Long> list) {
        int numremoved = 0;
        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        Iterator<Long> ii = list.iterator();
        if (ii.hasNext()) {
            while (true) {
                selection.append(ii.next());
                if (!ii.hasNext()) {
                    break;
                }
                selection.append(",");
            }
        }
        selection.append(")");
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    Uris.EXTERNAL_MEDIASTORE_MEDIA,
                    Projections.ID_DATA,
                    selection.toString(),
                    null, null);
            if (c != null) {
                // Remove selected tracks from the database
                context.getContentResolver().delete(
                        Uris.EXTERNAL_MEDIASTORE_MEDIA,
                        selection.toString(), null);

                // Remove files from card
                if (c.moveToFirst()) {
                    do {
                        final String name = c.getString(1);
                        final File f = new File(name);
                        try { // File.delete can throw a security exception
                            if (!f.delete()) {
                                // I'm not sure if we'd ever get here (deletion would
                                // have to fail, but no exception thrown)
                                Timber.e("Failed to delete file %s", name);
                            } else {
                                Timber.d("Deleted %s", name);
                                numremoved++;
                            }
                        } catch (final SecurityException ex) {
                        }
                    } while (c.moveToNext());
                }
            } else {
                return 0;
            }
        } finally {
            if (c != null) c.close();
        }
        // We deleted a number of tracks, which could affect any number of things
        // in the media content domain, so update everything.
        context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
        return numremoved;
    }


}
