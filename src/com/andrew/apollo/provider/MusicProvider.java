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
    public static final Uri FAVORITES_URI;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        RECENTS_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("recents").build();
        sUriMatcher.addURI(AUTHORITY, "recents", 1);
        FAVORITES_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("favorites").build();
        sUriMatcher.addURI(AUTHORITY, "favorites", 2);
    }

    private static SQLiteDatabase sRecents;
    private static SQLiteDatabase sFavorites;

    @Override
    public boolean onCreate() {
        sRecents = RecentStore.getInstance(getContext()).getWritableDatabase();
        sFavorites = FavoritesStore.getInstance(getContext()).getWritableDatabase();
        return true;
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor c = null;
        switch (sUriMatcher.match(uri)) {
            case 1:
                c = sRecents.query(RecentStore.RecentStoreColumns.NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case 2:
                c = sFavorites.query(FavoritesStore.FavoriteColumns.NAME,
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
        Uri ret = null;
        switch (sUriMatcher.match(uri)) {
            case 1:
                sRecents.beginTransaction();
                // Todo update playcount instead
                sRecents.delete(RecentStore.RecentStoreColumns.NAME,
                        BaseColumns._ID + " = ?",
                        new String[] {
                            String.valueOf(values.get(BaseColumns._ID))
                        }
                );
                sRecents.insert(RecentStore.RecentStoreColumns.NAME, null, values);
                sRecents.setTransactionSuccessful();
                sRecents.endTransaction();
                ret = RECENTS_URI.buildUpon().appendPath(values.getAsString(BaseColumns._ID)).build();
                break;
            case 2:
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
                ret = sRecents.delete(RecentStore.RecentStoreColumns.NAME, selection, selectionArgs);
                break;
            case 2:
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
