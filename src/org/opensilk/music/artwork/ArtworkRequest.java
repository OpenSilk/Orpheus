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

import android.content.ContentUris;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.utils.ApolloUtils;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
 * Created by drew on 3/23/14.
 */
public class ArtworkRequest implements IArtworkRequest, Listener<Bitmap> {
    private static final String TAG = ArtworkRequest.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    private static final Uri sArtworkUri;

    static {
        sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    }

    // stuff needed to build the requests
    final String mArtistName;
    final String mAlbumName;
    final long mAlbumId;
    final String mCacheKey;
    final int mMaxWidth;
    final int mMaxHeight;
    final Bitmap.Config mConfig;
    final Listener<Bitmap> mImageListener;
    final ErrorListener mImageErrorListener;

    private Request.Priority mPriority = Request.Priority.NORMAL;
    // true if current request has been canceled
    private boolean mCanceled = false;
    // currently active request
    private Request<?> mCurrentRequest;
    // art manager, holds all our context stuff
    private final ArtworkManager mManager;

    public ArtworkRequest(String artistName, String albumName, long albumId, String cacheKey,
                          Listener<Bitmap> listener,
                          int maxWidth, int maxHeight, Bitmap.Config config,
                          ErrorListener errorListener) {
        mArtistName = artistName;
        mAlbumName = albumName;
        mAlbumId = albumId;
        mCacheKey = cacheKey;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mConfig = config;
        mImageListener = listener;
        mImageErrorListener = errorListener;
        mManager = ArtworkManager.getInstance();
    }

    /**
     * Starts the request
     */
    public void start() {
        if (mManager != null) {
            ApolloUtils.execute(false, new DiskCacheTask());
        } else {
            // Defer posting error until after ArtworkLoader returns from get()
            // This will probably never even be used
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyError(new VolleyError("Unable to get ArtworkManager instance"));
                }
            });
        }
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
            mManager.mBackgroundRequestor.add(this);
        }
    }

    @Override
    public void setPriority(Request.Priority newPriority) {
        mPriority = newPriority;
    }

    /**
     * Wrapper for mImageListener so we can add the bitmap to the diskcache
     */
    @Override
    public void onResponse(final Bitmap response) {
        if (mImageListener != null && !mCanceled) {
            mImageListener.onResponse(response);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                // Add to the cache
                if (mManager.mL2Cache == null) {
                    return;
                }
                if (!mManager.mL2Cache.containsKey(mCacheKey)) {
                    mManager.mL2Cache.putBitmap(mCacheKey, response);
                }
                // if this is a thumbnail check if the fullscreen is in the cache
                // if not add it to the background request queue
                if (mMaxWidth == sDefaultThumbnailWidthPx) {
                    final String altKey = ArtworkLoader.getCacheKey(mArtistName, mAlbumName,
                            sDefaultMaxImageWidthPx, 0);
                    if (!mManager.mL2Cache.containsKey(altKey)) {
                        mManager.mBackgroundRequestor.add(mArtistName, mAlbumName, mAlbumId,
                                BackgroundRequestor.ImageType.FULLSCREEN);
                    }
                }
            }
        }).start();
    }

    /**
     * Calls onErrorResponse for the ImageListener
     * @param error
     */
    private void notifyError(VolleyError error) {
        if (mImageErrorListener != null && !mCanceled) {
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
    class DiskCacheTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... params) {
            // Check our diskcache
            if (mManager.mL2Cache != null) {
                return mManager.mL2Cache.getBitmap(mCacheKey);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (!mCanceled) {
                if (bitmap != null) {
                    if (D) Log.d(TAG, "L2Cache hit: " + mCacheKey);
                    //This response goes straight to the listener since it was
                    // already in the cache
                    if (mImageListener != null) {
                        mImageListener.onResponse(bitmap);
                    }
                } else {
                    if (!ApolloUtils.isOnline(mManager.mContext)) {
                        notifyError(new VolleyError("No network connection"));
                        return;
                    }
                    if (!TextUtils.isEmpty(mArtistName)) {
                        if (!TextUtils.isEmpty(mAlbumName)) {
                            if (mManager.mPreferences != null
                                    && mManager.mPreferences.preferDownloadArtwork()) {
                                queueAlbumRequest(true);
                            } else {
                                ApolloUtils.execute(false, new MediaStoreTask(true));
                            }
                        } else { //Assuming they meant to download artist images
                            if (mManager.mPreferences != null
                                    && mManager.mPreferences.downloadMissingArtistImages()) {
                                // Fetch our artist info
                                mCurrentRequest = Fetch.artistInfo(mArtistName, new ArtistResponseListener(), mPriority);
                                mManager.mApiQueue.add(mCurrentRequest);
                            } else {
                                notifyError(new VolleyError("Artist image downloading disabled"));
                            }
                        }
                    } else {
                        notifyError(new VolleyError("Artist name was null"));
                    }
                }
            }
        }
    }

    private void queueAlbumRequest(boolean tryMediaStore) {
        if (D) Log.d(TAG, "Building album request for " + mAlbumName);
        if (mManager.mPreferences != null
                && mManager.mPreferences.downloadMissingArtwork()) {
            // Fetch our album info
            mCurrentRequest = Fetch.albumInfo(mArtistName, mAlbumName, new AlbumResponseListener(tryMediaStore), mPriority);
            mManager.mApiQueue.add(mCurrentRequest);
        } else if (tryMediaStore) {
            ApolloUtils.execute(false, new MediaStoreTask(false));
        } else {
            notifyError(new VolleyError("Album art downloading is disabled"));
        }
    }

    class MediaStoreTask extends AsyncTask<Void, Void, Response<Bitmap>> {
        final boolean tryNetwork;
        final PriorityImageRequest fauxRequest;

        MediaStoreTask(boolean tryNetwork) {
            this.tryNetwork = tryNetwork;
            fauxRequest = new PriorityImageRequest("mediastore"+mAlbumId, ArtworkRequest.this,
                    mMaxWidth, mMaxHeight, mConfig, mImageErrorListener);
        }

        /**
         * We create a faux ImageRequest and feed it a fake response
         * with data from the mediastore,
         * this lets the imagerequest class process the bitmap serially
         * and saves copying all that code into here.
         * @param params
         * @return
         */
        @Override
        protected Response<Bitmap> doInBackground(Void... params) {
            if (D) Log.d(TAG, "Searching mediastore for " + mAlbumName);
            InputStream in = null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                final Uri uri = ContentUris.withAppendedId(sArtworkUri, mAlbumId);
                in = mManager.mContext.getContentResolver().openInputStream(uri);
                IOUtils.copy(in, out);
                final NetworkResponse response = new NetworkResponse(out.toByteArray());
                return fauxRequest.parseNetworkResponse(response);
            } catch (IOException|IllegalStateException e) {
                return Response.error(new VolleyError(e));
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }
        }

        @Override
        protected void onPostExecute(Response<Bitmap> response) {
            if (response.isSuccess()) {
                fauxRequest.deliverResponse(response.result);
            } else {
                if (D) Log.d(TAG, "No artwork for " + mAlbumName + " in mediastore");
                if (tryNetwork) {
                    queueAlbumRequest(false);
                } else {
//                    fauxRequest.deliverError(response.error);
                    notifyError(response.error);
                }
            }
        }
    }

    /**
     * Response listener for Fetch.albumInfo
     */
    class AlbumResponseListener implements MusicEntryResponseCallback<Album> {
        final boolean tryMediaStore;

        AlbumResponseListener(boolean tryMediaStore) {
            this.tryMediaStore = tryMediaStore;
        }

        @Override
        @DebugLog
        public void onResponse(Album response) {
            if (!TextUtils.isEmpty(response.getMbid())) {
                if (mManager.mPreferences != null && mManager.mPreferences.wantHighResolutionArt()) {
                    // Check coverart archive
                    mCurrentRequest = new CoverArtArchiveRequest(response.getMbid(), ArtworkRequest.this,
                            mMaxWidth,mMaxHeight, mConfig, new CoverArtArchiveErrorListener(response));
                    ((CoverArtArchiveRequest) mCurrentRequest).setPriority(mPriority);
                    mManager.mImageQueue.add(mCurrentRequest);
                } else {
                    String url = getBestImage(response, false);
                    if (!TextUtils.isEmpty(url)) {
                        mCurrentRequest = new PriorityImageRequest(url, ArtworkRequest.this, mMaxWidth,
                                mMaxHeight, mConfig, mImageErrorListener);
                        ((PriorityImageRequest) mCurrentRequest).setPriority(mPriority);
                        mManager.mImageQueue.add(mCurrentRequest);
                    } else {
                        notifyError(new VolleyError("No image urls for " + response.toString()));
                    }
                }
            } else if (tryMediaStore) {
                ApolloUtils.execute(false, new MediaStoreTask(false));
            } else {
                notifyError(new VolleyError("Unknown mbid"));
            }
        }

        @Override
        @DebugLog
        public void onErrorResponse(VolleyError error) {
            if (tryMediaStore) {
                ApolloUtils.execute(false, new MediaStoreTask(false));
            } else {
                notifyError(error);
            }
        }
    }

    /**
     * Response listener for Fetch.artistInfo
     */
    class ArtistResponseListener implements MusicEntryResponseCallback<Artist> {

        @Override
        @DebugLog
        public void onResponse(Artist response) {
            String url = getBestImage(response, mManager.mPreferences == null || mManager.mPreferences.wantHighResolutionArt());
            if (!TextUtils.isEmpty(url)) {
                mCurrentRequest = new PriorityImageRequest(url, ArtworkRequest.this, mMaxWidth,
                        mMaxHeight, mConfig, mImageErrorListener);
                ((PriorityImageRequest) mCurrentRequest).setPriority(mPriority);
                mManager.mImageQueue.add(mCurrentRequest);
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
    static class PriorityImageRequest extends ImageRequest {
        private Priority mPriority;

        public PriorityImageRequest(String url, Listener<Bitmap> listener,
                                    int maxWidth, int maxHeight,
                                    Bitmap.Config decodeConfig, ErrorListener errorListener) {
            super(url, listener, maxWidth, maxHeight, decodeConfig, errorListener);
            setRetryPolicy(new DefaultRetryPolicy(2500, 2, 1.6f));
        }

        public void setPriority(Priority newPriority) {
            mPriority = newPriority;
        }

        @Override
        public Priority getPriority() {
            return mPriority;
        }

        @Override
        public Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
            return super.parseNetworkResponse(response);
        }

        @Override
        public void deliverResponse(Bitmap response) {
            super.deliverResponse(response);
        }
    }

    /**
     * coverartarchive request, wraps imagerequest so we can build the url
     */
    static class CoverArtArchiveRequest extends PriorityImageRequest {
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
    class CoverArtArchiveErrorListener implements ErrorListener {
        private Album mAlbum;

        CoverArtArchiveErrorListener(Album album) {
            mAlbum = album;
        }

        @Override
        @DebugLog
        public void onErrorResponse(VolleyError error) {
            String url = getBestImage(mAlbum, true);
            if (!TextUtils.isEmpty(url)) {
                mCurrentRequest = new PriorityImageRequest(url, ArtworkRequest.this, mMaxWidth,
                        mMaxHeight, mConfig, mImageErrorListener);
                ((PriorityImageRequest) mCurrentRequest).setPriority(mPriority);
                mManager.mImageQueue.add(mCurrentRequest);
            } else {
                notifyError(error);
            }
        }
    }

}
