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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.ContextCompat;

import org.opensilk.common.core.dagger2.ForApplication;
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

    @Inject
    public ArtworkProviderHelper(
            @ForApplication Context context,
            @Named("artworkauthority") String authority,
            @Named("helpercache") BitmapLruCache l1cache
    ) {
        mContext = context;
        mAuthority = authority;
        mL1Cache = l1cache;
    }

    public Observable<Bitmap> getArtwork(final Uri uri) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                Bitmap bitmap = queryArtworkProvider(uri, uri.toString());
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
            } catch (FileNotFoundException ignored) {
                Timber.i("queryArtworkProvider(%s) provider miss", cacheKey);
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
        return bitmap;
    }

    @DebugLog
    public void evictL1() {
        mL1Cache.evictAll();
        Runtime.getRuntime().gc();
    }

    private Bitmap getDefaultArt() {
        return ((BitmapDrawable) ContextCompat.getDrawable(mContext, R.drawable.default_artwork)).getBitmap();
    }

    public CacheBitmap getCachedOrDefault(Uri uri) {
        boolean fromCache = true;
        Bitmap bitmap = mL1Cache.getBitmap(uri.toString());
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
