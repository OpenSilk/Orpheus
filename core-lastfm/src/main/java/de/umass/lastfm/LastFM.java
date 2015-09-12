/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package de.umass.lastfm;

import android.net.Uri;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;

import org.opensilk.music.lastfm.BuildConfig;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import timber.log.Timber;

/**
 * Util class to build volley requests for lastfm
 *
 * Created by drew on 3/14/14.
 */
@Singleton
public class LastFM {

    public static final String DEFAULT_API_ROOT = "http://ws.audioscrobbler.com/2.0/";

    private static final String PARAM_API_KEY = "api_key";
    private static final String PARAM_METHOD = "method";
    private static final String PARAM_ARTIST = "artist";
    private static final String PARAM_ALBUM = "album";
    private static final String PARAM_LANG = "lang";
    private static final String PARAM_AUTOCORRECT = "autocorrect";

    public static final String VAL_ARTIST_INFO = "artist.getInfo";
    public static final String VAL_ALBUM_INFO = "album.getInfo";

    private final RequestQueue mVolleyQueue;
    private final String mApiKey;

    @Inject
    public LastFM(
            RequestQueue mVolleyQueue
    ) {
        this.mVolleyQueue = mVolleyQueue;
        this.mApiKey = BuildConfig.LASTFM_KEY;//TODO allow using custom api key
    }

    public Observable<Album> newAlbumRequestObservable(final String artistName, final String albumName) {
        return Observable.create(new Observable.OnSubscribe<Album>() {
            @Override
            public void call(final Subscriber<? super Album> subscriber) {
                MusicEntryResponseCallback<Album> listener = new MusicEntryResponseCallback<Album>() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }
                    @Override
                    public void onResponse(Album album) {
                        if (subscriber.isUnsubscribed()) return;
                        if (!TextUtils.isEmpty(album.getMbid())) {
                            subscriber.onNext(album);
                            subscriber.onCompleted();
                        } else {
                            Timber.w("Api response does not contain mbid for %s", album.getName());
                            onErrorResponse(new VolleyError("Unknown mbid for " + album.getName()));
                        }
                    }
                };
                mVolleyQueue.add(newAlbumRequest(artistName, albumName, listener, Request.Priority.HIGH));
            }
        });
    }

    /**
     * @param artistName
     * @param albumName
     * @param listener
     * @return new Volley request
     */
    public Request<Album> newAlbumRequest(
            String artistName,
            String albumName,
            MusicEntryResponseCallback<Album> listener,
            Request.Priority priority
    ) {
        Uri fetchUrl = baseUriBuilder()
                .appendQueryParameter(PARAM_METHOD, VAL_ALBUM_INFO)
                .appendQueryParameter(PARAM_ARTIST, artistName)
                .appendQueryParameter(PARAM_ALBUM, albumName)
                .build();

        final AlbumRequest request = new AlbumRequest(fetchUrl.toString(), listener);
        request.setPriority(priority);
        return request;
    }

    public Observable<Artist> newArtistRequestObservable(final String artistName) {
        return Observable.create(new Observable.OnSubscribe<Artist>() {
            @Override
            public void call(final Subscriber<? super Artist> subscriber) {
                MusicEntryResponseCallback<Artist> listener = new MusicEntryResponseCallback<Artist>() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }

                    @Override
                    public void onResponse(Artist artist) {
                        if (subscriber.isUnsubscribed()) return;
                        if (!TextUtils.isEmpty(artist.getMbid())) {
                            subscriber.onNext(artist);
                            subscriber.onCompleted();
                        } else {
                            Timber.w("Api response does not contain mbid for %s", artist.getName());
                            onErrorResponse(new VolleyError("Unknown mbid for " + artist.getName()));
                        }
                    }
                };
                mVolleyQueue.add(newArtistRequest(artistName, listener, Request.Priority.HIGH));
            }
        });
    }

    /**
     * @param artistName
     * @param listener
     * @return new Volley request
     */
    public Request<Artist> newArtistRequest(
            String artistName,
            MusicEntryResponseCallback<Artist> listener,
            Request.Priority priority
    ) {
        Uri fetchUrl = baseUriBuilder()
                .appendQueryParameter(PARAM_METHOD, VAL_ARTIST_INFO)
                .appendQueryParameter(PARAM_ARTIST, artistName)
                .appendQueryParameter(PARAM_LANG, Locale.getDefault().getLanguage())
                .build();

        final ArtistRequest request = new ArtistRequest(fetchUrl.toString(), listener);
        request.setPriority(priority);
        return request;
    }

    private Uri.Builder baseUriBuilder() {
        Uri.Builder b = Uri.parse(DEFAULT_API_ROOT).buildUpon();
        b.appendQueryParameter(PARAM_API_KEY, mApiKey);
        b.appendQueryParameter(PARAM_AUTOCORRECT, "1");
        return b;
    }

    /**
     * @return url string for highest quality image available or null if none
     */
    public static String getBestImage(MusicEntry e, boolean wantHigResArt) {
        for (ImageSize q : ImageSize.values()) {
            if (q.equals(ImageSize.MEGA) && !wantHigResArt) {
                continue;
            }
            String url = e.getImageURL(q);
            if (!TextUtils.isEmpty(url)) {
                Timber.v("Found " + q.toString() + " url for " + e.getName());
                return url;
            }
        }
        return null;
    }

}
