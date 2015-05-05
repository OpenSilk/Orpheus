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
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;

import com.google.gson.Gson;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.artwork.shared.ArtworkPreferences;
import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.artwork.CrumbTrail;
import org.opensilk.music.artwork.Util;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.artwork.Artwork;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.ImageContainer;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.RequestKey;
import org.opensilk.music.artwork.cache.ArtworkCache;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/21/14.
 */
@Singleton
public class ArtworkRequestManagerImpl implements ArtworkRequestManager {
    final static boolean DROP_CRUMBS = true;
    final static boolean VERIFY_THREAD = true;

    final Context mContext;
    final ArtworkPreferences mPreferences;
    final ArtworkCache mL1Cache;
    final Gson mGson;
    final String mAuthority;

    final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    final Scheduler scheduler = Schedulers.computation();
    final Scheduler oScheduler = AndroidSchedulers.handlerThread(mMainThreadHandler);
    final Map<RequestKey, IArtworkRequest> mActiveRequests = new LinkedHashMap<>(10);


    @Inject
    public ArtworkRequestManagerImpl(@ForApplication Context mContext,
                                     ArtworkPreferences mPreferences,
                                     ArtworkCache mL1Cache,
                                     Gson mGson,
                                     @Named("artworkauthority") String authority
    ) {
        this.mContext = mContext;
        this.mPreferences = mPreferences;
        this.mL1Cache = mL1Cache;
        this.mGson = mGson;
        this.mAuthority = authority;
    }

    interface IArtworkRequest {
        void addRecipient(ImageContainer c);
    }

    /*
     * All interaction with this class must be on the main thread to avoid synchronization errors
     */
    abstract class BaseArtworkRequest implements Subscription, IArtworkRequest, ImageContainer.UnsubscribeListener {
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
            c.setUnsubscribeListener(this);
            if (!inflight) {
                inflight = true;
                start();
            } else {
                c.setDefaultImage();
            }
        }

        @Override
        public void onContainerUnsubscribed(ImageContainer c) {
            addBreadcrumb("onContainerUnsubscribed");
            c.setUnsubscribeListener(null);
            recipients.remove(c);
            if (recipients.isEmpty()) {
                unsubscribe();
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
            unregisterContentObserver();
            printTrail();
        }

        void setDefaultImage() {
            addBreadcrumb("setDefaultImage");
            if (unsubscribed) return;
            for (ImageContainer c : recipients) {
                c.setDefaultImage();
            }
        }

        void onResponse(Artwork artwork, boolean fromCache, boolean shouldAnimate) {
            addBreadcrumb("onResponse(%s)", fromCache);
            if (unsubscribed) return;
            for (ImageContainer c : recipients) {
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
            final String cacheKey = Util.getCacheKey(artInfo, artworkType);
            Artwork artwork = mL1Cache.getArtwork(cacheKey);
            if (artwork != null) {
                addBreadcrumb("tryForCache hit");
                onResponse(artwork, true, false);
                onComplete();
            } else {
                onCacheMiss();
            }
        }

        void onCacheMiss() {
            addBreadcrumb("onCacheMiss");
            setDefaultImage();
            tryForProvider(false);
        }

        abstract boolean validateArtInfo();
        abstract Uri getUri();

        void tryForProvider(final boolean secondTry) {
            final Uri uri = getUri();
            final String cacheKey = Util.getCacheKey(artInfo, artworkType);
            subscription = createArtworkProviderObservable(uri, cacheKey)
                    .subscribeOn(scheduler)
                    .observeOn(oScheduler)
                    .subscribe(new Action1<Artwork>() {
                        @Override
                        public void call(Artwork artwork) {
                            addBreadcrumb("tryForProvider hit");
                            onResponse(artwork, false, true);
                            onComplete();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            addBreadcrumb("tryForProvider(%s) miss", secondTry);
                            if (throwable instanceof FileNotFoundException && !secondTry) {
                                onProviderMiss(uri);
                            } else {
                                onComplete();
                            }
                        }
                    });
        }

        void onProviderMiss(Uri uri) {
            registerContentObserver(uri);
            //Make sure we dont stick around if we aren't notified
            subscription = Observable.timer(5, TimeUnit.MINUTES)
                    .observeOn(oScheduler)
                    .subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            onComplete();
                        }
                    });
        }

        void registerContentObserver(Uri uri) {
            addBreadcrumb("registerContentObserver(%s)", uri);
            mContext.getContentResolver().registerContentObserver(uri, false, contentObserver);
        }

        void unregisterContentObserver() {
            addBreadcrumb("unregisterContentObserver()");
            try {
                mContext.getContentResolver().unregisterContentObserver(contentObserver);
            } catch (Exception ignored) {/*safety i dont think anything is thrown*/}
        }

        final ContentObserver contentObserver = new ContentObserver(mMainThreadHandler) {
            @Override
            public void onChange(boolean selfChange) {
                addBreadcrumb("ContentObserver#onChange");
                if (subscription != null) {
                    //Unsubscribe the timer
                    subscription.unsubscribe();
                }
                unregisterContentObserver();
                tryForProvider(true);
            }
        };

        CrumbTrail crumbTrail;
        void addBreadcrumb(String crumb, Object... args) {
            if (VERIFY_THREAD) assertMainThread();
            if (!DROP_CRUMBS) return;
            if (crumbTrail == null)
                crumbTrail = new CrumbTrail();
            crumbTrail.drop(String.format(Locale.US, crumb, args));
        }

        void printTrail() {
            if (!DROP_CRUMBS) return;
            crumbTrail.follow();
        }

        void assertMainThread() {
            if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                throw new IllegalStateException("Called from wrong thread " + Thread.currentThread().getName());
            }
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
        Uri getUri() {
            return ArtworkUris.createArtistReq(mAuthority, Util.base64EncodedJsonArtInfo(mGson, artInfo), artworkType);
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
        Uri getUri() {
            return ArtworkUris.createAlbumReq(mAuthority, Util.base64EncodedJsonArtInfo(mGson, artInfo), artworkType);
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

    Observable<Artwork> createArtworkProviderObservable(final Uri uri, final String cacheKey) {
        return Observable.create(new Observable.OnSubscribe<Artwork>() {
            @Override
            public void call(Subscriber<? super Artwork> subscriber) {
                ParcelFileDescriptor pfd = null;
                try {
                    pfd = mContext.getContentResolver().openFileDescriptor(uri, "r");
                    if (pfd == null) throw new FileNotFoundException("Null descriptor");
                    Bitmap bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                    if (bitmap == null) throw new NullPointerException("Error decoding bitmap");
                    Palette palette = new Palette.Builder(bitmap).generate();
                    Artwork artwork = new Artwork(bitmap, palette);
                    //always add to cache
                    mL1Cache.putArtwork(cacheKey, artwork);
                    if (subscriber.isUnsubscribed()) return;
                    subscriber.onNext(artwork);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    if (subscriber.isUnsubscribed()) return;
                    subscriber.onError(e);
                } finally {
                    if (pfd != null) {
                        //ICS at least is not a Closeable
                        try { pfd.close(); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

}
