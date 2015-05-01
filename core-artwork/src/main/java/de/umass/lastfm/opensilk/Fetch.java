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

package de.umass.lastfm.opensilk;

import com.android.volley.Request;

import org.opensilk.music.artwork.BuildConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;

/**
 * Util class to build volley requests for lastfm
 *
 * Created by drew on 3/14/14.
 */
public class Fetch {

    public static final String DEFAULT_API_ROOT = "http://ws.audioscrobbler.com/2.0/";

    private static final String PARAM_API_KEY = "api_key";
    private static final String PARAM_METHOD = "method";
    public static final String PARAM_METHOD_ARTIST_INFO = "artist.getInfo";
    public static final String PARAM_METHOD_ALBUM_INFO = "album.getInfo";
    private static final String PARAM_ARTIST = "artist";
    private static final String PARAM_ALBUM = "album";

    /**
     * @param artistName
     * @param listener
     * @return new Volley request
     */
    public static Request<Artist> artistInfo(String artistName,
                                             MusicEntryResponseCallback<Artist> listener,
                                             Request.Priority priority) {
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
     * @param artistName
     * @param albumName
     * @param listener
     * @return new Volley request
     */
    public static Request<Album> albumInfo(String artistName, String albumName,
                                           MusicEntryResponseCallback<Album> listener,
                                           Request.Priority priority) {
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
