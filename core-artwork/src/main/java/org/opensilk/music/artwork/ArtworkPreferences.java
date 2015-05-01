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

import android.content.Context;
import android.content.SharedPreferences;

import org.opensilk.common.core.app.PreferencesWrapper;
import org.opensilk.common.core.dagger2.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 4/30/15.
 */
@Singleton
public class ArtworkPreferences extends PreferencesWrapper {
    public static final String NAME = "artwork";

    // Artwork
    public static final String ONLY_ON_WIFI = "only_on_wifi";
    public static final String DOWNLOAD_MISSING_ARTWORK = "download_missing_artwork";
    public static final String PREFER_DOWNLOAD_ARTWORK = "prefer_download_artwork";
    public static final String DOWNLOAD_MISSING_ARTIST_IMAGES = "download_missing_artist_images";
    public static final String IMAGE_DISK_CACHE_SIZE = "pref_cache_size";
    public static final String WANT_LOW_RESOLUTION_ART = "pref_low_resolution";

    final Context appcontext;
    private final Object sPrefLock = new Object();
    private volatile boolean needReload;
    private SharedPreferences prefs;

    @Inject
    public ArtworkPreferences(@ForApplication Context appcontext) {
        this.appcontext = appcontext;
    }

    public void reloadPrefs() {
        needReload = true;
    }

    @Override
    protected SharedPreferences getPrefs() {
        synchronized (sPrefLock) {
            if (needReload || prefs == null) {
                prefs = appcontext.getSharedPreferences(NAME, Context.MODE_MULTI_PROCESS);
            }
        }
        return prefs;
    }
}
