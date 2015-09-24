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
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
import timber.log.Timber;

import static android.provider.MediaStore.Audio.keyFor;

/**
 * Created by drew on 9/16/15.
 */
@Singleton
public class IndexDatabaseImpl implements IndexDatabase {

    final ReadWriteLock mLock = new ReentrantReadWriteLock(true);
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
            c = query(IndexSchema.ArtistInfo.TABLE, artists_cols, null, null, null, null, sortOrder);
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
            c = query(IndexSchema.AlbumInfo.TABLE, albums_cols, null, null, null, null, sortOrder);
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
            IndexSchema.TrackInfo.TRACK_KEY,
            IndexSchema.TrackInfo.DISC, //15
    };

    @Override
    public List<Track> getTracks(String sortOrder, boolean excludeOrphaned) {
        List<Track> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.TrackInfo.TABLE, tracks_cols, null, null, null, null, sortOrder);
            if (c != null && c.moveToFirst()) {
                LinkedHashMap<String, Track.Builder> tobs = new LinkedHashMap<>(c.getCount());
                do {
                    //concatenation of title_key,artist_id,album_id,track,disc
                    final String key = c.getString(14);
                    //we only need to constuct track once
                    if (!tobs.containsKey(key)) {
                        final String id = c.getString(0);
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
                        tobs.put(key, tob);
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
                    tobs.get(key).addRes(res);
                } while (c.moveToNext());
                //add all tracks to list
                for (Track.Builder tob : tobs.values()) {
                    lst.add(tob.build());
                }
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    @Override
    @DebugLog
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
        try {
            mLock.readLock().lock();
            return helper.getReadableDatabase().query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        try {
            mLock.writeLock().lock();
            return helper.getWritableDatabase().delete(table, whereClause, whereArgs);
        } finally {
            mLock.writeLock().unlock();
        }
    }

    @Override
    @DebugLog
    public long insert(String table, String nullColumnHack, ContentValues values, int conflictAlgorithm) {
        try {
            mLock.writeLock().lock();
            return helper.getWritableDatabase().insertWithOnConflict(table, nullColumnHack, values, conflictAlgorithm);
        } finally {
            mLock.writeLock().unlock();
        }
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        try {
            mLock.writeLock().lock();
            return helper.getWritableDatabase().update(table, values, whereClause, whereArgs);
        } finally {
            mLock.writeLock().unlock();
        }
    }

    static final String containerUriSel = IndexSchema.Containers.URI + "=?";

    Cursor getContainer(Uri uri) {
        String[] selArgs = new String[] {uri.toString()};
        return query(IndexSchema.Containers.TABLE, idCols,
                containerUriSel, selArgs, null, null, null);
    }

    @Override
    public long hasContainer(Uri uri) {
        Cursor c = null;
        try {
            c = getContainer(uri);
            if (c != null && c.moveToFirst()) {
                return c.getLong(0);
            }
        } finally {
            closeCursor(c);
        }
        return -1;
    }

    static final String[] findToplevelContainersCols = new String[] {
            IndexSchema.Containers.URI,
            IndexSchema.Containers.PARENT_URI,
    };

    @Override
    public @NonNull List<Pair<Uri, Uri>> findTopLevelContainers(String authority) {
        List<Pair<Uri,Uri>> topLevel = new ArrayList<>();
        Cursor c = null;
        try {
            final String sel = authority != null ? IndexSchema.Containers.AUTHORITY + "=?" : null;
            final String[] selArgs = authority != null ? new String[] {authority} : null;
            c = query(IndexSchema.Containers.TABLE, findToplevelContainersCols,
                    sel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    final Uri uri = Uri.parse(c.getString(0));
                    final Uri parentUri = Uri.parse(c.getString(1));
                    if (hasContainer(parentUri) == -1) {
                        //Our parent wasn't found, we are topLevel
                        topLevel.add(Pair.create(uri, parentUri));
                    }
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return topLevel;
    }

    static final String[] containerUriCols = new String[] {
            IndexSchema.Containers.URI,
    };

    @Override
    public int removeContainersInError(@Nullable String authority) {
        Cursor c = null;
        try {
            String sel = IndexSchema.Containers.IN_ERROR + "=?";
            if (authority != null) {
                sel += " AND " + IndexSchema.Containers.AUTHORITY + "=?";
            }
            String[] selArgs = authority != null ?
                    new String[] {"1", authority} : new String[] {"1"};
            c = query(IndexSchema.Containers.TABLE, containerUriCols,
                    sel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                int numRemoved = 0;
                do {
                    numRemoved += removeContainer(Uri.parse(c.getString(0)));
                } while (c.moveToNext());
                return numRemoved;
            }
            return 0;
        } finally {
            closeCursor(c);
        }
    }

    @Override
    public boolean markContainerInError(Uri uri) {
        ContentValues cv = new ContentValues();
        cv.put(IndexSchema.Containers.IN_ERROR, 1);
        return update(IndexSchema.Containers.TABLE, cv,
                containerUriSel, new String[]{uri.toString()}) == 1;
    }

    @Override
    public long insertContainer(Uri uri, Uri parentUri) {
        ContentValues cv = new ContentValues(5);
        cv.put(IndexSchema.Containers.URI, uri.toString());
        cv.put(IndexSchema.Containers.PARENT_URI, parentUri.toString());
        cv.put(IndexSchema.Containers.AUTHORITY, uri.getAuthority());
        return insert(IndexSchema.Containers.TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    static final String removeContainerSel = IndexSchema.Containers.URI + "=?";

    @Override
    public int removeContainer(Uri uri) {
        String[] containers = null;
        Cursor c = null;
        try {
            final String[] selArgs = new String[]{uri.toString()};
            c = query(IndexSchema.Containers.TABLE, idCols,
                    removeContainerSel, selArgs, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return 0;
            }
            containers = new String[] {c.getString(0)};
        } finally {
            closeCursor(c);
        }

        containers = ArrayUtils.addAll(containers, findChildrenUnder(uri));

        StringBuilder where = new StringBuilder();
        where.append(IndexSchema.Containers._ID).append(" IN (");
        where.append("?");
        for (int ii=1; ii<containers.length; ii++) {
            where.append(",?");
        }
        where.append(")");

        return delete(IndexSchema.Containers.TABLE, where.toString(), containers);
    }

    static final String[] findChildrenUnderCols = new String[] {
            IndexSchema.Containers._ID,
            IndexSchema.Containers.URI,
    };
    static final String findChildrenUnderSel = IndexSchema.Containers.PARENT_URI + "=?";

    @DebugLog
    private String[] findChildrenUnder(Uri uri) {
        String[] containers = null;
        HashSet<String> children = new HashSet<>();
        Cursor c = null;
        try {
            final String[] selArgs = new String[]{uri.toString()};
            c = query(IndexSchema.Containers.TABLE, findChildrenUnderCols,
                    findChildrenUnderSel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    containers = ArrayUtils.add(containers, c.getString(0));
                    children.add(c.getString(1));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        for (String child : children) {
            ArrayUtils.addAll(containers, findChildrenUnder(Uri.parse(child)));
        }
        return containers;
    }

    static final String checkTrackSel = IndexSchema.TrackInfo.URI + "=?";

    @Override
    public long hasTrackResource(Uri uri) {
        Cursor c = null;
        try {
            String[] selArgs = new String[]{uri.toString()};
            c = query(IndexSchema.TrackInfo.TABLE, idCols,
                    checkTrackSel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getLong(0);
            }
        } finally {
            closeCursor(c);
        }
        return -1;
    }

    @Override
    public long insertTrackResource(Metadata metadata, long albumId, long artistId, long containerId) {
        ContentValues cv = new ContentValues(20);
        String trackName = metadata.getString(Metadata.KEY_TRACK_NAME);
        cv.put(IndexSchema.TrackResMeta.TRACK_NAME, trackName);
        int trackNum = metadata.getInt(Metadata.KEY_TRACK_NUMBER);
        if (trackNum > 0) {
            cv.put(IndexSchema.TrackResMeta.TRACK_NUMBER, trackNum);
        }
        int discNum = metadata.getInt(Metadata.KEY_DISC_NUMBER);
        if (discNum > 0) {
            cv.put(IndexSchema.TrackResMeta.DISC_NUMBER, discNum);
        }
        /* This is really the only way i can think of to ensure uniqueness
         *
         * Some possible scenarios:
         * Album has multiple tracks with same name, but position will be different
         * Album has multiple discs with tracks with same name possibly same position on each disc
         * Multiple albums have tracks with same name and same artist possibly same position
         * Track has same name with different artist possible on same album
         *
         * But it seems unlikely that non identical tracks will have
         * the same name, same album, same artist and same position on the same disc
         */
        cv.put(IndexSchema.TrackResMeta.TRACK_KEY, keyFor(trackName + albumId + artistId + trackNum + discNum));
        cv.put(IndexSchema.TrackResMeta.GENRE, metadata.getString(Metadata.KEY_GENRE_NAME));
        Uri trackUri = metadata.getUri(Metadata.KEY_TRACK_URI);
        cv.put(IndexSchema.TrackResMeta.AUTHORITY, trackUri.getAuthority());
        cv.put(IndexSchema.TrackResMeta.URI, trackUri.toString());
        long size = metadata.getLong(Metadata.KEY_SIZE);
        if (size > 0) {
            cv.put(IndexSchema.TrackResMeta.SIZE, size);
        }
        cv.put(IndexSchema.TrackResMeta.MIME_TYPE, metadata.getString(Metadata.KEY_MIME_TYPE));
        cv.put(IndexSchema.TrackResMeta.DATE_ADDED, System.currentTimeMillis());
        long lastmod = metadata.getLong(Metadata.KEY_LAST_MODIFIED);
        if (lastmod > 0) {
            cv.put(IndexSchema.TrackResMeta.LAST_MOD, lastmod);
        }
        long bitrate = metadata.getLong(Metadata.KEY_BITRATE);
        if (bitrate > 0) {
            cv.put(IndexSchema.TrackResMeta.BITRATE, bitrate);
        }
        long duration = metadata.getLong(Metadata.KEY_DURATION);
        if (duration > 0) {
            cv.put(IndexSchema.TrackResMeta.DURATION, duration);
        }
        if (artistId > 0) {
            cv.put(IndexSchema.TrackResMeta.ARTIST_ID, artistId);
        }
        if (albumId > 0) {
            cv.put(IndexSchema.TrackResMeta.ALBUM_ID, albumId);
        }
        cv.put(IndexSchema.TrackResMeta.CONTAINER_ID, containerId);
        return insert(IndexSchema.TrackResMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    static final String[] idCols = new String[] {
            BaseColumns._ID,
    };

    static final String checkAlbumSel = IndexSchema.AlbumInfo.ALBUM_KEY
            + "=? AND " + IndexSchema.AlbumInfo.ARTIST_KEY + "=?";

    @Override
    public long hasAlbumMeta(String albumArtist, String album) {
        long id = -1;
        Cursor c = null;
        try {
            final String[] selArgs = new String[]{keyFor(album), keyFor(albumArtist)};
            c = query(IndexSchema.AlbumMeta.TABLE, idCols, checkAlbumSel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);
            }
            return id;
        } finally {
            closeCursor(c);
        }
    }

    static final String checkAlbumSel2 = IndexSchema.AlbumInfo.ALBUM_KEY
            + "=? AND " + IndexSchema.AlbumInfo.ARTIST_ID + "=?";

    @Override
    public long hasAlbumMeta(String album, long albumArtistId) {
        long id = -1;
        Cursor c = null;
        try {
            final String[] selArgs = new String[]{keyFor(album), String.valueOf(albumArtistId)};
            c = query(IndexSchema.AlbumInfo.TABLE, idCols, checkAlbumSel2, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);
            }
            return id;
        } finally {
            closeCursor(c);
        }
    }

    @DebugLog
    @Override
    public long insertAlbum(Metadata meta, long albumArtistId) {
        ContentValues cv = new ContentValues(10);
        cv.put(IndexSchema.AlbumMeta.ALBUM_NAME, meta.getString(Metadata.KEY_ALBUM_NAME));
        cv.put(IndexSchema.AlbumMeta.ALBUM_KEY, keyFor(meta.getString(Metadata.KEY_ALBUM_NAME)));
        cv.put(IndexSchema.AlbumMeta.ALBUM_MBID, meta.getString(Metadata.KEY_ALBUM_MBID));
        cv.put(IndexSchema.AlbumMeta.ALBUM_ARTIST_ID, albumArtistId);
        String bioSummary = meta.getString(Metadata.KEY_ALBUM_SUMMARY);
        String bioContent = meta.getString(Metadata.KEY_ALBUM_BIO);
        long lastMod = meta.getLong(Metadata.KEY_LAST_MODIFIED);
        if (!StringUtils.isEmpty(bioSummary) && !StringUtils.isEmpty(bioContent)) {
            cv.put(IndexSchema.AlbumMeta.ALBUM_BIO_SUMMARY, bioSummary);
            cv.put(IndexSchema.AlbumMeta.ALBUM_BIO_CONTENT, bioContent);
            cv.put(IndexSchema.AlbumMeta.ALBUM_BIO_DATE_MOD, lastMod > 0 ? lastMod : System.currentTimeMillis());
        }
        return insert(IndexSchema.AlbumMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    static final String checkArtistSel = IndexSchema.ArtistInfo.ARTIST_KEY + "=?";

    @Override
    public long hasArtist(String artist) {
        long id = -1;
        Cursor c = null;
        try {
            final String[] selArgs = new String[]{keyFor(artist)};
            c = query(IndexSchema.ArtistInfo.TABLE, idCols, checkArtistSel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);
            }
            return id;
        } finally {
            closeCursor(c);
        }
    }

    @DebugLog
    @Override
    public long insertArtist(Metadata meta) {
        ContentValues cv = new ContentValues(10);
        cv.put(IndexSchema.ArtistMeta.ARTIST_NAME, meta.getString(Metadata.KEY_ARTIST_NAME));
        cv.put(IndexSchema.ArtistMeta.ARTIST_KEY, keyFor(meta.getString(Metadata.KEY_ARTIST_NAME)));
        cv.put(IndexSchema.ArtistMeta.ARTIST_MBID, meta.getString(Metadata.KEY_ARTIST_MBID));
        String bioSummary = meta.getString(Metadata.KEY_ARTIST_SUMMARY);
        String bioContent = meta.getString(Metadata.KEY_ARTIST_BIO);
        long lastMod = meta.getLong(Metadata.KEY_LAST_MODIFIED);
        if (!StringUtils.isEmpty(bioSummary) && !StringUtils.isEmpty(bioContent)) {
            cv.put(IndexSchema.ArtistMeta.ARTIST_BIO_SUMMARY, bioSummary);
            cv.put(IndexSchema.ArtistMeta.ARTIST_BIO_CONTENT, bioContent);
            cv.put(IndexSchema.ArtistMeta.ARTIST_BIO_DATE_MOD, lastMod > 0 ? lastMod : System.currentTimeMillis());
        }
        return insert(IndexSchema.ArtistMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static void closeCursor(Cursor c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }
}
