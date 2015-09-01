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

import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;

import org.opensilk.music.lastfm.BuildConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
    public static final String PARAM_METHOD_ARTIST_INFO = "artist.getInfo";
    public static final String PARAM_METHOD_ALBUM_INFO = "album.getInfo";
    private static final String PARAM_ARTIST = "artist";
    private static final String PARAM_ALBUM = "album";

    private final RequestQueue mVolleyQueue;

    @Inject
    public LastFM(
            RequestQueue mVolleyQueue
    ) {
        this.mVolleyQueue = mVolleyQueue;
        //TODO allow using custom api key
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
                            onErrorResponse(new VolleyError("Unknown mbid"));
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
    public static Request<Album> newAlbumRequest(
            String artistName,
            String albumName,
            MusicEntryResponseCallback<Album> listener,
            Request.Priority priority
    ) {
        StringBuilder fetchUrl = baseUrl();
        fetchUrl.append(PARAM_METHOD_ALBUM_INFO)
                .append("&")
                .append(PARAM_ARTIST).append("=").append(encode(artistName))
                .append("&")
                .append(PARAM_ALBUM).append("=").append(encode(albumName));

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
                        subscriber.onNext(artist);
                        subscriber.onCompleted();
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
    public static Request<Artist> newArtistRequest(
            String artistName,
            MusicEntryResponseCallback<Artist> listener,
            Request.Priority priority
    ) {
        StringBuilder fetchUrl = baseUrl();
        fetchUrl.append(PARAM_METHOD_ARTIST_INFO)
                .append("&")
                .append(PARAM_ARTIST).append("=").append(encode(artistName))
                .append("&")
                .append("lang").append("=").append(Locale.getDefault().getLanguage());

        final ArtistRequest request = new ArtistRequest(fetchUrl.toString(), listener);
        request.setPriority(priority);
        return request;
    }



    /**
     * @return StringBuilder with default params already included
     */
    private static StringBuilder baseUrl() {
        StringBuilder baseUrl = new StringBuilder(200);
        baseUrl.append(DEFAULT_API_ROOT)
                .append("?")
                .append(PARAM_API_KEY).append("=").append(BuildConfig.LASTFM_KEY)
                .append("&")
                .append("autocorrect=1")
                .append("&")
                .append(PARAM_METHOD).append("=");
        return baseUrl;
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

    /**
     * @param param
     * @return UrlEncoded String
     */
    private static String encode(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;//never happen
    }

}
