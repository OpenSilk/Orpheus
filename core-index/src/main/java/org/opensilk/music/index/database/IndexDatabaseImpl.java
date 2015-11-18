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
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.index.model.BioSummary;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.index.provider.LastFMHelper;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.TrackList;
import org.opensilk.music.model.sort.TrackSortOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    final LastFMHelper mLastFM;
    final Context mAppContext;

    @Inject
    public IndexDatabaseImpl(
            IndexDatabaseHelper helper,
            @Named("IndexProviderAuthority") String indexAuthority,
            LastFMHelper lastFMHelper,
            @ForApplication Context context
    ) {
        this.helper = helper;
        this.indexAuthority = indexAuthority;
        this.mLastFM = lastFMHelper;
        this.mAppContext = context;
    }

    static final String[] idCols = new String[] {
            BaseColumns._ID,
    };
    static final String idSelection = BaseColumns._ID + "=?";

    @Override
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy) {
        try {
            mLock.readLock().lock();
            return helper.getReadableDatabase().query(table, columns, selection,
                    selectionArgs, groupBy, having, orderBy);
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
    public long insert(String table, String nullColumnHack, ContentValues values,
                       int conflictAlgorithm) {
        try {
            mLock.writeLock().lock();
            return helper.getWritableDatabase().insertWithOnConflict(table, nullColumnHack,
                    values, conflictAlgorithm);
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

    static final String[] artists_cols = new String[] {
            IndexSchema.Info.Artist._ID,
            IndexSchema.Info.Artist.TITLE,
            IndexSchema.Info.Artist.NUMBER_OF_ALBUMS,
            IndexSchema.Info.Artist.NUMBER_OF_TRACKS,
    };

    Artist buildArtist(Cursor c, Uri parentUri) {
        final String id = c.getString(0);
        final String name = c.getString(1);
        final int num_albums = c.getInt(2);
        final int num_tracks = c.getInt(3);
        return Artist.builder()
                .setUri(IndexUris.artist(indexAuthority, id))
                .setParentUri(parentUri)
                .setName(name)
                .setAlbumCount(num_albums)
                .setTrackCount(num_tracks)
                .setTracksUri(IndexUris.artistTracks(indexAuthority, id))
                .build();
    }

    @Override
    public List<Artist> getArtists(String sortOrder) {
        List<Artist> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Artist.TABLE, artists_cols, null, null, null, null, sortOrder);
            if (c != null && c.moveToFirst()) {
                final Uri parentUri = IndexUris.artists(indexAuthority);
                do {
                    lst.add(buildArtist(c, parentUri));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    @Override
    public List<Artist> getAlbumArtists(String sortOrder) {
        List<Artist> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Artist.ALBUM_ARSTIST_TABLE, artists_cols, null, null, null, null, sortOrder);
            if (c != null && c.moveToFirst()) {
                final Uri parentUri = IndexUris.albumArtists(indexAuthority);
                do {
                    lst.add(buildArtist(c, parentUri));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    static final String artist_albums_sel = IndexSchema.Info.Album.ARTIST_ID + "=?";

    @Override
    public List<Album> getArtistAlbums(String id, String sortOrder) {
        List<Album> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Album.TABLE, albums_cols,
                    artist_albums_sel, new String[]{id}, null, null, sortOrder);
            if (c != null && c.moveToFirst()) {
                final Uri parentUri = IndexUris.artistAlbums(indexAuthority, id);
                do {
                    lst.add(buildAlbum(c, parentUri));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    static final String[] artist_details_cols = new String[] {
            IndexSchema.Info.Artist._ID,
            IndexSchema.Info.Artist.TITLE,
            IndexSchema.Info.Artist.MBID,
            IndexSchema.Info.Artist.SUMMARY,
            IndexSchema.Info.Artist.NUMBER_OF_TRACKS,
    };
    static final String artist_details_sel = IndexSchema.Info.Artist._ID + "=?";
    static final String album_map_id_sel = IndexSchema.Misc.AlbumMap._ID + "=?";

    public List<Model> getArtistDetails(String id, String sortOrder) {
        List<Model> lst = new ArrayList<>();
        Cursor c = null;
        Cursor c2 = null;
        try {
            c = query(IndexSchema.Info.Artist.TABLE, artist_details_cols,
                    artist_details_sel, new String[]{id}, null, null, null);
            c2 = query(IndexSchema.Misc.ArtistAlbumMap.TABLE, album_map_cols,
                    album_map_id_sel, new String[]{id}, null, null, null);
            if (c != null && c.moveToFirst()) {
                final String title = getStringOrNull(c, 1);
                final String mbid = getStringOrNull(c, 2);
                final String summary = getStringOrNull(c, 3);
                if (!StringUtils.isEmpty(mbid) && !StringUtils.isEmpty(summary)) {
                    BioSummary bioSummary = BioSummary.builder()
                            .setKind(BioSummary.Kind.ARTIST)
                            .setUri(IndexUris.artistBio(indexAuthority, id))
                            .setParentUri(IndexUris.artistDetails(indexAuthority, id))
                            .setMbid(mbid)
                            .setName(title)
                            .setSummary(summary)
                            .build();
                    lst.add(bioSummary);
                }
                final int songCount = getIntOrNeg(c, 4);
                if (songCount > 0) {
                    TrackList.Builder tlb = TrackList.builder()
                            .setUri(IndexUris.artistTracks(indexAuthority, id))
                            .setParentUri(IndexUris.artistDetails(indexAuthority, id))
                            .setName(title)
                            .setTrackCount(songCount)
                            .setTracksUri(IndexUris.artistTracks(indexAuthority, id));
                    if (c2 != null && c2.moveToFirst()) {
                        do {
                            final String album = getStringOrNull(c2, 1);
                            final String albumArtist = getStringOrNull(c2, 2);
                            if (album != null && albumArtist != null) {
                                tlb.addArtInfo(ArtInfo.forAlbum(albumArtist, album, null));
                            }
                        } while (c2.moveToNext());
                    }
                    lst.add(tlb.build());
                }
            }
        } finally {
            closeCursor(c);
            closeCursor(c2);
        }
        //add the albums after
        lst.addAll(getArtistAlbums(id, sortOrder));
        return lst;
    }

    static final String artist_tracks_sel = IndexSchema.Info.Track.ARTIST_ID + "=?";

    @Override
    public List<Track> getArtistTracks(String id, String sortOrder) {
        List<Track> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Track.TABLE, tracks_cols,
                    artist_tracks_sel, new String[]{id}, null, null, sortOrder);
            if (c != null && c.moveToFirst()) {
                do {
                    lst.add(buildTrack(c));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    @Override
    public @Nullable Artist getArtist(String id) {
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Artist.TABLE, artists_cols,
                    idSelection, new String[]{id}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return buildArtist(c, IndexUris.artists(indexAuthority));
            }
        } finally {
            closeCursor(c);
        }
        return null;
    }

    static final String[] artistMbidCols = new String[] {
            IndexSchema.Info.Artist.MBID,
    };

    @Override
    public @Nullable String getArtistMbid(String id) {
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Artist.TABLE, artistMbidCols,
                    idSelection, new String[]{id}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } finally {
            closeCursor(c);
        }
        return null;
    }

    static final String[] albums_cols = new String[] {
            IndexSchema.Info.Album._ID,
            IndexSchema.Info.Album.TITLE,
            IndexSchema.Info.Album.ARTIST,
            IndexSchema.Info.Album.ARTIST_ID,
            IndexSchema.Info.Album.TRACK_COUNT
    };

    Album buildAlbum(Cursor c, Uri parentUri) {
        final String id = c.getString(0);
        final String name = c.getString(1);
        final String artistName = c.getString(2);
        final String artistId = c.getString(3);
        final int trackNum = c.getInt(4);
        return Album.builder()
                .setUri(IndexUris.album(indexAuthority, id))
                .setParentUri(parentUri)
                .setName(name)
                .setArtistName(artistName)
                .setArtistUri(IndexUris.artist(indexAuthority, artistId))
                .setTrackCount(trackNum)
                .setTracksUri(IndexUris.albumTracks(indexAuthority, id))
                .build();
    }

    @Override
    public List<Album> getAlbums(String sortOrder) {
        List<Album> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Album.TABLE, albums_cols, null, null, null, null, sortOrder);
            if (c != null && c.moveToFirst()) {
                final Uri parentUri = IndexUris.albums(indexAuthority);
                do {
                    lst.add(buildAlbum(c, parentUri));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    static final String albumTracksSel = IndexSchema.Info.Track.ALBUM_ID + "=?";

    @Override
    public List<Track> getAlbumTracks(String id, String sortOrder) {
        List<Track> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Track.TABLE, tracks_cols,
                    albumTracksSel, new String[]{id}, null, null, sortOrder);
            if (c != null && c.moveToFirst()) {
                do {
                    lst.add(buildTrack(c));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    static final String[] album_details_cols = new String[] {
            IndexSchema.Info.Album._ID,
            IndexSchema.Info.Album.TITLE,
            IndexSchema.Info.Album.MBID,
            IndexSchema.Info.Album.SUMMARY,
    };
    static final String album_detals_sel = IndexSchema.Info.Album._ID + "=?";

    @Override
    public List<Model> getAlbumDetails(String id, String sortOrder) {
        List<Model> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Album.TABLE, album_details_cols,
                    album_detals_sel, new String[]{id}, null, null, null);
            if (c != null && c.moveToFirst()) {
                final String title = getStringOrNull(c, 1);
                final String mbid = getStringOrNull(c, 2);
                final String summary = getStringOrNull(c, 3);
                if (!StringUtils.isEmpty(mbid) && !StringUtils.isEmpty(summary)) {
                    BioSummary bioSummary = BioSummary.builder()
                            .setKind(BioSummary.Kind.ALBUM)
                            .setUri(IndexUris.albumBio(indexAuthority, id))
                            .setParentUri(IndexUris.albumDetails(indexAuthority, id))
                            .setMbid(mbid)
                            .setName(title)
                            .setSummary(summary)
                            .build();
                    lst.add(bioSummary);
                }
            }
        } finally {
            closeCursor(c);
        }
        //add the tracks after
        lst.addAll(getAlbumTracks(id, sortOrder));
        return lst;
    }

    @Override
    public @Nullable Album getAlbum(String id) {
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Album.TABLE, albums_cols,
                    idSelection, new String[]{id}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return buildAlbum(c, IndexUris.albums(indexAuthority));
            }
        } finally {
            closeCursor(c);
        }
        return null;
    }

    static final String[] albumMbidCols = new String[] {
            IndexSchema.Info.Album.MBID,
    };

    @Override
    public @Nullable String getAlbumMbid(String id) {
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Album.TABLE, albumMbidCols,
                    idSelection, new String[]{id}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } finally {
            closeCursor(c);
        }
        return null;
    }

    static final String[] genres_cols = new String[] {
            IndexSchema.Info.Genre._ID,
            IndexSchema.Info.Genre.TITLE,
            IndexSchema.Info.Genre.NUMBER_OF_ARTISTS,
            IndexSchema.Info.Genre.NUMBER_OF_ALBUMS,
            IndexSchema.Info.Genre.NUMBER_OF_TRACKS,
    };

    Genre buildGenre(Cursor c, Uri parentUri, Cursor c2) {
        final String id = c.getString(0);
        final String name = c.getString(1);
        //2
        final int albumNum = c.getInt(3);
        final int trackNum = c.getInt(4);
        Genre.Builder bob = Genre.builder()
                .setUri(IndexUris.genre(indexAuthority, id))
                .setParentUri(parentUri)
                .setName(name)
                .setAlbumCount(albumNum)
                .setAlbumsUri(IndexUris.genreAlbums(indexAuthority, id))
                .setTrackCount(trackNum)
                .setTracksUri(IndexUris.genreTracks(indexAuthority, id))
                ;
        long gid = Long.valueOf(id);
        if (c2 != null && c2.moveToFirst()) {
            do {
                if (c2.getLong(0) == gid) {
                    final String album = getStringOrNull(c2, 1);
                    final String albumArtist = getStringOrNull(c2, 2);
                    if (album != null && albumArtist != null) {
                        final String artwork = getStringOrNull(c2, 3);
                        Uri artworkUri = artwork != null ? Uri.parse(artwork) : null;
                        bob.addArtInfo(ArtInfo.forAlbum(albumArtist, album, artworkUri));
                    }
                }
            } while (c2.moveToNext());
        }
        return bob.build();
    }

    static final String[] album_map_cols = new String[] {
            IndexSchema.Misc.AlbumMap._ID,
            IndexSchema.Misc.AlbumMap.ALBUM_NAME,
            IndexSchema.Misc.AlbumMap.ALBUM_ARTIST,
            IndexSchema.Misc.AlbumMap.ARTWORK_URI,
    };

    @Override
    public List<Genre> getGenres(String sortOrder) {
        List<Genre> lst = new ArrayList<>();
        Cursor c = null;
        Cursor c2 = null;
        try {
            c = query(IndexSchema.Info.Genre.TABLE, genres_cols, null, null, null, null, sortOrder);
            c2 = query(IndexSchema.Misc.GenreAlbumMap.TABLE, album_map_cols, null, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                final Uri parentUri = IndexUris.genres(indexAuthority);
                do {
                    lst.add(buildGenre(c, parentUri, c2));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
            closeCursor(c2);
        }
        return lst;
    }

    static final String[] genre_details_cols = new String[] {
            IndexSchema.Info.Genre._ID,
            IndexSchema.Info.Genre.TITLE,
            IndexSchema.Info.Genre.NUMBER_OF_TRACKS,
    };
    static final String genre_details_sel = IndexSchema.Info.Genre._ID + "=?";

    @Override
    public List<Model> getGenreDetails(String id, String sortOrder) {
        List<Model> lst = new ArrayList<>();
        Cursor c = null;
        Cursor c2 = null;
        try {
            c = query(IndexSchema.Info.Genre.TABLE, genre_details_cols,
                    genre_details_sel, new String[]{id}, null, null, null);
            c2 = query(IndexSchema.Misc.GenreAlbumMap.TABLE, album_map_cols,
                    album_map_id_sel, new String[]{id}, null, null, null);
            if (c != null && c.moveToFirst()) {
                final String title = getStringOrNull(c, 1);
                final int songCount = getIntOrNeg(c, 2);
                if (songCount > 0) {
                    TrackList.Builder tlb = TrackList.builder()
                            .setUri(IndexUris.genreTracks(indexAuthority, id))
                            .setParentUri(IndexUris.genreDetails(indexAuthority, id))
                            .setName(title)
                            .setTrackCount(songCount)
                            .setTracksUri(IndexUris.genreTracks(indexAuthority, id));
                    if (c2 != null && c2.moveToFirst()) {
                        do {
                            final String album = getStringOrNull(c2, 1);
                            final String albumArtist = getStringOrNull(c2, 2);
                            if (album != null && albumArtist != null) {
                                final String artwork = getStringOrNull(c2, 3);
                                Uri artworkUri = artwork != null ? Uri.parse(artwork) : null;
                                tlb.addArtInfo(ArtInfo.forAlbum(albumArtist, album, artworkUri));
                            }
                        } while (c2.moveToNext());
                    }
                    lst.add(tlb.build());
                }
            }
        } finally {
            closeCursor(c);
            closeCursor(c2);
        }
        //add the albums after
        lst.addAll(getGenreAlbums(id, sortOrder));
        return lst;
    }

    static final String[] genre_albums_map_cols = new String[] {
            IndexSchema.Misc.AlbumMap.ALBUM_ID,
    };

    @Override
    public List<Album> getGenreAlbums(String id, String sortOrder) {
        List<Album> lst = new ArrayList<>();
        List<String> albums = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Misc.GenreAlbumMap.TABLE, genre_albums_map_cols,
                    album_map_id_sel, new String[]{id}, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    final String aid = getStringOrNull(c, 0);
                    if (aid != null) {
                        albums.add(aid);
                    }
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        if (!albums.isEmpty()) {
            StringBuilder sel = new StringBuilder(IndexSchema.Info.Album._ID)
                    .append(" IN (?");
            for (int ii=1; ii<albums.size(); ii++) {
                sel.append(",?");
            }
            sel.append(")");
            Cursor c2 = null;
            try {
                c2 = query(IndexSchema.Info.Album.TABLE, albums_cols, sel.toString(),
                        albums.toArray(new String[albums.size()]), null, null, sortOrder);
                if (c2 != null && c2.moveToFirst()) {
                    Uri parentUri = IndexUris.genreAlbums(indexAuthority, id);
                    do {
                        lst.add(buildAlbum(c2,parentUri));
                    } while (c2.moveToNext());
                }
            } finally {
                closeCursor(c2);
            }
        }
        return lst;
    }

    static final String genre_tracks_sel = IndexSchema.Info.Track.GENRE_ID + "=?";

    @Override
    public List<Track> getGenreTracks(String id, String sortOrder) {
        List<Track> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Track.TABLE, tracks_cols,
                    genre_tracks_sel, new String[]{id}, null, null, sortOrder);
            if (c != null && c.moveToFirst()) {
                do {
                    lst.add(buildTrack(c));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    static final String[] playlistCols = new String[] {
            IndexSchema.Info.Playlist._ID,
            IndexSchema.Info.Playlist.NAME,
            IndexSchema.Info.Playlist.NUMBER_OF_ARTISTS,
            IndexSchema.Info.Playlist.NUMBER_OF_ALBUMS,
            IndexSchema.Info.Playlist.NUMBER_OF_GENRES,
            IndexSchema.Info.Playlist.NUMBER_OF_TRACKS,//5
    };

    Playlist buildPlaylist(Cursor c, Uri parentUri, Cursor c2) {
        Playlist.Builder bob = Playlist.builder();
        final String id = c.getString(0);
        final String name = c.getString(1);
        //final int numArtists = c.getInt(2);
        //final int numAlbums = c.getInt(3);
        //final int numGenres = c.getInt(4);
        final int numTracks = c.getInt(5);
        bob.setUri(IndexUris.playlist(indexAuthority, id))
                .setName(name)
                .setParentUri(parentUri)
                .setTracksUri(IndexUris.playlistTracks(indexAuthority, id))
                .setTrackCount(numTracks)
        ;
        long gid = Long.valueOf(id);
        if (c2 != null && c2.moveToFirst()) {
            do {
                if (c2.getLong(0) == gid) {
                    final String album = getStringOrNull(c2, 1);
                    final String albumArtist = getStringOrNull(c2, 2);
                    if (album != null && albumArtist != null) {
                        final String artwork = getStringOrNull(c2, 3);
                        Uri artworkUri = artwork != null ? Uri.parse(artwork) : null;
                        bob.addArtInfo(ArtInfo.forAlbum(albumArtist, album, artworkUri));
                    }
                }
            } while (c2.moveToNext());
        }
        return bob.build();
    }

    @Override
    public List<Playlist> getPlaylists(String sortOrder) {
        List<Playlist> lst = new ArrayList<>();
        Cursor c = null;
        Cursor c2 = null;
        try {
            c = query(IndexSchema.Info.Playlist.TABLE, playlistCols, null, null, null, null, sortOrder);
            c2 = query(IndexSchema.Misc.PlaylistAlbumMap.TABLE, album_map_cols, null, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                final Uri parentUri = IndexUris.genres(indexAuthority);
                do {
                    lst.add(buildPlaylist(c, parentUri, c2));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
            closeCursor(c2);
        }
        return lst;
    }

    @Override
    public Playlist getPlaylist(String id) {
        Cursor c = null;
        Cursor c2 = null;
        try {
            c = query(IndexSchema.Info.Playlist.TABLE, playlistCols,
                    IndexSchema.Info.Playlist._ID + "=?", new String[]{id}, null, null, null);
            c2 = query(IndexSchema.Misc.PlaylistAlbumMap.TABLE, album_map_cols, null, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                final Uri parentUri = IndexUris.genres(indexAuthority);
                return buildPlaylist(c, parentUri, c2);
            }
        } finally {
            closeCursor(c);
            closeCursor(c2);
        }
        return null;
    }

    static final String playlistTracksSel = IndexSchema.Info.PlaylistTrack.PLAYLIST_ID + "=?";

    @Override
    public List<Track> getPlaylistTracks(String id, String sortOrder) {
        List<Track> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.PlaylistTrack.TABLE, playlistTrackCols, playlistTracksSel,
                    new String[]{id}, null, null, TrackSortOrder.PLAYORDER);//ignore sort order
            if (c != null && c.moveToFirst()) {
                do {
                    lst.add(buildTrack(c));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    static final String[] tracks_cols = new String[] {
            IndexSchema.Info.Track._ID,
            IndexSchema.Info.Track.URI,
            IndexSchema.Info.Track.PARENT_URI,
            IndexSchema.Info.Track.TITLE,
            IndexSchema.Info.Track.ARTIST,
            IndexSchema.Info.Track.ARTIST_ID, //5
            IndexSchema.Info.Track.ALBUM,
            IndexSchema.Info.Track.ALBUM_ID,
            IndexSchema.Info.Track.ALBUM_ARTIST,
            IndexSchema.Info.Track.ALBUM_ARTIST_ID,
            IndexSchema.Info.Track.TRACK, //10
            IndexSchema.Info.Track.DISC,
            IndexSchema.Info.Track.COMPILATION,
            IndexSchema.Info.Track.GENRE,
            IndexSchema.Info.Track.GENRE_ID,
            IndexSchema.Info.Track.RES_URI, //15
            IndexSchema.Info.Track.RES_HEADERS,
            IndexSchema.Info.Track.RES_SIZE,
            IndexSchema.Info.Track.RES_MIME_TYPE,
            IndexSchema.Info.Track.RES_BITRATE,
            IndexSchema.Info.Track.RES_DURATION, //20
            IndexSchema.Info.Track.ARTWORK_URI
    };
    static final String[] playlistTrackCols = Arrays.copyOf(tracks_cols, tracks_cols.length);
    static {
        //fix sort
        playlistTrackCols[10] = IndexSchema.Info.PlaylistTrack.PLAY_ORDER + " AS " +
                IndexSchema.Info.Track.TRACK;
    }

    Track buildTrack(Cursor c) {
        Track.Builder bob = Track.builder();
        bob.setUri(Uri.parse(c.getString(1)))
                .setParentUri(Uri.parse(c.getString(2)))
                .setName(c.getString(3))
        ;
        String artistName = getStringOrNull(c, 4);
        if (!StringUtils.isEmpty(artistName)) {
            bob.setArtistName(artistName);
        }
        String artistId = getStringOrNull(c, 5);
        if (!StringUtils.isEmpty(artistId)) {
            bob.setArtistUri(IndexUris.artist(indexAuthority, artistId));
        }
        String albumName = getStringOrNull(c, 6);
        if (!StringUtils.isEmpty(albumName)) {
            bob.setAlbumName(albumName);
        }
        String albumId = getStringOrNull(c, 7);
        if (!StringUtils.isEmpty(albumId)) {
            bob.setAlbumUri(IndexUris.album(indexAuthority, albumId));
        }
        String albumArtistName = getStringOrNull(c, 8);
        if (!StringUtils.isEmpty(albumArtistName)) {
            bob.setAlbumArtistName(albumArtistName);
        }
        //9
        int track = getIntOrNeg(c, 10);
        if (track >= 0) {
            bob.setTrackNumber(track);
        }
        int disc = getIntOrNeg(c, 11);
        if (disc > 0) {
            bob.setDiscNumber(disc);
        }
        int comp = getIntOrNeg(c, 12);
        if (comp > 0) {
            bob.setIsCompliation(true);
        }
        String genre = getStringOrNull(c, 13);
        if (!StringUtils.isEmpty(genre)) {
            bob.setGenre(genre);
        }
        //14
        Track.Res.Builder rob = Track.Res.builder();
        rob.setUri(Uri.parse(c.getString(15)));
        String headers = getStringOrNull(c, 16);
        if (!StringUtils.isEmpty(headers)) {
            String[] lns = StringUtils.split(headers, '\n');
            if (lns != null && lns.length > 0) {
                for (String ln : lns) {
                    String[] kv = StringUtils.split(ln, ':');
                    if (kv != null && kv.length == 2) {
                        rob.addHeader(kv[0], kv[1]);
                    }
                }
            }
        }
        long size = getLongOrNeg(c, 17);
        if (size > 0) {
            rob.setSize(size);
        }
        String mime = c.getString(18);
        if(!StringUtils.isEmpty(mime)) {
            rob.setMimeType(mime);
        }
        long bitrate = getLongOrNeg(c, 19);
        if (bitrate > 0) {
            rob.setBitrate(bitrate);
        }
        long dur = getLongOrNeg(c, 20);
        if (dur > 0) {
            rob.setDuration(dur);
        }
        bob.addRes(rob.build());
        String artUri = getStringOrNull(c, 21);
        if (artUri != null) {
            bob.setArtworkUri(Uri.parse(artUri));
        }
        return bob.build();
    }

    @Override
    public List<Track> getTracks(String sortOrder) {
        return getTracks(sortOrder, false);
    }

    @Override
    public List<Track> getTracks(String sortOrder, boolean excludeOrphaned) {
        List<Track> lst = new ArrayList<>();
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Track.TABLE, tracks_cols, null, null, null, null, sortOrder);
            if (c != null && c.moveToFirst()) {
                do {
                    lst.add(buildTrack(c));
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    static final String trackUriSel = IndexSchema.Info.Track.URI + "=?";

    @Override
    public Track getTrack(Uri uri) {
        Cursor c = null;
        try {
            c = query(IndexSchema.Info.Track.TABLE, tracks_cols, trackUriSel,
                    new String[]{uri.toString()}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return buildTrack(c);
            }
        } finally {
            closeCursor(c);
        }
        return null;
    }

    @Override
    public List<Track> getTracksInList(final List<Uri> uris) {
        List<Track> lst = new ArrayList<>(uris.size());
        Cursor c = null;
        try {
            c = getTrackListCursor(uris, tracks_cols);
            if (c != null && c.moveToFirst()) {
                do {
                    lst.add(buildTrack(c));
                } while (c.moveToNext());
            }
            //get them in the same order as the passed list
            Collections.sort(lst, new Comparator<Track>() {
                @Override
                public int compare(Track lhs, Track rhs) {
                    int idx1 = uris.indexOf(lhs.getUri());
                    int idx2 = uris.indexOf(rhs.getUri());
                    return idx1 - idx2;
                }
            });
        } finally {
            closeCursor(c);
        }
        return lst;
    }

    Cursor getTrackListCursor(final List<Uri> uris, String[] cols) {
        StringBuilder sel = new StringBuilder(IndexSchema.Info.Track.URI)
                .append(" IN (");
        String[] selArgs = new String[uris.size()];
        int ii=0;
        for (Uri u : uris) {
            selArgs[ii] = u.toString();
            if (ii++ > 0) {
                sel.append(",");
            }
            sel.append("?");
        }
        sel.append(")");
        return query(IndexSchema.Info.Track.TABLE, cols,
                sel.toString(), selArgs, null, null, null);
    }

    static final String containerUriSel = IndexSchema.Containers.URI + "=?";

    Cursor getContainerCursor(Uri uri) {
        String[] selArgs = new String[] {uri.toString()};
        return query(IndexSchema.Containers.TABLE, idCols,
                containerUriSel, selArgs, null, null, null);
    }

    @Override
    public long hasContainer(Uri uri) {
        synchronized (mConainerIdsCache) {
            if (mConainerIdsCache.containsKey(uri)) {
                return mConainerIdsCache.get(uri);
            }
        }
        Cursor c = null;
        try {
            c = getContainerCursor(uri);
            if (c != null && c.moveToFirst()) {
                long id = c.getLong(0);
                if (id > 0) {
                    synchronized (mConainerIdsCache) {
                        mConainerIdsCache.put(uri, id);
                    }
                }
                return id;
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
        Timber.v("Inserting container %s", cv);
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

        containers = ArrayUtils.addAll(containers, findChildrenUnder(uri, true));

        StringBuilder where = new StringBuilder();
        where.append(IndexSchema.Containers._ID).append(" IN (");
        where.append("?");
        for (int ii=1; ii<containers.length; ii++) {
            where.append(",?");
        }
        where.append(")");

        Timber.d("Removing containers %s", Arrays.toString(containers));
        int num = delete(IndexSchema.Containers.TABLE, where.toString(), containers);
        if (num > 0) {
            //notify everyone
            mAppContext.getContentResolver().notifyChange(IndexUris.call(indexAuthority), null);
            clearCaches();
        }
        return num;
    }

    static final String[] findChildrenUnderCols = new String[] {
            IndexSchema.Containers._ID,
            IndexSchema.Containers.URI,
    };
    static final String findChildrenUnderSel = IndexSchema.Containers.PARENT_URI + "=?";

    @DebugLog
    private String[] findChildrenUnder(Uri uri, boolean recursive) {
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
        if (recursive) {
            for (String child : children) {
                ArrayUtils.addAll(containers, findChildrenUnder(Uri.parse(child), true));
            }
        }
        return containers;
    }

    public boolean trackNeedsScan(Track track) {
        Track t = getTrack(track.getUri());
        //TODO compare meta
        if (t != null) {
            //always update with new info
            insertTrack(track);
            return false;
        }
        return true;
    }

    static final String[] buildTreeContainerCols = new String[] {
            IndexSchema.Containers.URI,
            IndexSchema.Containers.PARENT_URI,
    };

    @Override
    public TreeNode buildTree(Uri uri, Uri parentUri) {
        final TreeNode tree = new TreeNode(uri, parentUri);
        long containerId = hasContainer(uri);
        if (containerId <= 0) {
            return tree;
        }
        //add tracks under us
        addTracksUnderContainer(String.valueOf(containerId), tree);
        //find our direct decendents
        String[] containers = findChildrenUnder(uri, false);
        if (containers == null) {
            return tree;
        }
        Cursor c = null;
        final String[] selArgs = new String[1];
        for (String id : containers) {
            selArgs[0] = id;
            c = query(IndexSchema.Containers.TABLE, buildTreeContainerCols,
                    idSelection, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                TreeNode childTree = new TreeNode(
                        Uri.parse(c.getString(0)),
                        Uri.parse(c.getString(1)));
                addTracksUnderContainer(id, childTree);
                tree.children.add(childTree);
            }
            closeCursor(c);
        }
        return tree;
    }

    static final String[] buildTreeTrackCols = new String[] {
            IndexSchema.Tracks.URI,
    };
    static final String buildTreeTrackSel = IndexSchema.Tracks.CONTAINER_ID + "=?";

    private void addTracksUnderContainer(String containerId, TreeNode tree) {
        Cursor c = null;
        final String[] selArgs = new String[]{containerId};
        try {
            c = query(IndexSchema.Tracks.TABLE, buildTreeTrackCols,
                    buildTreeTrackSel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    Track track = getTrack(Uri.parse(c.getString(0)));
                    if (track != null) {
                        tree.tracks.add(track);
                    }
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
    }

    static final String tracksUriSel = IndexSchema.Tracks.URI + "=?";

    @Override
    public boolean removeTrack(Uri uri, Uri parentUri) {
        int num = delete(IndexSchema.Tracks.TABLE, tracksUriSel, new String[]{uri.toString()});
        if (num > 0) {
            //notify everyone
            mAppContext.getContentResolver().notifyChange(IndexUris.call(indexAuthority), null);
        }
        return num > 0;
    }

    private long getTracksId(Uri uri) {
        Cursor c = null;
        try {
            c = query(IndexSchema.Tracks.TABLE, idCols, tracksUriSel,
                    new String[]{uri.toString()}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getLong(0);
            }
        } finally {
            closeCursor(c);
        }
        return -1;
    }

    @Override
    public long insertTrack(Track track, Metadata metadata) {
        ContentValues cv = new ContentValues(10);

        long trackId = insertTrack(track);
        if (trackId < 0) {
            Timber.d("No track id for track %s", track);
            return -1;
        }
        cv.put(IndexSchema.Meta.Track.TRACK_ID, trackId);

        long artistId = getArtistIdForName(metadata.getString(Metadata.KEY_ARTIST_NAME));
        if (artistId > 0) {
            cv.put(IndexSchema.Meta.Track.ARTIST_ID, artistId);
        }

        long albumId = getAlbumIdForName(metadata.getString(Metadata.KEY_ALBUM_ARTIST_NAME),
                metadata.getString(Metadata.KEY_ALBUM_NAME));
        if (albumId > 0) {
            cv.put(IndexSchema.Meta.Track.ALBUM_ID, albumId);
        }

        long genreId = getGenreIdForName(metadata.getString(Metadata.KEY_GENRE_NAME));
        if (genreId > 0) {
            cv.put(IndexSchema.Meta.Track.GENRE_ID, genreId);
        }

        String trackName = metadata.getString(Metadata.KEY_TRACK_NAME);
        if (!StringUtils.isEmpty(trackName)) {
            cv.put(IndexSchema.Meta.Track.TRACK_NAME, trackName);
            cv.put(IndexSchema.Meta.Track.TRACK_KEY, keyFor(trackName));
        }

        int trackNum = metadata.getInt(Metadata.KEY_TRACK_NUMBER);
        if (trackNum > 0) {
            cv.put(IndexSchema.Meta.Track.TRACK_NUMBER, trackNum);
        }

        int discNum = metadata.getInt(Metadata.KEY_DISC_NUMBER);
        if (discNum > 0) {
            cv.put(IndexSchema.Meta.Track.DISC_NUMBER, discNum);
        }

        int compilation = metadata.getInt(Metadata.KEY_IS_COMPILATION);
        if (compilation >= 0) {
            cv.put(IndexSchema.Meta.Track.COMPILATION, compilation);
        }

        String mime = metadata.getString(Metadata.KEY_MIME_TYPE);
        if (!StringUtils.isEmpty(mime)) {
            cv.put(IndexSchema.Meta.Track.MIME_TYPE, mime);
        }

        long bitrate = metadata.getLong(Metadata.KEY_BITRATE);
        if (bitrate > 0) {
            cv.put(IndexSchema.Meta.Track.BITRATE, bitrate);
        }
        long duration = metadata.getLong(Metadata.KEY_DURATION);
        if (duration > 0) {
            cv.put(IndexSchema.Meta.Track.DURATION, duration);
        }

        Timber.v("Inserting track metadata %s", cv.toString());
        long id = insert(IndexSchema.Meta.Track.TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (id > 0) {
            mAppContext.getContentResolver().notifyChange(IndexUris.tracks(indexAuthority), null);
        }
        return id;
    }

    long insertTrack(Track track) {
        ContentValues cv = new ContentValues(20);

        long containerId = hasContainer(track.getParentUri());
        if (containerId < 0) {
            Timber.e("No container for track %s", track.getName());
            return -1;
        }
        cv.put(IndexSchema.Tracks.CONTAINER_ID, containerId);
        cv.put(IndexSchema.Tracks.URI, track.getUri().toString());
        cv.put(IndexSchema.Tracks.AUTHORITY, track.getUri().getAuthority());
        cv.put(IndexSchema.Tracks.TRACK_NAME, track.getName());
        cv.put(IndexSchema.Tracks.TRACK_KEY, keyFor(track.getName()));
        String artistName = track.getArtistName();
        if (!StringUtils.isEmpty(artistName)) {
            cv.put(IndexSchema.Tracks.ARTIST_NAME, artistName);
            cv.put(IndexSchema.Tracks.ARTIST_KEY, keyFor(artistName));
        }
        String albumName = track.getAlbumName();
        if (!StringUtils.isEmpty(albumName)) {
            cv.put(IndexSchema.Tracks.ALBUM_NAME, albumName);
            cv.put(IndexSchema.Tracks.ALBUM_KEY, keyFor(albumName));
        }
        String albumArtistName = track.getAlbumArtistName();
        if (!StringUtils.isEmpty(albumArtistName)) {
            cv.put(IndexSchema.Tracks.ALBUM_ARTIST_NAME, albumArtistName);
            cv.put(IndexSchema.Tracks.ALBUM_ARTIST_KEY, keyFor(albumArtistName));
        }
        int trackNum = track.getTrackNumber();
        if (trackNum > 0) {
            cv.put(IndexSchema.Tracks.TRACK_NUMBER, trackNum);
        }
        int discNum = track.getDiscNumber();
        if (discNum > 0) {
            cv.put(IndexSchema.Tracks.DISC_NUMBER, discNum);
        }
        cv.put(IndexSchema.Tracks.COMPILATION, track.isCompilation() ? 1 : 0);
        String genre = track.getGenre();
        if (!StringUtils.isEmpty(genre)) {
            cv.put(IndexSchema.Tracks.GENRE, genre);
            cv.put(IndexSchema.Tracks.GENRE_KEY, keyFor(genre));
        }
        Uri artworkUri = track.getArtworkUri();
        if (artworkUri != null && !Uri.EMPTY.equals(artworkUri)) {
            cv.put(IndexSchema.Tracks.ARTWORK_URI, artworkUri.toString());
        }
        org.opensilk.music.model.Track.Res res = track.getResources().get(0);
        cv.put(IndexSchema.Tracks.RES_URI, res.getUri().toString());
        Map<String,String> headers = res.getHeaders();
        if (!headers.isEmpty()) {
            StringBuilder sb = new StringBuilder(10);
            for (Map.Entry<String,String> entry : headers.entrySet()) {
                sb.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
            }
            cv.put(IndexSchema.Tracks.RES_HEADERS, sb.toString());
        }
        long size = res.getSize();
        if (size > 0) {
            cv.put(IndexSchema.Tracks.RES_SIZE, size);
        }
        cv.put(IndexSchema.Tracks.RES_MIME_TYPE, res.getMimeType());
        long bitrate = res.getBitrate();
        if (bitrate > 0) {
            cv.put(IndexSchema.Tracks.RES_BITRATE, bitrate);
        }
        long lastmod = res.getLastMod();
        if (lastmod > 0) {
            cv.put(IndexSchema.Tracks.RES_LAST_MOD, lastmod);
        }
        long duration = res.getDuration();
        if (duration > 0) {
            cv.put(IndexSchema.Tracks.RES_DURATION, duration);
        }
        cv.put(IndexSchema.Tracks.DATE_ADDED, System.currentTimeMillis());
        long id = getTracksId(track.getUri());
        if (id > 0) {
            Timber.v("Updating track %s", cv.toString());
            cv.remove(IndexSchema.Tracks.URI);
            int num = update(IndexSchema.Tracks.TABLE, cv, idSelection, new String[]{String.valueOf(id)});
            if (num != 1) {
                Timber.e("Error updating track");
            }
        } else {
            Timber.v("Inserting track %s", cv.toString());
            id = insert(IndexSchema.Tracks.TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
        return id;
    }

    static final String checkAlbumSel = IndexSchema.Info.Album.ALBUM_KEY
            + "=? AND " + IndexSchema.Info.Album.ARTIST_KEY + "=?";

    public long getAlbumIdForName(String albumArtist, String album) {
        if (StringUtils.isEmpty(albumArtist) || StringUtils.isEmpty(album)) {
            return -1;
        }
        synchronized (mAlbumIdsCache) {
            if (mAlbumIdsCache.containsKey(albumArtist+album)) {
                return mAlbumIdsCache.get(albumArtist+album);
            }
        }
        albumArtist = StringUtils.trim(albumArtist);
        album = StringUtils.trim(album);
        long id = -1;
        Cursor c = null;
        try {
            final String[] selArgs = new String[]{keyFor(album), keyFor(albumArtist)};
            c = query(IndexSchema.Info.Album.TABLE, idCols, checkAlbumSel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);
            } else {
                long artistId = getArtistIdForName(albumArtist);
                //try to populate from lastfm info
                Metadata albumMeta = mLastFM.lookupAlbumInfo(albumArtist, album);
                if (albumMeta == null) {
                    //lookup failed, used bare minimum
                    albumMeta = Metadata.builder()
                            .putString(Metadata.KEY_ALBUM_NAME, album)
                            .putString(Metadata.KEY_ARTIST_NAME, albumArtist)
                            .build();
                }
                id = insertAlbum(albumMeta, artistId);
                if (id > 0) {
                    mAppContext.getContentResolver().notifyChange(
                            IndexUris.album(indexAuthority, String.valueOf(id)), null);
                }
            }
            if (id > 0) {
                synchronized (mAlbumIdsCache) {
                    mAlbumIdsCache.put(albumArtist + album, id);
                }
            }
            return id;
        } finally {
            closeCursor(c);
        }
    }

    long insertAlbum(Metadata meta, long albumArtistId) {
        ContentValues cv = new ContentValues(10);
        cv.put(IndexSchema.Meta.Album.ALBUM_NAME, meta.getString(Metadata.KEY_ALBUM_NAME));
        cv.put(IndexSchema.Meta.Album.ALBUM_KEY, keyFor(meta.getString(Metadata.KEY_ALBUM_NAME)));
        cv.put(IndexSchema.Meta.Album.ALBUM_MBID, meta.getString(Metadata.KEY_ALBUM_MBID));
        cv.put(IndexSchema.Meta.Album.ALBUM_ARTIST_ID, albumArtistId);
        String bioSummary = meta.getString(Metadata.KEY_ALBUM_SUMMARY);
        String bioContent = meta.getString(Metadata.KEY_ALBUM_BIO);
        long lastMod = meta.getLong(Metadata.KEY_LAST_MODIFIED);
        if (!StringUtils.isEmpty(bioSummary)) {
            cv.put(IndexSchema.Meta.Album.ALBUM_BIO_SUMMARY, bioSummary);
            cv.put(IndexSchema.Meta.Album.ALBUM_BIO_DATE_MOD, lastMod > 0 ? lastMod : System.currentTimeMillis());
        }
        Timber.v("Inserting album %s", cv.toString());
        long id = insert(IndexSchema.Meta.Album.TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id > 0) {
            mAppContext.getContentResolver().notifyChange(IndexUris.albums(indexAuthority), null);
        }
        return id;
    }

    static final String checkArtistSel = IndexSchema.Info.Artist.ARTIST_KEY + "=?";

    public long getArtistIdForName(String artist) {
        if (StringUtils.isEmpty(artist)) {
            return -1;
        }
        artist = StringUtils.trim(artist);
        synchronized (mArtistIdsCache) {
            if (mArtistIdsCache.containsKey(artist)) {
                return mArtistIdsCache.get(artist);
            }
        }
        long id = -1;
        Cursor c = null;
        try {
            final String[] selArgs = new String[]{keyFor(artist)};
            c = query(IndexSchema.Info.Artist.TABLE, idCols, checkArtistSel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);
            } else {
                //try to populate from lastfm info
                Metadata artistMeta = mLastFM.lookupArtistInfo(artist);
                if (artistMeta == null) {
                    //lookup failed, use bare minimum
                    artistMeta = Metadata.builder()
                            .putString(Metadata.KEY_ARTIST_NAME, artist)
                            .build();
                } else {
                    String lfmname = artistMeta.getString(Metadata.KEY_ARTIST_NAME);
                    if (StringUtils.equalsIgnoreCase(artist, lfmname)) {
                        // unless we were auto corrected we will prefer our name
                        // this is to preserve capitalization in CHVRCHES
                        artistMeta = artistMeta.buildUpon()
                                .putString(Metadata.KEY_ARTIST_NAME, artist)
                                .build();
                    }
                }
                id = insertArtist(artistMeta);
                if (id > 0) {
                    mAppContext.getContentResolver().notifyChange(
                            IndexUris.artist(indexAuthority, String.valueOf(id)), null);
                }
            }
            if (id > 0) {
                synchronized (mArtistIdsCache) {
                    mArtistIdsCache.put(artist, id);
                }
            }
            return id;
        } finally {
            closeCursor(c);
        }
    }

    long insertArtist(Metadata meta) {
        ContentValues cv = new ContentValues(10);
        cv.put(IndexSchema.Meta.Artist.ARTIST_NAME, meta.getString(Metadata.KEY_ARTIST_NAME));
        cv.put(IndexSchema.Meta.Artist.ARTIST_KEY, keyFor(meta.getString(Metadata.KEY_ARTIST_NAME)));
        cv.put(IndexSchema.Meta.Artist.ARTIST_MBID, meta.getString(Metadata.KEY_ARTIST_MBID));
        String bioSummary = meta.getString(Metadata.KEY_ARTIST_SUMMARY);
        String bioContent = meta.getString(Metadata.KEY_ARTIST_BIO);
        long lastMod = meta.getLong(Metadata.KEY_LAST_MODIFIED);
        if (!StringUtils.isEmpty(bioSummary)) {
            cv.put(IndexSchema.Meta.Artist.ARTIST_BIO_SUMMARY, bioSummary);
            cv.put(IndexSchema.Meta.Artist.ARTIST_BIO_DATE_MOD, lastMod > 0 ? lastMod : System.currentTimeMillis());
        }
        Timber.d("Inserting Artist %s", cv.toString());
        long id = insert(IndexSchema.Meta.Artist.TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id > 0) {
            mAppContext.getContentResolver().notifyChange(IndexUris.artists(indexAuthority), null);
            mAppContext.getContentResolver().notifyChange(IndexUris.albumArtists(indexAuthority), null);
        }
        return id;
    }

    static final String checkGenreSel = IndexSchema.Info.Genre.GENRE_KEY + "=?";

    public long getGenreIdForName(String genre) {
        if (StringUtils.isEmpty(genre)) {
            return -1;
        }
        synchronized (mGenreIdsCache) {
            if(mGenreIdsCache.containsKey(genre)) {
                return mGenreIdsCache.get(genre);
            }
        }
        long id = -1;
        Cursor c = null;
        try {
            final String[] selArgs = new String[]{keyFor(genre)};
            c = query(IndexSchema.Info.Genre.TABLE, idCols, checkGenreSel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);
            } else {
                //try to populate from lastfm info
                Metadata genreMeta = Metadata.builder()
                        .putString(Metadata.KEY_GENRE_NAME, genre)
                        .build();
                id = insertGenre(genreMeta);
                if (id > 0) {
                    mAppContext.getContentResolver().notifyChange(
                            IndexUris.genre(indexAuthority, String.valueOf(id)), null);
                }
            }
            if (id > 0) {
                synchronized (mGenreIdsCache) {
                    mGenreIdsCache.put(genre, id);
                }
            }
            return id;
        } finally {
            closeCursor(c);
        }
    }

    long insertGenre(Metadata meta) {
        ContentValues cv = new ContentValues(2);
        String name = meta.getString(Metadata.KEY_GENRE_NAME);
        cv.put(IndexSchema.Meta.Genre.GENRE_NAME, name);
        cv.put(IndexSchema.Meta.Genre.GENRE_KEY, keyFor(name));
        Timber.d("Inserting Genre %s", cv.toString());
        long id = insert(IndexSchema.Meta.Genre.TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id > 0) {
            mAppContext.getContentResolver().notifyChange(IndexUris.genres(indexAuthority), null);
        }
        return id;
    }

    public long insertPlaylist(String name) {
        ContentValues cv = new ContentValues(5);
        cv.put(IndexSchema.Meta.Playlist.NAME, name);
        cv.put(IndexSchema.Meta.Playlist.DATE_ADDED, System.currentTimeMillis());
        cv.put(IndexSchema.Meta.Playlist.DATE_MODIFIED, System.currentTimeMillis());
        long id = insert(IndexSchema.Meta.Playlist.TABLE, null, cv, SQLiteDatabase.CONFLICT_NONE);
        if (id > 0) {
            mAppContext.getContentResolver().notifyChange(IndexUris.playlists(indexAuthority), null);
        }
        return id;
    }

    static final String[] highestPlaylistPlayPosCols = new String[] {
            IndexSchema.Meta.PlaylistTrack.PLAY_ORDER
    };
    static final String playlistTrackPlaylistIdSel = IndexSchema.Meta.PlaylistTrack.PLAYLIST_ID + "=?";

    static final String[] addToPlaylistCols = new String[] {
            IndexSchema.Info.Track._ID,
            IndexSchema.Info.Track.URI,
    };

    int addToPlaylistLocked(String playlist_id, List<Uri> uriList, SQLiteDatabase db) {
        int numinserted = 0;
        Cursor c = null;
        try {
            c = db.query(IndexSchema.Meta.PlaylistTrack.TABLE, highestPlaylistPlayPosCols,
                    playlistTrackPlaylistIdSel, new String[]{playlist_id}, null, null,
                    IndexSchema.Meta.PlaylistTrack.PLAY_ORDER);
            int startOrder = 0;
            if (c != null && c.moveToLast()) {
                startOrder = c.getInt(0) + 1;
            }
            closeCursor(c);
            c = getTrackListCursor(uriList, addToPlaylistCols);
            if (c != null && c.moveToFirst()) {
                ContentValues cv = new ContentValues(5);
                do {
                    long id = c.getLong(0);
                    Uri uri = Uri.parse(c.getString(1));
                    int idx = uriList.indexOf(uri);
                    cv.put(IndexSchema.Meta.PlaylistTrack.PLAYLIST_ID, playlist_id);
                    cv.put(IndexSchema.Meta.PlaylistTrack.TRACK_ID, id);
                    cv.put(IndexSchema.Meta.PlaylistTrack.PLAY_ORDER, startOrder + idx);
                    if (db.insert(IndexSchema.Meta.PlaylistTrack.TABLE, null, cv) > 0) {
                        numinserted++;
                    }
                    cv.clear();
                } while (c.moveToNext());
            }
        } finally {
            closeCursor(c);
        }
        return numinserted;
    }

    public int addToPlaylist(String playlist_id, List<Uri> uriList) {
        int numinserted = 0;
        mLock.writeLock().lock();
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            numinserted = addToPlaylistLocked(playlist_id, uriList, db);
            if (numinserted > 0) {
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
            mLock.writeLock().unlock();
        }
        mAppContext.getContentResolver().notifyChange(IndexUris.playlist(indexAuthority, playlist_id), null);
        //todo if num inserted != list.size reorder the playlist tracks
        return numinserted;
    }

    @Override
    public int updatePlaylist(String playlist_id, List<Uri> uriList) {
        int numinserted = 0;
        mLock.writeLock().lock();
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(IndexSchema.Meta.PlaylistTrack.TABLE,
                    IndexSchema.Meta.PlaylistTrack.PLAYLIST_ID + "=" + playlist_id, null);
            if (!uriList.isEmpty()) {
                numinserted = addToPlaylistLocked(playlist_id, uriList, db);
            } else {
                numinserted = 1;//todo num removed
            }
            if (numinserted > 0) {
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
            mLock.writeLock().unlock();
        }
        mAppContext.getContentResolver().notifyChange(IndexUris.playlist(indexAuthority, playlist_id), null);
        //todo if num inserted != list.size reorder the playlist tracks
        return numinserted;
    }

    static final String[] movePlaylistEntryCols = new String[] {
            IndexSchema.Meta.PlaylistTrack.PLAY_ORDER,
    };
    static final String movePlaylistEntrySel =
            IndexSchema.Meta.PlaylistTrack.PLAYLIST_ID + "=?";

    @Override
    public int movePlaylistEntry(String playlist_id, int from, int to) {
        if (from == to) {
            return 0;
        }
        int numlines = 0;
        mLock.writeLock().lock();
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        Cursor c = null;
        try {
            //find the from play_order
            c = db.query(IndexSchema.Meta.PlaylistTrack.TABLE,
                    movePlaylistEntryCols, movePlaylistEntrySel,
                    new String[] {playlist_id}, null, null,
                    IndexSchema.Meta.PlaylistTrack.PLAY_ORDER,
                    from + ",1");
            if (c == null || !c.moveToFirst()) {
                return 0;
            }
            int from_play_order = c.getInt(0);
            closeCursor(c);
            //find the to play_order
            c = db.query(IndexSchema.Meta.PlaylistTrack.TABLE,
                    movePlaylistEntryCols, movePlaylistEntrySel,
                    new String[] {playlist_id}, null, null,
                    IndexSchema.Meta.PlaylistTrack.PLAY_ORDER,
                    to + ",1");
            if (c == null || !c.moveToFirst()) {
                return 0;
            }
            int to_play_order = c.getInt(0);
            //re order the list
            db.execSQL("UPDATE " + IndexSchema.Meta.PlaylistTrack.TABLE + " SET" +
                    " play_order=-1" +
                    " WHERE play_order=" + from_play_order +
                    " AND playlist_id=" + playlist_id);
            if (from < to) {
                db.execSQL("UPDATE " + IndexSchema.Meta.PlaylistTrack.TABLE + " SET" +
                        " play_order=play_order-1" +
                        " WHERE play_order<=" + to_play_order +
                        " AND play_order>" + from_play_order +
                        " AND playlist_id=" + playlist_id);
                numlines = to - from + 1;
            } else {
                db.execSQL("UPDATE " + IndexSchema.Meta.PlaylistTrack.TABLE + " SET" +
                        " play_order=play_order+1" +
                        " WHERE play_order>=" + to_play_order +
                        " AND play_order<" + from_play_order +
                        " AND playlist_id=" + playlist_id);
                numlines = from - to + 1;
            }
            db.execSQL("UPDATE " + IndexSchema.Meta.PlaylistTrack.TABLE + " SET" +
                    " play_order=" + to_play_order +
                    " WHERE play_order=-1 AND playlist_id=" + playlist_id);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            mLock.writeLock().unlock();
            closeCursor(c);
        }
        mAppContext.getContentResolver().notifyChange(IndexUris.playlist(indexAuthority, playlist_id), null);
        return numlines;
    }

    @Override
    public int removeFromPlaylist(String playlist_id, Uri uri) {
        int numlines = 0;
        mLock.writeLock().lock();
        Cursor c = null;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            //first get the track id from the uri
            c = db.query(IndexSchema.Info.Track.TABLE, idCols, trackUriSel,
                    new String[]{uri.toString()}, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return 0;
            }
            String id = c.getString(0);
            closeCursor(c);
            //then find that tracks play_order in the playlist
            c = db.query(IndexSchema.Meta.PlaylistTrack.TABLE,
                    movePlaylistEntryCols,
                    " track_id=" + id +
                    " AND playlist_id=" + playlist_id,
                    null, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return 0;
            }
            int play_order = c.getInt(0);
            //then delete the item
            db.delete(IndexSchema.Meta.PlaylistTrack.TABLE,
                    " track_id=" + id +
                    " AND playlist_id=" + playlist_id,
                    null);
            //decrement the play orders following it
            db.execSQL("UPDATE " + IndexSchema.Meta.PlaylistTrack.TABLE + " SET" +
                    " play_order=play_order-1" +
                    " WHERE play_order>" + play_order +
                    " AND playlist_id=" + playlist_id);
            numlines = 1;//TODO actual number of entries changed;
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            mLock.writeLock().unlock();
            closeCursor(c);
        }
        mAppContext.getContentResolver().notifyChange(IndexUris.playlist(indexAuthority, playlist_id), null);
        return numlines;
    }

    @Override
    public int removeFromPlaylist(String playlist_id, int position) {
        int numlines = 0;
        mLock.writeLock().lock();
        Cursor c = null;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            //see if its a valid pos
            c = db.query(IndexSchema.Meta.PlaylistTrack.TABLE,
                    movePlaylistEntryCols,
                    " play_order=" + position + " AND playlist_id=" + playlist_id,
                    null, null, null, null);
            if (c == null || c.getCount() == 0) {
                return 0;
            }
            //then delete the item
            db.delete(IndexSchema.Meta.PlaylistTrack.TABLE,
                    " play_order=" + position +
                            " AND playlist_id=" + playlist_id,
                    null);
            //decrement the play orders following it
            db.execSQL("UPDATE " + IndexSchema.Meta.PlaylistTrack.TABLE + " SET" +
                    " play_order=play_order-1" +
                    " WHERE play_order>" + position +
                    " AND playlist_id=" + playlist_id);
            numlines = 1;//TODO actual number of entries changed;
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            mLock.writeLock().unlock();
            closeCursor(c);
        }
        mAppContext.getContentResolver().notifyChange(IndexUris.playlist(indexAuthority, playlist_id), null);
        return numlines;
    }

    @Override
    public int removePlaylists(String[] playlist_ids) {
        int num = 0;
        if (playlist_ids != null && playlist_ids.length > 0) {
            StringBuilder sb = new StringBuilder(IndexSchema.Meta.Playlist._ID).append(" IN (?");
            for (int ii=1; ii<playlist_ids.length; ii++) {
                sb.append(",?");
            }
            sb.append(")");
            num = delete(IndexSchema.Meta.Playlist.TABLE, sb.toString(), playlist_ids);
        }
        if (num > 0) {
            mAppContext.getContentResolver().notifyChange(IndexUris.playlists(indexAuthority), null);
        }
        return num;
    }

    final Map<Uri, Long> mConainerIdsCache = new HashMap<>();
    final Map<String, Long> mAlbumIdsCache = new HashMap<>();
    final Map<String, Long> mArtistIdsCache = new HashMap<>();
    final Map<String, Long> mGenreIdsCache = new HashMap<>();

    void clearCaches() {
        synchronized (mConainerIdsCache) {
            mConainerIdsCache.clear();
        }
        synchronized (mAlbumIdsCache) {
            mAlbumIdsCache.clear();
        }
        synchronized (mArtistIdsCache) {
            mArtistIdsCache.clear();
        }
        synchronized (mGenreIdsCache) {
            mGenreIdsCache.clear();
        }
    }

    static final String playbackSettingsSel = IndexSchema.PlaybackSettings.KEY + "=?";
    static final String[] intValCols = new String[] {
            IndexSchema.PlaybackSettings.INT_VALUE,
    };
    static final String[] textValCols = new String[] {
            IndexSchema.PlaybackSettings.TEXT_VALUE,
    };
    static final String[] lastQueueListKey = new String[] {
            IndexSchema.PlaybackSettings.KEY_LAST_QUEUE_LIST,
    };
    static final String[] lastQueuePosKey = new String[] {
            IndexSchema.PlaybackSettings.KEY_LAST_QUEUE_POS,
    };
    static final String[] lastQueueRepeatKey = new String[] {
            IndexSchema.PlaybackSettings.KEY_LAST_QUEUE_REPEAT,
    };
    static final String[] lastQueueShuffleKey = new String[] {
            IndexSchema.PlaybackSettings.KEY_LAST_QUEUE_SHUFFLE,
    };
    static final String[] lastSeekPosKey = new String[] {
            IndexSchema.PlaybackSettings.KEY_LAST_SEEK_POS,
    };
    static final String[] broadcastMetaKey = new String[] {
            IndexSchema.PlaybackSettings.BROADCAST_META,
    };


    public List<Uri> getLastQueue() {
        Cursor c = null;
        try {
            c = query(IndexSchema.PlaybackSettings.TABLE, textValCols,
                    playbackSettingsSel, lastQueueListKey, null, null, null);
            if (c != null && c.moveToFirst()) {
                String q = c.getString(0);
                if (!StringUtils.isEmpty(q)) {
                    String[] strings = StringUtils.split(q, ',');
                    List<Uri> lst = new ArrayList<>(strings.length);
                    for (String s : strings) {
                        lst.add(Uri.parse(s));
                    }
                    return lst;
                }
            }
        } finally {
            closeCursor(c);
        }
        return Collections.emptyList();
    }

    @Override
    public void saveQueue(List<Uri> queue) {
        if (queue != null && queue.size() > 0) {
            ContentValues cv = new ContentValues(2);
            cv.put(IndexSchema.PlaybackSettings.KEY, IndexSchema.PlaybackSettings.KEY_LAST_QUEUE_LIST);
            String q = StringUtils.join(queue, ',');
            cv.put(IndexSchema.PlaybackSettings.TEXT_VALUE, q);
            insert(IndexSchema.PlaybackSettings.TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } else {
            delete(IndexSchema.PlaybackSettings.TABLE, playbackSettingsSel, lastQueueListKey);
        }
    }

    @Override
    public int getLastQueuePosition() {
        Cursor c = null;
        try {
            c = query(IndexSchema.PlaybackSettings.TABLE, intValCols,
                    playbackSettingsSel, lastQueuePosKey, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            closeCursor(c);
        }
        return -1;
    }

    @Override
    public void saveQueuePosition(int pos) {
        if (pos < 0) {
            delete(IndexSchema.PlaybackSettings.TABLE, playbackSettingsSel, lastQueuePosKey);
        } else {
            ContentValues cv = new ContentValues(2);
            cv.put(IndexSchema.PlaybackSettings.KEY, IndexSchema.PlaybackSettings.KEY_LAST_QUEUE_POS);
            cv.put(IndexSchema.PlaybackSettings.INT_VALUE, pos);
            insert(IndexSchema.PlaybackSettings.TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    @Override
    public int getLastQueueShuffleMode() {
        Cursor c = null;
        try {
            c = query(IndexSchema.PlaybackSettings.TABLE, intValCols,
                    playbackSettingsSel, lastQueueShuffleKey, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            closeCursor(c);
        }
        return -1;
    }

    @Override
    public void saveQueueShuffleMode(int mode) {
        if (mode < 0) {
            delete(IndexSchema.PlaybackSettings.TABLE, playbackSettingsSel, lastQueueShuffleKey);
        } else {
            ContentValues cv = new ContentValues(2);
            cv.put(IndexSchema.PlaybackSettings.KEY, IndexSchema.PlaybackSettings.KEY_LAST_QUEUE_SHUFFLE);
            cv.put(IndexSchema.PlaybackSettings.INT_VALUE, mode);
            insert(IndexSchema.PlaybackSettings.TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    @Override
    public int getLastQueueRepeatMode() {
        Cursor c = null;
        try {
            c = query(IndexSchema.PlaybackSettings.TABLE, intValCols,
                    playbackSettingsSel, lastQueueRepeatKey, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            closeCursor(c);
        }
        return -1;
    }

    @Override
    public void saveQueueRepeatMode(int mode) {
        if (mode < 0) {
            delete(IndexSchema.PlaybackSettings.TABLE, playbackSettingsSel, lastQueueRepeatKey);
        } else {
            ContentValues cv = new ContentValues(2);
            cv.put(IndexSchema.PlaybackSettings.KEY, IndexSchema.PlaybackSettings.KEY_LAST_QUEUE_REPEAT);
            cv.put(IndexSchema.PlaybackSettings.INT_VALUE, mode);
            insert(IndexSchema.PlaybackSettings.TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    @Override
    public long getLastSeekPosition() {
        Cursor c = null;
        try {
            c = query(IndexSchema.PlaybackSettings.TABLE, intValCols,
                    playbackSettingsSel, lastSeekPosKey, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getLong(0);
            }
        } finally {
            closeCursor(c);
        }
        return -1;
    }

    @Override
    public void saveLastSeekPosition(long pos) {
        if (pos < 0) {
            delete(IndexSchema.PlaybackSettings.TABLE, playbackSettingsSel, lastSeekPosKey);
        } else {
            ContentValues cv = new ContentValues(2);
            cv.put(IndexSchema.PlaybackSettings.KEY, IndexSchema.PlaybackSettings.KEY_LAST_SEEK_POS);
            cv.put(IndexSchema.PlaybackSettings.INT_VALUE, pos);
            insert(IndexSchema.PlaybackSettings.TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    @Override
    public boolean getBroadcastMeta() {
        Cursor c = null;
        try {
            c = query(IndexSchema.PlaybackSettings.TABLE, intValCols,
                    playbackSettingsSel, broadcastMetaKey, null, null, null);
            if (c != null && c.moveToFirst()) {
                return getIntOrNeg(c, 0) == 1;
            }
        } finally {
            closeCursor(c);
        }
        return false;
    }

    @Override
    public void setBroadcastMeta(boolean broadcast) {
        ContentValues cv = new ContentValues(2);
        cv.put(IndexSchema.PlaybackSettings.KEY, IndexSchema.PlaybackSettings.BROADCAST_META);
        cv.put(IndexSchema.PlaybackSettings.INT_VALUE, broadcast ? 1 : 0);
        insert(IndexSchema.PlaybackSettings.TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static String getStringOrNull(Cursor c, int idx) {
        try {
            return c.getString(idx);
        } catch (IllegalArgumentException|NullPointerException e) {
            return null;
        }
    }

    public static long getLongOrNeg(Cursor c, int idx) {
        try {
            return c.getLong(idx);
        } catch (IllegalArgumentException|NullPointerException e) {
            return -1;
        }
    }

    public static int getIntOrNeg(Cursor c, int idx) {
        try {
            return c.getInt(idx);
        } catch (IllegalArgumentException|NullPointerException e) {
            return -1;
        }
    }

    public static void closeCursor(Cursor c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }
}
