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

package org.opensilk.music;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.theme.OrpheusTheme;
import org.opensilk.music.ui2.gallery.GalleryPage;
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 6/21/14.
 */
@Singleton
public class AppPreferences {

    //Gallery pages
    public static final int DEFAULT_PAGE = 2;
    public static final String START_PAGE = "start_page";
    public static final String HOME_PAGES = "pref_home_pages";

    //Gallery sort orders
    public static final String ARTIST_SORT_ORDER = "artist_sort_order";
    public static final String ALBUM_SORT_ORDER = "album_sort_order";
    public static final String SONG_SORT_ORDER = "song_sort_order";
    public static final String GENRE_SORT_ORDER = "genre_sort_order";
    public static final String PLAYLIST_SORT_ORDER = "playlist_sort_order";

    //profile sort orders
    public static final String ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order";
    public static final String ALBUM_SONG_SORT_ORDER = "album_song_sort_order";
    public static final String GENRE_ALBUM_SORT_ORDER = "genre_album_sort_order";
    public static final String SONG_COLLECTION_SORT_ORDER = "song_collection_sort_order";

    //Gallery list styles
    public static final String ARTIST_LAYOUT = "artist_layout";
    public static final String ALBUM_LAYOUT = "album_layout";
    public static final String GENRE_LAYOUT = "genre_layout";
    public static final String PLAYLIST_LAYOUT = "playlist_layout";
    // values for list styles
    public static final String SIMPLE = "simple";
    public static final String GRID = "grid";

    // Artwork
    public static final String ONLY_ON_WIFI = "only_on_wifi";
    public static final String DOWNLOAD_MISSING_ARTWORK = "download_missing_artwork";
    public static final String PREFER_DOWNLOAD_ARTWORK = "prefer_download_artwork";
    public static final String DOWNLOAD_MISSING_ARTIST_IMAGES = "download_missing_artist_images";
    public static final String IMAGE_DISK_CACHE_SIZE = "pref_cache_size";
    public static final String WANT_LOW_RESOLUTION_ART = "pref_low_resolution";

    //Theme
    public static final String WANT_DARK_THEME = "pref_dark_theme";
    public static final String ORPHEUS_THEME = "orpheus_theme";

    //Now Playing
    public static final String NOW_PLAYING_START_SCREEN = "now_playing_start_screen";
    //values must mirror array
    public static final String NOW_PLAYING_SCREEN_ARTWORK = "artwork";
    public static final String NOW_PLAYING_SCREEN_CONTROLS = "controls";

    // action button
    public static final String FAB_CLICK = "fab_click";
    public static final String FAB_DOUBLE_CLICK = "fab_double_click";
    public static final String FAB_LONG_CLICK = "fab_long_click";
    public static final String FAB_FLING = "fab_fling";
    public static final String FAB_ACTION_PLAYPAUSE = "play_pause";
    public static final String FAB_ACTION_QUICK_CONTROLS = "quick_controls";
    public static final String FAB_ACTION_OPEN_NOW_PLAYING = "open_now_playing";
    public static final String FAB_ACTION_NONE = "none";
    public static final String FAB_SHOWCASE = "was_shown_fab_showcase";

    //Misc
    public static final String AUTO_SHUFFLE_FOLDER = "auto_shuffle_directory";
    public static final String SEND_CRASH_REPORTS = "send_crash_reports";


    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson;

    @Inject
    public AppPreferences(@ForApplication Context context, Gson gson) {
        appContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.gson = gson;
    }

    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public void putLong(String key, long value) {
        prefs.edit().putLong(key, value).apply();
    }

    public void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public boolean getBoolean(String key, boolean def) {
        return prefs.getBoolean(key, def);
    }

    public long getLong(String key, long def) {
        return prefs.getLong(key, def);
    }

    public int getInt(String key, int def) {
        return prefs.getInt(key, def);
    }

    public String getString(String key, String def) {
        return prefs.getString(key, def);
    }

    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    /*
     * Home pages
     */

    public final void saveGalleryPages(List<GalleryPage> pages) {
        try {
            Type type = new TypeToken<List<GalleryPage>>() {}.getType();
            putString(HOME_PAGES, gson.toJson(pages, type));
        } catch (Exception e) {
            remove(HOME_PAGES);
        }
    }

    public final List<GalleryPage> getGalleryPages() {
        String pgs = getString(HOME_PAGES, null);
        if (pgs != null) {
            try {
                Type type = new TypeToken<List<GalleryPage>>() {}.getType();
                return gson.fromJson(pgs, type);
            } catch (Exception ignored) {
                remove(HOME_PAGES);
            }
        }
        return Arrays.asList(GalleryPage.values());
    }

    /*
     * Plugins
     */

    public LibraryInfo getDefaultLibraryInfo(PluginInfo pluginInfo) {
        String json = getString(makePluginPrefKey(pluginInfo), null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, LibraryInfo.class);
        } catch (Exception e) {
            removeDefaultLibraryInfo(pluginInfo);
            return null;
        }
    }

    public void setDefaultLibraryInfo(PluginInfo pluginInfo, LibraryInfo libraryInfo) {
        String json = gson.toJson(libraryInfo);
        if (json != null) {
            putString(makePluginPrefKey(pluginInfo), json);
        }
    }

    public void removeDefaultLibraryInfo(PluginInfo pluginInfo) {
        remove(makePluginPrefKey(pluginInfo));
    }

    private String makePluginPrefKey(PluginInfo pluginInfo) {
        return pluginInfo.componentName.flattenToString().replaceAll("/", "_")+"_defInfo";
    }

    /*
     * Auto shuffle
     */

    /*
     * This might not be the best way but since it is used in multiple processes
     * im excluding it from the standard prefs in order to avoid MODE_MULTI_PROCESS
     * since that doesnt cache values and we hit the SharedPrefs /a lot/
     */
    public static boolean writeAutoShuffleDirectory(Context context, String directory) {
        try {
            File f = new File(context.getFilesDir(), AUTO_SHUFFLE_FOLDER);
            FileUtils.writeLines(f, Collections.singleton(directory));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String readAutoShuffleDirectory(Context context) {
        try {
            File f = new File(context.getFilesDir(), AUTO_SHUFFLE_FOLDER);
            return FileUtils.readLines(f).get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /*
     * Theme
     */

    public OrpheusTheme getTheme() {
        try {
            return OrpheusTheme.valueOf(getString(ORPHEUS_THEME, OrpheusTheme.DEFAULT.toString()));
        } catch (Exception e) {
            remove(ORPHEUS_THEME);
            return OrpheusTheme.DEFAULT;
        }
    }

    public boolean isDarkTheme() {
        return getBoolean(WANT_DARK_THEME, false);
    }

}
