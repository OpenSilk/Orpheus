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

package org.opensilk.music.artloader;

import android.app.ActivityManager;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.cache.BitmapLruCache;

import java.lang.ref.WeakReference;

import de.umass.lastfm.opensilk.Fetch;
import hugo.weaving.DebugLog;

import static com.andrew.apollo.ApolloApplication.sDefaultThumbnailWidthPx;

/**
 * Created by drew on 3/12/14.
 */
public class ArtworkLoader {

    private static final String TAG = ArtworkLoader.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    /**
     * Default memory cache size as a percent of device memory class
     */
    private static final float THUMB_MEM_CACHE_DIVIDER = 0.25f;

    /**
     * Uri for album thumbs
     */
    private static final Uri sArtworkUri;
    static {
        sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    }

    private Context mContext;
    private PreferenceUtils mPreferences;
    private Drawable mDefaultArtwork;

    private static ArtworkLoader sInstance;

    public static synchronized void create(Context context) {
        sInstance = new ArtworkLoader(context);
    }

    public static ArtworkLoader getInstance() {
        return sInstance;
    }

    private ArtworkLoader(Context context) {
        mContext = context;
        mPreferences = PreferenceUtils.getInstance(mContext);
        mDefaultArtwork = mContext.getResources().getDrawable(R.drawable.default_artwork);
        mDefaultArtwork.setFilterBitmap(false);
        mDefaultArtwork.setDither(false);
        initLoaderManager();
    }

    @DebugLog
    private void initLoaderManager() {
        final ActivityManager activityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        int memClass = mContext.getResources().getBoolean(R.bool.config_largeHeap) ?
                activityManager.getLargeMemoryClass() : activityManager.getMemoryClass();
        final int lruThumbCacheSize = Math.round(THUMB_MEM_CACHE_DIVIDER * memClass * 1024 * 1024);
        if (D) Log.d(TAG, "thumbcache=" + ((float)lruThumbCacheSize/1024/1024) + "MB");
//        mLoaderManager = new ImageLoaderManager(new BitmapLruCache(lruThumbCacheSize));
    }

    /**
     * Used to fetch album images.
     */
    public void loadAlbumImage(final String artistName, final String albumName, final long albumId,
                               final ArtworkImageView imageView) {
        if (TextUtils.isEmpty(artistName) || TextUtils.isEmpty(albumName) || albumId < 0 || imageView == null) {
            return;
        }
//        Bitmap bitmap = mLoaderManager.getCache().getBitmap(ArtworkImageLoader
//                .getCacheKey(artistName, albumName, imageView.getRequestedWidth(), 0));
//        if (bitmap != null) {
//            imageView.setImageBitmap(bitmap);
//            return;
//        }
        boolean preferNetwork = true;
        if (mPreferences.downloadMissingArtwork() && preferNetwork) {
//            AlbumResponseListener listener = new AlbumResponseListener(albumId, imageView);
//            Fetch.albumInfo(artistName, albumName, listener);
        } else {
            ApolloUtils.execute(false, new AlbumLocalImageLoaderTask(artistName, albumName, albumId, imageView, true));
        }
    }

    /**
     * Used to fetch the current artwork.
     */
    public void loadCurrentArtwork(final ArtworkImageView imageView) {
        loadAlbumImage(MusicUtils.getArtistName(), MusicUtils.getAlbumName(),
                MusicUtils.getCurrentAlbumId(), imageView);
    }

    /**
     * Used to fetch artist images.
     */
    public void loadArtistImage(final String artistName, final ArtworkImageView imageView) {
        if (TextUtils.isEmpty(artistName) || imageView == null) {
            return;
        }
        if (mPreferences.downloadMissingArtistImages()) {
//            ArtistResponseListener listener = new ArtistResponseListener(imageView);
//            Fetch.artistInfo(artistName, listener);
        }
    }

    /**
     * Loads album art from the media store, optionally checks the network if
     * no local art is found.
     */
    private class AlbumLocalImageLoaderTask extends AsyncTask<Void, Void, Bitmap> {
        private WeakReference<ArtworkImageView> image;
        private String artistName;
        private String albumName;
        private long albumId;
        private boolean canTryNetwork;
        private boolean isThumbnail;

        AlbumLocalImageLoaderTask(final String artistName, final String albumName, final long albumId,
                                  final ArtworkImageView imageView, boolean canTryNetwork) {
            this.image = new WeakReference<ArtworkImageView>(imageView);
            this.artistName = artistName;
            this.albumName = albumName;
            this.albumId = albumId;
            this.canTryNetwork = canTryNetwork;
            this.isThumbnail = imageView.getRequestedWidth() != sDefaultThumbnailWidthPx;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap = null;
            Uri artworkUri = ContentUris.withAppendedId(sArtworkUri, albumId);
            // Check cache
//            bitmap = mLoaderManager.getCache().getBitmap(artworkUri.toString() + String.valueOf(isThumbnail));
            if (bitmap != null) {
                return bitmap;
            }
            //Load from content
            bitmap = MediaStore.Images.Thumbnails.getThumbnail(mContext.getContentResolver(),
                    albumId,
                    isThumbnail ? MediaStore.Images.Thumbnails.MINI_KIND : MediaStore.Images.Thumbnails.FULL_SCREEN_KIND,
                    null);
            // put in cache
            if (bitmap != null) {
//                mLoaderManager.getCache().putBitmap(artworkUri.toString() + String.valueOf(isThumbnail), bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ArtworkImageView image = this.image.get();
            if (image != null) {
                if (bitmap != null) {
                    image.setImageBitmap(bitmap);
                } else if (mPreferences.downloadMissingArtwork() && canTryNetwork) {
//                    AlbumResponseListener listener = new AlbumResponseListener(this.albumId, image);
//                    Fetch.albumInfo(this.artistName, this.albumName, listener);
                }
            }
        }
    }

}
