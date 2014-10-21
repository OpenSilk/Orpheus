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
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.andrew.apollo.model.LocalAlbum;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.RequestFuture;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.artwork.cache.BitmapLruCache;
import org.opensilk.music.ui2.loader.AlbumArtInfoLoader;
import org.opensilk.music.ui2.loader.RxCursorLoader;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.lang.ref.WeakReference;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.umass.lastfm.Album;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.MusicEntry;
import de.umass.lastfm.opensilk.AlbumRequest;
import de.umass.lastfm.opensilk.Fetch;
import de.umass.lastfm.opensilk.MusicEntryResponseCallback;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/21/14.
 */
@Singleton
public class RxAlbumRequest {

    final Context mContext;
    final AppPreferences mPreferences;
    final BitmapLruCache mL1Cache;
    final BitmapDiskLruCache mL2Cache;
    final RequestQueue mVolleyQueue;

    @Inject
    public RxAlbumRequest(@ForApplication Context mContext, AppPreferences mPreferences,
                          BitmapLruCache mL1Cache, BitmapDiskLruCache mL2Cache,
                          RequestQueue mVolleyQueue) {
        this.mContext = mContext;
        this.mPreferences = mPreferences;
        this.mL1Cache = mL1Cache;
        this.mL2Cache = mL2Cache;
        this.mVolleyQueue = mVolleyQueue;
    }

    public class Req {
        WeakReference<ImageView> imageViewWeakReference;
        ArtInfo artInfo;
        ArtworkType artworkType;
        Subscription subscription;

        public Req(ImageView imageView, ArtInfo artInfo, ArtworkType artworkType) {
            this.imageViewWeakReference = new WeakReference<>(imageView);
            this.artInfo = artInfo;
            this.artworkType = artworkType;
        }

        public void tryForCache() {
            subscription = createCacheObservable(artInfo, artworkType)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                            imageViewWeakReference.get().setImageBitmap(bitmap);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            if (throwable instanceof CacheMissException) {
                                onCacheMiss();
                            }
                        }
                    });
        }

        void onCacheMiss() {
            //TODO set default image
            //TODO check mediastore
            tryForNetwork();
        }

        public void tryForNetwork() {
            subscription = createNetworkObservable(artInfo, artworkType)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                            imageViewWeakReference.get().setImageBitmap(bitmap);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            onNetworkMiss();
                        }
                    });
        }

        void onNetworkMiss() {

        }
    }

    public Req newRequest(ImageView imageView, ArtInfo artInfo, ArtworkType artworkType) {
        return new Req(imageView, artInfo, artworkType);
    }

    public Observable<Bitmap> createCacheObservable(final ArtInfo artInfo, final ArtworkType artworkType) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
                @Override
                public void call(Subscriber<? super Bitmap> subscriber) {
                    Timber.v("Trying L1 for %s, %s", artInfo.albumName, Thread.currentThread().getName());
                    Bitmap bitmap = mL1Cache.getBitmap(getCacheKey(artInfo, artworkType));
                    if (!subscriber.isUnsubscribed()) {
                        if (bitmap != null) {
                            subscriber.onNext(bitmap);
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new CacheMissException());
                        }
                    }
                }
            })
            // We missed the l1cache try l2
            .onErrorResumeNext(new Func1<Throwable, Observable<? extends Bitmap>>() {
                @Override
                public Observable<? extends Bitmap> call(Throwable throwable) {
                    if (throwable instanceof CacheMissException) {
                        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
                            @Override
                            public void call(Subscriber<? super Bitmap> subscriber) {
                                Timber.v("Trying L2 for %s, %s", artInfo.albumName, Thread.currentThread().getName());
                                Bitmap bitmap = mL2Cache.getBitmap(getCacheKey(artInfo, artworkType));
                                if (!subscriber.isUnsubscribed()) {
                                    if (bitmap != null) {
                                        subscriber.onNext(bitmap);
                                        subscriber.onCompleted();
                                    } else {
                                        subscriber.onError(new CacheMissException());
                                    }
                                }
                            }
                        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
                    } else {
                        return Observable.error(throwable);
                    }
                }
            });
    }

    public Observable<Bitmap> createCacheObservable(final long id, final ArtworkType artworkType) {
        // first we have to get the artinfo for the album
        return new AlbumArtInfoLoader(mContext, new long[]{id}).createObservable()
                // then we can creat the cache observable with the artinfo
            .flatMap(new Func1<ArtInfo, Observable<Bitmap>>() {
                @Override
                public Observable<Bitmap> call(final ArtInfo artInfo) {
                    return createCacheObservable(artInfo, artworkType);
                }
            });
    }

    public Observable<Bitmap> createNetworkObservable(final ArtInfo artInfo, final ArtworkType artworkType) {
        return createApiRequestObservable(artInfo)
                .flatMap(new Func1<Album, Observable<String>>() {
                    @Override
                    public Observable<String> call(Album album) {
                        Timber.v("Creating CoverArtRequest %s", Thread.currentThread().getName());
                        //TODO prefs highres
                        return createCoverArtRequestObservable(album);
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Observable<String>>() {
                    @Override
                    public Observable<String> call(Throwable throwable) {
                        if (throwable instanceof CoverArtRequestException) {
                            Timber.v("Handling CoverArtRequestException %s", Thread.currentThread().getName());
                            Album album = ((CoverArtRequestException) throwable).album;
                            String url = getBestImage(album, true);//TODO prefs
                            if (!TextUtils.isEmpty(url)) {
                                return Observable.just(url);
                            } else {
                                return Observable.error(new NullPointerException("Couldn't get LastFm artwork url"));
                            }
                        } else {
                            return Observable.error(throwable);
                        }
                    }
                })
                .flatMap(new Func1<String, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(String s) {
                        return createImageRequestObservable(s, artworkType);
                    }
                });
    }

    public Observable<Album> createApiRequestObservable(final ArtInfo artInfo) {
        return Observable.create(new Observable.OnSubscribe<Album>() {
            @Override
            public void call(Subscriber<? super Album> subscriber) {
                mVolleyQueue.add(Fetch.albumInfo(artInfo.artistName, artInfo.albumName,
                        new AlbumResponseListener(subscriber), Request.Priority.NORMAL));
            }
        });
    }

    public Observable<String> createCoverArtRequestObservable(final Album album) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                mVolleyQueue.add(new CoverArtRequest(album, new CoverArtRequestListener(subscriber)));
            }
        });
    }

    public Observable<Bitmap> createImageRequestObservable(final String url, final ArtworkType artworkType) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                ImageResponseListener listener = new ImageResponseListener(subscriber);
                ArtworkImageRequest request = new ArtworkImageRequest(url, listener, artworkType, listener);
                mVolleyQueue.add(request);
            }
        });
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
                Timber.i("Found " + q.toString() + " url for " + e.getName());
                return url;
            }
        }
        return null;
    }

    static class CacheMissException extends Exception {
        public CacheMissException() {
        }
    }

    static class CoverArtRequestException extends VolleyError {
        final Album album;
        CoverArtRequestException(Album album) {
            super();
            this.album = album;
        }
    }

    /**
     * Response listener for Fetch.albumInfo
     */
    static class AlbumResponseListener implements MusicEntryResponseCallback<Album> {
        final Subscriber<? super Album> subscriber;

        AlbumResponseListener(Subscriber<? super Album> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        //@DebugLog
        public void onResponse(Album response) {
            if (!TextUtils.isEmpty(response.getMbid())) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(response);
                    subscriber.onCompleted();
                }
            } else {
                onErrorResponse(new VolleyError("Unknown mbid"));
            }
        }

        @Override
        //@DebugLog
        public void onErrorResponse(VolleyError error) {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onError(error);
            }
        }
    }

    /**
     *
     */
    static class CoverArtRequest extends Request<String> {
        final Album album;
        final CoverArtRequestListener listener;

        CoverArtRequest(Album album, CoverArtRequestListener listener) {
            super(Method.HEAD, makeUrl(album.getMbid()), listener);
            this.album = album;
            this.listener = listener;
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            Timber.v("CoverArtRequest header=%s", response.headers.toString());
            String location = response.headers.get("Location");
            Timber.v("CoverArtRequest Location=%s", location);
            if (!TextUtils.isEmpty(location)) {
                return Response.success(location, HttpHeaderParser.parseCacheHeaders(response));
            } else {
                return Response.error(new CoverArtRequestException(album));
            }
        }

        @Override
        protected void deliverResponse(String response) {
            listener.onResponse(response);
        }

        private static final String API_ROOT = "http://coverartarchive.org/release/";
        private static final String FRONT_COVER_URL = API_ROOT+"%s/front";
        private static String makeUrl(String mbid) {
            return String.format(Locale.US, FRONT_COVER_URL, mbid);
        }
    }

    /**
     *
     */
    static class CoverArtRequestListener implements Response.Listener<String>, Response.ErrorListener {
        final Subscriber<? super String> subscriber;

        CoverArtRequestListener(Subscriber<? super String> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onResponse(String response) {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(response);
                subscriber.onCompleted();
            }
        }

        @Override
        public void onErrorResponse(VolleyError volleyError) {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onError(volleyError);
            }
        }
    }

    /**
     *
     */
    static class ImageResponseListener implements Response.Listener<Bitmap>, Response.ErrorListener {
        Subscriber<? super Bitmap> subscriber;

        ImageResponseListener(Subscriber<? super Bitmap> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onResponse(Bitmap response) {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(response);
                subscriber.onCompleted();
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onError(error);
            }
        }

    }

}
