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

package org.opensilk.music.artwork;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.MusicUtils;
import com.jakewharton.disklrucache.DiskLruCache;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import hugo.weaving.DebugLog;

/**
 * IArtworkService implementation
 *
 * Created by drew on 3/23/14.
 */
public class IArtworkServiceImpl extends IArtworkService.Stub {
    private static final String TAG = "IArtworkServiceImpl";
    private static final boolean D = BuildConfig.DEBUG;

    private WeakReference<ArtworkService> mService;

    public IArtworkServiceImpl(ArtworkService service) {
        mService = new WeakReference<>(service);
    }

    /**
     * @param id album id
     * @return ParcelFileDescriptor pipe to disk cache snapshot of image
     * @throws RemoteException
     */
    @Override
    @DebugLog
    public ParcelFileDescriptor getArtwork(long id) throws RemoteException {
        ArtworkService service = mService.get();
        if (service != null) {
            maybeWaitForCache(service);
            Album album = MusicUtils.makeAlbum(service.getApplicationContext(), id);
            if (album != null) {
                String cacheKey = ArtworkLoader.getCacheKey(album.mArtistName, album.mAlbumName, ArtworkType.LARGE);
                final ParcelFileDescriptor pfd = pullSnapshot(service, cacheKey);
                if (pfd != null) {
                    return pfd;
                }
                //Add to background request queue so we will have it next time
                BackgroundRequestor.add(album.mArtistName, album.mAlbumName,
                        album.mAlbumId, ArtworkType.LARGE);
            }
        }
        return null;
    }

    /**
     * @param id album id
     * @return ParcelFileDescriptor pipe to disk cache snapshot of thumbnail
     * @throws RemoteException
     */
    @Override
    @DebugLog
    public ParcelFileDescriptor getArtworkThumbnail(long id) throws RemoteException {
        ArtworkService service = mService.get();
        if (service != null) {
            maybeWaitForCache(service);
            Album album = MusicUtils.makeAlbum(service, id);
            if (album != null) {
                String cacheKey = ArtworkLoader.getCacheKey(album.mArtistName, album.mAlbumName, ArtworkType.THUMBNAIL);
                final ParcelFileDescriptor pfd = pullSnapshot(service, cacheKey);
                if (pfd != null) {
                    return pfd;
                }
                //Add to background request queue so we will have it next time
                BackgroundRequestor.add(album.mArtistName, album.mAlbumName,
                        album.mAlbumId, ArtworkType.THUMBNAIL);
            }
        }
        return null;
    }

    private synchronized void maybeWaitForCache(ArtworkService service) {
        int waitTime = 0;
        while (service.mManager.mL2Cache == null) {
            try {
                // Don' block for too long
                if (waitTime >= 500) {
                    return;
                }
                Log.i(TAG, "Waiting on cache init");
                // The cache hasn't initialized yet, block until its ready
                long start = System.currentTimeMillis();
                // small increments, we will not be interrupted so don't block
                // unnecessarily long
                wait(10);
                Log.i(TAG, "Waited for " + (waitTime += (System.currentTimeMillis() - start)) + "ms");
            } catch (InterruptedException ignored) { }
        }
    }

    private static ParcelFileDescriptor pullSnapshot(ArtworkService service, String cacheKey) {
        if (D) Log.d(TAG, "Checking DiskCache for " + cacheKey);
        try {
            if (service.mManager.mL2Cache == null) {
                throw new IOException("Unable to obtain cache instance");
            }
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
            final DiskLruCache.Snapshot snapshot = service.mManager.mL2Cache.get(cacheKey);
            if (snapshot != null && snapshot.getInputStream(0) != null) {
                new Thread(new PipeRunnable(snapshot, out)).start();
                return pipe[0];
            } else {
                pipe[0].close();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Runnable to copy inputstream from cache snapshot into
     * ParcelFileDescriptor's pipe outputsteam
     */
    private static class PipeRunnable implements Runnable {
        final DiskLruCache.Snapshot snapshot;
        final OutputStream out;

        PipeRunnable(DiskLruCache.Snapshot snapshot, OutputStream out) {
            this.snapshot = snapshot;
            this.out = out;
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                IOUtils.copy(snapshot.getInputStream(0), out);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                snapshot.close();
                IOUtils.closeQuietly(out);
            }
        }
    }

}
