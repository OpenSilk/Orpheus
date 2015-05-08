/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.artwork.service;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import com.google.gson.Gson;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.artwork.R;
import org.opensilk.music.artwork.UtilsArt;
import org.opensilk.music.artwork.cache.BitmapLruCache;
import org.opensilk.music.model.ArtInfo;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Helper class for MusicPlaybackService to request and manage
 * artwork from the ArtworkProvider
 *
 * Created by drew on 3/23/14.
 */
@Singleton
public class ArtworkProviderHelper {
    private static final Object sDecodeLock = new Object();

    private final Context mContext;
    private final String mAuthority;
    private final BitmapLruCache mL1Cache;
    private final Gson mGson;

    @Inject
    public ArtworkProviderHelper(
            @ForApplication Context context,
            @Named("artworkauthority") String authority,
            @Named("helpercache") BitmapLruCache l1cache,
            Gson gson
    ) {
        mContext = context;
        mAuthority = authority;
        mL1Cache = l1cache;
        mGson = gson;
    }

    public Observable<Bitmap> getArtwork(final ArtInfo artInfo, final ArtworkType artworkType) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                if (artInfo == ArtInfo.NULLINSTANCE) {
                    subscriber.onNext(getDefaultArt());
                }
                final String cacheKey = UtilsArt.getCacheKey(artInfo, artworkType);
                final Uri artworkUri = ArtworkUris.createAlbumReq(mAuthority,
                        UtilsArt.base64EncodedJsonArtInfo(mGson, artInfo), artworkType);
                Bitmap bitmap = queryArtworkProvider(artworkUri, cacheKey);
                if (bitmap == null && artworkType == ArtworkType.LARGE) {
                    // Fullscreen not available try the thumbnail for a temp fix
                    final Uri artworkUri2 = ArtworkUris.createAlbumReq(mAuthority,
                            UtilsArt.base64EncodedJsonArtInfo(mGson, artInfo), ArtworkType.THUMBNAIL);
                    final String cacheKey2 = UtilsArt.getCacheKey(artInfo, ArtworkType.THUMBNAIL);
                    bitmap = queryArtworkProvider(artworkUri2, cacheKey2);
                }
                if (!subscriber.isUnsubscribed()) {
                    if (bitmap != null) {
                        subscriber.onNext(bitmap);
                        subscriber.onCompleted();
                    } else {
                        //send them default
                        subscriber.onNext(getDefaultArt());
                        //register content observer so if new art comes in we can update
                        //adding it to the subscriper so when upstream unsubscribes we wont
                        //leak the content observer
                        subscriber.add(new Notifyer(subscriber, artworkUri, cacheKey));
                    }
                }
            }
        });
    }

    /**
     * Queries ArtworkProvider for given uri, first checking local cache
     * @return Decoded bitmap
     */
    public Bitmap queryArtworkProvider(Uri artworkUri, String cacheKey) {
        Bitmap bitmap = mL1Cache.getBitmap(cacheKey);
        if (bitmap == null) {
            ParcelFileDescriptor pfd = null;
            try {
                pfd = mContext.getContentResolver().openFileDescriptor(artworkUri, "r");
                if (pfd != null) {
                    synchronized (sDecodeLock) {
                        try {
                            bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                        } catch (OutOfMemoryError e) {
                            bitmap = null;
                        }
                    }
                    if (bitmap != null) {
                        mL1Cache.putBitmap(cacheKey, bitmap);
                    }
                }
            } catch (Exception e) {
                Timber.w(e, "queryArtworkProvider()");
                bitmap = null;
            } finally {
                if (pfd != null) {
                    try {
                        pfd.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return bitmap;
    }

    @DebugLog
    public void evictL1() {
        mL1Cache.evictAll();
        Runtime.getRuntime().gc();
    }

    private Bitmap getDefaultArt() {
        return ((BitmapDrawable) mContext.getResources().getDrawable(R.drawable.default_artwork)).getBitmap();
    }

    class Notifyer extends ContentObserver implements Subscription {
        final Subscriber<? super Bitmap> subscriber;
        final Uri uri;
        final String cacheKey;

        public Notifyer(Subscriber<? super Bitmap> subscriber, Uri uri, String cacheKey) {
            super(null);
            this.subscriber = subscriber;
            this.uri = uri;
            this.cacheKey = cacheKey;
            init();
        }

        void init() {
            mContext.getContentResolver().registerContentObserver(uri, false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            Bitmap bitmap = queryArtworkProvider(uri, cacheKey);
            if (!isUnsubscribed()) {
                if (bitmap != null) {
                    subscriber.onNext(bitmap);
                }
                subscriber.onCompleted();
                unsubscribe();
            }
        }

        @Override
        public void unsubscribe() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public boolean isUnsubscribed() {
            return subscriber.isUnsubscribed();
        }
    }

}
