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
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import com.google.gson.Gson;
import com.jakewharton.disklrucache.DiskLruCache;

import org.apache.commons.io.IOUtils;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.artwork.Constants;
import org.opensilk.music.artwork.Util;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.fetcher.ArtworkFetcherService;
import org.opensilk.music.model.ArtInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

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
    @Inject Gson mGson;

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
    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        return openFile(uri, mode);
    }

    @Override
    //@DebugLog
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Provider is read only");
        }
        switch (mUriMatcher.match(uri)) {
            case ArtworkUris.MATCH.ARTWORK: { //Fullscreen
                final List<String> seg = uri.getPathSegments();
                if (seg == null || seg.size() < 2) {
                    break;
                }
                final ArtInfo artInfo = new ArtInfo(seg.get(seg.size() - 2), seg.get(seg.size() - 1), null);
                final ParcelFileDescriptor pfd = getArtwork(uri, artInfo);
                if (pfd != null) {
                    return pfd;
                }
                break;
            } case ArtworkUris.MATCH.THUMBNAIL: { //Thumbnail
                final List<String> seg = uri.getPathSegments();
                if (seg == null || seg.size() < 2) {
                    break;
                }
                final ArtInfo artInfo = new ArtInfo(seg.get(seg.size() - 2), seg.get(seg.size() - 1), null);
                final ParcelFileDescriptor pfd = getArtworkThumbnail(uri, artInfo);
                if (pfd != null) {
                    return pfd;
                }
                break;
            } case ArtworkUris.MATCH.ALBUM_REQ:
              case ArtworkUris.MATCH.ARTIST_REQ: {
                  final String q = uri.getQueryParameter("q");
                  final String t = uri.getQueryParameter("t");
                  if (q != null) {
                    final ArtInfo artInfo = Util.artInfoFromBase64EncodedJson(mGson, q);
                    ArtworkType artworkType = ArtworkType.THUMBNAIL;
                    if (t != null) {
                        artworkType = ArtworkType.valueOf(t);
                    }
                    final ParcelFileDescriptor pfd;
                    switch (artworkType) {
                        case LARGE:
                            pfd = getArtwork(uri, artInfo);
                            break;
                        default:
                            pfd = getArtworkThumbnail(uri, artInfo);
                            break;
                    }
                    if (pfd != null) {
                        return pfd;
                    }
                }
                break;
            }
        }
        throw new FileNotFoundException("Could not obtain image from cache");
    }

    public ParcelFileDescriptor getArtwork(Uri uri, ArtInfo artInfo) {
        final String cacheKey = Util.getCacheKey(artInfo, ArtworkType.LARGE);
        ParcelFileDescriptor pfd = pullSnapshot(cacheKey);
        // Create request so it will be there next time
        if (pfd == null) ArtworkFetcherService.newTask(getContext(), uri, artInfo, ArtworkType.LARGE);
        return pfd;
    }

    public ParcelFileDescriptor getArtworkThumbnail(Uri uri, ArtInfo artInfo) {
        final String cacheKey = Util.getCacheKey(artInfo, ArtworkType.THUMBNAIL);
        ParcelFileDescriptor pfd = pullSnapshot(cacheKey);
        // Create request so it will be there next time
        if (pfd == null) ArtworkFetcherService.newTask(getContext(), uri, artInfo, ArtworkType.THUMBNAIL);
        return pfd;
    }

    private ParcelFileDescriptor pullSnapshot(String cacheKey) {
        Timber.d("Checking DiskCache for " + cacheKey);
        try {
            if (mL2Cache == null) {
                throw new IOException("Unable to obtain cache instance");
            }
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
            final DiskLruCache.Snapshot snapshot = mL2Cache.getSnapshot(cacheKey);
            if (snapshot != null && snapshot.getInputStream(0) != null) {
                final Scheduler.Worker worker = mScheduler.createWorker();
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        try {
                            IOUtils.copy(snapshot.getInputStream(0), out);
                        } catch (IOException e) {
                            Timber.e(e, "ParcelFileDescriptorPipe");
                        } finally {
                            snapshot.close();
                            IOUtils.closeQuietly(out);
                            worker.unsubscribe();
                        }
                    }
                });
                return pipe[0];
            } else {
                pipe[0].close();
                out.close();
            }
        } catch (IOException e) {
            Timber.w("pullSnapshot failed: %s", e.getMessage());
        }
        return null;
    }

}
