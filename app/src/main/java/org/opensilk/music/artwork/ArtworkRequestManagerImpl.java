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

package org.opensilk.music.artwork;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.jakewharton.disklrucache.DiskLruCache;

import org.apache.commons.io.IOUtils;
import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.cache.ArtworkCache;
import org.opensilk.music.artwork.cache.ArtworkLruCache;
import org.opensilk.music.artwork.cache.BitmapCache;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.ui2.loader.AlbumArtInfoLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.MusicEntry;
import de.umass.lastfm.opensilk.Fetch;
import de.umass.lastfm.opensilk.MusicEntryResponseCallback;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/21/14.
 */
@Singleton
public class ArtworkRequestManagerImpl implements ArtworkRequestManager {
    final static boolean DROP_CRUMBS = false;

    final Context mContext;
    final AppPreferences mPreferences;
    final ArtworkCache mL1Cache;
    final BitmapDiskCache mL2Cache;
    final RequestQueue mVolleyQueue;
    final Gson mGson;
    final ConnectivityManager mConnectivityManager;

    final Map<RequestKey, IArtworkRequest> mActiveRequests = new LinkedHashMap<>(10);

    @Inject
    public ArtworkRequestManagerImpl(@ForApplication Context mContext,
                                     AppPreferences mPreferences,
                                     @Named("L1Cache") ArtworkCache mL1Cache,
                                     @Named("L2Cache") BitmapDiskCache mL2Cache,
                                     RequestQueue mVolleyQueue,
                                     Gson mGson,
                                     ConnectivityManager mConnectivityManager
    ) {
        this.mContext = mContext;
        this.mPreferences = mPreferences;
        this.mL1Cache = mL1Cache;
        this.mL2Cache = mL2Cache;
        this.mVolleyQueue = mVolleyQueue;
        this.mGson = mGson;
        this.mConnectivityManager = mConnectivityManager;
    }

    static class CrumbTrail {
        final ArrayList<String> breadcrumbs = new ArrayList<>();
        boolean followed = false;

        void drop(String msg) {
            breadcrumbs.add(msg);
        }

        void follow() {
            followed = true;
            Timber.v(assembleTrail());
        }

        private String assembleTrail() {
            StringBuilder b = new StringBuilder(500);
            for (int ii=0; ii<breadcrumbs.size(); ii++) {
                b.append(breadcrumbs.get(ii));
                if (ii < breadcrumbs.size()-1)
                    b.append(" -> ");
            }
            return b.toString();
        }

        @Override
        protected void finalize() throws Throwable {
            if (!followed) {
                Timber.e("CrumbTrail never followed: %s", assembleTrail());
            }
            super.finalize();
        }
    }

    interface IArtworkRequest {
        void addRecipient(ImageContainer c);
    }

    /**
     * TODO hook ImageContainer to unsubscribe request when all containers
     * TODO have unsubscribed, and find way to cancel volley requests when unsubscribed
     */
    abstract class BaseArtworkRequest implements Subscription, IArtworkRequest {
        final RequestKey key;
        final ArtInfo artInfo;
        final ArtworkType artworkType;

        final List<ImageContainer> recipients = new LinkedList<>();

        Subscription subscription;
        boolean unsubscribed = false;
        boolean inflight = false;
        boolean complete = false;

        BaseArtworkRequest(RequestKey key) {
            this.key = key;
            this.artInfo = key.artInfo;
            this.artworkType = key.artworkType;
            addBreadcrumb(getCacheKey(artInfo, artworkType));
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
        public void addRecipient(ImageContainer c) {
            if (complete) {
                mActiveRequests.remove(key);
                throw new IllegalStateException("Tried to add recipient after complete");
            }
            recipients.add(c);
            if (!inflight) {
                inflight = true;
                start();
            } else {
                c.setDefaultImage();
            }
        }

        void start() {
            addBreadcrumb("start");
            tryForCache();
        }

        void onComplete() {
            addBreadcrumb("complete");
            complete = true;
            mActiveRequests.remove(key);
            printTrail();
        }

        void setDefaultImage() {
            addBreadcrumb("setDefaultImage");
            if (unsubscribed) return;
            for (ImageContainer c : recipients) {
                if (c.isUnsubscribed()) continue;
                c.setDefaultImage();
            }
        }

        void onResponse(Artwork artwork, boolean fromCache, boolean shouldAnimate) {
            addBreadcrumb("onResponse("+fromCache+")");
            if (unsubscribed) return;
            for (ImageContainer c : recipients) {
                if (c.isUnsubscribed()) continue;
                c.setImageBitmap(artwork.bitmap, shouldAnimate);
                c.notifyPaletteObserver(artwork.palette, shouldAnimate);
            }
        }

        void tryForCache() {
            addBreadcrumb("tryForCache");
            if (!validateArtInfo()) {
                addBreadcrumb("malformedArtInfo");
                setDefaultImage();
                onComplete();
                return;
            }
            subscription = createCacheObservable(artInfo, artworkType)
                    .subscribe(new Action1<CacheResponse>() {
                        @Override
                        public void call(CacheResponse cr) {
                            addBreadcrumb("tryForCache hit");
                            onResponse(cr.artwork, true, !cr.fromL1);
                            onComplete();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForCache miss");
                            if (throwable instanceof CacheMissException) {
                                onCacheMiss();
                            } else {
                                setDefaultImage();
                                onComplete();
                            }
                        }
                    });
        }

        abstract boolean validateArtInfo();
        abstract void onCacheMiss();

        CrumbTrail crumbTrail;
        void addBreadcrumb(String crumb) {
            if (!DROP_CRUMBS) return;
            if (crumbTrail == null)
                crumbTrail = new CrumbTrail();
            crumbTrail.drop(crumb);
        }

        void printTrail() {
            if (!DROP_CRUMBS) return;
            crumbTrail.follow();
        }
    }

    class ArtistArtworkRequest extends BaseArtworkRequest {

        ArtistArtworkRequest(RequestKey key) {
            super(key);
        }

        @Override
        boolean validateArtInfo() {
            return !TextUtils.isEmpty(artInfo.artistName);
        }

        @Override
        void onCacheMiss() {
            addBreadcrumb("onCacheMiss");
            setDefaultImage();
            boolean isOnline = isOnline(mPreferences.getBoolean(AppPreferences.ONLY_ON_WIFI, true));
            boolean wantArtistImages = mPreferences.getBoolean(AppPreferences.DOWNLOAD_MISSING_ARTIST_IMAGES, true);
            if (isOnline && wantArtistImages) {
                addBreadcrumb("goingForNetwork");
                tryForNetwork();
            } else {
                onComplete();
            }
        }

        void tryForNetwork() {
            subscription = createArtistNetworkRequest(artInfo, artworkType)
                    .subscribe(new Action1<Artwork>() {
                        @Override
                        public void call(Artwork artwork) {
                            addBreadcrumb("tryForNetwork hit");
                            onResponse(artwork, false, true);
                            onComplete();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForNetwork miss");
//                            Timber.w(throwable, "Unable to obtain image for %s", artInfo);
                            onComplete();
                        }
                    });
        }
    }

    class AlbumArtworkRequest extends BaseArtworkRequest {

        AlbumArtworkRequest(RequestKey key) {
            super(key);
        }

        @Override
        boolean validateArtInfo() {
            return (!TextUtils.isEmpty(artInfo.artistName) && !TextUtils.isEmpty(artInfo.albumName))
                    || (artInfo.artworkUri != null && !artInfo.artworkUri.equals(Uri.EMPTY));
        }

        @Override
        void onCacheMiss() {
            addBreadcrumb("onCacheMiss");
            setDefaultImage();
            //check if we have everything we need to download artwork
            boolean hasAlbumArtist = !TextUtils.isEmpty(artInfo.albumName) && !TextUtils.isEmpty(artInfo.artistName);
            boolean hasUri = artInfo.artworkUri != null && !artInfo.artworkUri.equals(Uri.EMPTY);
            boolean isOnline = isOnline(mPreferences.getBoolean(AppPreferences.ONLY_ON_WIFI, true));
            boolean wantAlbumArt = mPreferences.getBoolean(AppPreferences.DOWNLOAD_MISSING_ARTWORK, true);
            boolean preferDownload = mPreferences.getBoolean(AppPreferences.PREFER_DOWNLOAD_ARTWORK, false);
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
                    .subscribe(new Action1<Artwork>() {
                        @Override
                        public void call(Artwork artwork) {
                            addBreadcrumb("tryForNetwork hit");
                            onResponse(artwork, false, true);
                            onComplete();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForNetwork miss");
                            onNetworkMiss(tryFallbackOnFail);
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
                    .subscribe(new Action1<Artwork>() {
                        @Override
                        public void call(Artwork artwork) {
                            addBreadcrumb("tryForMediaStore hit");
                            onResponse(artwork, false, true);
                            onComplete();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForMediaStore miss");
                            onMediaStoreMiss(tryNetworkOnFailure);
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
                    .subscribe(new Action1<Artwork>() {
                        @Override
                        public void call(Artwork artwork) {
                            addBreadcrumb("tryForUrl hit");
                            onResponse(artwork, false, true);
                            onComplete();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForUrl miss");
//                            Timber.w(throwable, "tryForUrl %s", artInfo);
                            onComplete();
                        }
                    });
        }
    }

    class WrappedImageContainer implements Subscription {

        final ImageContainer container;

        Subscription subscription;

        WrappedImageContainer(AnimatedImageView imageView, PaletteObserver paletteObserver,
                              long albumId, ArtworkType artworkType) {
            this.container = new ImageContainer(imageView, paletteObserver);
            getArtInfo(albumId, artworkType);
        }

        @Override
        public void unsubscribe() {
            container.unsubscribe();
            if (subscription != null) {
                subscription.unsubscribe();
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return container.isUnsubscribed();
        }

        void getArtInfo(final long albumId, final ArtworkType artworkType) {
            subscription = new AlbumArtInfoLoader(mContext, new long[]{albumId})
                    .createObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<ArtInfo>() {
                        @Override
                        public void call(ArtInfo artInfo) {
                            RequestKey k = new RequestKey(artInfo, artworkType);
                            queueRequest(container, k, true);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            container.setDefaultImage();
                        }
                    });
        }
    }

    /*
     * Start IMPL
     */

    @Override
    public Subscription newAlbumRequest(AnimatedImageView imageView, PaletteObserver paletteObserver,
                                        ArtInfo artInfo, ArtworkType artworkType) {
        ImageContainer c = new ImageContainer(imageView, paletteObserver);
        RequestKey k = new RequestKey(artInfo, artworkType);
        queueRequest(c, k, true);
        return c;
    }

    @Override
    public Subscription newAlbumRequest(AnimatedImageView imageView, PaletteObserver paletteObserver,
                                        long albumId, ArtworkType artworkType) {
        return new WrappedImageContainer(imageView, paletteObserver, albumId, artworkType);
    }

    @Override
    public Subscription newArtistRequest(AnimatedImageView imageView, PaletteObserver paletteObserver,
                                         ArtInfo artInfo, ArtworkType artworkType) {
        ImageContainer c = new ImageContainer(imageView, paletteObserver);
        RequestKey k = new RequestKey(artInfo, artworkType);
        queueRequest(c, k, false);
        return c;
    }

    @Override
    public ParcelFileDescriptor getArtwork(String artistName, String albumName) {
        final ArtInfo artInfo = new ArtInfo(artistName, albumName, null);
        final String cacheKey = getCacheKey(artInfo, ArtworkType.LARGE);
        ParcelFileDescriptor pfd = pullSnapshot(cacheKey);
        // Create request so it will be there next time
        if (pfd == null) newAlbumRequest(null, null, artInfo, ArtworkType.LARGE);
        return pfd;
    }

    @Override
    public ParcelFileDescriptor getArtworkThumbnail(String artistName, String albumName) {
        final ArtInfo artInfo = new ArtInfo(artistName, albumName, null);
        final String cacheKey = getCacheKey(artInfo, ArtworkType.THUMBNAIL);
        ParcelFileDescriptor pfd = pullSnapshot(cacheKey);
        // Create request so it will be there next time
        if (pfd == null) newAlbumRequest(null, null, artInfo, ArtworkType.THUMBNAIL);
        return pfd;
    }

    @Override
    public boolean clearCaches() {
        boolean success;
        clearVolleyQueue();
        mVolleyQueue.getCache().clear();
        evictL1();
        success = mL2Cache.clearCache();
        return success;
    }

    @Override
    @DebugLog
    public void evictL1() {
        mL1Cache.clearCache();
    }

    @Override
    @DebugLog
    public void onDeathImminent() {
//        diskCacheQueue.clear();
        clearVolleyQueue();
    }

    /*
     * End IMPL
     */

    void queueRequest(ImageContainer c, RequestKey k, boolean isAlbum) {
        IArtworkRequest r = mActiveRequests.get(k);
        if (r == null) {
            r = isAlbum ? new AlbumArtworkRequest(k) : new ArtistArtworkRequest(k);
            mActiveRequests.put(k, r);
        } else {
            Timber.d("Attaching recipient to running request %s", k.artInfo);
        }
        r.addRecipient(c);
    }

    void clearVolleyQueue() {
        mVolleyQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    public Observable<CacheResponse> createCacheObservable(final ArtInfo artInfo, final ArtworkType artworkType) {
        final String cacheKey = getCacheKey(artInfo, artworkType);
        return Observable.create(new Observable.OnSubscribe<CacheResponse>() {
                @Override
                public void call(Subscriber<? super CacheResponse> subscriber) {
//                    Timber.v("Trying L1 for %s, from %s", cacheKey, Thread.currentThread().getName());
                    Artwork artwork = mL1Cache.getArtwork(cacheKey);
                    if (!subscriber.isUnsubscribed()) {
                        if (artwork != null) {
                            subscriber.onNext(new CacheResponse(artwork, true));
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new CacheMissException());
                        }
                    }
                }
            })
            // We missed the l1cache try l2
            .onErrorResumeNext(new Func1<Throwable, Observable<? extends CacheResponse>>() {
                @Override
                public Observable<? extends CacheResponse> call(Throwable throwable) {
                    if (throwable instanceof CacheMissException) {
                        return Observable.create(new Observable.OnSubscribe<CacheResponse>() {
                            @Override
                            public void call(Subscriber<? super CacheResponse> subscriber) {
//                                Timber.v("Trying L2 for %s, from %s", cacheKey, Thread.currentThread().getName());
                                Bitmap bitmap = mL2Cache.getBitmap(cacheKey);
                                if (bitmap != null) {
                                    //Always add to cache
                                    Palette palette = Palette.generate(bitmap);
                                    Artwork artwork = new Artwork(bitmap, palette);
                                    mL1Cache.putArtwork(cacheKey, artwork);
                                    if (subscriber.isUnsubscribed()) return;
                                    subscriber.onNext(new CacheResponse(artwork, false));
                                    subscriber.onCompleted();
                                } else {
                                    if (subscriber.isUnsubscribed()) return;
                                    subscriber.onError(new CacheMissException());
                                }
                            }
                        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
                    } else {
                        return Observable.error(throwable);
                    }
                }
            });
    }

    public Observable<Artwork> createAlbumNetworkObservable(final ArtInfo artInfo, final ArtworkType artworkType) {
        return createAlbumLastFmApiRequestObservable(artInfo)
                // remap the album info returned by last fm into a url where we can find an image
                .flatMap(new Func1<Album, Observable<String>>() {
                    @Override
                    public Observable<String> call(final Album album) {
                        // try coverartarchive
                        if (!mPreferences.getBoolean(AppPreferences.WANT_LOW_RESOLUTION_ART, false)) {
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
                .flatMap(new Func1<String, Observable<Artwork>>() {
                    @Override
                    public Observable<Artwork> call(String s) {
                        return createImageRequestObservable(s, artInfo, artworkType);
                    }
                });
    }

    public Observable<Artwork> createArtistNetworkRequest(final ArtInfo artInfo, final ArtworkType artworkType) {
        return createArtistLastFmApiRequestObservable(artInfo)
                .map(new Func1<Artist, String>() {
                    @Override
                    public String call(Artist artist) {
                        String url = getBestImage(artist, !mPreferences.getBoolean(AppPreferences.WANT_LOW_RESOLUTION_ART, false));
                        if (!TextUtils.isEmpty(url)) {
                            return url;
                        }
                        Timber.v("ArtistApiRequest: No image urls for %s", artist.getName());
                        throw new NullPointerException("No image urls for " + artist.getName());
                    }
                })
                .flatMap(new Func1<String, Observable<Artwork>>() {
                    @Override
                    public Observable<Artwork> call(String s) {
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

    public Observable<Artwork> createImageRequestObservable(final String url, final ArtInfo artInfo, final ArtworkType artworkType) {
        return Observable.create(new Observable.OnSubscribe<Artwork>() {
            @Override
            public void call(final Subscriber<? super Artwork> subscriber) {
                Timber.v("creating ImageRequest %s, from %s", url, Thread.currentThread().getName());
                ArtworkRequest2.Listener listener = new ArtworkRequest2.Listener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }
                    @Override
                    public void onResponse(Artwork artwork) {
                        // always add to cache
                        String cacheKey = getCacheKey(artInfo, artworkType);
                        mL1Cache.putArtwork(cacheKey, artwork);
                        putInDiskCache(cacheKey, artwork.bitmap);
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onNext(artwork);
                        subscriber.onCompleted();
                    }
                };
                mVolleyQueue.add(new ArtworkRequest2(url, artworkType, listener).setTag(artInfo));
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
            public void onResponse(Artwork artwork) {
                putInDiskCache(getCacheKey(artInfo, artworkType), artwork.bitmap);
            }
        };
        mVolleyQueue.add(new ArtworkRequest2(url, artworkType, listener).setTag(artInfo));
    }

    public Observable<Artwork> createMediaStoreRequestObservable(final ArtInfo artInfo, final ArtworkType artworkType) {
        return Observable.create(new Observable.OnSubscribe<Artwork>() {
            @Override
            public void call(final Subscriber<? super Artwork> subscriber) {
                Timber.v("creating MediaStoreRequest %s, from %s", artInfo, Thread.currentThread().getName());
                InputStream in = null;
                try {
                    final Uri uri = artInfo.artworkUri;
                    in = mContext.getContentResolver().openInputStream(uri);
                    final NetworkResponse response = new NetworkResponse(IOUtils.toByteArray(in));
                    // We create a faux ImageRequest so we can cheat and use it
                    // to processs the bitmap like a real network request
                    // this is not only easier but safer since all bitmap processing is serial.
                    ArtworkRequest2 request = new ArtworkRequest2("fauxrequest", artworkType, null);
                    Response<Artwork> result = request.parseNetworkResponse(response);
                    // make the opposite as well
                    ArtworkType artworkType2 = ArtworkType.opposite(artworkType);
                    ArtworkRequest2 request2 = new ArtworkRequest2("fauxrequest2", artworkType2, null);
                    Response<Artwork> result2 = request2.parseNetworkResponse(response);
                    // add to cache first so we dont return before its added
                    if (result2.isSuccess()) {
                        String cacheKey = getCacheKey(artInfo, artworkType2);
                        putInDiskCache(cacheKey, result2.result.bitmap);
                    }
                    if (result.isSuccess()) {
                        //always add to cache
                        String cacheKey = getCacheKey(artInfo, artworkType);
                        mL1Cache.putArtwork(cacheKey, result.result);
                        putInDiskCache(cacheKey, result.result.bitmap);
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
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    final BlockingDeque<Map.Entry<String, Bitmap>> diskCacheQueue = new LinkedBlockingDeque<>();
    Scheduler.Worker diskCacheWorker;

    public void putInDiskCache(final String key, final Bitmap bitmap) {
        Timber.v("putInDiskCache(%s)", key);
        diskCacheQueue.addLast(new AbstractMap.SimpleEntry<>(key, bitmap));
        if (diskCacheWorker == null || diskCacheWorker.isUnsubscribed()) {
            diskCacheWorker = Schedulers.io().createWorker();
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

    private ParcelFileDescriptor pullSnapshot(String cacheKey) {
        Timber.d("Checking DiskCache for " + cacheKey);
        try {
            if (mL2Cache == null) {
                throw new IOException("Unable to obtain cache instance");
            }
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
            final DiskLruCache.Snapshot snapshot = mL2Cache.getSnapshot(cacheKey);
            if (snapshot != null && snapshot.getInputStream(0) != null) {
                final Scheduler.Worker worker = Schedulers.io().createWorker();
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        try {
                            IOUtils.copy(snapshot.getInputStream(0), out);
                        } catch (IOException e) {
//                            e.printStackTrace();
                        } finally {
                            snapshot.close();
                            IOUtils.closeQuietly(out);
                            worker.unsubscribe();
                        }
                    }
                });
                return pipe[0];
            } else {
                pipe[0].close();
                out.close();
            }
        } catch (IOException e) {
            Timber.w(e, "pullSnapshot");
        }
        return null;
    }

    /**
     * Creates a cache key for use with the L1 cache.
     *
     * if both artist and album are null we must use the uri or all
     * requests will return the same cache key, to maintain backwards
     * compat with orpheus versions < 0.5 we use artist,album when we can.
     *
     * if all fields in artinfo are null we cannot fetch any art so an npe will
     * be thrown
     */
    public static String getCacheKey(ArtInfo artInfo, ArtworkType imageType) {
        int size = 0;
        if (artInfo.artistName == null && artInfo.albumName == null) {
            if (artInfo.artworkUri == null) {
                throw new NullPointerException("Cant fetch art with all null fields");
            }
            size += artInfo.artworkUri.toString().length();
            return new StringBuilder(size+12)
                    .append("#").append(imageType).append("#")
                    .append(artInfo.artworkUri.toString())
                    .toString();
        } else {
            size += artInfo.artistName != null ? artInfo.artistName.length() : 4;
            size += artInfo.albumName != null ? artInfo.albumName.length() : 4;
            return new StringBuilder(size+12)
                    .append("#").append(imageType).append("#")
                    .append(artInfo.artistName).append("#")
                    .append(artInfo.albumName)
                    .toString();
        }
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
