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

package org.opensilk.music.index.client;

import android.content.ContentProviderClient;
import android.content.Context;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.index.provider.Methods;
import org.opensilk.music.library.internal.BundleableListSlice;
import org.opensilk.music.library.internal.IBundleableObserver;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.loader.TypedBundleableLoader;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import timber.log.Timber;

import static android.media.MediaMetadata.METADATA_KEY_ALBUM;
import static android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST;
import static android.media.MediaMetadata.METADATA_KEY_ARTIST;
import static android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE;
import static android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE;
import static android.media.MediaMetadata.METADATA_KEY_DURATION;
import static android.media.MediaMetadata.METADATA_KEY_TITLE;

/**
 * Created by drew on 9/17/15.
 */
@Singleton
public class IndexClientImpl implements IndexClient {

    final Context appContext;
    final Uri callUri;
    ContentProviderClient client;

    @Inject
    public IndexClientImpl(
            @ForApplication Context appContext,
            @Named("IndexProviderAuthority") String authority
    ) {
        this.appContext = appContext;
        callUri = IndexUris.call(authority);
    }

    @Override
    public boolean isIndexed(Container container) {
        Bundle args = LibraryExtras.b().putBundleable(container).get();
        return makeCheckedCall(Methods.IS_INDEXED, args);
    }

    @Override
    public boolean add(Container container) {
        Bundle args = LibraryExtras.b().putBundleable(container).get();
        return makeCheckedCall(Methods.ADD, args);
    }

    @Override
    public boolean remove(Container container) {
        Bundle args = LibraryExtras.b().putBundleable(container).get();
        return makeCheckedCall(Methods.REMOVE, args);
    }

    @Override
    public MediaBrowserService.BrowserRoot browserGetRoot(@NonNull String clientPackageName,
                                                          int clientUid, Bundle rootHints) {
        return new MediaBrowserService.BrowserRoot("__ROOT__", null);
    }

    @Override
    public void browserLoadChildren(@NonNull String parentId,
                                    @NonNull MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result) {

    }

    @Override
    public List<Uri> getLastQueue() {
        Bundle repl = makeCall(Methods.LAST_QUEUE_LIST, null);
        if (checkCall(repl)) {
            return BundleHelper.<Uri>getList(repl);
        }
        return Collections.emptyList();
    }

    @Override
    public int getLastQueuePosition() {
        Bundle repl = makeCall(Methods.LAST_QUEUE_POSITION, null);
        if (checkCall(repl)) {
            return BundleHelper.getInt(repl);
        }
        return -1;
    }

    @Override
    public int getLastQueueShuffleMode() {
        Bundle repl = makeCall(Methods.LAST_QUEUE_SHUFFLE, null);
        if (checkCall(repl)) {
            return BundleHelper.getInt(repl);
        }
        return -1;
    }

    @Override
    public int getLastQueueRepeatMode() {
        Bundle repl = makeCall(Methods.LAST_QUEUE_REPEAT, null);
        if (checkCall(repl)) {
            return BundleHelper.getInt(repl);
        }
        return -1;
    }

    @Override
    public void saveQueue(List<Uri> queue) {
        makeCheckedCall(Methods.SAVE_QUEUE_LIST, BundleHelper.b().putList(queue).get());
    }

    @Override
    public void saveQueuePosition(int pos) {
        makeCheckedCall(Methods.SAVE_QUEUE_POSITION, BundleHelper.b().putInt(pos).get());
    }

    @Override
    public void saveQueueShuffleMode(int mode) {
        makeCheckedCall(Methods.SAVE_QUEUE_SHUFFLE, BundleHelper.b().putInt(mode).get());
    }

    @Override
    public void saveQueueRepeatMode(int mode) {
        makeCheckedCall(Methods.SAVE_QUEUE_REPEAT, BundleHelper.b().putInt(mode).get());
    }

    @Override
    public long getLastSeekPosition() {
        Bundle repl = makeCall(Methods.LAST_SEEK_POSITION, null);
        if (checkCall(repl)) {
            return BundleHelper.getLong(repl);
        }
        return -1;
    }

    @Override
    public void saveLastSeekPosition(long pos) {
        makeCheckedCall(Methods.SAVE_SEEK_POSITION, BundleHelper.b().putLong(pos).get());
    }

    @Override
    public Observable<Track> getTrack(Uri uri) {
        Bundle repl = makeCall(Methods.GET_TRACK, BundleHelper.b().putUri(uri).get());
        if (checkCall(repl)) {
            return Observable.just(LibraryExtras.<Track>getBundleable(repl));
        }
        return TypedBundleableLoader.<Track>create(appContext)
                .setUri(uri)
                .setMethod(LibraryMethods.GET)
                .createObservable()
                .flatMap(new Func1<List<Track>, Observable<Track>>() {
                    @Override
                    public Observable<Track> call(List<Track> tracks) {
                        return Observable.from(tracks);
                    }
                });
    }

    @Override
    public Observable<List<Track>> getTracks(Uri uri, String sortOrder) {
        return Observable.empty();
    }

    @Override
    public Observable<List<Uri>> getTrackUris(Uri uri, String sordOrder) {
        return Observable.empty();
    }

    @Override
    public Observable<List<MediaDescription>> getDescriptions(final List<Uri> queue) {
        return Observable.create(new Observable.OnSubscribe<List<Track>>() {
            @Override
            public void call(final Subscriber<? super List<Track>> subscriber) {
                final IBundleableObserver o = new IBundleableObserver.Stub() {
                    @Override public void onNext(BundleableListSlice slice) throws RemoteException {
                        List<Track> list = new ArrayList<>(slice.getList());
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(list);
                        }
                    }
                    @Override public void onError(LibraryException e) throws RemoteException {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(e);
                        }
                    }
                    @Override public void onCompleted() throws RemoteException {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                        }
                    }
                };

                final Bundle extras = LibraryExtras.b()
                        .putUriList(queue)
                        .putBundleableObserverCallback(o)
                        .get();

                Bundle repl = makeCall(Methods.GET_TRACK_LIST, extras);
                if (!checkCall(repl) && !subscriber.isUnsubscribed()) {
                    subscriber.onError(new Exception("Call failed"));
                }
            }
        }).flatMap(new Func1<List<Track>, Observable<List<Track>>>() {
            @Override
            public Observable<List<Track>> call(List<Track> tracks) {
                if (tracks.size() == queue.size()) {
                    return Observable.just(tracks);
                }
                //some of them werent found in the db
                List<Uri> unknowns = new ArrayList<Uri>(queue.size() - tracks.size());
                for (Uri uri : queue) {
                    boolean found = false;
                    for (Track track : tracks) {
                        if (track.getUri().equals(uri)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        unknowns.add(uri);
                    }
                }
                //todo throw multiget into the mix
                //xxx this is *probably* very very bad
                List<Observable<List<Track>>> loaders = new ArrayList<>(unknowns.size());
                for (Uri uri : unknowns) {
                    loaders.add(TypedBundleableLoader.<Track>create(appContext)
                            .setUri(uri).setMethod(LibraryMethods.GET)
                            .createObservable());
                }
                return Observable.merge(loaders);
            }
        }).map(new Func1<List<Track>, List<MediaDescription>>() {
            @Override
            public List<MediaDescription> call(List<Track> tracks) {
                List<MediaDescription> descs = new ArrayList<MediaDescription>(tracks.size());
                for (Track track : tracks) {
                    MediaDescription description = new MediaDescription.Builder()
                            .setTitle(track.getName())
                            .setSubtitle(track.getArtistName())
                            .setMediaId(track.getUri().toString())
                            //.setIconUri()TODO
                            .build();
                    descs.add(description);
                }
                return descs;
            }
        });
    }

    @Override
    public MediaMetadata convertToMediaMetadata(Track track) {
        Track t = track;
        Track.Res r = track.getResources().get(0);
        MediaMetadata m = new MediaMetadata.Builder()
                .putString(METADATA_KEY_TITLE, t.getName())
                .putString(METADATA_KEY_DISPLAY_TITLE, t.getName())
                .putString(METADATA_KEY_ARTIST, t.getArtistName())
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, t.getArtistName())
                        //.putString(METADATA_KEY_DISPLAY_DESCRIPTION, TODO)
                .putString(METADATA_KEY_ALBUM_ARTIST,
                        StringUtils.isEmpty(t.getAlbumArtistName()) ? t.getArtistName() : t.getAlbumArtistName())
                .putString(METADATA_KEY_ALBUM, t.getAlbumName())
                .putLong(METADATA_KEY_DURATION, r.getDuration())
//                .putBitmap(METADATA_KEY_ALBUM_ART, b)
                        //.putString(METADATA_KEY_ALBUM_ART_URI, TODO)
//                .putString(METADATA_KEY_MEDIA_ID, trackUri.toString())
                        //Dispaly uri is prefered over arturi, we only set arturi for internal
                        //purposes and cant use a custom key cause of the conversion compat does
                        //strips away custom keys, so we set a display uri to avoid anyone
                        //using the art uri. even though we also set a bitmap
//                .putString(METADATA_KEY_DISPLAY_ICON_URI, artUri.toString())
//                .putString(METADATA_KEY_ART_URI, //used by now playing
//                        t.getArtworkUri() != null ? t.getArtworkUri().toString() : null)
                .build();
        return m;
    }

    private boolean makeCheckedCall(String method, Bundle args) {
        return checkCall(appContext.getContentResolver().call(callUri, method, null, args));
    }

    private Bundle makeCall(String method, Bundle args) {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    //XXX we are using an unstable provider since we never actually release it
                    // but we *need* this cached as it greatly improves performance
                    client = appContext.getContentResolver().acquireUnstableContentProviderClient(callUri);
                }
            }
        }
        try {
            return client.call(method, null, args);
        } catch (RemoteException e) {
            synchronized (this) {
                client.release();
                client = null;
            }
            return makeCall(method, args);
        }
    }

    private static boolean checkCall(Bundle result) {
        if (result == null) {
            Timber.e("Got null reply from index provider");
            return false;
        }
        return LibraryExtras.getOk(result);
    }
}
