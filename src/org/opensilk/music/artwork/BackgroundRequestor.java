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
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.andrew.apollo.ApolloApplication.sDefaultMaxImageWidthPx;
import static com.andrew.apollo.ApolloApplication.sDefaultThumbnailWidthPx;


/**
 * Single thread executor service to process canceled/missing requests
 * without clogging up the RequestQueue
 *
 * Created by drew on 3/26/14.
 */
public class BackgroundRequestor {
    private static final String TAG = "BGR";
    private static final boolean D = BuildConfig.DEBUG;

    ThreadPoolExecutor mExecutor;

    BackgroundRequestor() {
        initExecutor();
    }

    void initExecutor() {
        mExecutor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public enum ImageType {
        THUMBNAIL,
        FULLSCREEN,
    }

    public void add(ArtworkRequest request) {
        BackgroundRequest newRequest = new BackgroundRequest(
                request.mArtistName,
                request.mAlbumName,
                -1, //TODO
                sDefaultThumbnailWidthPx == request.mMaxWidth ? ImageType.THUMBNAIL : ImageType.FULLSCREEN
        );
        if (mExecutor.getQueue().contains(newRequest)) {
            if (D) Log.d(TAG, "Rejecting '" + newRequest.toString() + "' already in queue");
            return;
        }
        if (D) Log.d(TAG, "Adding '" + newRequest.toString() + "'");
        mExecutor.execute(newRequest);
    }

    public void add(String artist, String album, long albumId, ImageType type) {
        BackgroundRequest newRequest = new BackgroundRequest(
                artist, album, albumId, type
        );
        if (mExecutor.getQueue().contains(newRequest)) {
            if (D) Log.d(TAG, "Rejecting '" + newRequest.toString() + "' already in queue");
            return;
        }
        if (D) Log.d(TAG, "Adding '" + newRequest.toString() + "'");
        mExecutor.execute(newRequest);
    }

    static class BackgroundRequest implements Runnable, Response.Listener<Bitmap>, Response.ErrorListener {

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
            final String cacheKey = ArtworkLoader.getCacheKey(artist, album, size, 0);
            final ArtworkRequest request = new ArtworkRequest(artist, album,
                    cacheKey, this, size, 0, Bitmap.Config.RGB_565, this);
            request.setPriority(Request.Priority.LOW);
            request.start();
            try {
                synchronized (this) {
                    wait();
                }
            } catch (Exception ignored) { }
            if (D) Log.d(TAG, "Request " + cacheKey + " took " + (System.currentTimeMillis() - start) + "ms");
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            synchronized (this) {
                notifyAll();
            }
        }

        @Override
        public void onResponse(Bitmap response) {
            synchronized (this) {
                notifyAll();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof BackgroundRequest)) {
                return false;
            }
            BackgroundRequest r = (BackgroundRequest)o;
            if (!TextUtils.equals(artist, r.artist)) {
                return false;
            }
            if (!TextUtils.equals(album, r.album)) {
                return false;
            }
            if (albumId != r.albumId) {
                return false;
            }
            if (!type.equals(r.type)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return ""+artist+":"+album+":"+albumId+":"+type;
        }

    }

}
