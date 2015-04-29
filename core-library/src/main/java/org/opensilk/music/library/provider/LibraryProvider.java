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

import org.opensilk.music.core.exception.ParcelableException;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

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
    private final Map<String, ParcelableException> mCaughtExceptions =
            Collections.synchronizedMap(new HashMap<String, ParcelableException>());

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

    protected Cursor queryAlbumsInternal(final String library, final Bundle args) {
        MatrixCursor c = null;
        try {
            c = doQueryInternal(
                    new Observable.OnSubscribe<Album>() {
                        @Override
                        public void call(Subscriber<? super Album> subscriber) {
                            queryAlbums(library, subscriber, args);
                        }
                    },
                    new Func2<Album, Album, Integer>() {
                        @Override
                        public Integer call(Album album, Album album2) {
                            return AlbumCompare.comparator(args.getString(ARG_SORTORDER)).compare(album, album2);
                        }
                    },
                    new Func0<MatrixCursor>() {
                        @Override
                        public MatrixCursor call() {
                            return CursorUtil.newAlbumCursor();
                        }
                    },
                    new Action2<MatrixCursor, Album>() {
                        @Override
                        public void call(MatrixCursor matrixCursor, Album album) {
                            CursorUtil.populateRow(matrixCursor.newRow(), album);
                        }
                    }
            );
        } catch (Exception e) {
            Log.w(TAG, "getAlbumsInternal", e);
            saveForLater(args.<Uri>getParcelable(ARG_URI), e);
        }
        return c;
    }

    protected Cursor getAlbumInternal(final String library, final String identity, final Bundle args) {
        MatrixCursor c = null;
        try {
            Album album = Observable.create(new Observable.OnSubscribe<Album>() {
                @Override
                public void call(Subscriber<? super Album> subscriber) {
                    getAlbum(library, identity, subscriber, args);
                }
            }).toBlocking().first();
            c = CursorUtil.newAlbumCursor();
            CursorUtil.populateRow(c.newRow(), album);
        } catch (Exception e) {
            Log.w(TAG, "getAlbumInternal", e);
            saveForLater(args.<Uri>getParcelable(ARG_URI), e);
        }
        return c;
    }

    protected Cursor queryArtistsInternal(final String library, final Bundle args) {
        MatrixCursor c = null;
        try {
            c = doQueryInternal(
                    new Observable.OnSubscribe<Artist>() {
                        @Override
                        public void call(Subscriber<? super Artist> subscriber) {
                            queryArtists(library, subscriber, args);
                        }
                    },
                    new Func2<Artist, Artist, Integer>() {
                        @Override
                        public Integer call(Artist artist, Artist artist2) {
                            return ArtistCompare.comparator(args.getString(ARG_SORTORDER)).compare(artist, artist2);
                        }
                    },
                    new Func0<MatrixCursor>() {
                        @Override
                        public MatrixCursor call() {
                            return CursorUtil.newArtistCursor();
                        }
                    },
                    new Action2<MatrixCursor, Artist>() {
                        @Override
                        public void call(MatrixCursor matrixCursor, Artist artist) {
                            CursorUtil.populateRow(matrixCursor.newRow(), artist);
                        }
                    }
            );
        } catch (Exception e) {
            Log.w(TAG, "queryArtistsInternal", e);
            saveForLater(args.<Uri>getParcelable(ARG_URI), e);
        }
        return c;
    }

    protected Cursor getArtistInternal(final String library, final String identity, final Bundle args) {
        MatrixCursor c = null;
        try {
            Artist artist = Observable.create(new Observable.OnSubscribe<Artist>() {
                @Override
                public void call(Subscriber<? super Artist> subscriber) {
                    getArtist(library, identity, subscriber, args);
                }
            }).toBlocking().first();
            c = CursorUtil.newArtistCursor();
            CursorUtil.populateRow(c.newRow(), artist);
        } catch (Exception e) {
            Log.w(TAG, "getArtistInternal", e);
            saveForLater(args.<Uri>getParcelable(ARG_URI), e);
        }
        return c;
    }

    @DebugLog
    protected Cursor getFoldersTracksInternal(final String library, final String identity, final Bundle args) {
        MatrixCursor c = null;
        try {
            c = Observable.create(new Observable.OnSubscribe<Bundleable>() {
                @Override
                public void call(Subscriber<? super Bundleable> subscriber) {
                    getFoldersTracks(library, identity, subscriber, args);
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
                    return FolderTrackCompare.comparator(args.getString(ARG_SORTORDER)).compare(bundleable, bundleable2);
                }
            }).flatMap(new Func1<List<Bundleable>, Observable<Bundleable>>() {
                @Override
                public Observable<Bundleable> call(List<Bundleable> bundleables) {
                    return Observable.from(bundleables);
                }
            }).collect(CursorUtil.newFolderTrackCursor(), new Action2<MatrixCursor, Bundleable>() {
                @Override
                public void call(MatrixCursor matrixCursor, Bundleable bundleable) {
                    CursorUtil.populateFolderTrackRow(matrixCursor.newRow(), bundleable);
                }
            }).toBlocking().first();
        } catch (Exception e) {
            Log.w(TAG, "getFoldersTracksInternal", e);
            saveForLater(args.<Uri>getParcelable(ARG_URI), e);
        }
        return c;
    }

    protected Cursor queryTracksInternal(final String library, final Bundle args) {
        MatrixCursor c = null;
        try {
            c = doQueryInternal(
                    new Observable.OnSubscribe<Track>() {
                        @Override
                        public void call(Subscriber<? super Track> subscriber) {
                            queryTracks(library, subscriber, args);
                        }
                    },
                    new Func2<Track, Track, Integer>() {
                        @Override
                        public Integer call(Track track, Track track2) {
                            return TrackCompare.comparator(args.getString(ARG_SORTORDER)).compare(track, track2);
                        }
                    },
                    new Func0<MatrixCursor>() {
                        @Override
                        public MatrixCursor call() {
                            return CursorUtil.newTrackCursor();
                        }
                    },
                    new Action2<MatrixCursor, Track>() {
                        @Override
                        public void call(MatrixCursor matrixCursor, Track track) {
                            CursorUtil.populateRow(matrixCursor.newRow(), track);
                        }
                    }
            );
        } catch (Exception e) {
            Log.w(TAG, "queryTracksInternal", e);
            saveForLater(args.<Uri>getParcelable(ARG_URI), e);
        }
        return c;
    }

    protected Cursor getTrackInternal(final String library, final String identity, final Bundle args) {
        MatrixCursor c = null;
        try {
            Track track = Observable.create(new Observable.OnSubscribe<Track>() {
                @Override
                public void call(Subscriber<? super Track> subscriber) {
                    getTrack(library, identity, subscriber, args);
                }
            }).toBlocking().first();
            c = CursorUtil.newTrackCursor();
            CursorUtil.populateRow(c.newRow(), track);
        } catch (Exception e) {
            Log.w(TAG, "getTrackInternal", e);
            saveForLater(args.<Uri>getParcelable(ARG_URI), e);
        }
        return c;
    }

    //The magick function
    protected <T> MatrixCursor doQueryInternal(
            final Observable.OnSubscribe<T> onSubscribe,
            final Func2<T, T, Integer> sortFunc,
            final Func0<MatrixCursor> collector,
            final Action2<MatrixCursor, T> collectAction
    ) {
        return Observable.create(onSubscribe)
                .toSortedList(sortFunc)
                .flatMap(new Func1<List<T>, Observable<T>>() {
                    @Override
                    public Observable<T> call(List<T> ts) {
                        return Observable.from(ts);
                    }
                })
                .collect(collector.call(), collectAction)
                .toBlocking()
                .first();
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

    /**
     * Out of band stuffs
     */
    public interface OB {
        /**
         * Methods
         */
        interface M {
            /**
             * Called after receiving a null cursor
             * arg = Uri string
             */
            String ONNULLCURSOR = "m_onnullcursor";
        }

        /**
         * Response Bundle keys
         */
        interface K {
            /**
             * ParcelalbeException
             */
            String EX = "k_ex";
        }
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method == null) method = "";
        switch (method) {
            case OB.M.ONNULLCURSOR:
                return retrieveSavedException(arg);
            default:
                return null;
        }
    }

    protected Bundle retrieveSavedException(String uri) {
        ParcelableException e = mCaughtExceptions.remove(uri);
        if (e != null) {
            Bundle b = new Bundle();
            b.putParcelable(OB.K.EX, e);
            return b;
        }
        return null;
    }

    protected void saveForLater(Uri uri, Exception e) {
        Exception ex = unwrapE(e);
        if (ex instanceof ParcelableException) {
            mCaughtExceptions.put(uri.toString(), (ParcelableException) ex);
        } else {
            Log.w(TAG, "Thrown exception not instance of ParcelableException");
        }
    }

    //Blocking observable always throws RuntimeExceptions
    private static Exception unwrapE(Exception e) {
        if (e instanceof RuntimeException) {
            Throwable c = e.getCause();
            if (c instanceof Exception) {
                return (Exception) c;
            }
        }
        return e;
    }
}
