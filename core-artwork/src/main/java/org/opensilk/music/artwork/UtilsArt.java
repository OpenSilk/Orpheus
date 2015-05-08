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

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.opensilk.common.core.app.BaseApp;
import org.opensilk.music.model.ArtInfo;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;

import de.umass.util.StringUtilities;

import static org.opensilk.music.artwork.Constants.*;

/**
 * Created by drew on 4/30/15.
 */
public class UtilsArt {
    /**
     * Creates a cache key for use with the L1 cache.
     *
     * if both artist and album are null we must use the uri or all
     * requests will return the same cache key, to maintain backwards
     * compat with orpheus versions < 0.5 we use artist,album when we can.
     *
     * if all fields in artinfo are null we cannot fetch any art so an npe will
     * be thrown
     */
    public static String getCacheKey(ArtInfo artInfo, ArtworkType imageType) {
        int size = 0;
        if (artInfo.artistName == null && artInfo.albumName == null) {
            if (artInfo.artworkUri == null) {
                throw new NullPointerException("Cant fetch art with all null fields");
            }
            size += artInfo.artworkUri.toString().length();
            return new StringBuilder(size+12)
                    .append("#").append(imageType).append("#")
                    .append(artInfo.artworkUri.toString())
                    .toString();
        } else {
            size += artInfo.artistName != null ? artInfo.artistName.length() : 4;
            size += artInfo.albumName != null ? artInfo.albumName.length() : 4;
            return new StringBuilder(size+12)
                    .append("#").append(imageType).append("#")
                    .append(artInfo.artistName).append("#")
                    .append(artInfo.albumName)
                    .toString();
        }
    }

    public static String base64EncodedJsonArtInfo(Gson gson, ArtInfo artInfo) {
        return Base64.encodeToString(gson.toJson(artInfo).getBytes(), Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
    }

    public static ArtInfo artInfoFromBase64EncodedJson(Gson gson, String string) {
        return gson.fromJson(new String(Base64.decode(string, Base64.URL_SAFE), Charset.defaultCharset()), ArtInfo.class);
    }

    public static int calculateL1CacheSize(Context context, boolean forceLarge) {
        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final int memClass = (forceLarge || !BaseApp.isLowEndHardware(context)) ? am.getLargeMemoryClass() : am.getMemoryClass();
        return Math.round(THUMB_MEM_CACHE_DIVIDER * memClass * 1024 * 1024);
    }

    public static ArtInfo makeBestfitArtInfo(String artist, String altArtist, String album, Uri uri) {
        if (uri != null) {
            if (artist == null || album == null) {
                // we need both to make a query but we have uri so just use that,
                // note this will prevent cache from returning artist images when album is null
                return new ArtInfo(null, null, uri);
            } else {
                return new ArtInfo(artist, album, uri);
            }
        } else {
            if (artist == null && altArtist != null) {
                // cant fallback to uri so best guess the artist
                // note this is a problem because the song artist may not be the
                // album artist but we have no choice here, also note the service
                // does the same thing so at least it will be consistent
                return new ArtInfo(altArtist, album, null);
            } else {
                // if everything is null the artworkmanager will set the default image
                // so no further validation is needed here.
                return ArtInfo.NULLINSTANCE;
            }
        }
    }
}
