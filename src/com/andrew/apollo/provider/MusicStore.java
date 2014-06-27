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

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 6/26/14.
 */
@Singleton
public class MusicStore extends SQLiteOpenHelper {

    public static final int VERSION = 1;
    public static final String FILENAME = "music.db";

    public static final String RECENT_TABLE = "recent";

    @Inject
    public MusicStore(Context context) {
        super(context, FILENAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + RECENT_TABLE + " ("
                + Cols._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Cols.IDENTITY + " TEXT NOT NULL,"
                + Cols.NAME + " TEXT NOT NULL,"
                + Cols.ALBUM_NAME + " TEXT,"
                + Cols.ARTIST_NAME + " TEXT,"
                + Cols.ALBUM_ARTIST_NAME + " TEXT,"
                + Cols.ALBUM_IDENTITY + " TEXT,"
                + Cols.DURATION + " INTEGER,"
                + Cols.DATA_URI + " TEXT NOT NULL,"
                + Cols.ARTWORK_URI + " TEXT,"
                + Cols.MIME_TYPE + " TEXT NOT NULL,"
                + Cols.ISLOCAL + " INTEGER NOT NULL,"
                + Cols.PLAYCOUNT + " INTEGER NOT NULL,"
                + Cols.LAST_PLAYED + " INTEGER NOT NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RECENT_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RECENT_TABLE);
        onCreate(db);
    }

    public static class Cols implements BaseColumns {
        // From external model
        public static final String IDENTITY = "identity";
        public static final String NAME = "name";
        public static final String ALBUM_NAME = "albumname";
        public static final String ARTIST_NAME = "artistname";
        public static final String ALBUM_ARTIST_NAME = "albumartistname";
        public static final String ALBUM_IDENTITY = "albumidentity";
        public static final String DURATION = "duration";
        public static final String DATA_URI = "datauri";
        public static final String ARTWORK_URI = "artworkUri";
        public static final String MIME_TYPE = "mimetype";
        // internal use
        public static final String ISLOCAL = "islocal";
        public static final String PLAYCOUNT = "playcount";
        public static final String LAST_PLAYED = "lastplayed";

    }

}
