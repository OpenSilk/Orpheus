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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;

import com.jakewharton.disklrucache.DiskLruCache;

import org.apache.commons.io.IOUtils;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 6/21/14.
 */
public class ArtworkServiceImpl implements ArtworkService {

    @Inject
    ArtworkManager mManager;
    @Inject @ForApplication
    Context mAppContext;

    /**
     * @return ParcelFileDescriptor pipe to disk cache snapshot of image
     */
    @Override
    public ParcelFileDescriptor getArtwork(String artistName, String albumName) {
        String cacheKey = ArtworkLoader.getCacheKey(new ArtInfo(artistName, albumName, null), ArtworkType.LARGE);
        final ParcelFileDescriptor pfd = pullSnapshot(cacheKey);
        if (pfd != null) {
            return pfd;
        }
        //Add to background request queue so we will have it next time
//        BackgroundRequestor.add(artistName, albumName, album.mAlbumId, ArtworkType.LARGE);
        return null;
    }

    /**
     * @return ParcelFileDescriptor pipe to disk cache snapshot of image
     */
    @Override
    public ParcelFileDescriptor getArtworkThumbnail(String artistName, String albumName) {
        String cacheKey = ArtworkLoader.getCacheKey(new ArtInfo(artistName, albumName, null), ArtworkType.THUMBNAIL);
        final ParcelFileDescriptor pfd = pullSnapshot(cacheKey);
        if (pfd != null) {
            return pfd;
        }
        //Add to background request queue so we will have it next time
//        BackgroundRequestor.add(artistName, albumName, album.mAlbumId, ArtworkType.THUMBNAIL);
        return null;
    }

    @Override
    @DebugLog
    public void clearCache() {
        mManager.mL1Cache.evictAll();
    }

    @Override
    @DebugLog
    public void scheduleCacheClear() {
        AlarmManager am = (AlarmManager) mAppContext.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC, 60 * 1000, getClearCacheIntent());
    }

    @Override
    @DebugLog
    public void cancelCacheClear() {
        AlarmManager am = (AlarmManager) mAppContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getClearCacheIntent());
    }

    public PendingIntent getClearCacheIntent() {
        return PendingIntent.getBroadcast(mAppContext, 0,
                new Intent(mAppContext, ArtworkBroadcastReceiver.class)
                        .setAction(ArtworkBroadcastReceiver.CLEAR_CACHE),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private synchronized void maybeWaitForCache() {
        int waitTime = 0;
        while (mManager.mL2Cache == null) {
            try {
                // Don' block for too long
                if (waitTime >= 500) {
                    return;
                }
                Timber.i("Waiting on cache init");
                // The cache hasn't initialized yet, block until its ready
                long start = System.currentTimeMillis();
                // small increments, we will not be interrupted so don't block
                // unnecessarily long
                wait(10);
                Timber.i("Waited for " + (waitTime += (System.currentTimeMillis() - start)) + "ms");
            } catch (InterruptedException ignored) { }
        }
    }

    private ParcelFileDescriptor pullSnapshot(String cacheKey) {
        Timber.d("Checking DiskCache for " + cacheKey);
        try {
            if (mManager.mL2Cache == null) {
                throw new IOException("Unable to obtain cache instance");
            }
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
            final DiskLruCache.Snapshot snapshot = mManager.mL2Cache.get(cacheKey);
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
