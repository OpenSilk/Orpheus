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
import android.util.Log;

import org.opensilk.music.core.model.Album;
import org.opensilk.music.core.model.Artist;
import org.opensilk.music.core.model.Folder;
import org.opensilk.music.core.model.Track;
import org.opensilk.music.core.spi.Bundleable;
import org.opensilk.music.library.compare.AlbumCompare;
import org.opensilk.music.library.compare.ArtistCompare;
import org.opensilk.music.library.compare.FolderTrackCompare;
import org.opensilk.music.library.util.CursorUtils;

import java.util.List;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.functions.Func2;

import static org.opensilk.music.library.provider.LibraryUris.*;

/**
 * Created by drew on 4/26/15.
 */
public class LibraryProvider extends ContentProvider {

    public static final String AUTHORITY_PFX = "orpheus.library.";

    private UriMatcher mMatcher;

    @Override
    public boolean onCreate() {
        mMatcher = LibraryUris.makeMatcher(AUTHORITY_PFX + getAuthority());
        return true;
    }

    protected String getAuthority() {
        return "";
    }

    @Override
    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.i("LibraryProvider", "uri="+uri);
        if (sortOrder == null) sortOrder = "";
        Cursor c = null;
        switch (mMatcher.match(uri)) {
            case M_ALBUMS:
                c = queryAlbumsInternal(projection, selection, selectionArgs, sortOrder);
                break;
            case M_ALBUM:
                c = getAlbumInternal(uri.getLastPathSegment());
                break;
            case M_ARTISTS:
                c = queryArtistsInternal(projection, selection, selectionArgs, sortOrder);
                break;
            case M_ARTIST:
                c = getArtistInternal(uri.getLastPathSegment());
                break;
            case M_FOLDERS:
                c = getFoldersTracksInternal(null, projection, selection, selectionArgs, sortOrder);
                break;
            case M_FOLDER:
                c = getFoldersTracksInternal(uri.getLastPathSegment(), projection, selection, selectionArgs, sortOrder);
                break;
            case M_TRACKS:
                c = queryTracksInternal(projection, selection, selectionArgs, sortOrder);
                break;
            case M_TRACK:
                c = getTrackInternal(uri.getLastPathSegment());
                break;
        }
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    protected Cursor queryAlbumsInternal(String[] projection, String selection, String[] selectionArgs, final String sortOrder) {
        MatrixCursor c = null;
        try {
            c = Observable.create(new Observable.OnSubscribe<Album>() {
                @Override
                public void call(Subscriber<? super Album> subscriber) {
                    queryAlbums(subscriber);
                }
            }).toSortedList(new Func2<Album, Album, Integer>() {
                @Override
                public Integer call(Album album, Album album2) {
                    return AlbumCompare.comparator(sortOrder).compare(album, album2);
                }
            }).flatMap(new Func1<List<Album>, Observable<Album>>() {
                @Override
                public Observable<Album> call(List<Album> albums) {
                    return Observable.from(albums);
                }
            }).collect(CursorUtils.newAlbumCursor(), new Action2<MatrixCursor, Album>() {
                @Override
                public void call(MatrixCursor matrixCursor, Album album) {
                    CursorUtils.populateRow(matrixCursor.newRow(), album);
                }
            }).toBlocking().first();
        } catch (Exception e) {
            Log.w("LibraryProvider", "getAlbumsInternal", e);
        }
        return c;
    }

    protected void queryAlbums(Subscriber<? super Album> subscriber) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected Cursor getAlbumInternal(final String identity) {
        MatrixCursor c = null;
        try {
            Album album = Observable.create(new Observable.OnSubscribe<Album>() {
                @Override
                public void call(Subscriber<? super Album> subscriber) {
                    getAlbum(identity, subscriber);
                }
            }).toBlocking().first();
            c = CursorUtils.newAlbumCursor();
            CursorUtils.populateRow(c.newRow(), album);
        } catch (Exception e) {
            Log.w("LibraryProvider", "getAlbumsInternal", e);
        }
        return c;
    }

    protected void getAlbum(String identity, Subscriber<? super Album> subscriber) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected Cursor queryArtistsInternal(String[] projection, String selection, String[] selectionArgs, final String sortOrder) {
        MatrixCursor c = null;
        try {
            c = Observable.create(new Observable.OnSubscribe<Artist>() {
                @Override
                public void call(Subscriber<? super Artist> subscriber) {
                    queryArtists(subscriber);
                }
            }).toSortedList(new Func2<Artist, Artist, Integer>() {
                @Override
                public Integer call(Artist artist, Artist artist2) {
                    return ArtistCompare.comparator(sortOrder).compare(artist, artist2);
                }
            }).flatMap(new Func1<List<Artist>, Observable<Artist>>() {
                @Override
                public Observable<Artist> call(List<Artist> artists) {
                    return Observable.from(artists);
                }
            }).collect(CursorUtils.newArtistCursor(), new Action2<MatrixCursor, Artist>() {
                @Override
                public void call(MatrixCursor matrixCursor, Artist artist) {
                    CursorUtils.populateRow(matrixCursor.newRow(), artist);
                }
            }).toBlocking().first();
        } catch (Exception e) {
            Log.w("LibraryProvider", "getAlbumsInternal", e);
        }
        return c;
    }

    protected void queryArtists(Subscriber<? super Artist> subscriber) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected Cursor getArtistInternal(String identity) {
        return null;
    }

    protected void getArtist(String identity, Subscriber<? super Artist> subscriber) {
        subscriber.onError(new UnsupportedOperationException());
    }

    @DebugLog
    protected Cursor getFoldersTracksInternal(final String identity, String[] projection, String selection, String[] selectionArgs, final String sortOrder) {
        MatrixCursor c = null;
        try {
            c = Observable.create(new Observable.OnSubscribe<Bundleable>() {
                @Override
                public void call(Subscriber<? super Bundleable> subscriber) {
                    getFoldersTracks(identity, subscriber);
                }
            }).filter(new Func1<Bundleable, Boolean>() {
                @Override
                public Boolean call(Bundleable bundleable) {
                    //In a perfect world this wouldn't be needed
                    return (bundleable instanceof Folder) || (bundleable instanceof Track);
                }
            }).toSortedList(new Func2<Bundleable, Bundleable, Integer>() {
                @Override
                public Integer call(Bundleable bundleable, Bundleable bundleable2) {
                    return FolderTrackCompare.comparator(sortOrder).compare(bundleable, bundleable2);
                }
            }).flatMap(new Func1<List<Bundleable>, Observable<Bundleable>>() {
                @Override
                public Observable<Bundleable> call(List<Bundleable> bundleables) {
                    return Observable.from(bundleables);
                }
            }).collect(CursorUtils.newFolderTrackCursor(), new Action2<MatrixCursor, Bundleable>() {
                @Override
                public void call(MatrixCursor matrixCursor, Bundleable bundleable) {
                    CursorUtils.populateFolderTrackRow(matrixCursor.newRow(), bundleable);
                }
            }).toBlocking().first();
        } catch (Exception e) {
            Log.w("LibraryProvider", "getFoldersTracksInternal", e);
        }
        return c;
    }

    protected void getFoldersTracks(String identity, Subscriber<? super Bundleable> subscriber) {
        subscriber.onError(new Throwable(new UnsupportedOperationException()));
    }

    protected Cursor queryTracksInternal(String[] projection, String selection, String[] selectionArgs, final String sortOrder) {
        return null;
    }

    protected void queryTracks(Subscriber<? super Track> subscriber) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected Cursor getTrackInternal(String identity) {
        return null;
    }

    protected void getTrack(String identity, Subscriber<? super Track> subscriber) {
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
