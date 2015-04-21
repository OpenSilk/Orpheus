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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import org.opensilk.music.BuildConfig;

import timber.log.Timber;

/**
 * Created by drew on 2/24/14.
 */
public class MusicProvider extends ContentProvider {

    private static final String AUTHORITY = BuildConfig.MUSIC_AUTHORITY;
    private static final UriMatcher sUriMatcher;

    /** Uri for recents store */
    public static final Uri RECENTS_URI;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        RECENTS_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("recents").build();
        sUriMatcher.addURI(AUTHORITY, "recents", 1);
    }

    MusicStore mStore;

    @Override
    public boolean onCreate() {
        mStore = new MusicStore(getContext());
        return true;
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor c = null;
        SQLiteDatabase db;
        switch (sUriMatcher.match(uri)) {
            case 1: // Recents
                db = getMusicStoreDatabase(true);
                if (db != null) {
                    c = db.query(MusicStore.RECENT_TABLE,
                            projection, selection, selectionArgs, null, null, sortOrder);
//                    db.close();
                }
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
                SQLiteDatabase db = getMusicStoreDatabase(false);
                if (db != null) {
                    long id = db.insert(MusicStore.RECENT_TABLE, null, values);
//                    db.close();
                    if (id >= 0) {
                        ret = ContentUris.withAppendedId(RECENTS_URI, id);
                    }
                }
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
                SQLiteDatabase db = getMusicStoreDatabase(false);
                if (db != null) {
                    ret = db.delete(MusicStore.RECENT_TABLE, selection, selectionArgs);
//                    db.close();
                }
                break;
        }
        if (ret != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ret;
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int ret = 0;
        switch (sUriMatcher.match(uri)) {
            case 1:
                SQLiteDatabase db = getMusicStoreDatabase(false);
                if (db != null) {
                    ret = db.update(MusicStore.RECENT_TABLE, values, selection, selectionArgs);
//                    db.close();
                }
                break;
        }
        if (ret != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ret;
    }

    protected SQLiteDatabase getMusicStoreDatabase(boolean tryReadonly) {
        SQLiteDatabase db = null;
        try {
            db = mStore.getWritableDatabase();
        } catch (SQLiteException e) {
            Timber.w(e, "Unable to get writable MusicStore database.");
            db = null;
            if (tryReadonly) {
                try {
                    db = mStore.getReadableDatabase();
                } catch (SQLiteException e1) {
                    Timber.e(e1, "Unable to get readonly MusicStore database");
                    db = null;
                }
            }
        }
        return db;
    }
}
