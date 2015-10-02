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

package org.opensilk.music.index.provider;

import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.index.scanner.ScannerService;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.internal.BundleableSubscriber;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Track;
import org.opensilk.bundleable.Bundleable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import static org.opensilk.music.index.provider.IndexUris.M_ALBUMS;
import static org.opensilk.music.index.provider.IndexUris.M_ALBUM_TRACKS;
import static org.opensilk.music.index.provider.IndexUris.M_ARTISTS;
import static org.opensilk.music.index.provider.IndexUris.M_GENRES;
import static org.opensilk.music.index.provider.IndexUris.M_TRACKS;
import static org.opensilk.music.index.provider.IndexUris.makeMatcher;

/**
 * Created by drew on 7/11/15.
 */
public class IndexProvider extends LibraryProvider {

    @Inject @Named("IndexProviderAuthority") String mAuthority;
    @Inject IndexDatabase mDataBase;

    private UriMatcher mUriMatcher;

    @Override
    @DebugLog
    public boolean onCreate() {
        final IndexComponent acc = DaggerService.getDaggerComponent(getContext());
        IndexProviderComponent.FACTORY.call(acc).inject(this);
        super.onCreate();
        mUriMatcher = makeMatcher(mAuthority);
        return true;
    }

    @Override
    protected LibraryConfig getLibraryConfig() {
        return LibraryConfig.builder()
                .setAuthority(mAuthority)
                .setLabel("index") //Not used but can't be null.
                .build();
    }

    @Override
    protected String getAuthority() {
        return mAuthority;
    }

    @Override
    protected Bundle callCustom(String method, String arg, Bundle extras) {
        LibraryExtras.Builder reply = LibraryExtras.b();
        switch (method) {
            case Methods.IS_INDEXED: {
                Container c = LibraryExtras.getBundleable(extras);
                long id = mDataBase.hasContainer(c.getUri());
                return reply.putOk((id > 0)).get();
            }
            case Methods.ADD: {
                Intent i = new Intent(getContext(), ScannerService.class)
                        .putExtra(ScannerService.EXTRA_LIBRARY_EXTRAS, extras);
                getContext().startService(i);
                return reply.putOk(true).get();
            }
            case Methods.REMOVE: {
                Container c = LibraryExtras.getBundleable(extras);
                int numremoved = mDataBase.removeContainer(c.getUri());
                return reply.putOk(numremoved > 0).get();
            }
            case Methods.LAST_QUEUE_LIST: {
                final List<Uri> queue = mDataBase.getLastQueue();
                if (queue.isEmpty()) {
                    return reply.putOk(false).get();
                } else {
                    return BundleHelper.from(reply.putOk(true).get())
                            .putList(queue).get();
                }
            }
            case Methods.LAST_QUEUE_POSITION: {
                final int pos = mDataBase.getLastQueuePosition();
                if (pos < 0) {
                    return reply.putOk(false).get();
                } else {
                    return BundleHelper.from(reply.putOk(true).get())
                            .putInt(pos).get();
                }
            }
            case Methods.LAST_QUEUE_REPEAT: {
                final int rep = mDataBase.getLastQueueRepeatMode();
                if (rep < 0) {
                    return reply.putOk(false).get();
                } else {
                    return BundleHelper.from(reply.putOk(true).get())
                            .putInt(rep).get();
                }
            }
            case Methods.LAST_QUEUE_SHUFFLE: {
                final int shuf = mDataBase.getLastQueueShuffleMode();
                if (shuf < 0) {
                    return reply.putOk(false).get();
                } else {
                    return BundleHelper.from(reply.putOk(true).get())
                            .putInt(shuf).get();
                }
            }
            case Methods.SAVE_QUEUE_LIST: {
                mDataBase.saveQueue(BundleHelper.<Uri>getList(extras));
                return reply.putOk(true).get();
            }
            case Methods.SAVE_QUEUE_POSITION: {
                mDataBase.saveQueuePosition(BundleHelper.getInt(extras));
                return reply.putOk(true).get();
            }
            case Methods.SAVE_QUEUE_REPEAT: {
                mDataBase.saveQueueRepeatMode(BundleHelper.getInt(extras));
                return reply.putOk(true).get();
            }
            case Methods.SAVE_QUEUE_SHUFFLE: {
                mDataBase.saveQueueShuffleMode(BundleHelper.getInt(extras));
                return reply.putOk(true).get();
            }
            case Methods.LAST_SEEK_POSITION: {
                final long pos = mDataBase.getLastSeekPosition();
                if (pos < 0) {
                    return reply.putOk(false).get();
                } else {
                    return BundleHelper.from(reply.putOk(true).get())
                            .putLong(pos).get();
                }
            }
            case Methods.SAVE_SEEK_POSITION: {
                mDataBase.saveLastSeekPosition(BundleHelper.getLong(extras));
                return reply.putOk(true).get();
            }
            case Methods.MEDIA_DESCRIPTIONS: {
                return null;
            }
            case Methods.GET_TRACK: {
                Track track = mDataBase.getTrack(BundleHelper.getUri(extras));
                if (track == null) {
                    return reply.putOk(false).get();
                } else {
                    return reply.putOk(true).putBundleable(track).get();
                }
            }
            case Methods.GET_TRACK_LIST: {
                IBinder binder = LibraryExtras.getBundleableObserverBinder(extras);
                if (!binder.isBinderAlive()) {
                    return reply.putOk(false).get();
                }
                getTracksInList(LibraryExtras.getUriList(extras),
                        binder, LibraryExtras.sanitize(extras));
                return reply.putOk(true).get();
            }
            default: {
                return super.callCustom(method, arg, extras);
            }
        }
    }

    @Override
    protected void listObjsInternal(Uri uri, final IBinder binder, Bundle args) {
        switch (mUriMatcher.match(uri)) {
            case M_ALBUMS: {
                final BundleableSubscriber<Album> subscriber = new BundleableSubscriber<>(binder);
                final List<Album> lst = mDataBase.getAlbums(LibraryExtras.getSortOrder(args));
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(lst);
                    subscriber.onCompleted();
                }
                break;
            }
            case M_ALBUM_TRACKS: {
                final BundleableSubscriber<Track> subscriber = new BundleableSubscriber<>(binder);
                List<String> segments = uri.getPathSegments();
                String id = segments.get(segments.size() - 2);
                final List<Track> lst = mDataBase.getAlbumTracks(id, LibraryExtras.getSortOrder(args));
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(lst);
                    subscriber.onCompleted();
                }
                break;
            }
            case M_ARTISTS: {
                final BundleableSubscriber<Artist> subscriber = new BundleableSubscriber<>(binder);
                final List<Artist> lst = mDataBase.getArtists(LibraryExtras.getSortOrder(args));
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(lst);
                    subscriber.onCompleted();
                }
                break;
            }
            case M_TRACKS: {
                final BundleableSubscriber<Track> subscriber = new BundleableSubscriber<>(binder);
                final List<Track> lst = mDataBase.getTracks(LibraryExtras.getSortOrder(args), false);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(lst);
                    subscriber.onCompleted();
                }
                break;
            }
            case M_GENRES: {
                final BundleableSubscriber<Genre> subscriber = new BundleableSubscriber<>(binder);
                final List<Genre> lst = mDataBase.getGenres(LibraryExtras.getSortOrder(args));
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(lst);
                    subscriber.onCompleted();
                }
                break;
            }
            default: {
                final BundleableSubscriber<Bundleable> subscriber = new BundleableSubscriber<>(binder);
                subscriber.onError(new LibraryException(LibraryException.Kind.ILLEGAL_URI,
                        new IllegalArgumentException("Unknown uri: " + uri.toString())));
            }
        }
    }

    @Override
    protected void getObjInternal(Uri uri, IBinder binder, Bundle args) {
        switch (mUriMatcher.match(uri)) {
//            case M_ALBUM: {
//                final BundleableSubscriber<Album> subscriber = new BundleableSubscriber<>(binder);
//                final List<Album> lst = mDataBase.getAlbums(LibraryExtras.getSortOrder(args));
//                if (!subscriber.isUnsubscribed()) {
//                    subscriber.onNext(lst);
//                    subscriber.onCompleted();
//                }
//                break;
//            }
//            case M_ARTIST: {
//                final BundleableSubscriber<Artist> subscriber = new BundleableSubscriber<>(binder);
//                final List<Artist> lst = mDataBase.getArtists(LibraryExtras.getSortOrder(args));
//                if (!subscriber.isUnsubscribed()) {
//                    subscriber.onNext(lst);
//                    subscriber.onCompleted();
//                }
//                break;
//            }
//            case M_GENRE: {
//                final BundleableSubscriber<Genre> subscriber = new BundleableSubscriber<>(binder);
//                final List<Genre> lst = mDataBase.getGenres(LibraryExtras.getSortOrder(args));
//                if (!subscriber.isUnsubscribed()) {
//                    subscriber.onNext(lst);
//                    subscriber.onCompleted();
//                }
//                break;
//            }
            default: {
                final BundleableSubscriber<Bundleable> subscriber = new BundleableSubscriber<>(binder);
                subscriber.onError(new LibraryException(LibraryException.Kind.ILLEGAL_URI,
                        new IllegalArgumentException("Unknown uri: " + uri.toString())));
            }
        }
    }

    void getTracksInList(final List<Uri> uris, IBinder binder, Bundle args) {
        final BundleableSubscriber<Track> subscriber = new BundleableSubscriber<>(binder);
        Observable.create(new Observable.OnSubscribe<List<Track>>() {
            @Override
            public void call(Subscriber<? super List<Track>> subscriber) {
                List<Track> tracks = mDataBase.getTracksInList(uris);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(tracks);
                    subscriber.onCompleted();
                }
            }
        }).subscribeOn(Schedulers.computation()).subscribe(subscriber);
    }
}
