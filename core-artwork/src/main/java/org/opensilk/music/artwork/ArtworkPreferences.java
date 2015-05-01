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

import android.content.SharedPreferences;

import org.opensilk.common.core.app.PreferencesWrapper;

/**
 * Created by drew on 4/30/15.
 */
public class ArtworkPreferences extends PreferencesWrapper {
    // Artwork
    public static final String ONLY_ON_WIFI = "only_on_wifi";
    public static final String DOWNLOAD_MISSING_ARTWORK = "download_missing_artwork";
    public static final String PREFER_DOWNLOAD_ARTWORK = "prefer_download_artwork";
    public static final String DOWNLOAD_MISSING_ARTIST_IMAGES = "download_missing_artist_images";
    public static final String IMAGE_DISK_CACHE_SIZE = "pref_cache_size";
    public static final String WANT_LOW_RESOLUTION_ART = "pref_low_resolution";

    @Override
    protected SharedPreferences getPrefs() {
        return null;
    }
}
