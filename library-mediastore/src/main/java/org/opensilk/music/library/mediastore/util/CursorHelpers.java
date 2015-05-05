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
import android.database.Cursor;
import android.net.Uri;

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
        return ContentUris.withAppendedId(base, Long.valueOf(id));
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

}
