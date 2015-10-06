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

package org.opensilk.music.artwork.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import org.apache.commons.io.IOUtils;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.artwork.Constants;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.fetcher.ArtworkFetcherManager;
import org.opensilk.music.artwork.fetcher.ArtworkFetcherService;
import org.opensilk.music.model.ArtInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Scheduler;
import rx.functions.Action0;
import timber.log.Timber;

/**
 * Created by drew on 3/25/14.
 */
public class ArtworkProvider extends ContentProvider {
    private static final String TAG = ArtworkProvider.class.getSimpleName();

    final Scheduler mScheduler = Constants.ARTWORK_SCHEDULER;

    @Inject @Named("artworkauthority") String mAuthority;
    @Inject BitmapDiskCache mL2Cache;

    private UriMatcher mUriMatcher;

    @Override
    //@DebugLog
    public boolean onCreate() {
        DaggerService.<ArtworkComponent>getDaggerComponent(getContext()).inject(this);
        mUriMatcher = ArtworkUris.makeMatcher(mAuthority);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("query not implemented");
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case 1:
                return "image/*";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Provider is read only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Provider is read only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Provider is read only");
    }

    @Override
    //@DebugLog
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Provider is read only");
        }
        switch (mUriMatcher.match(uri)) {
            case ArtworkUris.MATCH.ARTINFO: {
                final ArtInfo artInfo = ArtInfo.fromUri(uri);
                final ParcelFileDescriptor pfd = getArtwork(uri, artInfo);
                if (pfd != null) {
                    return pfd;
                }
                break;
            }
        }
        throw new FileNotFoundException("Could not obtain image from cache");
    }

    public ParcelFileDescriptor getArtwork(Uri uri, ArtInfo artInfo) {
        ParcelFileDescriptor pfd = pullSnapshot(artInfo.cacheKey());
        if (pfd != null) {
            return pfd;
        }
        OptionalBitmap bitmap = null;
        try {
            //not in cache, make a new request and wait for it to come in.
            final ArtworkFetcherService.Connection serviceConnection =
                    ArtworkFetcherService.bindService(getContext());
            final ArtworkFetcherService.Binder binder = serviceConnection.getService();
            final BlockingQueue<OptionalBitmap> queue = new LinkedBlockingQueue<>(1);
            final ArtworkFetcherManager.CompletionListener listener =
                    new ArtworkFetcherManager.CompletionListener() {
                        @Override public void onError(Throwable e) {
                            queue.offer(new OptionalBitmap(null));
                        }
                        @Override public void onNext(Bitmap o) {
                            queue.offer(new OptionalBitmap(o));
                        }
                        @Override public void onCompleted() { }
            };
            binder.newRequest(artInfo, listener);
            bitmap = queue.take();
            if (bitmap.hasBitmap()) {
                return createPipe(mL2Cache.bitmapToBytes(bitmap.getBitmap()));
            } else {
                return null;
            }
        } catch (InterruptedException e) {
            Timber.w(e, "getArtwork(%s)", uri);
            return null;
        } finally {
            if (bitmap != null) bitmap.recycle();
        }
    }

    private ParcelFileDescriptor pullSnapshot(String cacheKey) {
        final byte[] snapshot = mL2Cache.getBytes(cacheKey);
        if (snapshot != null) {
            return createPipe(snapshot);
        } else {
            return null;
        }
    }

    private ParcelFileDescriptor createPipe(final byte[] bytes) {
        try {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
            final ParcelFileDescriptor in = pipe[0];
            final Scheduler.Worker worker = mScheduler.createWorker();
            worker.schedule(new Action0() {
                @Override
                public void call() {
                    try {
                        IOUtils.write(bytes, out);
                    } catch (IOException e) {
                        Timber.e(e, "ParcelFileDescriptorPipe");
                    } finally {
                        IOUtils.closeQuietly(out);
                        worker.unsubscribe();
                    }
                }
            });
            return in;
        } catch (IOException e) {
            Timber.w(e, "createPipe");
        }
        return null;
    }

    /** Wrapper so we can notify error */
    private static final class OptionalBitmap {
        final Bitmap bitmap;
        public OptionalBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
        boolean hasBitmap() {
            return bitmap != null;
        }
        Bitmap getBitmap() {
            return bitmap;
        }
        void recycle() {
            if (hasBitmap()) bitmap.recycle();
        }
    }

}
