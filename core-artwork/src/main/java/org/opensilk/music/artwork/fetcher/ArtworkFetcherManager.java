/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.artwork.fetcher;

import android.content.ContentResolver;
import android.content.Context;
import android.content.UriMatcher;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.coverartarchive.CoverArtArchive;
import org.opensilk.music.artwork.coverartarchive.Metadata;
import org.opensilk.music.artwork.shared.ArtworkPreferences;
import org.opensilk.music.model.ArtInfo;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.LastFM;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by drew on 10/21/14.
 */
@ArtworkFetcherScope
public class ArtworkFetcherManager {

    final Context mContext;
    final ArtworkPreferences mPreferences;
    final BitmapDiskCache mL2Cache;
    final ConnectivityManager mConnectivityManager;
    final LastFM mLastFM;
    final CoverArtArchive mCoverArtArchive;
    final OkHttpClient mOkHttpClient;

    final UriMatcher mUriMatcher;
    final Scheduler mObserveOn;
    final Scheduler mSubscribeOn;

    @Inject
    public ArtworkFetcherManager(
            @ForApplication Context mContext,
            ArtworkPreferences mPreferences,
            BitmapDiskCache mL2Cache,
            ConnectivityManager mConnectivityManager,
            UriMatcher mUriMatcher,
            @Named("ObserveOnScheduler") Scheduler mObserveOn,
            @Named("SubscribeOnScheduler") Scheduler mSubscribeOn,
            LastFM mLastFM,
            CoverArtArchive mCoverArtArchive,
            OkHttpClient mOkHttpClient
    ) {
        this.mContext = mContext;
        this.mPreferences = mPreferences;
        this.mL2Cache = mL2Cache;
        this.mConnectivityManager = mConnectivityManager;
        this.mUriMatcher = mUriMatcher;
        this.mObserveOn = mObserveOn;
        this.mSubscribeOn = mSubscribeOn;
        this.mLastFM = mLastFM;
        this.mCoverArtArchive = mCoverArtArchive;
        this.mOkHttpClient = mOkHttpClient;
    }

    /**
     * Entry point
     */
    public Subscription fetch(ArtInfo artInfo, CompletionListener l) {
        if (artInfo.forArtist) {
            if (StringUtils.isEmpty(artInfo.artistName)) {
                return Observable.<Bitmap>error(new Exception("Invalid artInfo: " +
                        "must have artistName set")).subscribe(l);
            } else {
                return fetchArtistImage(artInfo).observeOn(mObserveOn).subscribe(l);
            }
        } else {
            if ((StringUtils.isEmpty(artInfo.artistName)
                    && StringUtils.isEmpty(artInfo.albumName))
                    && (artInfo.artworkUri == null || Uri.EMPTY.equals(artInfo.artworkUri))) {
                return Observable.<Bitmap>error(new Exception("Invalid artInfo: must have artistName " +
                        "and albumName set or valid artworkUri")).subscribe(l);
            } else {
                return fetchAlbumCover(artInfo).observeOn(mObserveOn).subscribe(l);
            }
        }
    }

    /**
     * Clears the disk caches
     */
    public void clearCaches() {
        mL2Cache.clearCache();
    }

    public void onDestroy() {
    }

    /*
     * End public methods
     */

    private Observable<Boolean> baseObservable(final ArtInfo artInfo) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                //in case a request just finished
                subscriber.onNext(mL2Cache.containsKey(artInfo.cacheKey()));
                subscriber.onCompleted();
            }
        }).subscribeOn(mSubscribeOn);
    }

    private Observable<Bitmap> fetchArtistImage(final ArtInfo artInfo) {
        return baseObservable(artInfo).flatMap(new Func1<Boolean, Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> call(Boolean inCache) {
                if (inCache) {
                    return Observable.just(mL2Cache.getBitmap(artInfo.cacheKey()));
                }
                boolean isOnline = isOnline(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true));
                boolean wantArtistImages = mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTIST_IMAGES, true);
                if (isOnline && wantArtistImages) {
                    return createArtistNetworkRequest(artInfo);
                } else {
                    return Observable.error(new Exception("Must defer #0"));
                }
            }
        });
    }

    private Observable<Bitmap> fetchAlbumCover(final ArtInfo artInfo) {
        return baseObservable(artInfo).flatMap(new Func1<Boolean, Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> call(Boolean inCache) {
                if (inCache) {
                    return Observable.just(mL2Cache.getBitmap(artInfo.cacheKey()));
                }
                boolean hasAlbumArtist = !TextUtils.isEmpty(artInfo.albumName) && !TextUtils.isEmpty(artInfo.artistName);
                boolean hasUri = artInfo.artworkUri != null && !artInfo.artworkUri.equals(Uri.EMPTY);
                boolean isOnline = isOnline(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true));
                boolean wantAlbumArt = mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true);
                boolean preferDownload = mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false);
                boolean isLocalArt = isLocalArtwork(artInfo.artworkUri);
                if (hasAlbumArtist && hasUri) {
                    // We have everything we may need
                    if (isOnline && wantAlbumArt) {
                        // were online and want artwork
                        if (!preferDownload) {
                            if (isLocalArt) {
                                // try mediastore first if parsing fails go to network
                                return tryForMediaStore(artInfo, true);
                            } else {
                                // remote art and dont want to try for lfm
                                return createImageObservable(artInfo.artworkUri.toString(), artInfo);
                            }
                        } else {
                            // go to network, falling back on fail
                            return tryForNetwork(artInfo, true);
                        }
                    } else if (isOnline && !isLocalArt) {
                        // were online and have an external uri lets get it
                        // regardless of user preference
                        return createImageObservable(artInfo.artworkUri.toString(), artInfo);
                    } else if (!isOnline && isLocalArt && !preferDownload) {
                        // were offline, this is a local source
                        // and the user doesnt want to try network first
                        // go ahead and fetch the mediastore image
                        return tryForMediaStore(artInfo, false);
                    } else {
                        //  were offline and cant get artwork or the user wants to defer
                        return Observable.error(new Exception("Must defer #1"));
                    }
                } else if (hasAlbumArtist) {
                    if (isOnline && wantAlbumArt) {
                        // try for network, we dont have a uri so dont fallback on failure
                        return tryForNetwork(artInfo, false);
                    } else {
                        return Observable.error(new Exception("Must defer #2"));
                    }
                } else if (hasUri) {
                    if (isLocalArt) {
                        //we cant fallback without album/artist
                        return tryForMediaStore(artInfo, false);
                    } else if (isOnline) {
                        //all we have is a url so go for it
                        return createImageObservable(artInfo.artworkUri.toString(), artInfo);
                    } else {
                        return Observable.error(new Exception("Must defer #3"));
                    }
                } else { // just ignore the request
                    return Observable.error(new Exception("Must defer #4"));
                }
            }
        });
    }

    private Observable<Bitmap> createArtistNetworkRequest(final ArtInfo artInfo) {
        return mLastFM.getArtistObservable(artInfo.artistName)
                .map(new Func1<Artist, String>() {
                    @Override
                    public String call(Artist artist) {
                        String url = LastFM.GET_BEST_IMAGE.call(artist);
                        if (!TextUtils.isEmpty(url)) {
                            return url;
                        }
                        Timber.v("ArtistApiRequest: No image urls for %s", artist.getName());
                        throw OnErrorThrowable.from(new Exception("No artwork found for " +
                                artist.getName()));
                    }
                }).flatMap(new Func1<String, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(String s) {
                        return createImageObservable(mangleImageUrl(s), artInfo);
                    }
                });
    }

    private Observable<Bitmap> createAlbumNetworkRequest(final ArtInfo artInfo) {
        return mLastFM.getAlbumObservable(artInfo.artistName, artInfo.albumName)
                .flatMap(new Func1<Album, Observable<String>>() {
                    @Override
                    public Observable<String> call(final Album album) {
                        String mbid = album.getMbid();
                        if (StringUtils.isEmpty(mbid)) {
                            return Observable.error(new Exception("No mbid for album " + album.getName()));
                        } else {
                            return mCoverArtArchive.getReleaseObservable(album.getMbid())
                                    .map(new Func1<Metadata, String>() {
                                        @Override
                                        public String call(Metadata metadata) {
                                            for (Metadata.Image image : metadata.images) {
                                                if (image.front && image.approved) {
                                                    return image.image;
                                                }
                                            }
                                            throw OnErrorThrowable.from(new Exception("No suitable " +
                                                    "artwork found for release " + metadata.release));
                                        }
                                    }).onErrorReturn(new Func1<Throwable, String>() {
                                        @Override
                                        public String call(Throwable throwable) {
                                            String url = LastFM.GET_BEST_IMAGE.call(album);
                                            if (!StringUtils.isEmpty(url)) {
                                                return url;
                                            }
                                            throw OnErrorThrowable.from(new Exception("No " +
                                                    "artwork found for album " + album.getName()));
                                        }
                                    });
                        }
                    }
                }).flatMap(new Func1<String, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(final String s) {
                        return createImageObservable(mangleImageUrl(s), artInfo);
                    }
                });
    }

    private Observable<Bitmap> tryForNetwork(final ArtInfo artInfo, final boolean tryFallbackOnFail) {
        Observable<Bitmap> o = createAlbumNetworkRequest(artInfo);
        if (tryFallbackOnFail) {
            o = o.onErrorResumeNext(new Func1<Throwable, Observable<? extends Bitmap>>() {
                @Override
                public Observable<? extends Bitmap> call(Throwable throwable) {
                    boolean isLocalArt = isLocalArtwork(artInfo.artworkUri);
                    if (isLocalArt) {
                        return tryForMediaStore(artInfo, false);
                    } else {
                        return createImageObservable(artInfo.artworkUri.toString(), artInfo);
                    }
                }
            });
        }
        return o;
    }

    private Observable<Bitmap> createImageObservable(final String url, final ArtInfo artInfo) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                InputStream is = null;
                try {
                    //We don't want okhttp clogging its cache with these images
                    CacheControl cc = new CacheControl.Builder().noStore().build();
                    Request req = new Request.Builder()
                            .url(url).get().cacheControl(cc).build();
                    Response response = mOkHttpClient.newCall(req).execute();
                    if (response.isSuccessful()) {
                        is = response.body().byteStream();
                        Bitmap bitmap = decodeBitmap(is, artInfo);
                        if (bitmap != null && !subscriber.isUnsubscribed()) {
                            subscriber.onNext(bitmap);
                            subscriber.onCompleted();
                            return;
                        } // else fall
                    } // else fall
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(new Exception("unable to decode " +
                                "bitmap for " + url));
                    }
                } catch (IOException | OutOfMemoryError e) {
                    if (!subscriber.isUnsubscribed()) subscriber.onError(e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        });
    }

    private Observable<Bitmap> tryForMediaStore(final ArtInfo artInfo, final boolean tryNetworkOnFailure) {
        Observable<Bitmap> o = createMediaStoreRequestObservable(artInfo);
        if (tryNetworkOnFailure) {
            o = o.onErrorResumeNext(new Func1<Throwable, Observable<Bitmap>>() {
                @Override
                public Observable<Bitmap> call(Throwable throwable) {
                    return tryForNetwork(artInfo, false);
                }
            });
        }
        return o;
    }

    private Observable<Bitmap> createMediaStoreRequestObservable(final ArtInfo artInfo) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                InputStream in = null;
                try {
                    final Uri uri = artInfo.artworkUri;
                    in = mContext.getContentResolver().openInputStream(uri);
                    Bitmap bitmap = decodeBitmap(in, artInfo);
                    if (bitmap != null && !subscriber.isUnsubscribed()) {
                        subscriber.onNext(bitmap);
                        subscriber.onCompleted();
                        return;
                    } //else fall
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(new Exception("Unable to decode bitmap for " + artInfo.toString()));
                    }
                } catch (Exception e) { //too many to keep track of
                    if (!subscriber.isUnsubscribed()) subscriber.onError(e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        }).subscribeOn(mSubscribeOn);
    }

    private static final Object sDecodeLock = new Object();
    @DebugLog
    private Bitmap decodeBitmap(InputStream is, ArtInfo artInfo) {
        if (is == null) return null;
        Bitmap bitmap = null;
        synchronized (sDecodeLock) {
            Bitmap tempBitmap2 = BitmapFactory.decodeStream(is);
            if (tempBitmap2 != null) {
                // Clip to squares so our circles dont become ovals
                int w = tempBitmap2.getWidth();
                int h = tempBitmap2.getHeight();
                StringBuilder sb = new StringBuilder();
                if (w > h) {
                    sb.append("Center cropping: ");
                    //center crop
                    bitmap = Bitmap.createBitmap(tempBitmap2, w / 2 - h / 2, 0, h, h);
                    tempBitmap2.recycle();
                } else if (h > w) {
                    sb.append("Top cropping: ");
                    // top crop
                    bitmap = Bitmap.createBitmap(tempBitmap2, 0, 0, w, w);
                    tempBitmap2.recycle();
                } else {
                    sb.append("Not cropping: ");
                    bitmap = tempBitmap2;
                }
                Timber.v(sb.append(" from %dx%d to %dx%d for %s").toString(),
                        w, h, bitmap.getWidth(), bitmap.getHeight(), artInfo.toString());
            }
        }
        if (bitmap != null) {
            writeToL2(artInfo.cacheKey(), bitmap);
        }
        return bitmap;
    }

    private void writeToL2(final String key, final Bitmap bitmap) {
        Timber.v("writeToL2(%s)", key);
        mL2Cache.putBitmap(key, bitmap);
    }

    //for testing
    protected String mangleImageUrl(String url) {
        return url;
    }

    public static boolean isLocalArtwork(Uri u) {
        if (u != null) {
            if (ContentResolver.SCHEME_CONTENT.equals(u.getScheme())
                    || ContentResolver.SCHEME_FILE.equals(u.getScheme())
                    || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(u.getScheme())){
                return true;
            }
        }
        return false;
    }

    private boolean isOnline(boolean wifiOnly) {
        boolean state = false;
        final NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            state = activeNetwork.isConnectedOrConnecting();
        }
        if (wifiOnly && state) {
            return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return state;
    }

}
