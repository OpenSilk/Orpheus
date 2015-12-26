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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.artwork.R;
import org.opensilk.music.artwork.cache.BitmapLruCache;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;
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
    private final BitmapLruCache mL1Cache;

    @Inject
    public ArtworkProviderHelper(
            @ForApplication Context context,
            @Named("helpercache") BitmapLruCache l1cache
    ) {
        mContext = context;
        mL1Cache = l1cache;
    }

    public Observable<Bitmap> getArtwork(final @NonNull Uri uri) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                final String cacheKey = uri.toString();
                Bitmap bitmap = mL1Cache.getBitmap(cacheKey);
                if (bitmap == null) {
                    ParcelFileDescriptor pfd = null;
                    try {
                        if (VersionUtils.hasKitkat()) {
                            final CancellationSignal cancellationSignal = new CancellationSignal();
                            subscriber.add(Subscriptions.create(new Action0() {
                                @Override @TargetApi(19)
                                public void call() {
                                    cancellationSignal.cancel();
                                }
                            }));
                            pfd = getParcelFileDescriptior(uri, cancellationSignal);
                        } else {
                            pfd = getParcelFileDescriptior(uri);
                        }
                        if (pfd != null) {
                            synchronized (sDecodeLock) {
                                try {
                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                                    bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
                                } catch (OutOfMemoryError e) {
                                    bitmap = null;
                                }
                            }
                            if (bitmap != null) {
                                mL1Cache.putBitmap(cacheKey, bitmap);
                            }
                        }
                    } catch (Exception e) {
                        Timber.w(e, "queryArtworkProvider(%s) error", cacheKey);
                        bitmap = null;
                    } finally {
                        try {
                            if (pfd != null) pfd.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
                if (!subscriber.isUnsubscribed()) {
                    if (bitmap != null) {
                        subscriber.onNext(bitmap);
                    } else {
                        subscriber.onNext(getDefaultArt());
                    }
                    subscriber.onCompleted();
                }
            }
        });
    }

    public ParcelFileDescriptor getParcelFileDescriptior(@NonNull Uri artworkUri) {
        try {
            return mContext.getContentResolver().openFileDescriptor(artworkUri, "r");
        } catch (FileNotFoundException ignored) {
            Timber.i("queryArtworkProvider(%s) provider miss", artworkUri);
            return null;
        }
    }

    @TargetApi(19)
    public ParcelFileDescriptor getParcelFileDescriptior(@NonNull Uri artworkUri, CancellationSignal signal) {
        try {
            return mContext.getContentResolver().openFileDescriptor(artworkUri, "r", signal);
        } catch (FileNotFoundException ignored) {
            Timber.i("queryArtworkProvider(%s) provider miss", artworkUri);
            return null;
        }
    }

    @DebugLog
    public void evictL1() {
        mL1Cache.evictAll();
        Runtime.getRuntime().gc();
    }

    private Bitmap getDefaultArt() {
        return ((BitmapDrawable) ContextCompat.getDrawable(mContext, R.drawable.default_artwork)).getBitmap();
    }

    public @NonNull CacheBitmap getCachedOrDefault(@Nullable Uri uri) {
        boolean fromCache = true;
        Bitmap bitmap = null;
        if (uri != null) {
            bitmap = mL1Cache.getBitmap(uri.toString());
        }
        if (bitmap == null) {
            fromCache = false;
            bitmap = getDefaultArt();
        }
        return new CacheBitmap(fromCache, bitmap);
    }

    public static final class CacheBitmap {
        final boolean fromCache;
        final Bitmap bitmap;

        public CacheBitmap(boolean fromCache, Bitmap bitmap) {
            this.fromCache = fromCache;
            this.bitmap = bitmap;
        }

        public boolean fromCache() {
            return fromCache;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }
    }

}
