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

import android.annotation.TargetApi;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.artwork.UtilsArt;
import org.opensilk.music.index.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.index.provider.Methods;
import org.opensilk.music.index.scanner.ScannerService;
import org.opensilk.music.library.client.BundleableObserver;
import org.opensilk.music.library.client.LibraryClient;
import org.opensilk.music.library.client.TypedBundleableLoader;
import org.opensilk.music.library.internal.DeleteSubscriber;
import org.opensilk.music.library.internal.IBundleableObserver;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import timber.log.Timber;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;

/**
 * Created by drew on 9/17/15.
 */
@Singleton
public class IndexClientImpl implements IndexClient {

    final Context appContext;
    final Uri callUri;
    final String indexAuthority;
    final String artworkAuthority;

    final ThreadLocal<ClientCompat> localClientCompat = new ThreadLocal<>();

    @Inject
    public IndexClientImpl(
            @ForApplication Context appContext,
            @Named("IndexProviderAuthority") String authority,
            @Named("artworkauthority") String artworkAuthority
    ) {
        this.appContext = appContext;
        this.callUri = IndexUris.call(authority);
        this.indexAuthority = authority;
        this.artworkAuthority = artworkAuthority;
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
    public void rescan() {
        Intent intent = new Intent(appContext, ScannerService.class)
                .setAction(ScannerService.ACTION_RESCAN);
        appContext.startService(intent);
    }

    @Override
    public void rescan(List<Model> models) {
        for (Model b : models) {
            if (b instanceof Container) {
                appContext.startService(new Intent(appContext, ScannerService.class)
                        .setAction(ScannerService.ACTION_RESCAN)
                        .putExtra(ScannerService.EXTRA_LIBRARY_EXTRAS,
                                LibraryExtras.b().putBundleable(b).get()));
            }
        }
    }

    @Override
    public boolean deleteItems(List<Model> items, final Uri notifyUri) {
        if (items.isEmpty()) {
            return true;
        }
        //items will all be from same authority
        final List<Uri> uris = new ArrayList<>(items.size());
        for (Model item: items) {
            uris.add(item.getUri());
        }
        Subscription s = Observable.using(
                new Func0<LibraryClient>() {
                    @Override
                    public LibraryClient call() {
                        return LibraryClient.create(appContext, uris.get(0));
                    }
                },
                new Func1<LibraryClient, Observable<List<Uri>>>() {
                    @Override
                    public Observable<List<Uri>> call(final LibraryClient libraryClient) {
                        return Observable.create(new Observable.OnSubscribe<List<Uri>>() {
                            @Override
                            public void call(final Subscriber<? super List<Uri>> subscriber) {
                                final ResultReceiver resultReceiver = new ResultReceiver(null) {
                                    @Override
                                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                                        switch (resultCode) {
                                            case DeleteSubscriber.RESULT:
                                                subscriber.onNext(LibraryExtras.getUriList(resultData));
                                                break;
                                            case DeleteSubscriber.COMPLETE:
                                                subscriber.onCompleted();
                                                break;
                                            case DeleteSubscriber.ERROR:
                                                subscriber.onError(LibraryExtras.getCause(resultData));
                                                break;
                                        }
                                    }
                                };
                                Bundle reply = libraryClient.makeCall(LibraryMethods.DELETE,
                                        LibraryExtras.b().putUriList(uris)
                                                .putResultReceiver(resultReceiver)
                                                .putNotifyUri(notifyUri)
                                                .get());
                                if (!LibraryExtras.getOk(reply)) {
                                    subscriber.onError(LibraryExtras.getCause(reply));
                                }
                            }
                        });
                    }
                },
                new Action1<LibraryClient>() {
                    @Override
                    public void call(LibraryClient libraryClient) {
                        libraryClient.release();
                    }
                },
                true//release eagerly
        ).last().subscribe(new Action1<List<Uri>>() {
            @Override
            public void call(List<Uri> uris) {
                rescan();//TODO handle better;
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                //todo handle;
            }
        });
        return true;
    }

    @Override
    public List<MediaDescriptionCompat> getAutoRoots() {
        Pair[] roots = new Pair[] {
                Pair.of(IndexUris.albums(indexAuthority), R.string.title_albums),
                Pair.of(IndexUris.artists(indexAuthority), R.string.title_artists),
                Pair.of(IndexUris.genres(indexAuthority), R.string.title_genres),
                Pair.of(IndexUris.playlists(indexAuthority), R.string.title_playlists)
        };
        List<MediaDescriptionCompat> list = new ArrayList<>(roots.length);
        for (Pair p : roots) {
            list.add(new MediaDescriptionCompat.Builder()
                    .setMediaId(p.getLeft().toString())
                    .setTitle(appContext.getString((int)p.getRight()))
                    .build());
        }
        return list;
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
    public boolean broadcastMeta() {
        Bundle repl = makeCall(Methods.GET_BROADCAST_META, null);
        if (checkCall(repl)) {
            return BundleHelper.getInt(repl) == 1;
        }
        return false;
    }

    @Override
    public void setBroadcastMeta(boolean broadcastMeta) {
        makeCheckedCall(Methods.SAVE_BROADCAST_META, BundleHelper.b().putInt(broadcastMeta ? 1 : 0).get());
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
                .retry(1)
                .flatMap(new Func1<List<Track>, Observable<Track>>() {
                    @Override
                    public Observable<Track> call(List<Track> tracks) {
                        return Observable.from(tracks);
                    }
                });
    }

    /**
     * Convert a list of uris into a list of mediadescriptions
     * Emitted list is not guaranteed (or event tries) to retain the order of the passed list
     */
    @Override
    @DebugLog
    public Observable<List<MediaDescriptionCompat>> getDescriptions(final List<Uri> queue) {
        //first we ask the index what it knows
        return Observable.create(new Observable.OnSubscribe<List<Track>>() {
            @Override
            public void call(final Subscriber<? super List<Track>> subscriber) {
                final IBundleableObserver o = new BundleableObserver<Track>(subscriber);
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
                //if the index had all of them we pass them through
                if (tracks.size() == queue.size()) {
                    return Observable.just(tracks);
                }
                //some of them werent found in the db
                List<Uri> unknowns = new ArrayList<Uri>(queue.size() - tracks.size());
                if (tracks.size() == 0) {
                    //all of them werent found
                    unknowns.addAll(queue);
                } else {
                    //filter out the found ones
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
                }
                //todo throw multiget into the mix
                Observable<Observable<List<Track>>> loaderCreator = Observable.from(unknowns)
                        .map(new Func1<Uri, Observable<List<Track>>>() {
                            @Override
                            public Observable<List<Track>> call(final Uri uri) {
                                //Use defer for lazy creation
                                return Observable.defer(new Func0<Observable<List<Track>>>() {
                                    @Override
                                    public Observable<List<Track>> call() {
                                        return TypedBundleableLoader.<Track>create(appContext)
                                                .setUri(uri).setMethod(LibraryMethods.GET)
                                                .createObservable()
                                                .retry(1);
                                    }
                                });
                            }
                        });
                //we make requests for all the remaining uris and
                //merge them into the same stream
                if (!tracks.isEmpty()) {
                    return Observable.just(tracks).mergeWith(Observable.mergeDelayError(loaderCreator, 10));
                } else {
                    return Observable.mergeDelayError(loaderCreator, 10);
                }
            }
            //we use collect here instead of map because the merge will
        }).collect(new Func0<List<MediaDescriptionCompat>>() {
            //we use collect here instead of map because the merge above
            //will emmit several lists and we only want one onNext call to
            //the subscriber
            @Override
            public List<MediaDescriptionCompat> call() {
                return new ArrayList<MediaDescriptionCompat>();
            }
        }, new Action2<List<MediaDescriptionCompat>, List<Track>>() {
            @Override
            public void call(List<MediaDescriptionCompat> mediaDescriptions, List<Track> tracks) {
                //convert tracks to descriptions on the fly then add them all to the final list
                for (Track track : tracks) {
                    Timber.d("track=%s", track.toString());
                    MediaDescriptionCompat.Builder description = new MediaDescriptionCompat.Builder()
                            .setTitle(track.getName())
                            .setSubtitle(track.getArtistName())
                            .setMediaId(track.getUri().toString());
                    ArtInfo artInfo = UtilsArt.makeBestfitArtInfo(track.getAlbumArtistName(),
                            track.getArtistName(), track.getAlbumName(),
                            track.getArtworkUri());
                    if (artInfo != ArtInfo.NULLINSTANCE) {
                        description.setIconUri(artInfo.asContentUri(artworkAuthority));
                    }
                    mediaDescriptions.add(description.build());
                }
            }
        });
    }

    @Override
    public MediaMetadataCompat convertToMediaMetadata(Track track) {
        Track t = track;
        Track.Res r = track.getResources().get(0);
        MediaMetadataCompat.Builder m = new MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_MEDIA_ID, track.getUri().toString())
                .putString(METADATA_KEY_TITLE, t.getName())
                .putString(METADATA_KEY_DISPLAY_TITLE, t.getName())
                .putString(METADATA_KEY_ARTIST, t.getArtistName())
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, t.getArtistName())
                        //.putString(METADATA_KEY_DISPLAY_DESCRIPTION, TODO)
                .putString(METADATA_KEY_ALBUM_ARTIST,
                        StringUtils.isEmpty(t.getAlbumArtistName())
                                ? t.getArtistName() : t.getAlbumArtistName())
                .putString(METADATA_KEY_ALBUM, t.getAlbumName())
                .putLong(METADATA_KEY_DURATION, r.getDuration());
        ArtInfo artInfo = UtilsArt.makeBestfitArtInfo(track.getAlbumArtistName(),
                track.getArtistName(), track.getAlbumName(), track.getArtworkUri());
        if (artInfo != ArtInfo.NULLINSTANCE) {
            m.putString(METADATA_KEY_ALBUM_ART_URI, artInfo.asContentUri(artworkAuthority).toString());
        }
        return m.build();
    }

    @Override
    public Uri createPlaylist(String name) {
        Bundle reply = makeCall(Methods.CREATE_PLAYLIST, BundleHelper.b().putString(name).get());
        if (checkCall(reply)) {
            return BundleHelper.getUri(LibraryExtras.getExtrasBundle(reply));
        } else {
            return Uri.EMPTY;
        }
    }

    @Override
    @DebugLog
    public int addToPlaylist(Uri playlist, List<Uri> tracks) {
        Bundle reply = makeCall(Methods.ADD_TO_PLAYLIST, BundleHelper.b()
                .putUri(playlist).putList(tracks).get());
        if (checkCall(reply)) {
            return BundleHelper.getInt(LibraryExtras.getExtrasBundle(reply));
        } else {
            return 0;
        }
    }

    @Override
    public int movePlaylistEntry(Uri playlist, int from, int to) {
        Bundle reply = makeCall(Methods.MOVE_PLAYLIST_MEMBER, BundleHelper.b()
                .putUri(playlist).putInt(from).putInt2(to).get());
        if (checkCall(reply)) {
            return BundleHelper.getInt(LibraryExtras.getExtrasBundle(reply));
        } else {
            return 0;
        }
    }

    @Override
    public int removeFromPlaylist(Uri playlist, int position) {
        Bundle reply = makeCall(Methods.REMOVE_FROM_PLAYLIST, BundleHelper.b()
                .putUri(playlist).putInt(position).get());
        if (checkCall(reply)) {
            return BundleHelper.getInt(LibraryExtras.getExtrasBundle(reply));
        } else {
            return 0;
        }
    }

    @Override
    public int updatePlaylist(Uri playlist, List<Uri> uris) {
        Bundle reply = makeCall(Methods.UPDATE_PLAYLIST, BundleHelper.b()
                .putUri(playlist).putList(uris).get());
        if (checkCall(reply)) {
            return BundleHelper.getInt(LibraryExtras.getExtrasBundle(reply));
        } else {
            return 0;
        }
    }

    @Override
    public boolean removePlaylists(List<Uri> playlists) {
        return makeCheckedCall(Methods.REMOVE_PLAYLISTS, BundleHelper.b().putList(playlists).get());
    }

    @Override
    public Playlist getPlaylist(Uri playlist) {
        Bundle reply = makeCall(Methods.GET_PLAYLIST, BundleHelper.b().putUri(playlist).get());
        if (checkCall(reply)) {
            return LibraryExtras.getBundleable(reply);
        } else {
            return null;
        }
    }

    @Override
    public void startBatch() {
        ClientCompat client = localClientCompat.get();
        if (client == null) {
            client = makeClient();
            client.setBatch();
            localClientCompat.set(client);
        }
    }

    @Override
    public void endBatch() {
        ClientCompat client = localClientCompat.get();
        if (client != null) {
            client.release();
            localClientCompat.set(null);
        }
    }

    private ClientCompat getClient() {
        ClientCompat client = localClientCompat.get();
        if (client == null) {
            client = makeClient();
        }
        return client;
    }

    private ClientCompat makeClient() {
        if (VersionUtils.hasJellyBeanMR1()) {
            return new ClientCompatJBMR1(appContext, callUri);
        } else {
            return new ClientCompatBase(appContext, callUri);
        }
    }

    private boolean makeCheckedCall(String method, Bundle args) {
        return checkCall(makeCall(method, args));
    }

    private Bundle makeCall(String method, Bundle args) {
        ClientCompat client = getClient();
        try {
            return client.call(method, args);
        } catch (RemoteException e) {
            endBatch();
            return LibraryExtras.b().putOk(false).get();
        } finally {
            if (!client.isBatch()) {
                client.release();
            }
        }
    }

    private static boolean checkCall(Bundle result) {
        if (result == null) {
            Timber.e("Got null reply from index provider");
            return false;
        }
        return LibraryExtras.getOk(result);
    }

    interface ClientCompat {
        Bundle call(String method, Bundle args) throws RemoteException;
        void release();
        boolean isBatch();
        void setBatch();
    }

    static class ClientCompatBase implements ClientCompat {
        final ContentResolver contentResolver;
        final Uri callUri;
        boolean batch = false;

        public ClientCompatBase(Context context, Uri callUri) {
            this.contentResolver = context.getContentResolver();
            this.callUri = callUri;
        }

        @Override
        public Bundle call(String method, Bundle args) throws RemoteException {
            return contentResolver.call(callUri, method, null, args);
        }

        @Override
        public void release() {
            //noop
        }

        @Override
        public boolean isBatch() {
            return batch;
        }

        @Override
        public void setBatch() {
            batch = true;
        }
    }

    @TargetApi(17)
    static class ClientCompatJBMR1 implements ClientCompat {
        final ContentProviderClient client;
        boolean batch = false;

        public ClientCompatJBMR1(Context context, Uri callUri) {
            this.client = context.getContentResolver()
                    .acquireUnstableContentProviderClient(callUri);
        }

        @Override
        public Bundle call(String method, Bundle args) throws RemoteException {
            return client.call(method, null, args);
        }

        @Override
        public void release() {
            try {
                client.release();
            } catch (IllegalStateException e) {
                Timber.e("Client released twice!");
            } catch (Exception ignored) {}
        }

        @Override
        public boolean isBatch() {
            return batch;
        }

        @Override
        public void setBatch() {
            batch = true;
        }
    }
}
