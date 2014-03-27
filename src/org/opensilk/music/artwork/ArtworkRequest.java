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
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.utils.ApolloUtils;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;

import java.util.Locale;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.MusicEntry;
import de.umass.lastfm.opensilk.Fetch;
import de.umass.lastfm.opensilk.MusicEntryResponseCallback;
import hugo.weaving.DebugLog;

import static com.andrew.apollo.ApolloApplication.sDefaultMaxImageWidthPx;
import static com.andrew.apollo.ApolloApplication.sDefaultThumbnailWidthPx;

/**
 * A wrapper class for a volley request that acts as an interface
 * for real volley requests that are chained together using this
 * class to return a bitmap from a series of network calls and
 * disk access.
 *
 * This class should never be added to the request queue
 * it will add its own requests to the queue and manage the responses
 *
 * Created by drew on 3/23/14.
 */
public class ArtworkRequest extends Request<Bitmap> implements Listener<Bitmap> {
    private static final String TAG = ArtworkRequest.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    // stuff needed to build the requests
    final String mArtistName;
    final String mAlbumName;
    final String mCacheKey;
    final int mMaxWidth;
    final int mMaxHeight;
    final Bitmap.Config mConfig;
    final Listener<Bitmap> mImageListener;
    final ErrorListener mImageErrorListener;

    // true if current request has been canceled
    private boolean mCanceled = false;
    // currently active request
    private Request<?> mCurrentRequest;
    // art manager, holds all our context stuff
    private final ArtworkManager mManager;

    public ArtworkRequest(String artistName, String albumName, String cacheKey,
                          Listener<Bitmap> listener,
                          int maxWidth, int maxHeight, Bitmap.Config config,
                          ErrorListener errorListener) {
        super(null, null);//dont care we will never be added to the queue
        mArtistName = artistName;
        mAlbumName = albumName;
        mCacheKey = cacheKey;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mConfig = config;
        mImageListener = listener;
        mImageErrorListener = errorListener;

        mManager = ArtworkManager.getInstance();
        if (mManager != null) {
            doRequest();
        }
    }

    // Initiates the request
    private void doRequest() {
        ApolloUtils.execute(false, new DiskCacheTask());
    }

    /**
     * Cancels whichever request is currently active
     */
    @Override
    @DebugLog
    public void cancel() {
        mCanceled = true;
        if (mCurrentRequest != null) {
            mCurrentRequest.cancel();
            // Readd this request to the background request queue
            mManager.mBackgroundRequester.add(this);
        }
    }

    /**
     * Wrapper for mImageListener so we can add the bitmap to the diskcache
     */
    @Override
    public void onResponse(final Bitmap response) {
        mImageListener.onResponse(response);
        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                // Add to the cache
                mManager.mL2Cache.putBitmap(mCacheKey, response);
                // if this is a thumbnail check if the fullscreen is in the cache
                // if not add it to the background request queue
                if (mMaxWidth == sDefaultThumbnailWidthPx) {
                    final String altKey = ArtworkLoader.getCacheKey(mArtistName, mAlbumName,
                            sDefaultMaxImageWidthPx, 0);
                    if (!mManager.mL2Cache.containsKey(altKey)) {
                        mManager.mBackgroundRequester.add(mArtistName, mAlbumName, -1,
                                BackgroundRequester.ImageType.FULLSCREEN);
                    }
                }
            }
        }).start();
    }

    private void notifyError(VolleyError error) {
        if (mImageErrorListener != null) {
            mImageErrorListener.onErrorResponse(error);
        }
    }

    /**
     * @return url string for highest quality image available or null if none
     */
    private static String getBestImage(MusicEntry e, boolean wantHigResArt) {
        for (ImageSize q : ImageSize.values()) {
            if (q.equals(ImageSize.MEGA) && !wantHigResArt) {
                continue;
            }
            String url = e.getImageURL(q);
            if (!TextUtils.isEmpty(url)) {
                if (D) Log.i(TAG, "Found " + q.toString() + " url for " + e.getName());
                return url;
            }
        }
        return null;
    }

    /**
     * Async task to check our disk cache, if not present we call into volley
     */
    private class DiskCacheTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... params) {
            // Check our diskcache
            if (mManager.getL2Cache() != null) {
                return mManager.getL2Cache().getBitmap(mCacheKey);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (!mCanceled) {
                if (bitmap != null) {
                    if (D) Log.d(TAG, "L2Cache hit: " + mCacheKey);
                    mImageListener.onResponse(bitmap);
                } else {
                    if (!ApolloUtils.isOnline(mManager.mContext)) {
                        notifyError(new VolleyError("No network connection"));
                        return;
                    }
                    if (!TextUtils.isEmpty(mArtistName)) {
                        if (!TextUtils.isEmpty(mAlbumName)) {
                            if (mManager.mPreferences != null
                                    && mManager.mPreferences.downloadMissingArtwork()) {
                                // Fetch our album info TODO mediastore
                                mCurrentRequest = Fetch.albumInfo(mArtistName, mAlbumName, new AlbumResponseListener());
                                mManager.mRequestQueue.add(mCurrentRequest);
                            } else {
                                notifyError(new VolleyError("Album art downloading is disabled"));
                            }
                        } else { //Assuming they meant to download artist images
                            if (mManager.mPreferences != null
                                    && mManager.mPreferences.downloadMissingArtistImages()) {
                                // Fetch our artist info
                                mCurrentRequest = Fetch.artistInfo(mArtistName, new ArtistResponseListener());
                                mManager.mRequestQueue.add(mCurrentRequest);
                            } else {
                                notifyError(new VolleyError("Artist image downloading disabled"));
                            }
                        }
                    } else {
                        notifyError(new VolleyError("Artist name was null"));
                    }
                }
            } else {
                notifyError(new VolleyError("Request was canceled"));
            }
        }
    }

    /**
     * Response listener for Fetch.albumInfo
     */
    private class AlbumResponseListener implements MusicEntryResponseCallback<Album> {
        @Override
        @DebugLog
        public void onResponse(Album response) {
            if (mManager.mPreferences != null && mManager.mPreferences.wantHighResolutionArt()) {
                // Check coverart archive
                mCurrentRequest = new CoverArtArchiveRequest(response.getMbid(), ArtworkRequest.this,
                        mMaxWidth,mMaxHeight, mConfig, new CoverArtArchiveErrorListener(response));
                mManager.mRequestQueue.add(mCurrentRequest);
            } else {
                String url = getBestImage(response, false);
                if (!TextUtils.isEmpty(url)) {
                    mCurrentRequest = new HiPriImageRequest(url, ArtworkRequest.this, mMaxWidth,
                            mMaxHeight, mConfig, mImageErrorListener);
                    mManager.mRequestQueue.add(mCurrentRequest);
                } else {
                    notifyError(new VolleyError("No image urls for " + response.toString()));
                }
            }
        }

        @Override
        @DebugLog
        public void onErrorResponse(VolleyError error) {
            notifyError(error);
        }

    }

    /**
     * Response listener for Fetch.artistInfo
     */
    private class ArtistResponseListener implements MusicEntryResponseCallback<Artist> {

        @Override
        @DebugLog
        public void onResponse(Artist response) {
            String url = getBestImage(response, mManager.mPreferences == null || mManager.mPreferences.wantHighResolutionArt());
            if (!TextUtils.isEmpty(url)) {
                mCurrentRequest = new HiPriImageRequest(url, ArtworkRequest.this, mMaxWidth,
                        mMaxHeight, mConfig, mImageErrorListener);
                mManager.mRequestQueue.add(mCurrentRequest);
            } else {
                notifyError(new VolleyError("No image urls for " + response.toString()));
            }
        }

        @Override
        @DebugLog
        public void onErrorResponse(VolleyError error) {
            notifyError(error);
        }

    }

    /**
     * ImageRequest with high priority so it jumps in front of the lastfm calls
     */
    private static class HiPriImageRequest extends ImageRequest {

        public HiPriImageRequest(String url, Listener<Bitmap> listener,
                                 int maxWidth, int maxHeight,
                                 Bitmap.Config decodeConfig, ErrorListener errorListener) {
            super(url, listener, maxWidth, maxHeight, decodeConfig, errorListener);
        }

        @Override
        public Priority getPriority() {
            return Priority.HIGH;
        }
    }

    /**
     * coverartarchive request, wraps imagerequest so we can build the url
     */
    private static class CoverArtArchiveRequest extends HiPriImageRequest {
        private static final String API_ROOT = "http://coverartarchive.org/release/";
        private static final String FRONT_COVER_URL = API_ROOT+"%s/front";

        public CoverArtArchiveRequest(String mbid, Listener<Bitmap> listener,
                                      int maxWidth, int maxHeight,
                                      Bitmap.Config decodeConfig, ErrorListener errorListener) {
            super(makeUrl(mbid), listener, maxWidth, maxHeight, decodeConfig, errorListener);
        }

        private static String makeUrl(String mbid) {
            return String.format(Locale.US, FRONT_COVER_URL, mbid);
        }
    }

    /**
     * Error listener for coverartarchive requests, on errore we fall back to lastfm art urls
     */
    private class CoverArtArchiveErrorListener implements ErrorListener {
        private Album mAlbum;

        CoverArtArchiveErrorListener(Album album) {
            mAlbum = album;
        }

        @Override
        @DebugLog
        public void onErrorResponse(VolleyError error) {
            String url = getBestImage(mAlbum, true);
            if (!TextUtils.isEmpty(url)) {
                mCurrentRequest = new HiPriImageRequest(url, ArtworkRequest.this, mMaxWidth,
                        mMaxHeight, mConfig, mImageErrorListener);
                mManager.mRequestQueue.add(mCurrentRequest);
            } else {
                notifyError(error);
            }
        }
    }


    /*
     * Abstract Methods for Request, we are never added to queue so will not need these.
     */

    @Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        return null;
    }

    @Override
    protected void deliverResponse(Bitmap response) {

    }

}
