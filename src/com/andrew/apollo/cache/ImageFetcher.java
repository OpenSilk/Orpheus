/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.cache;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.Config;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.util.coverartarchive.CoverArtFetcher;

import java.io.File;
import java.io.IOException;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.MusicEntry;

/**
 * A subclass of {@link ImageWorker} that fetches images from a URL.
 */
public class ImageFetcher extends ImageWorker {

    private static final String TAG = ImageFetcher.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    private static ImageFetcher sInstance = null;

    /**
     * Creates a new instance of {@link ImageFetcher}.
     *
     * @param context The {@link Context} to use.
     */
    public ImageFetcher(final Context context) {
        super(context);
    }

    /**
     * Used to create a singleton of the image fetcher
     *
     * @param context The {@link Context} to use
     * @return A new instance of this class.
     */
    public static final ImageFetcher getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new ImageFetcher(context.getApplicationContext());
        }
        return sInstance;
    }

    @Deprecated
    protected Bitmap processBitmap(final String url, String key) {
        return processBitmap(url, key, true);
    }

    @Override
    protected Bitmap processBitmap(final String url, String key, final boolean isThumbnail) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(key)) {
            return null;
        }
        File downloadDir = ImageCache.getDiskCacheDir(mContext, ImageCache.DOWNLOAD_CACHE_DIR);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        File temp = null;
        try {
            temp = File.createTempFile("temp", null, downloadDir);
            if (CoverArtFetcher.downloadFile(url, temp) && temp.exists() && temp.length() > 0) {
                // Return a sampled down version
                final Bitmap bitmap;
                if (isThumbnail) {
                    bitmap = ImageCache.decodeSampledBitmapFromFile(temp.toString(),
                            mImageCache.mDefaultThumbnailSizePx, mImageCache.mDefaultThumbnailSizePx);
                } else {
                    bitmap = ImageCache.decodeSampledBitmapFromFile(temp.toString(),
                            mImageCache.mDefaultMaxImageWidth, mImageCache.mDefaultMaxImageHeight);
                }
                if (bitmap != null) {
                    // Move the file to its real location so we can find it later
                    temp.renameTo(new File(downloadDir, ImageCache.hashKeyForDisk(key)));
                    return bitmap;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "processBitmap(1) "+e.getMessage());
        }
        if (temp != null && temp.exists()) {
            temp.delete();
        }
        return null;
    }

    private String getBestImage(MusicEntry e) {
        // For albums first try the coverartarchive, they seem to have higher quality images
        if (e instanceof Album) {
            if (!TextUtils.isEmpty(e.getMbid()) && PreferenceUtils.getInstance(mContext).wantHighResolutionArt()) {
                String url = CoverArtFetcher.getFrontCoverUrl(e.getMbid());
                if (!TextUtils.isEmpty(url)) {
                    if (D) Log.i(TAG, "Found coverartarchive url for " + e.getName());
                    return url;
                }
            }
        }
        for (ImageSize q : ImageSize.values()) {
            if (q.equals(ImageSize.MEGA) && !PreferenceUtils.getInstance(mContext).wantHighResolutionArt()) {
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
     * {@inheritDoc}
     */
    @Override
    protected String processImageUrl(final String artistName, final String albumName,
            final ImageType imageType) {
        switch (imageType) {
            case ARTIST:
                if (!TextUtils.isEmpty(artistName)) {
                    if (PreferenceUtils.getInstance(mContext).downloadMissingArtistImages()) {
                        if (D) Log.i(TAG, "Fetching artist info for " + artistName);
                        final Artist artist = Artist.getInfo(mContext, artistName);
                        if (artist != null) {
                            return getBestImage(artist);
                        }
                    }
                }
                break;
            case ALBUM:
                if (!TextUtils.isEmpty(artistName) && !TextUtils.isEmpty(albumName)) {
                    if (PreferenceUtils.getInstance(mContext).downloadMissingArtwork()) {
                        if (D) Log.i(TAG, "Fetching album info for " + albumName);
                        final Artist correction = Artist.getCorrection(mContext, artistName);
                        if (correction != null) {
                            final Album album = Album.getInfo(mContext, correction.getName(),
                                    albumName);
                            if (album != null) {
                                return getBestImage(album);
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
        return null;
    }

    /**
     * Used to fetch album images.
     */
    public void loadAlbumImage(final String artistName, final String albumName, final long albumId,
            final ImageView imageView) {
        loadImage(generateAlbumCacheKey(albumName, artistName), artistName, albumName, albumId, imageView,
                ImageType.ALBUM);
    }

    /**
     * Used to fetch the current artwork.
     */
    public void loadCurrentArtwork(final ImageView imageView) {
        loadImage(generateAlbumCacheKey(MusicUtils.getAlbumName(), MusicUtils.getArtistName()),
                MusicUtils.getArtistName(), MusicUtils.getAlbumName(), MusicUtils.getCurrentAlbumId(),
                imageView, ImageType.ALBUM);
    }

    /**
     * Used to fetch the current artwork.
     */
    public void loadCurrentLargeArtwork(final ImageView imageView) {
        loadImage(generateAlbumCacheKey(MusicUtils.getAlbumName(), MusicUtils.getArtistName()),
                MusicUtils.getArtistName(), MusicUtils.getAlbumName(), MusicUtils.getCurrentAlbumId(),
                imageView, ImageType.ALBUM, false);
    }

    /**
     * Used to fetch artist images.
     */
    public void loadArtistImage(final String key, final ImageView imageView) {
        loadImage(key, key, null, -1, imageView, ImageType.ARTIST);
    }

    /**
     * Used to fetch artist images.
     */
    public void loadLargeArtistImage(final String key, final ImageView imageView) {
        loadImage(key, key, null, -1, imageView, ImageType.ARTIST, false);
    }

    /**
     * Used to fetch the current artist image.
     */
    public void loadCurrentArtistImage(final ImageView imageView) {
        loadImage(MusicUtils.getArtistName(), MusicUtils.getArtistName(), null, -1, imageView,
                ImageType.ARTIST);
    }

    /**
     * @param pause True to temporarily pause the disk cache, false otherwise.
     */
    public void setPauseDiskCache(final boolean pause) {
        if (mImageCache != null) {
            mImageCache.setPauseDiskCache(pause);
        }
    }

    /**
     * Clears the disk and memory caches
     */
    public void clearCaches() {
        if (mImageCache != null) {
            mImageCache.clearCaches();
        }
    }

    /**
     * @param key The key used to find the image to remove
     */
    public void removeFromCache(final String key) {
        if (mImageCache != null) {
            mImageCache.removeFromCache(key);
        }
    }

    /**
     * @param key The key used to find the image to return
     */
    public Bitmap getCachedBitmap(final String key) {
        if (mImageCache != null) {
            return mImageCache.getCachedBitmap(key);
        }
        return getDefaultArtwork();
    }

    /**
     * @param keyAlbum The key (album name) used to find the album art to return
     * @param keyArtist The key (artist name) used to find the album art to return
     */
    public Bitmap getCachedArtwork(final String keyAlbum, final String keyArtist) {
        return getCachedArtwork(keyAlbum, keyArtist,
                MusicUtils.getIdForAlbum(mContext, keyAlbum, keyArtist));
    }

    /**
     * @param keyAlbum The key (album name) used to find the album art to return
     * @param keyArtist The key (artist name) used to find the album art to return
     * @param keyId The key (album id) used to find the album art to return
     */
    public Bitmap getCachedArtwork(final String keyAlbum, final String keyArtist,
            final long keyId) {
        if (mImageCache != null) {
            return mImageCache.getCachedArtwork(mContext,
                    generateAlbumCacheKey(keyAlbum, keyArtist),
                    keyId);
        }
        return getDefaultArtwork();
    }

    /**
     * Finds cached album art.
     *
     * @param albumName The name of the current album
     * @param albumId The ID of the current album
     * @param artistName The album artist in case we should have to download
     *            missing artwork
     * @return The album art as an {@link Bitmap}
     */
    public Bitmap getArtwork(final String albumName, final long albumId, final String artistName) {
        // Check the disk cache
        Bitmap artwork = null;

        if (artwork == null && albumName != null && mImageCache != null) {
            artwork = mImageCache.getBitmapFromDiskCache(
                    generateAlbumCacheKey(albumName, artistName));
        }
        if (artwork == null && albumId >= 0 && mImageCache != null) {
            // Check for local artwork
            artwork = mImageCache.getArtworkFromMediaStore(mContext, albumId);
        }
        // if (artwork == null && artistName != null && albumName != null) {
        // // Download missing artwork
        // artwork = processBitmap(processImageUrl(artistName, albumName,
        // ImageType.ALBUM));
        // }
        if (artwork != null) {
            return artwork;
        }
        return getDefaultArtwork();
    }

    /**
     * Used in {@link MusicPlaybackService}
     * to set the current album art in the notification and lock screen
     * we intentionally dont hit the memcache here so we dont inflate our
     * memory usage since the service is run in a separate process
     *
     * @param albumName The name of the current album
     * @param albumId The ID of the current album
     * @param artistName The album artist in case we should have to download
     *            missing artwork
     * @return The album art as an {@link Bitmap}
     */
    public Bitmap getLargeArtwork(final String albumName, final long albumId, final String artistName) {
        Bitmap artwork = null;
        if (albumId >= 0 && mImageCache != null) {
            // Check for local artwork
            artwork = mImageCache.getArtworkFromMediaStore(mContext, albumId);
        }
        if (artwork == null && artistName != null && albumName != null) {
            // Check the download cache
            artwork = mImageCache.getLargeArtworkFromDownloadCache(generateAlbumCacheKey(albumName, artistName));
        }
        if (artwork != null) {
            return artwork;
        }
        return getDefaultArtwork();
    }

    /**
     * Used in {@link org.opensilk.music.cast.CastWebServer}
     * Returns file of artwork found in mediastore or downloadcache
     *
     * @param albumName The name of the current album
     * @param albumId The ID of the current album
     * @param artistName The album artist in case we should have to download
     *            missing artwork
     * @return The album art as an {@link Bitmap}
     */
    public File getLargeArtworkFile(final String albumName, final long albumId, final String artistName) {
        File artwork = null;
        if (albumId >= 0) {
            Cursor c = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[] { MediaStore.Audio.Albums.ALBUM_ART },
                    BaseColumns._ID + "=?", new String[] { String.valueOf(albumId) }, null);
            if (c != null && c.moveToFirst()) {
                String path = c.getString(0);
                if (!TextUtils.isEmpty(path)) {
                    artwork = new File(c.getString(0));
                }
            }
            if (c != null) {
                c.close();
            }
        }
        if (artwork == null || !artwork.exists()) {
            artwork = new File(ImageCache.getDiskCacheDir(mContext, ImageCache.DOWNLOAD_CACHE_DIR),
                    ImageCache.hashKeyForDisk(generateAlbumCacheKey(albumName, artistName)));
        }
        if (artwork != null && artwork.exists()) {
            Log.e(TAG, "Found album art at " + artwork.getPath());
            return artwork;
        }
        return null;
    }

    /**
     * Generates key used by album art cache. It needs both album name and artist name
     * to let to select correct image for the case when there are two albums with the
     * same artist.
     *
     * @param albumName The album name the cache key needs to be generated.
     * @param artistName The artist name the cache key needs to be generated.
     * @return
     */
    public static String generateAlbumCacheKey(final String albumName, final String artistName) {
        if (albumName == null || artistName == null) {
            return null;
        }
        return new StringBuilder(albumName)
                .append("_")
                .append(artistName)
                .append("_")
                .append(Config.ALBUM_ART_SUFFIX)
                .toString();
    }

}
