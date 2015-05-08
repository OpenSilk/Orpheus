/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.artwork.fetcher;

import android.content.Context;
import android.content.UriMatcher;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.artwork.shared.ArtworkPreferences;
import org.opensilk.music.artwork.ArtworkRequest2;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.artwork.Constants;
import org.opensilk.music.artwork.CoverArtJsonRequest;
import org.opensilk.music.artwork.CrumbTrail;
import org.opensilk.music.artwork.RequestKey;
import org.opensilk.music.artwork.UtilsArt;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.model.ArtInfo;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.MusicEntry;
import de.umass.lastfm.opensilk.Fetch;
import de.umass.lastfm.opensilk.MusicEntryResponseCallback;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by drew on 10/21/14.
 */
@ArtworkFetcherScope
public class ArtworkFetcherManager {
    final static boolean DROP_CRUMBS = true;

    final Context mContext;
    final ArtworkPreferences mPreferences;
    final BitmapDiskCache mL2Cache;
    final RequestQueue mVolleyQueue;
    final Gson mGson;
    final ConnectivityManager mConnectivityManager;

    final UriMatcher mUriMatcher;
    /*
     * Scheduler our requests are observed on.
     * Normally volley dispatches results on the main thread but we are
     * using a custom dispatcher to receive them on a worker thread since we write
     * to disk. This also means our final onComplete will be called on that worker thread
     * which we dont want, so we use this to push the onComplete call over to the same
     * thread that created the request so we dont need to worry about synchronization
     */
    final Scheduler oScheduler;

    final Scheduler scheduler = Constants.ARTWORK_SCHEDULER;
    final Map<RequestKey, Uri> mActiveRequests = new LinkedHashMap<>();

    @Inject
    public ArtworkFetcherManager(@ForApplication Context mContext,
                                 ArtworkPreferences mPreferences,
                                 BitmapDiskCache mL2Cache,
                                 RequestQueue mVolleyQueue,
                                 Gson mGson,
                                 ConnectivityManager mConnectivityManager,
                                 UriMatcher mUriMatcher,
                                 @Named("oScheduler") Scheduler oScheduler
    ) {
        this.mContext = mContext;
        this.mPreferences = mPreferences;
        this.mL2Cache = mL2Cache;
        this.mVolleyQueue = mVolleyQueue;
        this.mGson = mGson;
        this.mConnectivityManager = mConnectivityManager;
        this.mUriMatcher = mUriMatcher;
        this.oScheduler = oScheduler;
    }

    interface CompletionListener {
        void onComplete();
    }

    interface IFetcherTask extends Subscription {
        void start();
    }

    abstract class BaseArtworkRequest implements IFetcherTask {
        final RequestKey key;
        final ArtInfo artInfo;
        final ArtworkType artworkType;
        final CompletionListener listener;

        Subscription subscription;
        boolean unsubscribed = false;
        boolean complete = false;

        BaseArtworkRequest(RequestKey key, CompletionListener listener) {
            this.key = key;
            this.artInfo = key.artInfo;
            this.artworkType = key.artworkType;
            this.listener = listener;
            addBreadcrumb("Fetcher: %s", UtilsArt.getCacheKey(artInfo, artworkType));
        }

        @Override
        public void unsubscribe() {
            addBreadcrumb("unsubscribe");
            unsubscribed = true;
            if (subscription != null) {
                subscription.unsubscribe();
                subscription = null;
            }
            if (artInfo != null) {
                mVolleyQueue.cancelAll(artInfo);
            }
            onComplete();
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }

        @Override
        public void start() {
            addBreadcrumb("start");
            if (validateArtInfo()) {
                onStart();
            } else {
                onComplete();
            }
        }

        void onComplete() {
            addBreadcrumb("complete");
            complete = true;
            mActiveRequests.remove(key);
            listener.onComplete();
            printTrail();
        }

        void onResponse() {
            addBreadcrumb("onResponse");
            if (unsubscribed) return;
            Uri uri = mActiveRequests.get(key);
            if (uri != null) {
                mContext.getContentResolver().notifyChange(uri, null);
            }
        }

        abstract boolean validateArtInfo();
        abstract void onStart();

        CrumbTrail crumbTrail;
        void addBreadcrumb(String crumb, Object... args) {
            if (!DROP_CRUMBS) return;
            if (crumbTrail == null)
                crumbTrail = new CrumbTrail();
            crumbTrail.drop(String.format(Locale.US, crumb, args));
        }

        void printTrail() {
            if (!DROP_CRUMBS) return;
            crumbTrail.follow();
        }
    }

    class ArtistArtworkRequest extends BaseArtworkRequest {

        ArtistArtworkRequest(RequestKey key, CompletionListener listener) {
            super(key, listener);
        }

        @Override
        boolean validateArtInfo() {
            return !TextUtils.isEmpty(artInfo.artistName);
        }

        @Override
        void onStart() {
            addBreadcrumb("onStart");
            boolean isOnline = isOnline(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true));
            boolean wantArtistImages = mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTIST_IMAGES, true);
            if (isOnline && wantArtistImages) {
                addBreadcrumb("goingForNetwork");
                tryForNetwork();
            } else {
                onComplete();
            }
        }

        void tryForNetwork() {
            subscription = createArtistNetworkRequest(artInfo, artworkType)
                    .observeOn(oScheduler)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForNetwork miss");
                            onComplete();
                        }
                    }, new Action0() {
                        @Override
                        public void call() {
                            addBreadcrumb("tryForNetwork hit");
                            onResponse();
                            onComplete();
                        }
                    });
        }
    }

    class AlbumArtworkRequest extends BaseArtworkRequest {

        AlbumArtworkRequest(RequestKey key, CompletionListener listener) {
            super(key, listener);
        }

        @Override
        boolean validateArtInfo() {
            return (!TextUtils.isEmpty(artInfo.artistName) && !TextUtils.isEmpty(artInfo.albumName))
                    || (artInfo.artworkUri != null && !artInfo.artworkUri.equals(Uri.EMPTY));
        }

        @Override
        void onStart() {
            addBreadcrumb("onStart");
            //check if we have everything we need to download artwork
            boolean hasAlbumArtist = !TextUtils.isEmpty(artInfo.albumName) && !TextUtils.isEmpty(artInfo.artistName);
            boolean hasUri = artInfo.artworkUri != null && !artInfo.artworkUri.equals(Uri.EMPTY);
            boolean isOnline = isOnline(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true));
            boolean wantAlbumArt = mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true);
            boolean preferDownload = mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false);
            boolean isLocalArt = isLocalArtwork(artInfo.artworkUri);
            if (hasAlbumArtist && hasUri) {
                addBreadcrumb("hasAlbumArtist && hasUri");
                // We have everything we may need
                if (isOnline && wantAlbumArt) {
                    addBreadcrumb("isOnline && wantAlbumArt");
                    // were online and want artwork
                    if (isLocalArt && !preferDownload) {
                        addBreadcrumb("goingForMediaStore(true)");
                        // try mediastore first if local and user prefers
                        tryForMediaStore(true);
                    } else if (!isLocalArt && !preferDownload) {
                        // remote art and dont want to try for lfm
                        addBreadcrumb("goingForUrl");
                        tryForUrl();
                    } else {
                        addBreadcrumb("goingForNetwork(true)");
                        // go to network, falling back on fail
                        tryForNetwork(true);
                    }
                } else if (isOnline && !isLocalArt) {
                    addBreadcrumb("goingForUrl");
                    // were online and have an external uri lets get it
                    // regardless of user preference
                    tryForUrl();
                } else if (!isOnline && isLocalArt && !preferDownload) {
                    addBreadcrumb("goingForMediaStore(false)");
                    // were offline, this is a local source
                    // and the user doesnt want to try network first
                    // go ahead and fetch the mediastore image
                    tryForMediaStore(false);
                } else {
                    //  were offline and cant get artwork or the user wants to defer
                    addBreadcrumb("defer fetching art");
                    onComplete();
                }
            } else if (hasAlbumArtist) {
                addBreadcrumb("hasAlbumArtist");
                if (isOnline) {
                    addBreadcrumb("tryForNetwork(false)");
                    // try for network, we dont have a uri so dont fallback on failure
                    tryForNetwork(false);
                } else {
                    onComplete();
                }
            } else if (hasUri) {
                addBreadcrumb("hasUri");
                if (isLocalArt) {
                    addBreadcrumb("goingForMediaStore(false)");
                    //Wait what? this should never happen
                    tryForMediaStore(false);
                } else if (isOnline(false)) { //ignore wifi only request for remote urls
                    addBreadcrumb("goingForUrl");
                    //all we have is a url so go for it
                    tryForUrl();
                } else {
                    onComplete();
                }
            } else { // just ignore the request
                onComplete();
            }
        }

        void tryForNetwork(final boolean tryFallbackOnFail) {
            subscription = createAlbumNetworkObservable(artInfo, artworkType)
                    .observeOn(oScheduler)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForNetwork miss");
                            onNetworkMiss(tryFallbackOnFail);
                        }
                    }, new Action0() {
                        @Override
                        public void call() {
                            addBreadcrumb("tryForNetwork hit");
                            onResponse();
                            onComplete();
                        }
                    });
        }

        void onNetworkMiss(final boolean tryFallback) {
            addBreadcrumb("onNetworkMiss");
            boolean isLocalArt = isLocalArtwork(artInfo.artworkUri);
            if (tryFallback) {
                if (isLocalArt) {
                    addBreadcrumb("goingForMediaStore(false)");
                    tryForMediaStore(false);
                } else {
                    addBreadcrumb("goingForUrl");
                    tryForUrl();
                }
            } else {
                onComplete();
            }
        }

        void tryForMediaStore(final boolean tryNetworkOnFailure) {
            subscription = createMediaStoreRequestObservable(artInfo, artworkType)
                    .observeOn(oScheduler)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForMediaStore miss");
                            onMediaStoreMiss(tryNetworkOnFailure);
                        }
                    }, new Action0() {
                        @Override
                        public void call() {
                            addBreadcrumb("tryForMediaStore hit");
                            onResponse();
                            onComplete();
                        }
                    });
        }

        void onMediaStoreMiss(boolean tryNetwork) {
            addBreadcrumb("onMediaStoreMiss");
            if (tryNetwork) {
                addBreadcrumb("goingForNetwork");
                tryForNetwork(false);
            } else {
                onComplete();
            }
        }

        void tryForUrl() {
            subscription = createImageRequestObservable(artInfo.artworkUri.toString(), artInfo, artworkType)
                    .observeOn(oScheduler)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {

                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForUrl miss");
//                            Timber.w(throwable, "tryForUrl %s", artInfo);
                            onComplete();
                        }
                    }, new Action0() {
                        @Override
                        public void call() {
                            addBreadcrumb("tryForUrl hit");
                            onResponse();
                            onComplete();
                        }
                    });
        }
    }

    /**
     * Entry point
     */
    public Subscription fetch(Uri uri, ArtInfo artInfo, ArtworkType artworkType, CompletionListener l) {
        IFetcherTask t = null;
        final RequestKey k = new RequestKey(artInfo, artworkType);
        if (!mActiveRequests.containsKey(k)) {
            t = newTaskForUri(uri, k, l);
            if (t != null) {
                mActiveRequests.put(k, uri);
                t.start();
            }
        }
        return t;
    }

    IFetcherTask newTaskForUri(Uri uri, RequestKey k, CompletionListener l) {
        switch (mUriMatcher.match(uri)) {
            case ArtworkUris.MATCH.ARTWORK:
            case ArtworkUris.MATCH.THUMBNAIL:
            case ArtworkUris.MATCH.ALBUM_REQ:
                return new AlbumArtworkRequest(k, l);
            case ArtworkUris.MATCH.ARTIST_REQ:
                return new ArtistArtworkRequest(k, l);
            default:
                return null;
        }
    }

    /**
     * Clears the disk caches
     */
    public void clearCaches() {
        clearVolleyQueue();
        mVolleyQueue.getCache().clear();
        mL2Cache.clearCache();
    }

    public void clearVolleyQueue() {
        mVolleyQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    public void onDestroy() {
        mVolleyQueue.stop();
    }

    public Observable<Bitmap> createAlbumNetworkObservable(final ArtInfo artInfo, final ArtworkType artworkType) {
        return createAlbumLastFmApiRequestObservable(artInfo)
                // remap the album info returned by last fm into a url where we can find an image
                .flatMap(new Func1<Album, Observable<String>>() {
                    @Override
                    public Observable<String> call(final Album album) {
                        // try coverartarchive
                        if (!mPreferences.getBoolean(ArtworkPreferences.WANT_LOW_RESOLUTION_ART, false)) {
                            Timber.v("Creating CoverArtRequest %s, from %s", album.getName(), Thread.currentThread().getName());
                            return createAlbumCoverArtRequestObservable(album.getMbid())
                                    // if coverartarchive fails fallback to lastfm
                                    // im using ResumeNext so i can propogate the error
                                    // not sure Return will do that properly TODO find out
                                    .onErrorResumeNext(new Func1<Throwable, Observable<String>>() {
                                        @Override
                                        public Observable<String> call(Throwable throwable) {
                                            Timber.v("CoverArtRequest failed %s, from %s", album.getName(), Thread.currentThread().getName());
                                            String url = getBestImage(album, true);
                                            if (!TextUtils.isEmpty(url)) {
                                                return Observable.just(url);
                                            } else {
                                                return Observable.error(new NullPointerException("No image urls for " + album.getName()));
                                            }
                                        }
                                    });
                        } else { // user wants low res go straight for lastfm
                            String url = getBestImage(album, false);
                            if (!TextUtils.isEmpty(url)) {
                                return Observable.just(url);
                            } else {
                                return Observable.error(new NullPointerException("No url for " + album.getName()));
                            }
                        }
                    }
                })
                // remap the url we found into a bitmap
                .flatMap(new Func1<String, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(String s) {
                        return createImageRequestObservable(s, artInfo, artworkType);
                    }
                });
    }

    public Observable<Bitmap> createArtistNetworkRequest(final ArtInfo artInfo, final ArtworkType artworkType) {
        return createArtistLastFmApiRequestObservable(artInfo)
                .map(new Func1<Artist, String>() {
                    @Override
                    public String call(Artist artist) {
                        String url = getBestImage(artist, !mPreferences.getBoolean(ArtworkPreferences.WANT_LOW_RESOLUTION_ART, false));
                        if (!TextUtils.isEmpty(url)) {
                            return url;
                        }
                        Timber.v("ArtistApiRequest: No image urls for %s", artist.getName());
                        throw new NullPointerException("No image urls for " + artist.getName());
                    }
                })
                .flatMap(new Func1<String, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(String s) {
                        return createImageRequestObservable(s, artInfo, artworkType);
                    }
                });
    }

    public Observable<Album> createAlbumLastFmApiRequestObservable(final ArtInfo artInfo) {
        return Observable.create(new Observable.OnSubscribe<Album>() {
            @Override
            public void call(final Subscriber<? super Album> subscriber) {
                MusicEntryResponseCallback<Album> listener = new MusicEntryResponseCallback<Album>() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }
                    @Override
                    public void onResponse(Album album) {
                        if (subscriber.isUnsubscribed()) return;
                        if (!TextUtils.isEmpty(album.getMbid())) {
                            subscriber.onNext(album);
                            subscriber.onCompleted();
                        } else {
                            Timber.w("Api response does not contain mbid for %s", album.getName());
                            onErrorResponse(new VolleyError("Unknown mbid"));
                        }
                    }
                };
                mVolleyQueue.add(Fetch.albumInfo(artInfo.artistName, artInfo.albumName, listener, Request.Priority.HIGH));
            }
        });
    }

    public Observable<Artist> createArtistLastFmApiRequestObservable(final ArtInfo artInfo) {
        return Observable.create(new Observable.OnSubscribe<Artist>() {
            @Override
            public void call(final Subscriber<? super Artist> subscriber) {
                MusicEntryResponseCallback<Artist> listener = new MusicEntryResponseCallback<Artist>() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }

                    @Override
                    public void onResponse(Artist artist) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onNext(artist);
                        subscriber.onCompleted();
                    }
                };
                mVolleyQueue.add(Fetch.artistInfo(artInfo.artistName, listener, Request.Priority.HIGH));
            }
        });
    }

    public Observable<String> createAlbumCoverArtRequestObservable(final String mbid) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                CoverArtJsonRequest.Listener listener = new CoverArtJsonRequest.Listener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Timber.v("CoverArtRequest:onErrorResponse %s", volleyError);
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }
                    @Override
                    public void onResponse(String s) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onNext(s);
                        subscriber.onCompleted();
                    }
                };
                mVolleyQueue.add(new CoverArtJsonRequest(mbid, listener, mGson));
            }
        });
    }

    public Observable<Bitmap> createImageRequestObservable(final String url, final ArtInfo artInfo, final ArtworkType artworkType) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(final Subscriber<? super Bitmap> subscriber) {
                Timber.v("creating ImageRequest %s, from %s", url, Thread.currentThread().getName());
                ArtworkRequest2.Listener listener = new ArtworkRequest2.Listener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        // always add to cache
                        String cacheKey = UtilsArt.getCacheKey(artInfo, artworkType);
                        writeToL2(cacheKey, bitmap);
                        if (subscriber.isUnsubscribed()) return;
                        //subscriber.onNext(bitmap);
                        subscriber.onCompleted();
                    }
                };
                mVolleyQueue.add(new ArtworkRequest2(mContext, url, artworkType, listener).setTag(artInfo));
                // Here we take advantage of volleys coolest feature,
                // We have 2 types of images, a thumbnail and a larger image suitable for
                // fullscreen use. these are almost never required at the same time so we create
                // a second request. The cool part is volley wont actually download the image twice
                // this second request is attached to the first and processed afterwards
                // saving bandwidth and ensuring both kinds of images are available next time
                // we need them.
                createImageRequestForCache(url, artInfo, ArtworkType.opposite(artworkType));
            }
        });
    }

    public void createImageRequestForCache(final String url, final ArtInfo artInfo, final ArtworkType artworkType) {
        ArtworkRequest2.Listener listener = new ArtworkRequest2.Listener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                //pass
            }
            @Override
            public void onResponse(Bitmap bitmap) {
                writeToL2(UtilsArt.getCacheKey(artInfo, artworkType), bitmap);
            }
        };
        mVolleyQueue.add(new ArtworkRequest2(mContext, url, artworkType, listener).setTag(artInfo));
    }

    public Observable<Bitmap> createMediaStoreRequestObservable(final ArtInfo artInfo, final ArtworkType artworkType) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(final Subscriber<? super Bitmap> subscriber) {
                Timber.v("creating MediaStoreRequest %s, from %s", artInfo, Thread.currentThread().getName());
                InputStream in = null;
                try {
                    final Uri uri = artInfo.artworkUri;
                    in = mContext.getContentResolver().openInputStream(uri);
                    final NetworkResponse response = new NetworkResponse(IOUtils.toByteArray(in));
                    // We create a faux ImageRequest so we can cheat and use it
                    // to processs the bitmap like a real network request
                    // this is not only easier but safer since all bitmap processing is serial.
                    ArtworkRequest2 request = new ArtworkRequest2(mContext, "fauxrequest", artworkType, null);
                    Response<Bitmap> result = request.parseNetworkResponse(response);
                    // make the opposite as well
                    ArtworkType artworkType2 = ArtworkType.opposite(artworkType);
                    ArtworkRequest2 request2 = new ArtworkRequest2(mContext, "fauxrequest2", artworkType2, null);
                    Response<Bitmap> result2 = request2.parseNetworkResponse(response);
                    // add to cache first so we dont return before its added
                    if (result2.isSuccess()) {
                        String cacheKey = UtilsArt.getCacheKey(artInfo, artworkType2);
                        writeToL2(cacheKey, result2.result);
                    }
                    if (result.isSuccess()) {
                        //always add to cache
                        String cacheKey = UtilsArt.getCacheKey(artInfo, artworkType);
                        writeToL2(cacheKey, result.result);
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onNext(result.result);
                        subscriber.onCompleted();
                    } else {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(result.error);
                    }
                } catch (Exception e) { //too many to keep track of
                    if (subscriber.isUnsubscribed()) return;
                    subscriber.onError(e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        }).subscribeOn(scheduler);
    }

    final BlockingDeque<Map.Entry<String, Bitmap>> diskCacheQueue = new LinkedBlockingDeque<>();
    Scheduler.Worker diskCacheWorker;

    //Unused but keeping around
    public void putInDiskCache(final String key, final Bitmap bitmap) {
        Timber.v("putInDiskCache(%s)", key);
        diskCacheQueue.addLast(new AbstractMap.SimpleEntry<>(key, bitmap));
        if (diskCacheWorker == null || diskCacheWorker.isUnsubscribed()) {
            diskCacheWorker = scheduler.createWorker();
            diskCacheWorker.schedule(new Action0() {
                @Override
                public void call() {
                    while (true) {
                        try {
                            Map.Entry<String, Bitmap> entry = diskCacheQueue.pollFirst(60, TimeUnit.SECONDS);
                            if (entry != null) {
                                writeToL2(entry.getKey(), entry.getValue());
                                continue;
                            }
                        } catch (InterruptedException ignored) {
                            //fall
                        }
                        if (diskCacheWorker != null) {
                            diskCacheWorker.unsubscribe();
                            diskCacheWorker = null;
                        }
                        break;
                    }
                }
            });
        }
    }

    void writeToL2(final String key, final Bitmap bitmap) {
        Timber.v("writeToL2(%s)", key);
        mL2Cache.putBitmap(key, bitmap);
    }

    /**
     * @return url string for highest quality image available or null if none
     */
    public static String getBestImage(MusicEntry e, boolean wantHigResArt) {
        for (ImageSize q : ImageSize.values()) {
            if (q.equals(ImageSize.MEGA) && !wantHigResArt) {
                continue;
            }
            String url = e.getImageURL(q);
            if (!TextUtils.isEmpty(url)) {
                Timber.v("Found " + q.toString() + " url for " + e.getName());
                return url;
            }
        }
        return null;
    }

    /**
     *
     */
    public static boolean isLocalArtwork(Uri u) {
        if (u != null) {
            if ("content".equals(u.getScheme())) {
                return true;
            }
        }
        return false;
    }

    boolean isOnline(boolean wifiOnly) {

        boolean state = false;

        /* Wi-Fi connection */
        final NetworkInfo wifiNetwork =
                mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null) {
            state = wifiNetwork.isConnectedOrConnecting();
        }

        // Don't bother checking the rest if we are connected or we have opted out of mobile
        if (wifiOnly || state) {
            return state;
        }

        /* Mobile data connection */
        final NetworkInfo mbobileNetwork =
                mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mbobileNetwork != null) {
            state = mbobileNetwork.isConnectedOrConnecting();
        }

        /* Other networks */
        final NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            state = activeNetwork.isConnectedOrConnecting();
        }

        return state;
    }

}
