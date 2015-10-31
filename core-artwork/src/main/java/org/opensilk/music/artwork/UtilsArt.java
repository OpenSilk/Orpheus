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

import org.opensilk.common.core.app.BaseApp;
import org.opensilk.music.model.ArtInfo;

import static org.opensilk.music.artwork.Constants.THUMB_MEM_CACHE_DIVIDER;

/**
 * Created by drew on 4/30/15.
 */
public class UtilsArt {

    public static int calculateL1CacheSize(Context context, boolean forceLarge) {
        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final int memClass = (forceLarge || !BaseApp.isLowEndHardware(context)) ? am.getLargeMemoryClass() : am.getMemoryClass();
        return Math.round(THUMB_MEM_CACHE_DIVIDER * memClass * 1024 * 1024);
    }

    /**
     * This function is used universally by the app to maintain consistent art and cache keys
     * This is only for album artwork
     *
     * @param artist The album artist
     * @param altArtist The track artist
     * @param album The album
     * @param uri Fallback uri
     * @return Bestfitted artinfo.
     */
    public static ArtInfo makeBestfitArtInfo(String artist, String altArtist, String album, Uri uri) {
        if (artist != null && album != null && uri != null) {
            // we have everything yay.
            return ArtInfo.forAlbum(artist, album, uri);
        } else if (artist != null && album != null) {
            // second best fit
            return ArtInfo.forAlbum(artist, album, null);
        } else if (uri != null) {
            // we need both artist and album to make a query but we have uri so just use that,
            // this will prevent cache from returning artist images when album is null
            return ArtInfo.forAlbum(null, null, uri);
        } else if (artist == null && altArtist != null && album != null) {
            // cant fallback to uri so best guess the artist
            // note this is a problem because the song artist may not be the
            // album artist but we have no choice here
            return ArtInfo.forAlbum(altArtist, album, null);
        } else {
            // Not enough info. Force the default image
            return ArtInfo.NULLINSTANCE;
        }
    }
}
