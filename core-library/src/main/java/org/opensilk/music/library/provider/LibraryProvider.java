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
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import org.opensilk.music.core.model.Album;
import org.opensilk.music.core.model.Artist;
import org.opensilk.music.core.model.Folder;
import org.opensilk.music.core.model.Track;
import org.opensilk.music.core.spi.Bundleable;
import org.opensilk.music.library.proj.FolderTrackProj;
import org.opensilk.music.library.compare.FolderTrackCompare;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.functions.Func2;

import static org.opensilk.music.library.provider.Uris.*;

/**
 * Created by drew on 4/26/15.
 */
public class LibraryProvider extends ContentProvider {

    public static final String AUTHORITY_PFX = "orpheus.library.";
    public static final String AUTHORITY;

    static {
        try {
            Class<?> clz = Class.forName("org.opensilk.music.library.Authority");
            Field f = clz.getDeclaredField("RESPECT_MY_ATHORITAH");

        }
        AUTHORITY = AUTHORITY_PFX + BuildConfig.RESPECT_MY_AUTHORITAH;
    }

    @Override
    public boolean onCreate() {

        return true;
    }

    @Override
    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor c = null;
        switch (MATCHER.match(uri)) {
            case M_FOLDERS:
                c = getFoldersTracksInternal(null, projection, selection, selectionArgs, sortOrder);
                break;
            case M_FOLDER:
                c = getFoldersTracksInternal(uri.getLastPathSegment(), projection, selection, selectionArgs, sortOrder);
                break;
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
        return null;
    }

    protected Cursor getFoldersTracksInternal(String identity, String[] projection, String selection, String[] selectionArgs, final String sortOrder) {
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
            }).collect(new MatrixCursor(FolderTrackProj.ALL), new Action2<MatrixCursor, Bundleable>() {
                @Override
                public void call(MatrixCursor matrixCursor, Bundleable bundleable) {
                    MatrixCursor.RowBuilder rb = matrixCursor.newRow();
                    rb.add(bundleable.getIdentity());
                    rb.add(bundleable.getName());
                    if (bundleable instanceof Folder) {
                        rb.add(FolderTrackProj.KIND_FOLDER);
                        Folder folder = (Folder) bundleable;
                        rb.add(folder.parentIdentity);
                        rb.add(folder.childCount);
                        rb.add(folder.date);
                    } else {
                        rb.add(FolderTrackProj.KIND_TRACK);
                        Track track = (Track) bundleable;
                        rb.add(track.albumName);
                        rb.add(track.artistName);
                        rb.add(track.albumArtistName);
                        rb.add(track.albumIdentity);
                        rb.add(track.duration);
                        rb.add(track.dataUri);
                        rb.add(track.artworkUri);
                        rb.add(track.mimeType);
                    }
                }
            }).toBlocking().first();
        } catch (Exception e) {
            Log.w("LibraryProvider", e);
        }
        return c;
    }

    protected void getFoldersTracks(String identity, Subscriber<? super Bundleable> subscriber) {
        subscriber.onError(new Throwable(new UnsupportedOperationException()));
    }

    protected Cursor queryAlbumsInternal(String[] projection, String selection, String[] selectionArgs, final String sortOrder) {
        return null;
    }

    protected void queryAlbums(Subscriber<? super Album> subscriber) {

    }

    protected Cursor getAlbumInternal(String identity) {
        return null;
    }

    protected void getAlbum(String identity, Subscriber<? super Album> subcriber) {

    }

    protected Cursor queryArtistsInternal(String[] projection, String selection, String[] selectionArgs, final String sortOrder) {
        return null;
    }

    protected void queryArtists(Subscriber<? super Artist> subscriber) {

    }

    protected Cursor getArtistInternal(String identity) {
        return null;
    }

    protected void getArtist(String identity, Subscriber<? super Artist> subscriber) {

    }

    protected Cursor queryTracksInternal(String[] projection, String selection, String[] selectionArgs, final String sortOrder) {
        return null;
    }

    protected void queryTracks(Subscriber<? super Track> subscriber) {

    }

    protected Cursor getTrackInternal(String identity) {
        return null;
    }

    protected void getTrack(String identity, Subscriber<? super Track> subscriber) {

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
