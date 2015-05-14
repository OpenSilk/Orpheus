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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.library.compare.AlbumCompare;
import org.opensilk.music.library.compare.ArtistCompare;
import org.opensilk.music.library.compare.BundleableCompare;
import org.opensilk.music.library.compare.FolderTrackCompare;
import org.opensilk.music.library.compare.TrackCompare;
import org.opensilk.music.library.internal.BundleableListTransformer;
import org.opensilk.music.library.internal.BundleableSubscriber;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.sort.BundleableSortOrder;
import org.opensilk.music.library.provider.LibraryMethods.Extras;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;

import java.lang.reflect.Method;
import java.util.List;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.opensilk.music.library.provider.LibraryUris.*;
import static org.opensilk.music.library.internal.LibraryException.Kind.*;

/**
 * Created by drew on 4/26/15.
 */
public abstract class LibraryProvider extends ContentProvider {
    public static final String TAG = LibraryProvider.class.getSimpleName();

    /**
     * Authority prefix all libraries must start with to be discoverable by orpheus
     */
    public static final String AUTHORITY_PFX = "orpheus.library.";

    /**
     * Our full authority
     */
    protected String mAuthority;

    private UriMatcher mMatcher;
    private Scheduler scheduler = Schedulers.computation();

    @Override
    public boolean onCreate() {
        mAuthority = AUTHORITY_PFX + getBaseAuthority();
        mMatcher = LibraryUris.makeMatcher(mAuthority);
        return true;
    }

    /**
     * Base authority for library appended to {@link #AUTHORITY_PFX}
     * default is package name, this is usually sufficient unless package contains
     * multiple libraries
     */
    protected String getBaseAuthority() {
        return getContext().getPackageName();
    }

    /**
     * @return This libraries config
     */
    protected abstract LibraryConfig getLibraryConfig();

    @Override
    public final Bundle call(String method, String arg, Bundle extras) {

        final LibraryExtras.Builder ok = LibraryExtras.b();
        ok.putOk(true);

        if (method == null) method = "";
        switch (method) {
            case LibraryMethods.QUERY: {
                extras.setClassLoader(getClass().getClassLoader());

                final IBinder binder = getBinderCallbackFromBundle(extras);
                if (binder == null || !binder.isBinderAlive()) {
                    //this is mostly for the null, if the binder is dead then
                    //sending them a reason is moot. but we check the binder here
                    //so we dont have to do it 50 times below and we can be pretty
                    //sure the linkToDeath will succeed
                    ok.putOk(false);
                    ok.putCause(new LibraryException(BAD_BINDER, null));
                    return ok.get();
                }

                final Uri uri = extras.getParcelable(Extras.URI);
                final List<String> pathSegments = uri.getPathSegments();
                if (pathSegments.size() < 3 || pathSegments.size() > 5) {
                    Log.e(TAG, "Wrong number of path segments: uri=" + uri);
                    ok.putOk(false);
                    ok.putCause(new LibraryException(ILLEGAL_URI,
                            new IllegalArgumentException(uri.toString())));
                    return ok.get();
                }

                final String library = pathSegments.get(0);
                final String identity;
                if (pathSegments.size() > 3) {
                    identity = pathSegments.get(3);
                } else {
                    identity = null;
                }

                String sortOrder = extras.getString(Extras.SORTORDER);
                final Bundle args = LibraryExtras.b()
                        .putUri(uri)
                        .putSortOrder(sortOrder != null ? sortOrder : BundleableSortOrder.A_Z)
                        .get();

                switch (mMatcher.match(uri)) {
                    case M_ALBUMS: {
                        final BundleableSubscriber<Album> subscriber = new BundleableSubscriber<>(binder);
                        queryAlbumsInternal(library, subscriber, args);
                        break;
                    }
                    case M_ALBUM: {
                        final BundleableSubscriber<Album> subscriber = new BundleableSubscriber<>(binder);
                        getAlbumInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_ALBUM_TRACKS: {
                        final BundleableSubscriber<Track> subscriber = new BundleableSubscriber<>(binder);
                        getAlbumTracksInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_ARTISTS: {
                        final BundleableSubscriber<Artist> subscriber = new BundleableSubscriber<>(binder);
                        queryArtistsInternal(library, subscriber, args);
                        break;
                    }
                    case M_ARTIST: {
                        final BundleableSubscriber<Artist> subscriber = new BundleableSubscriber<>(binder);
                        getArtistInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_ARTIST_ALBUMS: {
                        final BundleableSubscriber<Album> subscriber = new BundleableSubscriber<>(binder);
                        getArtistAlbumsInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_ARTIST_TRACKS: {
                        final BundleableSubscriber<Track> subscriber = new BundleableSubscriber<>(binder);
                        getArtistTracksInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_FOLDERS:
                    case M_FOLDER: {
                        final BundleableSubscriber<Bundleable> subscriber = new BundleableSubscriber<>(binder);
                        browseFoldersInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_GENRES: {
                        final BundleableSubscriber<Genre> subscriber = new BundleableSubscriber<>(binder);
                        queryGenresInternal(library, subscriber, args);
                        break;
                    }
                    case M_GENRE: {
                        final BundleableSubscriber<Genre> subscriber = new BundleableSubscriber<>(binder);
                        getGenreInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_GENRE_ALBUMS: {
                        final BundleableSubscriber<Album> subscriber = new BundleableSubscriber<>(binder);
                        getGenreAlbumsInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_GENRE_TRACKS: {
                        final BundleableSubscriber<Track> subscriber = new BundleableSubscriber<>(binder);
                        getGenreTracksInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_PLAYLISTS: {
                        final BundleableSubscriber<Playlist> subscriber = new BundleableSubscriber<>(binder);
                        queryPlaylistsInternal(library, subscriber, args);
                        break;
                    }
                    case M_PLAYLIST: {
                        final BundleableSubscriber<Playlist> subscriber = new BundleableSubscriber<>(binder);
                        getPlaylistInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_PLAYLIST_TRACKS: {
                        final BundleableSubscriber<Track> subscriber = new BundleableSubscriber<>(binder);
                        getPlaylistTracksInternal(library, identity, subscriber, args);
                        break;
                    }
                    case M_TRACKS: {
                        final BundleableSubscriber<Track> subscriber = new BundleableSubscriber<>(binder);
                        queryTracksInternal(library, subscriber, args);
                        break;
                    }
                    case M_TRACK: {
                        final BundleableSubscriber<Track> subscriber = new BundleableSubscriber<>(binder);
                        getTrackInternal(library, identity, subscriber, args);
                        break;
                    }
                    default: {
                        ok.putOk(false);
                        ok.putCause(new LibraryException(ILLEGAL_URI,
                                new IllegalArgumentException(uri.toString())));
                    }
                }
                return ok.get();
            }
            case LibraryMethods.LIBRARYCONF:
                return getLibraryConfig().dematerialize();
            default:
                Log.e(TAG, "Unknown method " + method);
                ok.putOk(false);
                ok.putCause(new LibraryException(METHOD_NOT_IMPLEMENTED, new UnsupportedOperationException(method)));
                return ok.get();
        }
    }

    //TODO cache all these observables

    /*
     * Start internal methods.
     * You can override these if you need specialized handling.
     *
     * You don't need to override these for caching, you can just send the cached list, then hit the network
     * and once it comes in send a notify on the Uri, Orpheus will requery and you can send the updated cached list.
     */

    @DebugLog
    protected void browseFoldersInternal(final String library, final String identity, final Subscriber<List<Bundleable>> subscriber, final Bundle args) {
        Observable<Bundleable> o = Observable.create(
                new Observable.OnSubscribe<Bundleable>() {
                    @Override
                    public void call(Subscriber<? super Bundleable> subscriber) {
                        browseFolders(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler);
        final String q = args.<Uri>getParcelable(Extras.URI).getQueryParameter(Q.Q);
        if (StringUtils.equals(q, Q.FOLDERS_ONLY)) {
            o = o.filter(new Func1<Bundleable, Boolean>() {
                @Override
                public Boolean call(Bundleable bundleable) {
                    return bundleable instanceof Folder;
                }
            });
        } else if (StringUtils.equals(q, Q.TRACKS_ONLY)) {
            o = o.filter(new Func1<Bundleable, Boolean>() {
                @Override
                public Boolean call(Bundleable bundleable) {
                    return bundleable instanceof Track;
                }
            });
        }
        o.compose(new BundleableListTransformer<Bundleable>(FolderTrackCompare.func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void queryAlbumsInternal(final String library, final Subscriber<List<Album>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Album>() {
                    @Override
                    public void call(Subscriber<? super Album> subscriber) {
                        queryAlbums(library, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Album>(AlbumCompare.func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void getAlbumInternal(final String library, final String identity, final Subscriber<List<Album>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Album>() {
                    @Override
                    public void call(Subscriber<? super Album> subscriber) {
                        getAlbum(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .first()
                .compose(new BundleableListTransformer<Album>(null))
                .subscribe(subscriber);
    }

    protected void getAlbumTracksInternal(final String library, final String identity, final Subscriber<List<Track>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Track>() {
                    @Override
                    public void call(Subscriber<? super Track> subscriber) {
                        getAlbumTracks(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Track>(TrackCompare.func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void queryArtistsInternal(final String library, final Subscriber<List<Artist>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Artist>() {
                    @Override
                    public void call(Subscriber<? super Artist> subscriber) {
                        queryArtists(library, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Artist>(ArtistCompare.func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void getArtistInternal(final String library, final String identity, final Subscriber<List<Artist>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Artist>() {
                    @Override
                    public void call(Subscriber<? super Artist> subscriber) {
                        getArtist(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .first()
                .compose(new BundleableListTransformer<Artist>(null))
                .subscribe(subscriber);
    }

    protected void getArtistAlbumsInternal(final String library, final String identity, final Subscriber<List<Album>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Album>() {
                    @Override
                    public void call(Subscriber<? super Album> subscriber) {
                        getArtistAlbums(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Album>(AlbumCompare.func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void getArtistTracksInternal(final String library, final String identity, final Subscriber<List<Track>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Track>() {
                    @Override
                    public void call(Subscriber<? super Track> subscriber) {
                        getArtistTracks(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Track>(TrackCompare.func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void queryGenresInternal(final String library, final Subscriber<List<Genre>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Genre>() {
                    @Override
                    public void call(Subscriber<? super Genre> subscriber) {
                        queryGenres(library, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Genre>(BundleableCompare.<Genre>func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void getGenreInternal(final String library, final String identity, final Subscriber<List<Genre>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Genre>() {
                    @Override
                    public void call(Subscriber<? super Genre> subscriber) {
                        getGenre(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .first()
                .compose(new BundleableListTransformer<Genre>(null))
                .subscribe(subscriber);
    }

    protected void getGenreAlbumsInternal(final String library, final String identity, final Subscriber<List<Album>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Album>() {
                    @Override
                    public void call(Subscriber<? super Album> subscriber) {
                        getGenreAlbums(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Album>(AlbumCompare.func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void getGenreTracksInternal(final String library, final String identity, final Subscriber<List<Track>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Track>() {
                    @Override
                    public void call(Subscriber<? super Track> subscriber) {
                        getGenreTracks(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Track>(TrackCompare.func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void queryPlaylistsInternal(final String library, final Subscriber<List<Playlist>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Playlist>() {
                    @Override
                    public void call(Subscriber<? super Playlist> subscriber) {
                        queryPlaylists(library, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Playlist>(BundleableCompare.<Playlist>func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void getPlaylistInternal(final String library, final String identity, final Subscriber<List<Playlist>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Playlist>() {
                    @Override
                    public void call(Subscriber<? super Playlist> subscriber) {
                        getPlaylist(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .first()
                .compose(new BundleableListTransformer<Playlist>(null))
                .subscribe(subscriber);
    }

    protected void getPlaylistTracksInternal(final String library, final String identity, final Subscriber<List<Track>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Track>() {
                    @Override
                    public void call(Subscriber<? super Track> subscriber) {
                        getPlaylistTracks(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Track>(null))//No sort
                .subscribe(subscriber);
    }

    protected void queryTracksInternal(final String library, final Subscriber<List<Track>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Track>() {
                    @Override
                    public void call(Subscriber<? super Track> subscriber) {
                        queryTracks(library, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .compose(new BundleableListTransformer<Track>(TrackCompare.func(LibraryExtras.getSortOrder(args))))
                .subscribe(subscriber);
    }

    protected void getTrackInternal(final String library, final String identity, final Subscriber<List<Track>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Track>() {
                    @Override
                    public void call(Subscriber<? super Track> subscriber) {
                        getTrack(library, identity, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .first()
                .compose(new BundleableListTransformer<Track>(null))
                .subscribe(subscriber);
    }

    /*
     * Start query stubs
     *
     * Primary handlers for library, You must override all methods corresponding to the abilitys
     * you declare in your config
     *
     * You must call onComplete after emitting the list
     */

    protected void browseFolders(String library, String identity, Subscriber<? super Bundleable> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void queryAlbums(String library, Subscriber<? super Album> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getAlbum(String library, String identity, Subscriber<? super Album> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getAlbumTracks(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void queryArtists(String library, Subscriber<? super Artist> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getArtist(String library, String identity, Subscriber<? super Artist> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getArtistAlbums(String library, String identity, Subscriber<? super Album> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getArtistTracks(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void queryGenres(String library, Subscriber<? super Genre> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getGenre(String library, String identity, Subscriber<? super Genre> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getGenreAlbums(String library, String identity, Subscriber<? super Album> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getGenreTracks(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void queryPlaylists(String library, Subscriber<? super Playlist> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getPlaylist(String library, String identity, Subscriber<? super Playlist> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getPlaylistTracks(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void queryTracks(String library, Subscriber<? super Track> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getTrack(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    /*
     * End query stubs
     */

    /*
     * Start abstract methods, we are 100% out-of-band and do not support any of these
     */

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    /*
     * End abstract methods
     */

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

    private Method _getIBinder = null;
    private IBinder getBinderCallbackFromBundle(Bundle b) {
        if (Build.VERSION.SDK_INT >= 18) {
            return b.getBinder(Extras.CALLBACK);
        } else {
            try {
                synchronized (this) {
                    if (_getIBinder == null) {
                        _getIBinder = Bundle.class.getDeclaredMethod("getIBinder", String.class);
                    }
                }
                return (IBinder) _getIBinder.invoke(b, Extras.CALLBACK);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
