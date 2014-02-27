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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by drew on 2/24/14.
 */
public class MusicProvider extends ContentProvider {

    private static final String AUTHORITY = "com.andrew.apollo.provider";
    private static final UriMatcher sUriMatcher;

    public static final Uri RECENTS_URI;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        RECENTS_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("recents").build();
        sUriMatcher.addURI(AUTHORITY, "recents", 1);
    }

    private static RecentStore sRecents;

    @Override
    public boolean onCreate() {
        sRecents = RecentStore.getInstance(getContext());
        return true;
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor c = null;
        switch (sUriMatcher.match(uri)) {
            case 1:
                c = sRecents.getReadableDatabase().query(RecentStore.RecentStoreColumns.NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
        }
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        if (uri == null || values == null) {
            return null;
        }
        Uri ret = null;
        switch (sUriMatcher.match(uri)) {
            case 1:
                SQLiteDatabase db = sRecents.getWritableDatabase();
                db.beginTransaction();
                // Todo update playcount instead
                db.delete(RecentStore.RecentStoreColumns.NAME,
                        BaseColumns._ID + " = ?",
                        new String[] {
                            String.valueOf(values.get(BaseColumns._ID))
                        }
                );
                db.insert(RecentStore.RecentStoreColumns.NAME, null, values);
                db.setTransactionSuccessful();
                db.endTransaction();
                ret = RECENTS_URI.buildUpon().appendPath(values.getAsString(BaseColumns._ID)).build();
                break;
        }
        if (ret != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ret;
    }

    @Override
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        int ret = 0;
        switch (sUriMatcher.match(uri)) {
            case 1:
                ret = sRecents.getWritableDatabase().delete(RecentStore.RecentStoreColumns.NAME, selection, selectionArgs);
                break;
        }
        if (ret != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ret;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
