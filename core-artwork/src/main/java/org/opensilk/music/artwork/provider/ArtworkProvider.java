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
import android.support.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.fetcher.ArtworkFetcher;
import org.opensilk.music.artwork.fetcher.ArtworkFetcherService;
import org.opensilk.music.artwork.fetcher.CompletionListener;
import org.opensilk.music.model.ArtInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import rx.Scheduler;
import rx.functions.Action0;
import timber.log.Timber;

/**
 * Created by drew on 3/25/14.
 */
public class ArtworkProvider extends ContentProvider {
    private static final String TAG = ArtworkProvider.class.getSimpleName();

    @Inject @Named("artworkauthority") String mAuthority;
    @Inject @Named("artworkscheduler") Scheduler mScheduler;
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
            case ArtworkUris.MATCH.ARTINFO:
                return "image/png";
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
    @DebugLog
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Provider is read only");
        }
        switch (mUriMatcher.match(uri)) {
            case ArtworkUris.MATCH.ARTINFO: {
                final ArtInfo artInfo = ArtInfo.fromUri(uri);
                final ParcelFileDescriptor pfd = getArtwork(artInfo);
                if (pfd != null) {
                    return pfd;
                }
                break;
            }
        }
        throw new FileNotFoundException("Could not obtain image from cache or network");
    }

    public @Nullable ParcelFileDescriptor getArtwork(ArtInfo artInfo) {
        ParcelFileDescriptor pfd = createPipe(artInfo);
        if (pfd == null) {
            pfd = createPipe2(artInfo);
        }
        return pfd;
    }

    /**
     * Pulls bitmap from diskcache
     */
    private @Nullable ParcelFileDescriptor createPipe(ArtInfo artInfo) {
        final byte[] bytes = mL2Cache.getBytes(artInfo.cacheKey());
        if (bytes == null) {
            return null;
        }
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
            Timber.w(e, "createPipe(%s)", artInfo);
            return null;
        }
    }

    /**
     * Eagerly creates a pipe, then blocks on a background thread while we
     * wait for the fetcher to return the bitmap, simply closing the pipe
     * if no art was found
     */
    private @Nullable ParcelFileDescriptor createPipe2(final ArtInfo artInfo) {
        try {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
            final ParcelFileDescriptor in = pipe[0];
            final Scheduler.Worker worker = mScheduler.createWorker();
            worker.schedule(new Action0() {
                @Override
                public void call() {
                    OptionalBitmap bitmap = null;
                    try {
                        //make a new request and wait for it to come in.
                        final ArtworkFetcher binder = getArtworkFetcher();
                        final BlockingQueue<OptionalBitmap> queue = new LinkedBlockingQueue<>(1);
                        final CompletionListener listener =
                                new CompletionListener() {
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
                            IOUtils.write(mL2Cache.bitmapToBytes(bitmap.getBitmap()), out);
                        }
                    } catch (InterruptedException|IOException e) {
                        Timber.w(e, "createPipe2(%s)", artInfo);
                    } finally {
                        if (bitmap != null) bitmap.recycle();
                        IOUtils.closeQuietly(out);
                        worker.unsubscribe();
                    }
                }
            });
            return in;
        } catch (IOException e) {
            Timber.w(e, "createPipe2(%s)", artInfo);
            return null;
        }
    }

    //allow tests to override
    protected ArtworkFetcher getArtworkFetcher() throws InterruptedException {
        final ArtworkFetcherService.Connection serviceConnection =
                ArtworkFetcherService.bindService(getContext());
        return serviceConnection.getService();
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
