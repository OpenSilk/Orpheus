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

package org.opensilk.music.library.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.opensilk.music.core.model.Album;
import org.opensilk.music.core.model.Artist;
import org.opensilk.music.core.model.Folder;
import org.opensilk.music.core.model.Track;
import org.opensilk.music.core.spi.Bundleable;
import org.opensilk.music.library.compare.AlbumCompare;
import org.opensilk.music.library.compare.ArtistCompare;
import org.opensilk.music.library.compare.FolderTrackCompare;
import org.opensilk.music.library.compare.TrackCompare;
import org.opensilk.music.library.sort.BundleableSortOrder;
import org.opensilk.music.library.util.CursorUtil;

import java.util.List;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;

import static org.opensilk.music.library.provider.LibraryUris.*;

/**
 * Created by drew on 4/26/15.
 */
public class LibraryProvider extends ContentProvider {
    public static final String TAG = LibraryProvider.class.getSimpleName();

    public static final String AUTHORITY_PFX = "orpheus.library.";

    /*
     * Bundle keys for args
     */

    //Never null
    public static final String ARG_URI = "arg_uri";
    public static final String ARG_PROJECTION = "arg_projection";
    public static final String ARG_SELECTION = "arg_selection";
    public static final String ARG_SELECTIONARGS = "arg_selectionargs";
    //Never null
    public static final String ARG_SORTORDER = "arg_sortorder";

    private UriMatcher mMatcher;

    @Override
    public boolean onCreate() {
        mMatcher = LibraryUris.makeMatcher(AUTHORITY_PFX + getAuthority());
        return true;
    }

    protected String getAuthority() {
        return getContext().getPackageName();
    }

    @Override
    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() < 2) {
            Log.w(TAG, "Not enough path segments: uri="+uri);
            return null;
        }
        //Merge everything into a bundle, This is much nicer than passing 50 arguments
        //and allows easier extending in the future.
        Bundle args = new Bundle(6);
        args.putParcelable(ARG_URI, uri);
        args.putStringArray(ARG_PROJECTION, projection);
        args.putString(ARG_SELECTION, selection);
        args.putStringArray(ARG_SELECTIONARGS, selectionArgs);
        args.putString(ARG_SORTORDER, sortOrder != null ? sortOrder : BundleableSortOrder.A_Z);
        final String library = pathSegments.get(0);

        Cursor c = null;
        switch (mMatcher.match(uri)) {
            case M_ALBUMS:
                c = queryAlbumsInternal(library, args);
                break;
            case M_ALBUM:
                c = getAlbumInternal(library, uri.getLastPathSegment(), args);
                break;
            case M_ARTISTS:
                c = queryArtistsInternal(library, args);
                break;
            case M_ARTIST:
                c = getArtistInternal(library, uri.getLastPathSegment(), args);
                break;
            case M_FOLDERS:
                c = getFoldersTracksInternal(library, null, args);
                break;
            case M_FOLDER:
                c = getFoldersTracksInternal(library, uri.getLastPathSegment(), args);
                break;
            case M_TRACKS:
                c = queryTracksInternal(library, args);
                break;
            case M_TRACK:
                c = getTrackInternal(library, uri.getLastPathSegment(), args);
                break;
        }
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    protected Cursor queryAlbumsInternal(String library, Bundle args) {
        MatrixCursor c = null;
        try {
            Observable<Album> o = Observable.create(subscriber -> {
                queryAlbums(library, subscriber, args);
            });
            c = o.toSortedList((album1, album2) -> AlbumCompare.comparator(args.getString(ARG_SORTORDER)).compare(album1, album2))
                    .flatMap(Observable::from)
                    .collect(CursorUtil.newAlbumCursor(), (matrixCursor, album) ->
                            CursorUtil.populateRow(matrixCursor.newRow(), album))
                    .toBlocking().first();
        } catch (Exception e) {
            Log.w("LibraryProvider", "getAlbumsInternal", e);
        }
        return c;
    }

    protected Cursor getAlbumInternal(String library, String identity, Bundle args) {
        MatrixCursor c = null;
        try {
            Observable<Album> o = Observable.create(subscriber -> {
                getAlbum(library, identity, subscriber, args);
            });
            Album album = o.toBlocking().first();
            c = CursorUtil.newAlbumCursor();
            CursorUtil.populateRow(c.newRow(), album);
        } catch (Exception e) {
            Log.w("LibraryProvider", "getAlbumInternal", e);
        }
        return c;
    }

    protected Cursor queryArtistsInternal(String library, Bundle args) {
        MatrixCursor c = null;
        try {
            Observable<Artist> o = Observable.create(subscriber -> {
                queryArtists(library, subscriber, args);
            });
            c = o.toSortedList((artist1, artist2) -> ArtistCompare.comparator(args.getString(ARG_SORTORDER)).compare(artist1, artist2))
                    .flatMap(Observable::from)
                    .collect(CursorUtil.newArtistCursor(), (matrixCursor, artist1) ->
                            CursorUtil.populateRow(matrixCursor.newRow(), artist1))
                    .toBlocking().first();
        } catch (Exception e) {
            Log.w("LibraryProvider", "queryArtistsInternal", e);
        }
        return c;
    }

    protected Cursor getArtistInternal(String library, String identity, Bundle args) {
        MatrixCursor c = null;
        try {
            Observable<Artist> o = Observable.create(subscriber -> {
                getArtist(library, identity, subscriber, args);
            });
            Artist artist = o.toBlocking().first();
            c = CursorUtil.newArtistCursor();
            CursorUtil.populateRow(c.newRow(), artist);
        } catch (Exception e) {
            Log.w("LibraryProvider", "getArtistInternal", e);
        }
        return c;
    }

    @DebugLog
    protected Cursor getFoldersTracksInternal(String library, String identity, Bundle args) {
        MatrixCursor c = null;
        try {
            Observable<Bundleable> o = Observable.create(subscriber -> {
                getFoldersTracks(library, identity, subscriber, args);
            });
            c = o.filter(
                    bundleable -> {
                        //In a perfect world this wouldn't be needed
                        return (bundleable instanceof Folder) || (bundleable instanceof Track);
                    })
                    .toSortedList((bundleable, bundleable2) ->
                            FolderTrackCompare.comparator(args.getString(ARG_SORTORDER)).compare(bundleable, bundleable2))
                    .flatMap(Observable::from)
                    .collect(CursorUtil.newFolderTrackCursor(), (matrixCursor, bundleable) ->
                                    CursorUtil.populateFolderTrackRow(matrixCursor.newRow(), bundleable))
                    .toBlocking().first();
        } catch (Exception e) {
            Log.w("LibraryProvider", "getFoldersTracksInternal", e);
        }
        return c;
    }

    protected Cursor queryTracksInternal(String library, Bundle args) {
        MatrixCursor c = null;
        try {
            Observable<Track> o = Observable.create(subscriber -> {
                queryTracks(library, subscriber, args);
            });
            c = o.toSortedList((track, track2) -> TrackCompare.comparator(args.getString(ARG_SORTORDER)).compare(track, track2))
                    .flatMap(Observable::from)
                    .collect(CursorUtil.newTrackCursor(), (matrixCursor, track) ->
                            CursorUtil.populateRow(matrixCursor.newRow(), track))
                    .toBlocking().first();
        } catch (Exception e) {
            Log.w("LibraryProvider", "queryTracksInternal", e);
        }
        return c;
    }

    protected Cursor getTrackInternal(String library, String identity, Bundle args) {
        MatrixCursor c = null;
        try {
            Observable<Track> o = Observable.create(subscriber -> {
                getTrack(library, identity, subscriber, args);
            });
            Track track = o.toBlocking().first();
            c = CursorUtil.newTrackCursor();
            CursorUtil.populateRow(c.newRow(), track);
        } catch (Exception e) {
            Log.w("LibraryProvider", "getTrackInternal", e);
        }
        return c;
    }

    protected void queryAlbums(String library, Subscriber<? super Album> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getAlbum(String library, String identity, Subscriber<? super Album> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void queryArtists(String library, Subscriber<? super Artist> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getArtist(String library, String identity, Subscriber<? super Artist> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getFoldersTracks(String library, String identity, Subscriber<? super Bundleable> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void queryTracks(String library, Subscriber<? super Track> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getTrack(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}
