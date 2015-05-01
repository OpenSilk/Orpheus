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

package org.opensilk.music.artwork.requestor;

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
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.artwork.ArtworkPreferences;
import org.opensilk.music.artwork.CrumbTrail;
import org.opensilk.music.artwork.Util;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.artwork.Artwork;
import org.opensilk.music.artwork.ArtworkRequest2;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.CacheMissException;
import org.opensilk.music.artwork.CacheArtworkResponse;
import org.opensilk.music.artwork.CoverArtJsonRequest;
import org.opensilk.music.artwork.ImageContainer;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.RequestKey;
import org.opensilk.music.artwork.cache.ArtworkCache;
import org.opensilk.music.artwork.cache.BitmapDiskCache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.Iterator;
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
    final ArtworkPreferences mPreferences;
    final ArtworkCache mL1Cache;

    final Map<RequestKey, IArtworkRequest> mActiveRequests = new LinkedHashMap<>(10);

    @Inject
    public ArtworkRequestManagerImpl(@ForApplication Context mContext,
                                     ArtworkPreferences mPreferences,
                                     ArtworkCache mL1Cache
    ) {
        this.mContext = mContext;
        this.mPreferences = mPreferences;
        this.mL1Cache = mL1Cache;
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
            addBreadcrumb(Util.getCacheKey(artInfo, artworkType));
        }

        @Override
        public void unsubscribe() {
            addBreadcrumb("unsubscribe");
            unsubscribed = true;
            if (subscription != null) {
                subscription.unsubscribe();
                subscription = null;
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
            Iterator<ImageContainer> ii = recipients.iterator();
            while (ii.hasNext()) {
                ImageContainer c = ii.next();
                if (c.isUnsubscribed()) {
                    ii.remove();
                } else {
                    c.setDefaultImage();
                }
            }
        }

        void onResponse(Artwork artwork, boolean fromCache, boolean shouldAnimate) {
            addBreadcrumb("onResponse("+fromCache+")");
            if (unsubscribed) return;
            Iterator<ImageContainer> ii = recipients.iterator();
            while (ii.hasNext()) {
                ImageContainer c = ii.next();
                if (c.isUnsubscribed()) {
                    ii.remove();
                } else {
                    c.setImageBitmap(artwork.bitmap, shouldAnimate);
                    c.notifyPaletteObserver(artwork.palette, shouldAnimate);
                }
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
                    .subscribe(new Action1<CacheArtworkResponse>() {
                        @Override
                        public void call(CacheArtworkResponse cr) {
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
            //TODO
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
            //TODO
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
    public Subscription newArtistRequest(AnimatedImageView imageView, PaletteObserver paletteObserver,
                                         ArtInfo artInfo, ArtworkType artworkType) {
        ImageContainer c = new ImageContainer(imageView, paletteObserver);
        RequestKey k = new RequestKey(artInfo, artworkType);
        queueRequest(c, k, false);
        return c;
    }

    @DebugLog
    public void evictL1() {
        mL1Cache.clearCache();
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

    public Observable<CacheArtworkResponse> createCacheObservable(final ArtInfo artInfo, final ArtworkType artworkType) {
        final String cacheKey = Util.getCacheKey(artInfo, artworkType);
        return Observable.create(new Observable.OnSubscribe<CacheArtworkResponse>() {
            @Override
            public void call(Subscriber<? super CacheArtworkResponse> subscriber) {
//                    Timber.v("Trying L1 for %s, from %s", cacheKey, Thread.currentThread().getName());
                Artwork artwork = mL1Cache.getArtwork(cacheKey);
                if (!subscriber.isUnsubscribed()) {
                    if (artwork != null) {
                        subscriber.onNext(new CacheArtworkResponse(artwork, true));
                        subscriber.onCompleted();
                    } else {
                        subscriber.onError(new CacheMissException());
                    }
                }
            }
        });
    }

}
