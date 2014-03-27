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

import android.graphics.Bitmap;
import android.util.Log;

import com.android.volley.toolbox.RequestFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hugo.weaving.DebugLog;

import static com.andrew.apollo.ApolloApplication.sDefaultMaxImageWidthPx;
import static com.andrew.apollo.ApolloApplication.sDefaultThumbnailWidthPx;


/**
 * Single thread executor service to process canceled/missing requests
 * without clogging up the RequestQueue
 *
 * Created by drew on 3/26/14.
 */
public class BackgroundRequester {

    final ExecutorService mExecutor;

    BackgroundRequester() {
        mExecutor = Executors.newSingleThreadExecutor();
    }

    public enum ImageType {
        THUMBNAIL,
        FULLSCREEN,
    }

    @DebugLog
    public void add(ArtworkRequest request) {
        mExecutor.submit(new BackgroundRequest(
                request.mArtistName,
                request.mAlbumName,
                -1, //TODO
                sDefaultThumbnailWidthPx == request.mMaxWidth ? ImageType.THUMBNAIL : ImageType.FULLSCREEN
        ));
    }

    @DebugLog
    public void add(String artist, String album, long albumId, ImageType type) {
        mExecutor.submit(new BackgroundRequest(
                artist, album, albumId, type
        ));
    }

    static class BackgroundRequest implements Runnable {

        final String artist;
        final String album;
        final long albumId;
        final ImageType type;


        BackgroundRequest(String artist, String album, long albumId, ImageType type) {
            this.artist = artist;
            this.album = album;
            this.albumId = albumId;
            this.type = type;
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            int size = type.equals(ImageType.THUMBNAIL) ? sDefaultThumbnailWidthPx : sDefaultMaxImageWidthPx;
            final RequestFuture<Bitmap> future = RequestFuture.newFuture();
            final String cacheKey = ArtworkLoader.getCacheKey(artist, album, size, 0);
            final ArtworkRequest request = new ArtworkRequest(artist, album,
                    cacheKey, future, size, 0, Bitmap.Config.RGB_565, future);
            future.setRequest(request);
            try {
                future.get();
            } catch (InterruptedException|ExecutionException ignored) {
            }
            Log.d("BGR", "Request " + cacheKey + " took " + (System.currentTimeMillis() - start) + "ms");
        }
    }

}
