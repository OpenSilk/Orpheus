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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.opensilk.music.BuildConfig;
import com.andrew.apollo.utils.ApolloUtils;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.apache.commons.io.IOUtils;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.util.PriorityAsyncTask;

import java.io.IOException;
import java.io.InputStream;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.MusicEntry;
import de.umass.lastfm.opensilk.Fetch;
import de.umass.lastfm.opensilk.MusicEntryResponseCallback;

/**
 * A wrapper class for a volley request that acts as an interface
 * for real volley requests that are chained together using this
 * class to return a bitmap from a series of network calls and
 * disk access.
 *
 * Created by drew on 3/23/14.
 */
public class ArtworkRequest implements IArtworkRequest {
    private static final String TAG = ArtworkRequest.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    // stuff needed to build the requests
    final ArtInfo mArtInfo;
    final String mCacheKey;
    final ArtworkType mImageType;
    final Listener<Bitmap> mImageListener;
    final ErrorListener mImageErrorListener;

    private Request.Priority mPriority = Request.Priority.NORMAL;
    // true if current request has been canceled
    private boolean mCanceled = false;
    // currently active request
    private Request<?> mCurrentRequest;
    // art manager, holds all our context stuff
    private final ArtworkManager mManager;

    public ArtworkRequest(ArtInfo artInfo, String cacheKey,
                          Listener<Bitmap> listener,
                          ArtworkType imageType,
                          ErrorListener errorListener) {
        mArtInfo = artInfo;
        mCacheKey = cacheKey;
        mImageType = imageType;
        mImageListener = listener;
        mImageErrorListener = errorListener;
        mManager = ArtworkManager.getInstance();
    }

    /**
     * Starts the request
     */
    public void start() {
        if (mManager != null) {
            new CheckDiskCacheTask().execute();
        } else {
            // Defer posting error until after ArtworkLoader returns from get()
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
    public void cancel() {
        mCanceled = true;
        if (mCurrentRequest != null) {
            if (mCurrentRequest instanceof ArtworkImageRequest) {
                // push into background our ImageResponse listener will make sure
                // its not delivered
                if (D) Log.d(TAG, "Setting background priority on " + mCurrentRequest.getCacheKey());
                ArtworkImageRequest req = (ArtworkImageRequest) mCurrentRequest;
                req.setBackground();
            }
            // For Album/Artist requests we just let them finish
            // our response listeners will queue the image request
            // in the background and prevent notifying on error
        }
    }

    /**
     * Sets priority used on spawned requests
     * @param newPriority
     */
    @Override
    public void setPriority(Request.Priority newPriority) {
        mPriority = newPriority;
    }

    /**
     * Adds lastfm album request to queue
     * @param tryMediaStore true to try media store if downloading is disabled or request returns an error
     */
    private void queueAlbumRequest(boolean tryMediaStore) {
        if (D) Log.d(TAG, "Building album request for " + mArtInfo.albumName);
        if (mManager.mPreferences.downloadMissingArtwork()) {
            // Fetch our album info
            mCurrentRequest = Fetch.albumInfo(mArtInfo.artistName, mArtInfo.albumName, new AlbumResponseListener(tryMediaStore), mPriority);
            mManager.mApiQueue.add(mCurrentRequest);
        } else if (tryMediaStore) {
            if (isLocalArtwork()) {
                new MediaStoreTask(false).execute();
            } else {
                queueImageRequest(mArtInfo.artworkUri);
            }
        } else {
            notifyError(new VolleyError("Album art downloading is disabled"));
        }
    }

    /**
     * Adds lastfm artist request to queue
     */
    private void queueArtistRequest() {
        if (mManager.mPreferences.downloadMissingArtistImages()) {
            // Fetch our artist info
            mCurrentRequest = Fetch.artistInfo(mArtInfo.artistName, new ArtistResponseListener(), mPriority);
            mManager.mApiQueue.add(mCurrentRequest);
        } else {
            notifyError(new VolleyError("Artist image downloading disabled"));
        }
    }

    /**
     * Starts async task to queue an image request
     * @param url
     */
    private void queueImageRequest(final String url) {
        new QueueImageRequestTask(url).execute();
    }

    //@DebugLog
    private void queueImageRequest(final Uri uri) {
        if (uri == null || uri.equals(Uri.EMPTY)) {
            notifyError(new VolleyError("Null uri"));
        } else {
            queueImageRequest(uri.toString());
        }
    }

    /**
     * Calls onErrorResponse for the ImageErrorListener if we havent been canceled
     * @param error
     */
    private void notifyError(VolleyError error) {
        if (!mCanceled && mImageErrorListener != null) {
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

    private boolean isLocalArtwork() {
        Uri u = mArtInfo.artworkUri;
        if (u != null) {
            if ("content".equals(u.getScheme())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Response listener, dispaches responses to listeners if we havent been canceled
     * and adds bitmap to the diskcache
     */
    static class ImageResponseListener implements Listener<Bitmap>, ErrorListener {

        private ArtworkImageRequest imageRequest;
        private final String cacheKey;
        private final ArtworkLoader.ImageCache diskCache;
        private final Listener<Bitmap> listener;
        private final ErrorListener errorListener;

        ImageResponseListener(String cacheKey,
                              ArtworkLoader.ImageCache diskCache,
                              Listener<Bitmap> listener, ErrorListener errorListener) {
            this.cacheKey = cacheKey;
            this.diskCache = diskCache;
            this.listener = listener;
            this.errorListener = errorListener;
        }

        public void setImageRequest(ArtworkImageRequest imageRequest) {
            this.imageRequest = imageRequest;
        }

        public ArtworkImageRequest getImageRequest() {
            return imageRequest;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            if (!imageRequest.isCanceled() && !imageRequest.isInBackground()) {
                // Request hasnt been canceled, notify the listener
                if (errorListener != null) {
                    errorListener.onErrorResponse(error);
                }
            }
        }

        @Override
        public void onResponse(Bitmap response) {
            if (!imageRequest.isCanceled() && !imageRequest.isInBackground()) {
                // Request hasnt been canceled, notify the listener
                if (listener != null) {
                    listener.onResponse(response);
                }
            }
            // Add to the disk cache
            BackgroundRequestor.execute(
                    new BackgroundRequestor.AddToCacheRunnable(diskCache, cacheKey, response)
            );
        }
    }

    /**
     * Async task to check our disk cache, if not present we call into volley
     */
    class CheckDiskCacheTask extends PriorityAsyncTask<Void, Void, Bitmap> {

        CheckDiskCacheTask() {
            super();
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            // Check our diskcache
            if (mManager.mL2Cache != null) {
                return mManager.mL2Cache.getBitmap(mCacheKey);
            }
            return null;
        }

        //TODO move the bulk of this logic to the background thread
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (!mCanceled) {
                if (bitmap != null) {
                    if (D) Log.d(TAG, "L2Cache hit: " + mCacheKey);
                    if (mImageListener != null) {
                        mImageListener.onResponse(bitmap);
                    }
                } else {
                    if (!TextUtils.isEmpty(mArtInfo.artistName)) {
                        if (!TextUtils.isEmpty(mArtInfo.albumName)) {
                            if (ApolloUtils.isOnline(mManager.mContext)) {
                                if (mManager.mPreferences.preferDownloadArtwork()) {
                                    queueAlbumRequest(true);
                                } else {
                                    if (isLocalArtwork()) {
                                        new MediaStoreTask(true).execute();
                                    } else {
                                        queueImageRequest(mArtInfo.artworkUri);
                                    }
                                }
                            //Not connected but want downloaded artwork, defer until later
                            } else if (mManager.mPreferences.preferDownloadArtwork()) {
                                notifyError(new VolleyError("No network connection"));
                            //Not connected and dont want downloaded art, just check mediastore
                            } else {
                                if (isLocalArtwork()) {
                                    new MediaStoreTask(false).execute();
                                } else {
                                    notifyError(new VolleyError("No network connection for remote Uri"));
                                }
                            }
                        } else { //Assume they meant to download artist images
                            if (ApolloUtils.isOnline(mManager.mContext)) {
                                queueArtistRequest();
                            } else {
                                notifyError(new VolleyError("No network connection"));
                            }
                        }
                        //no artist or album info just go strait for the artworUri
                    } else if (mArtInfo.artworkUri != null) {
                        if (isLocalArtwork()) {
                            // route local uris through the contentprovider
                            new MediaStoreTask(false).execute();
                        } else {
                            // assuming remote uris here
                            queueImageRequest(mArtInfo.artworkUri);
                        }
                    } else {
                        throw new RuntimeException("Hey dummy you made an ArtworkRequest will null info");
//                        notifyError(new VolleyError("Incomplete ArtInfo"));
                    }
                }
            }
        }
    }

    /**
     * AsyncTask to check media store for album art, on error will call
     * into volley if requested
     */
    class MediaStoreTask extends PriorityAsyncTask<Void, Void, Response<Bitmap>> {
        final boolean tryNetwork;
        final ArtworkImageRequest fauxRequest;

        MediaStoreTask(boolean tryNetwork) {
            super();
            this.tryNetwork = tryNetwork;
            ImageResponseListener listener = new ImageResponseListener(mCacheKey,
                    mManager.mL2Cache, mImageListener, mImageErrorListener);
            fauxRequest = new ArtworkImageRequest("mediastore#"+mArtInfo.artworkUri,
                    listener, mImageType, listener);
            listener.setImageRequest(fauxRequest);
            mCurrentRequest = fauxRequest;
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
            if (D) Log.d(TAG, "Searching mediastore for " + mCacheKey);
            InputStream in = null;
            try {
                final Uri uri = mArtInfo.artworkUri;
                in = mManager.mContext.getContentResolver().openInputStream(uri);
                final NetworkResponse response = new NetworkResponse(IOUtils.toByteArray(in));
                return fauxRequest.parseNetworkResponse(response);
            } catch (Exception e) { //too many to keep track of
                return Response.error(new VolleyError(e));
            } finally {
                IOUtils.closeQuietly(in);
            }
        }

        @Override
        protected void onPostExecute(Response<Bitmap> response) {
            if (response.isSuccess()) {
                fauxRequest.deliverResponse(response.result);
                if (mImageType.equals(ArtworkType.THUMBNAIL)) {
                    // Check if LARGE type exists in cache
                    BackgroundRequestor.execute(new BackgroundRequestor.CheckCacheRunnable(
                            mManager.mL2Cache, mArtInfo, ArtworkType.LARGE
                    ));
                }
            } else {
                if (D) Log.d(TAG, "No artwork for " + mArtInfo.albumName + " in mediastore");
                if (tryNetwork) {
                    queueAlbumRequest(false);
                } else {
                    fauxRequest.deliverError(response.error);
                }
            }
        }
    }

    /**
     * AsyncTask to add image requests to volley, This is done async
     * so we can check the disk cache for LARGE images if request is for
     * a THUMBNAIL. If not found we will send a second request to volley
     * for the LARGE artwork
     */
    class QueueImageRequestTask extends PriorityAsyncTask<Void, Void, Boolean> {
        final String url;
        final String altKey;

        QueueImageRequestTask(final String url) {
            super();
            this.url = url;
            this.altKey = ArtworkLoader.getCacheKey(mArtInfo, ArtworkType.LARGE);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mImageType.equals(ArtworkType.THUMBNAIL)) {
                if (mManager.mL2Cache != null && !mManager.mL2Cache.containsKey(altKey)) {
                    return true; // Wasn't in the cache
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean addLargeRequest) {
            ImageResponseListener listener = new ImageResponseListener(mCacheKey,
                    mManager.mL2Cache, mImageListener, mImageErrorListener);
            ArtworkImageRequest request = new ArtworkImageRequest(url, listener, mImageType, listener);
            listener.setImageRequest(request);
            if (mCanceled) {
                request.isInBackground();
            } else {
                request.setPriority(mPriority);
            }
            mCurrentRequest = request;
            mManager.mImageQueue.add(request);
            if (addLargeRequest) {
                if (D) Log.d(TAG, "Adding additional request for " + altKey);
                // We are requesting a thumbnail and the corresponding fullscreen image
                // isnt in the disk cache yet, so post a request for it.
                // Note: volley will not queue this instead it is attached to the previous request
                // we just queued and dispatched once that one comes in
                ImageResponseListener bglistener
                        = new ImageResponseListener(altKey, mManager.mL2Cache, null, null);
                ArtworkImageRequest largeRequest = new ArtworkImageRequest(url,
                        bglistener, ArtworkType.LARGE, bglistener);
                bglistener.setImageRequest(request);
                mManager.mImageQueue.add(largeRequest);
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
        //@DebugLog
        public void onResponse(Album response) {
            if (!TextUtils.isEmpty(response.getMbid())) {
                if (mManager.mPreferences.wantHighResolutionArt()) {
                    // Check coverart archive
                    ImageResponseListener listener = new ImageResponseListener(mCacheKey,
                            mManager.mL2Cache, mImageListener,
                            new CoverArtArchiveErrorListener(response));
                    CoverArtArchiveRequest request = new CoverArtArchiveRequest(response.getMbid(),
                            listener, mImageType, listener);
                    listener.setImageRequest(request);
                    if (mCanceled) {
                        request.setBackground();
                    } else {
                        request.setPriority(mPriority);
                    }
                    mCurrentRequest = request;
                    mManager.mImageQueue.add(request);
                } else {
                    String url = getBestImage(response, false);
                    if (!TextUtils.isEmpty(url)) {
                        queueImageRequest(url);
                    } else {
                        onErrorResponse(new VolleyError("No image urls for " + response.toString()));
                    }
                }
            } else {
                onErrorResponse(new VolleyError("Unknown mbid"));
            }
        }

        @Override
        //@DebugLog
        public void onErrorResponse(VolleyError error) {
            if (tryMediaStore) {
                if (isLocalArtwork()) {
                    new MediaStoreTask(false).execute();
                } else {
                    queueImageRequest(mArtInfo.artworkUri);
                }
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
        //@DebugLog
        public void onResponse(Artist response) {
            String url = getBestImage(response, mManager.mPreferences.wantHighResolutionArt());
            if (!TextUtils.isEmpty(url)) {
                queueImageRequest(url);
            } else {
                onErrorResponse(new VolleyError("No image urls for " + response.toString()));
            }
        }

        @Override
        //@DebugLog
        public void onErrorResponse(VolleyError error) {
            notifyError(error);
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
        //@DebugLog
        public void onErrorResponse(VolleyError error) {
            String url = getBestImage(mAlbum, true);
            if (!TextUtils.isEmpty(url)) {
                queueImageRequest(url);
            } else {
                notifyError(error);
            }
        }
    }

}
