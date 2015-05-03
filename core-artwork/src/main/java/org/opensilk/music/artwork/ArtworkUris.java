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

package org.opensilk.music.artwork;

import android.content.UriMatcher;
import android.net.Uri;

/**
 * Created by drew on 4/30/15.
 */
public class ArtworkUris {

    static String content = "content";
    static String artwork = "artwork";
    static String thumbnail = "thumbnail";
    static String albumReq = "req/album";
    static String artistReq = "req/artist";

    /**
     * @return Uri to retrieve large (fullscreen) artwork for specified albumId
     */
    public static Uri createArtworkUri(String authority, String artistName, String albumName) {
        return new Uri.Builder().scheme(content).authority(authority).appendPath(artwork)
                .appendPath(artistName).appendPath(albumName).build();
    }

    /**
     * @return Uri to retrieve thumbnail for specified albumId
     */
    public static Uri createThumbnailUri(String authority, String artistName, String albumName) {
        return new Uri.Builder().scheme(content).authority(authority).appendPath(thumbnail)
                .appendPath(artistName).appendPath(albumName).build();
    }

    public static Uri createAlbumReq(String authority, String base64Artinfo, ArtworkType type) {
        return new Uri.Builder().scheme(content).authority(authority).appendEncodedPath(albumReq)
                .appendQueryParameter("q", base64Artinfo).appendQueryParameter("t", type.toString()).build();
    }

    public static Uri createArtistReq(String authority, String base64Artinfo, ArtworkType type) {
        return new Uri.Builder().scheme(content).authority(authority).appendEncodedPath(artistReq)
                .appendQueryParameter("q", base64Artinfo).appendQueryParameter("t", type.toString()).build();
    }

    public interface MATCH {
        int ARTWORK = 1;
        int THUMBNAIL = 2;
        int ALBUM_REQ = 3;
        int ARTIST_REQ = 4;
    }

    //XXX when adding new matches be sure to update both the provider and the fetchermanager
    public static UriMatcher makeMatcher(String authority) {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(authority, artwork + "/*/*", MATCH.ARTWORK);
        matcher.addURI(authority, thumbnail + "/*/*", MATCH.THUMBNAIL);
        matcher.addURI(authority, albumReq, MATCH.ALBUM_REQ);
        matcher.addURI(authority, artistReq, MATCH.ARTIST_REQ);
        return matcher;
    }
}
