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
import android.net.Uri;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;

import com.andrew.apollo.utils.ApolloUtils;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.opensilk.common.rx.HoldsSubscription;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.cache.ArtworkLruCache;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.ui2.loader.AlbumArtInfoLoader;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

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
    final static boolean DEBUG = false;

    final Context mContext;
    final AppPreferences mPreferences;
    final ArtworkLruCache mL1Cache;
    final BitmapDiskLruCache mL2Cache;
    final RequestQueue mVolleyQueue;
    final Gson mGson;

    @Inject
    public ArtworkRequestManagerImpl(@ForApplication Context mContext, AppPreferences mPreferences,
                                     ArtworkLruCache mL1Cache, BitmapDiskLruCache mL2Cache,
                                     RequestQueue mVolleyQueue, Gson mGson) {
        this.mContext = mContext;
        this.mPreferences = mPreferences;
        this.mL1Cache = mL1Cache;
        this.mL2Cache = mL2Cache;
        this.mVolleyQueue = mVolleyQueue;
        this.mGson = mGson;
    }

    abstract class BaseArtworkRequest implements Subscription {
        final WeakReference<AnimatedImageView> imageViewWeakReference;
        final WeakReference<PaletteObserver> palleteObserverWeakReference;
        final ArtInfo artInfo;
        final ArtworkType artworkType;

        final StringBuilder breadcrumbs;

        Subscription subscription;
        boolean unsubscribed = false;

        BaseArtworkRequest(AnimatedImageView imageView, PaletteObserver paletteObserver,
                           ArtInfo artInfo, ArtworkType artworkType) {
            this.imageViewWeakReference = new WeakReference<>(imageView);
            this.palleteObserverWeakReference = new WeakReference<>(paletteObserver);
            this.artInfo = artInfo;
            this.artworkType = artworkType;
            breadcrumbs = new StringBuilder(500);
            if (this.artInfo != null) breadcrumbs.append(getCacheKey(artInfo, artworkType));
        }

        Subscription start() {
            registerWithImageView();
            addBreadcrumb("start");
            tryForCache();
            return this;
        }

        @Override
        public void unsubscribe() {
            addBreadcrumb("unsubscribe");
            if (subscription != null) {
                subscription.unsubscribe();
                subscription = null;
            }
            unregisterWithImageView();
            imageViewWeakReference.clear();
            palleteObserverWeakReference.clear();
            unsubscribed = true;
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }

        void registerWithImageView() {
            AnimatedImageView imageView = imageViewWeakReference.get();
            if (imageView != null && imageView instanceof HoldsSubscription) {
                ((HoldsSubscription) imageView).addSubscription(this);
            }
        }

        void unregisterWithImageView() {
            AnimatedImageView imageView = imageViewWeakReference.get();
            if (imageView != null && imageView instanceof HoldsSubscription) {
                ((HoldsSubscription) imageView).removeSubscription(this);
            }
        }

        void setDefaultImage() {
            addBreadcrumb("setDefaultImage");
            AnimatedImageView imageView = imageViewWeakReference.get();
            if (imageView == null) return;
            imageView.setDefaultImage();
        }

        void setImageBitmap(Bitmap bitmap) {
            setImageBitmap(bitmap, false, true);
        }

        void setImageBitmap(final Bitmap bitmap, boolean fromCache, boolean shouldAnimate) {
            addBreadcrumb("setImageBitmap("+fromCache+")");
            final AnimatedImageView imageView = imageViewWeakReference.get();
            if (imageView == null) return;
            unregisterWithImageView();
            if (fromCache) {
                imageView.setImageBitmap(bitmap, shouldAnimate);
            } else {
                imageView.setImageBitmap(bitmap, shouldAnimate);
            }
        }

        void notifyPaletteObserver(Palette palette, boolean shouldAnimate) {
            PaletteObserver po = palleteObserverWeakReference.get();
            if (po != null) {
                po.onNext(new PaletteResponse(palette, shouldAnimate));
                po.onCompleted();
            }
        }

        void tryForCache() {
            addBreadcrumb("tryForCache");
            subscription = createCacheObservable(artInfo, artworkType)
                    .subscribe(new Action1<CacheResponse>() {
                        @Override
                        public void call(CacheResponse cr) {
                            addBreadcrumb("tryForCache hit");
                            setImageBitmap(cr.artwork.bitmap, true, !cr.fromL1);
                            notifyPaletteObserver(cr.artwork.palette, !cr.fromL1);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForCache miss");
                            if (throwable instanceof CacheMissException) {
                                onCacheMiss();
                            } else {
                                setDefaultImage();
                            }
                        }
                    });
        }

        abstract void onCacheMiss();

        void addBreadcrumb(String crumb) {
            if (!DEBUG) return;
            breadcrumbs.append(" -> ").append(crumb);
            Timber.v(breadcrumbs.toString());
        }
    }

    class ArtistArtworkRequest extends BaseArtworkRequest {

        ArtistArtworkRequest(AnimatedImageView imageView, PaletteObserver paletteObserver,
                             ArtInfo artInfo, ArtworkType artworkType) {
            super(imageView, paletteObserver, artInfo, artworkType);
        }

        @Override
        void onCacheMiss() {
            addBreadcrumb("onCacheMiss");
            setDefaultImage();
            if (TextUtils.isEmpty(artInfo.artistName)) return;
            boolean isOnline = ApolloUtils.isOnline(mContext);
            boolean wantArtistImages = mPreferences.getBoolean(AppPreferences.DOWNLOAD_MISSING_ARTIST_IMAGES, true);
            if (isOnline && wantArtistImages) {
                addBreadcrumb("goingForNetwork");
                tryForNetwork();
            }
        }

        void tryForNetwork() {
            subscription = createArtistNetworkRequest(artInfo, artworkType)
                    .subscribe(new Action1<Artwork>() {
                        @Override
                        public void call(Artwork artwork) {
                            addBreadcrumb("tryForNetwork hit");
                            setImageBitmap(artwork.bitmap);
                            notifyPaletteObserver(artwork.palette, true);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForNetwork miss");
//                            Timber.e(throwable, "Unable to obtain image for %s", artInfo);
                        }
                    });
        }
    }

    class AlbumArtworkRequest extends BaseArtworkRequest {

        AlbumArtworkRequest(AnimatedImageView imageView, PaletteObserver paletteObserver,
                            ArtInfo artInfo, ArtworkType artworkType) {
            super(imageView, paletteObserver, artInfo, artworkType);
        }

        @Override
        void onCacheMiss() {
            addBreadcrumb("onCacheMiss");
            setDefaultImage();
            //check if we have everything we need to download artwork
            boolean hasAlbumArtist = !TextUtils.isEmpty(artInfo.albumName) && !TextUtils.isEmpty(artInfo.artistName);
            boolean hasUri = artInfo.artworkUri != null && !artInfo.artworkUri.equals(Uri.EMPTY);
            boolean isOnline = ApolloUtils.isOnline(mContext);
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
                }
            } else if (hasAlbumArtist) {
                addBreadcrumb("hasAlbumArtist");
                if (isOnline) {
                    addBreadcrumb("tryForNetwork(false)");
                    // try for network, we dont have a uri so dont fallback on failure
                    tryForNetwork(false);
                }
            } else if (hasUri) {
                addBreadcrumb("hasUri");
                if (isLocalArt) {
                    addBreadcrumb("goingForMediaStore("+isOnline+")");
                    //Wait what? this should never happen
                    tryForMediaStore(isOnline);
                } else if (isOnline) {
                    addBreadcrumb("goingForUrl");
                    //all we have is a url so go for it
                    tryForUrl();
                }
            } //else just ignore the request
        }

        void tryForNetwork(final boolean tryFallbackOnFail) {
            subscription = createAlbumNetworkObservable(artInfo, artworkType)
                    .subscribe(new Action1<Artwork>() {
                        @Override
                        public void call(Artwork artwork) {
                            addBreadcrumb("tryForNetwork hit");
                            setImageBitmap(artwork.bitmap);
                            notifyPaletteObserver(artwork.palette, true);
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
            if (tryFallback && !unsubscribed) {
                if (isLocalArt) {
                    addBreadcrumb("goingForMediaStore(false)");
                    tryForMediaStore(false);
                } else {
                    addBreadcrumb("goingForUrl");
                    tryForUrl();
                }
            }
        }

        void tryForMediaStore(final boolean tryNetworkOnFailure) {
            subscription = createMediaStoreRequestObservable(artInfo, artworkType)
                    .subscribe(new Action1<Artwork>() {
                        @Override
                        public void call(Artwork artwork) {
                            addBreadcrumb("tryForMediaStore hit");
                            setImageBitmap(artwork.bitmap);
                            notifyPaletteObserver(artwork.palette, true);
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
            }
        }

        void tryForUrl() {
            subscription = createImageRequestObservable(artInfo.artworkUri.toString(), artInfo, artworkType)
                    .subscribe(new Action1<Artwork>() {
                        @Override
                        public void call(Artwork artwork) {
                            addBreadcrumb("tryForUrl hit");
                            setImageBitmap(artwork.bitmap);
                            notifyPaletteObserver(artwork.palette, true);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForUrl miss");
//                            Timber.e(throwable, "tryForUrl %s", artInfo);
                        }
                    });
        }
    }

    public class AlbumArtworkRequestWrapped extends BaseArtworkRequest {

        final long id;
        AlbumArtworkRequest wrappedRequest;

        AlbumArtworkRequestWrapped(AnimatedImageView imageView, PaletteObserver paletteObserver,
                                   long id, ArtworkType artworkType) {
            super(imageView, paletteObserver, null, artworkType);
            this.id =id;
            addBreadcrumb("albumId="+id);
        }

        @Override
        Subscription start() {
            addBreadcrumb("start");
            getArtInfo();
            return this;
        }

        @Override
        public void unsubscribe() {
            addBreadcrumb("unsubscribe");
            if (wrappedRequest != null) {
                wrappedRequest.unsubscribe();
            } else {
                super.unsubscribe();
            }
        }

        @Override
        public boolean isUnsubscribed() {
            if (wrappedRequest != null) {
                return wrappedRequest.isUnsubscribed();
            } else {
                return super.isUnsubscribed();
            }
        }

        @Override
        void onCacheMiss() {
            addBreadcrumb("onCacheMiss");
            if (unsubscribed) return;
            setDefaultImage();
        }

        void getArtInfo() {
            subscription = new AlbumArtInfoLoader(mContext, new long[]{id}).createObservable()
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<ArtInfo>() {
                        @Override
                        public void call(ArtInfo artInfo) {
                            addBreadcrumb("getArtInfo hit");
                            if (!unsubscribed && imageViewWeakReference.get() != null) {
                                wrappedRequest = new AlbumArtworkRequest(imageViewWeakReference.get(),
                                        palleteObserverWeakReference.get(), artInfo, artworkType);
                                wrappedRequest.start();
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("getArtInfo miss");
//                            Timber.e(throwable, "Unable to obtain artinfo from mediastore for id=%d", id);
                            onCacheMiss();
                        }
                    });
        }

    }

    @Override
    public Subscription newAlbumRequest(AnimatedImageView imageView, PaletteObserver paletteObserver, ArtInfo artInfo, ArtworkType artworkType) {
        return new AlbumArtworkRequest(imageView, paletteObserver, artInfo, artworkType).start();
    }

    @Override
    public Subscription newAlbumRequest(AnimatedImageView imageView, PaletteObserver paletteObserver, long albumId, ArtworkType artworkType) {
        return new AlbumArtworkRequestWrapped(imageView, paletteObserver, albumId, artworkType).start();
    }

    @Override
    public Subscription newArtistRequest(AnimatedImageView imageView, PaletteObserver paletteObserver, ArtInfo artInfo, ArtworkType artworkType) {
        return new ArtistArtworkRequest(imageView, paletteObserver, artInfo, artworkType).start();
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
                .flatMap(new Func1<Artist, Observable<String>>() {
                    @Override
                    public Observable<String> call(Artist artist) {
                        String url = getBestImage(artist, !mPreferences.getBoolean(AppPreferences.WANT_LOW_RESOLUTION_ART, false));
                        if (!TextUtils.isEmpty(url)) {
                            return Observable.just(url);
                        } else {
                            Timber.v("ArtistApiRequest: No image urls for %s", artist.getName());
                            return Observable.error(new NullPointerException("No image urls for " + artist.getName()));
                        }
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
                            Timber.e("Api response does not contain mbid for %s", album.getName());
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
                ArtworkRequest2 request = new ArtworkRequest2(url, artworkType, listener);
                mVolleyQueue.add(request);
                // Here we take advantage of volleys coolest feature,
                // We have 2 types of images, a thumbnail and a larger image suitable for
                // fullscreen use. these are almost never required at the same time so we create
                // a second request. The cool part is volley wont actually download the image twice
                // this second request is attached to the first and processed afterwards
                // saving bandwidth and ensuring both kinds of images are available next time
                // we need them.
                final ArtworkType oppositeArtworkType = ArtworkType.opposite(artworkType);
                ArtworkRequest2.Listener oppositeListener = new ArtworkRequest2.Listener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        //pass
                    }
                    @Override
                    public void onResponse(Artwork artwork) {
                        putInDiskCache(getCacheKey(artInfo, oppositeArtworkType), artwork.bitmap);
                    }
                };
                ArtworkRequest2 oppositeRequest = new ArtworkRequest2(url, oppositeArtworkType, oppositeListener);
                mVolleyQueue.add(oppositeRequest);
            }
        });
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
                    if (subscriber.isUnsubscribed()) return;
                    // We create a faux ImageRequest so we can cheat and use it
                    // to processs the bitmap like a real network request
                    // this is not only easier but safer since all bitmap processing is serial.
                    ArtworkRequest2 request = new ArtworkRequest2("fauxrequest", artworkType, null);
                    Response<Artwork> result = request.parseNetworkResponse(response);
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
                                Timber.v("Adding %s to L2Cache", entry.getKey());
                                mL2Cache.putBitmap(entry.getKey(), entry.getValue());
                                continue;
                            }
                        } catch (InterruptedException ignored) {
                        }
                        diskCacheWorker.unsubscribe();
                        break;
                    }
                }
            });
        }
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

}
