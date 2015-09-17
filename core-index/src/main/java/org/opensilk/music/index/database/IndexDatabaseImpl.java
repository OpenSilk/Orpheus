/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.index.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Created by drew on 9/16/15.
 */
@Singleton
public class IndexDatabaseImpl implements IndexDatabase {

    final IndexDatabaseHelper helper;
    final String indexAuthority;

    @Inject
    public IndexDatabaseImpl(
            IndexDatabaseHelper helper,
            @Named("IndexProviderAuthority") String indexAuthority
    ) {
        this.helper = helper;
        this.indexAuthority = indexAuthority;
    }

    static final String[] artists_cols = new String[] {
            IndexSchema.ArtistInfo._ID,
            IndexSchema.ArtistInfo.ARTIST,
            IndexSchema.ArtistInfo.NUMBER_OF_ALBUMS,
            IndexSchema.ArtistInfo.NUMBER_OF_TRACKS,
    };

    @Override
    public List<Artist> getArtists(String sortOrder) {
        List<Artist> lst = new ArrayList<>();
        Cursor c = null;
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            if (db != null) {
                c = db.query(IndexSchema.ArtistInfo.TABLE, artists_cols, null, null, null, null, sortOrder);
                if (c != null && c.moveToFirst()) {
                    final Uri parentUri = IndexUris.artists(indexAuthority);
                    do {
                        final String id = c.getString(0);
                        final String name = c.getString(1);
                        final int num_albums = c.getInt(2);
                        final int num_tracks = c.getInt(3);
                        final Artist a = Artist.builder()
                                .setUri(IndexUris.artist(indexAuthority, id))
                                .setParentUri(parentUri)
                                .setName(name)
                                .setAlbumCount(num_albums)
                                .setTrackCount(num_tracks)
                                .build();
                        lst.add(a);
                    } while (c.moveToNext());
                }
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    static final String[] albums_cols = new String[] {
            IndexSchema.AlbumInfo._ID,
            IndexSchema.AlbumInfo.ALBUM,
            IndexSchema.AlbumInfo.ARTIST,
            IndexSchema.AlbumInfo.ARTIST_ID,
            IndexSchema.AlbumInfo.TRACK_COUNT
    };

    @Override
    public List<Album> getAlbums(String sortOrder) {
        List<Album> lst = new ArrayList<>();
        Cursor c = null;
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            if (db != null) {
                c = db.query(IndexSchema.AlbumInfo.TABLE, albums_cols, null, null, null, null, sortOrder);
                if (c != null && c.moveToFirst()) {
                    final Uri parentUri = IndexUris.albums(indexAuthority);
                    do {
                        final String id = c.getString(0);
                        final String name = c.getString(1);
                        final String artistName = c.getString(2);
                        final String artistId = c.getString(3);
                        final int trackNum = c.getInt(4);
                        final Album a = Album.builder()
                                .setUri(IndexUris.album(indexAuthority, id))
                                .setParentUri(parentUri)
                                .setName(name)
                                .setArtistName(artistName)
                                .setArtistUri(IndexUris.artist(indexAuthority, artistId))
                                .setTrackCount(trackNum)
                                        //.setArtworkUri()//TODO
                                .build();
                        lst.add(a);
                    } while (c.moveToNext());
                }
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    static final String[] tracks_cols = new String[] {
            IndexSchema.TrackInfo._ID,
            IndexSchema.TrackInfo.TITLE,
            IndexSchema.TrackInfo.ARTIST,
            IndexSchema.TrackInfo.ARTIST_ID,
            IndexSchema.TrackInfo.ALBUM,
            IndexSchema.TrackInfo.ALBUM_ID,
            IndexSchema.TrackInfo.ALBUM_ARTIST,
            IndexSchema.TrackInfo.ALBUM_ARTIST_ID,
            IndexSchema.TrackInfo.TRACK,
            IndexSchema.TrackInfo.URI,
            IndexSchema.TrackInfo.SIZE,
            IndexSchema.TrackInfo.MIME_TYPE,
            IndexSchema.TrackInfo.BITRATE,
            IndexSchema.TrackInfo.DURATION,
    };

    @Override
    public List<Track> getTracks(String sortOrder, boolean excludeOrphaned) {
        List<Track> lst = new ArrayList<>();
        Cursor c = null;
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            if (db != null) {
                c = db.query(IndexSchema.TrackInfo.TABLE, tracks_cols, null, null, null, null, sortOrder);
                if (c != null && c.moveToFirst()) {
                    LinkedHashMap<String, Track.Builder> tobs = new LinkedHashMap<>(c.getCount());
                    do {
                        final String id = c.getString(0);
                        //we only need to constuct track once
                        if (!tobs.containsKey(id)) {
                            final String name = c.getString(1);
                            final String artist = c.getString(2);
                            final String artistId = c.getString(3);
                            final String album = c.getString(4);
                            final String albumId = c.getString(5);
                            final String albumArtist = c.getString(6);
//                        final String albumArtistId = c.getString(7);
                            final int trackPos = c.getInt(8);
                            final Track.Builder tob = Track.builder()
                                    .setUri(IndexUris.track(indexAuthority, id))
                                    .setParentUri(IndexUris.tracks(indexAuthority))
                                    .setName(name)
                                    .setArtistName(artist)
                                    .setArtistUri(IndexUris.artist(indexAuthority, artistId))
                                    .setAlbumName(album)
                                    .setAlbumUri(IndexUris.album(indexAuthority, albumId))
                                    .setAlbumArtistName(albumArtist)
                                    .setTrackNumber(trackPos);
                            tobs.put(id, tob);
                        }
                        //attach resource to track
                        final Uri uri = Uri.parse(c.getString(9));
                        final int size = c.getInt(10);
                        final String mime = c.getString(11);
                        final long bitrate = c.getLong(12);
                        final int duration = c.getInt(13);
                        final Track.Res res = Track.Res.builder()
                                .setUri(uri)
                                .setSize(size)
                                .setMimeType(mime)
                                .setBitrate(bitrate)
                                .setDuration(duration)
                                .build();
                        tobs.get(id).addRes(res);
                    } while (c.moveToNext());
                    //add all tracks to list
                    for (Track.Builder tob : tobs.values()) {
                        lst.add(tob.build());
                    }
                }
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    @Override
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
        return helper.getReadableDatabase().query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        return helper.getWritableDatabase().delete(table, whereClause, whereArgs);
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues values) {
        return helper.getWritableDatabase().insert(table, nullColumnHack, values);
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return helper.getWritableDatabase().update(table, values, whereClause, whereArgs);
    }

    static void closeCursor(Cursor c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }
}
