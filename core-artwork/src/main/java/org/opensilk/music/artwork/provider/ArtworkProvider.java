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

import android.annotation.TargetApi;
import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
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
    public String getType(@NonNull Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case ArtworkUris.MATCH.ARTINFO:
                return "image/png";
        }
        return null;
    }

    @Override
    @DebugLog
    public void onTrimMemory(int level) {
        if (level >= TRIM_MEMORY_BACKGROUND) {
            mL2Cache.onTrimMemory();
        }
    }

    @Override
    public @Nullable ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Provider is read only");
        }
        ParcelFileDescriptor pfd = null;
        switch (mUriMatcher.match(uri)) {
            case ArtworkUris.MATCH.ARTINFO: {
                final ArtInfo artInfo = ArtInfo.fromUri(uri);
                pfd = createPipe(artInfo, null);
                if (pfd == null) {
                    pfd = createPipe2(artInfo, null);
                }
                break;
            }
        }
        if (pfd == null) {
            throw new FileNotFoundException("Could not obtain image from cache or network");
        }
        return pfd;
    }

    /**
     * Copied from super
     */
    @Override @TargetApi(19)
    public @Nullable AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri, @NonNull String mimeTypeFilter, Bundle opts, CancellationSignal signal) throws FileNotFoundException {
        if ("*/*".equals(mimeTypeFilter)) {
            // If they can take anything, the untyped open call is good enough.
            return openAssetFile(uri, "r", signal);
        }
        String baseType = getType(uri);
        if (baseType != null && ClipDescription.compareMimeTypes(baseType, mimeTypeFilter)) {
            // Use old untyped open call if this provider has a type for this
            // URI and it matches the request.
            return openAssetFile(uri, "r", signal);
        }
        throw new FileNotFoundException("Can't open " + uri + " as type " + mimeTypeFilter);
    }

    /**
     * Copied from super
     */
    @Override @TargetApi(19)
    public @Nullable AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode, CancellationSignal signal) throws FileNotFoundException {
        ParcelFileDescriptor fd = openFile(uri, mode, signal);
        return fd != null ? new AssetFileDescriptor(fd, 0, -1) : null;
    }

    @Override @TargetApi(19)
    public @Nullable ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode, CancellationSignal signal) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Provider is read only");
        }
        ParcelFileDescriptor pfd = null;
        switch (mUriMatcher.match(uri)) {
            case ArtworkUris.MATCH.ARTINFO: {
                final ArtInfo artInfo = ArtInfo.fromUri(uri);
                final Cancellation cancellation = new CancellationK(signal);
                pfd = createPipe(artInfo, cancellation);
                if (pfd == null && !cancellation.isCanceled()) {
                    pfd = createPipe2(artInfo, cancellation);
                }
                break;
            }
        }
        if (pfd == null) {
            throw new FileNotFoundException("Could not obtain image from cache or network");
        }
        return pfd;
    }

    /**
     * Pulls bitmap from diskcache
     */
    private @Nullable ParcelFileDescriptor createPipe(final ArtInfo artInfo, final @Nullable Cancellation cancellation) {
        final byte[] bytes = mL2Cache.getBytes(artInfo.cacheKey());
        if (bytes == null) {
            return null;
        }
        if (cancellation != null && cancellation.isCanceled()) {
            Timber.i("createPipe(%s) CANCELED", artInfo);
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
                        out.flush();
                    } catch (IOException e) {
                        Timber.w("createPipe(%s) %s", artInfo, e.getMessage());
                    } finally {
                        IOUtils.closeQuietly(out);
                        worker.unsubscribe();
                    }
                }
            });
            return in;
        } catch (IOException e) {
            Timber.e(e, "createPipe(%s) %s", artInfo, e.getMessage());
            return null;
        }
    }

    /**
     * Eagerly creates a pipe, then blocks on a background thread while we
     * wait for the fetcher to return the bitmap, simply closing the pipe
     * if no art was found
     */
    private @Nullable ParcelFileDescriptor _createPipe2(final ArtInfo artInfo, final @Nullable Cancellation cancellation) {
        try {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
            final ParcelFileDescriptor in = pipe[0];
            final Scheduler.Worker worker = mScheduler.createWorker();
            worker.schedule(new Action0() {
                @Override
                public void call() {
                    OptionalBitmap bitmap = null;
                    ArtworkFetcherService.Connection binder = null;
                    try {
                        //make a new request and wait for it to come in.
                        binder = ArtworkFetcherService.bindService(getContext());
                        final BlockingQueue<OptionalBitmap> queue = new LinkedBlockingQueue<>(1);
                        final CompletionListener listener =
                                new CompletionListener() {
                                    @Override public void onError(Throwable e) {
                                        Timber.w("onError(%s) %s", artInfo, e.getMessage());
                                        queue.offer(new OptionalBitmap(null));
                                    }
                                    @Override public void onNext(Bitmap o) {
                                        queue.offer(new OptionalBitmap(o));
                                    }
                                };
                        if (!binder.getService().newRequest(artInfo, listener)) {
                            return;
                        }
                        if (cancellation != null) {
                            final ArtworkFetcherService.Connection finalBinder = binder;
                            cancellation.setCancelAction(new Action0() {
                                @Override
                                public void call() {
                                    Timber.d("createPipe2(%s) CANCELED INFLIGHT", artInfo);
                                    finalBinder.getService().cancelRequest(artInfo);
                                    queue.offer(new OptionalBitmap(null));
                                }
                            });
                        }
                        bitmap = queue.take();
                        if (bitmap.hasBitmap()) {
                            byte[] bytes = mL2Cache.bitmapToBytes(bitmap.getBitmap());
                            IOUtils.write(bytes, out);
                            out.flush();
                        }
                    } catch (InterruptedException|IOException e) {
                        Timber.w("createPipe2(%s) %s", artInfo, e.getMessage());
                        if (binder != null) {
                            binder.getService().cancelRequest(artInfo);
                        }
                    } finally {
                        if (bitmap != null) bitmap.recycle();
                        IOUtils.closeQuietly(binder);
                        IOUtils.closeQuietly(out);
                        worker.unsubscribe();
                    }
                }
            });
            return in;
        } catch (IOException e) {
            Timber.e(e, "createPipe2(%s) %s", artInfo, e.getMessage());
            return null;
        }
    }

    /**
     * This version of createPipe2 blocks on the binder thread, this is to take advantage of
     * cancellations propagated to us from {@link org.opensilk.music.artwork.glide.ArtInfoRequestStreamFetcherK},
     * since glide uses a bounded thread pool we shouldn't be at risk of blocking other binder
     * transactions too badly. This also prevents spinning up a ton of new threads, as we are likely
     * to reuse a thread from the network request to do the copy.
     */
    private @Nullable ParcelFileDescriptor createPipe2(final ArtInfo artInfo, final @Nullable Cancellation cancellation) {
        OptionalBitmap bitmap = null;
        ArtworkFetcherService.Connection binder = null;
        try {
            //make a new request and wait for it to come in.
            binder = ArtworkFetcherService.bindService(getContext());
            if (cancellation != null && cancellation.isCanceled()) {
                Timber.d("createPipe2(%s) canceled");
                return null;
            }
            final BlockingQueue<OptionalBitmap> queue = new LinkedBlockingQueue<>(1);
            final CompletionListener listener =
                    new CompletionListener() {
                        @Override public void onError(Throwable e) {
                            Timber.w("onError(%s) %s", artInfo, e.getMessage());
                            queue.offer(new OptionalBitmap(null));
                        }
                        @Override public void onNext(Bitmap o) {
                            queue.offer(new OptionalBitmap(o));
                        }
                    };
            if (!binder.getService().newRequest(artInfo, listener)) {
                return null;
            }
            if (cancellation != null) {
                final ArtworkFetcherService.Connection finalBinder = binder;
                cancellation.setCancelAction(new Action0() {
                    @Override
                    public void call() {
                        Timber.d("Canceling %s", artInfo);
                        finalBinder.getService().cancelRequest(artInfo);
                        queue.offer(new OptionalBitmap(null));
                    }
                });
            }
            bitmap = queue.take();
            if (bitmap.hasBitmap()) {
                final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                final OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
                final ParcelFileDescriptor in = pipe[0];
                final byte[] bytes = mL2Cache.bitmapToBytes(bitmap.getBitmap());
                final Scheduler.Worker worker = mScheduler.createWorker();
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        try {
                            IOUtils.write(bytes, out);
                            out.flush();
                        } catch (IOException e) {
                            Timber.w("createPipe2(%s) %s", artInfo, e.getMessage());
                        } finally {
                            IOUtils.closeQuietly(out);
                            worker.unsubscribe();
                        }
                    }
                });
                return in;
            }
        } catch (InterruptedException|IOException e) {
            Timber.w("createPipe2(%s) %s", artInfo, e.getMessage());
            if (binder != null) {
                binder.getService().cancelRequest(artInfo);
            }
        } finally {
            if (bitmap != null) bitmap.recycle();
            IOUtils.closeQuietly(binder);
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

    /** Wrapper for CancellationSignal backwards compat*/
    private interface Cancellation {
        boolean isCanceled();
        void setCancelAction(Action0 action0);
    }

    @TargetApi(19)
    private static class CancellationK implements Cancellation {
        final CancellationSignal signal;

        public CancellationK(@Nullable CancellationSignal signal) {
            this.signal = signal;
        }

        @Override
        public boolean isCanceled() {
            return signal != null && signal.isCanceled();
        }

        @Override
        public void setCancelAction(final Action0 action0) {
            if (signal != null) {
                signal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        action0.call();
                    }
                });
            }
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("query not implemented");
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

}
