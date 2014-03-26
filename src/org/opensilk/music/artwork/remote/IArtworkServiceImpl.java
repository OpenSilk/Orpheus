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

package org.opensilk.music.artwork.remote;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.MusicUtils;
import com.jakewharton.disklrucache.DiskLruCache;

import org.apache.commons.io.IOUtils;
import org.opensilk.music.artwork.ArtworkLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import hugo.weaving.DebugLog;

import static com.andrew.apollo.ApolloApplication.sDefaultMaxImageWidthPx;

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
     * @return ParcelFileDescriptor to the cache file for currently playing album
     * @throws RemoteException
     */
    @Override
    @DebugLog
    public ParcelFileDescriptor getCurrentArtwork() throws RemoteException {
        return getArtwork(MusicUtils.getCurrentAlbumId());
    }

    /**
     * @param id album id
     * @return ParcelFileDescriptor to the cache file
     * @throws RemoteException
     */
    @Override
    @DebugLog //TODO add method to fetch either large or thumbnails
    public ParcelFileDescriptor getArtwork(long id) throws RemoteException {
        ArtworkService service = mService.get();
        if (service != null) {
            Album album = MusicUtils.makeAlbum(service.getApplicationContext(), id);
            if (album != null) {
                String cacheKey = ArtworkLoader.getCacheKey(album.mArtistName, album.mAlbumName, sDefaultMaxImageWidthPx, 0);
                if (D) Log.d(TAG, "Checking DiskCache for " + cacheKey);
                try {
                    final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                    final OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
                    final DiskLruCache.Snapshot snapshot = service.mManager.getDiskCache().get(cacheKey);
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
                //TODO send request to volley so it will be there next time
            }
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
