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

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.app.PreferencesWrapper;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.ui.theme.OrpheusTheme;
import org.opensilk.music.ui3.gallery.GalleryPage;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Created by drew on 6/21/14.
 */
@Singleton
public class AppPreferences extends PreferencesWrapper {

    private static final String VERSION = "__version__";
    private static final int MY_VERSION = 300;

    //Interface
    public static final String KEEP_SCREEN_ON = "keep_screen_on";

    //Gallery pages
    public static final int DEFAULT_PAGE = 2;
    public static final String GALLERY_START_PAGE = "gallery_start_page";
    public static final String GALLERY_HOME_PAGES = "gallery_home_pages";

    //Gallery sort orders
    public static final String ALBUM_SORT_ORDER = "album_sort_order";
    public static final String ARTIST_SORT_ORDER = "artist_sort_order";
    public static final String FOLDER_SORT_ORDER = "folder_sort_order";
    public static final String GENRE_SORT_ORDER = "genre_sort_order";
    public static final String PLAYLIST_SORT_ORDER = "playlist_sort_order";
    public static final String TRACK_SORT_ORDER = "track_sort_order";

    //profile sort orders
    public static final String ALBUM_TRACK_SORT_ORDER = "album_track_sort_order";
    public static final String ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order";
    public static final String ARTIST_TRACK_SORT_ORDER = "artist_track_sort_order";
    public static final String GENRE_ALBUM_SORT_ORDER = "genre_album_sort_order";
    public static final String GENRE_TRACK_SORT_ORDER = "genre_track_sort_order";

    //Gallery list styles
    public static final String ARTIST_LAYOUT = "artist_layout";
    public static final String ALBUM_LAYOUT = "album_layout";
    public static final String GENRE_LAYOUT = "genre_layout";
    public static final String PLAYLIST_LAYOUT = "playlist_layout";
    // values for list styles
    public static final String SIMPLE = "simple";
    public static final String GRID = "grid";

    //profile list styles
    public static final String ARTIST_ALBUM_LAYOUT = "artist_album_layout";
    public static final String GENRE_ALBUM_LAYOUT = "genre_album_layout";

    //Theme
    public static final String WANT_DARK_THEME = "pref_dark_theme";
    public static final String ORPHEUS_THEME = "orpheus_theme";

    //Now Playing
    public static final String NOW_PLAYING_VIEW = "now_playing_view";
    //values must mirror array
    public static final String NOW_PLAYING_VIEW_ARTWORK = "artwork";
    public static final String NOW_PLAYING_VIEW_VIS_CIRCLE = "vis_circle";
    public static final String NOW_PLAYING_VIEW_VIS_CIRCLE_BAR = "vis_circle_bar";
    public static final String NOW_PLAYING_VIEW_VIS_LINES = "vis_lines";

    public static final String NOW_PLAYING_ARTWORK_SCALE = "now_playing_artwork_scale";
    //values must mirror array
    public static final String NOW_PLAYING_ARTWORK_FILL = "fill";
    public static final String NOW_PLAYING_ARTWORK_FIT = "fit";

    // action button
    public static final String FAB_CLICK = "fab_click";
    public static final String FAB_DOUBLE_CLICK = "fab_double_click";
    public static final String FAB_LONG_CLICK = "fab_long_click";
    public static final String FAB_FLING = "fab_fling";

    //common actions
    public static final String ACTION_PLAYPAUSE = "play_pause";
    public static final String ACTION_QUICK_CONTROLS = "quick_controls";
    public static final String ACTION_OPEN_NOW_PLAYING = "open_now_playing";
    public static final String ACTION_OPEN_QUEUE = "open_queue";
    public static final String ACTION_NONE = "none";

    //Misc
    public static final String FIRST_RUN = "is_first_run";

    //library plugins
    public static final String DISABLED_PLUGINS = "disabled_plugins";
    public static final String DEFAULT_LIBRARY = "default_library";

    public static final String LAST_PLUGIN_AUTHORITY = "last_used_plugin";

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson;

    @Inject
    public AppPreferences(@ForApplication Context context, Gson gson) {
        appContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.gson = gson;
        checkSchemaVersion();
    }

    @Override
    public SharedPreferences getPrefs() {
        return prefs;
    }

    private void checkSchemaVersion() {
        int schema = getInt(VERSION, 0);
        //I only started versioning in 3.0 so fresh installs will just have to deal.
        //made up version. walked githistory and found all the stuff i removed
        if (schema < 200) {
            getPrefs().edit()
                    .remove("was_shown_fab_showcase")
                    .remove("send_crash_reports")
                    .remove("now_playing_start_screen")
                    .remove("now_playing_colorize")
                    .remove("now_playing_palette")
                    .remove("artist_song_sort_order")
                    .remove("last_folder_browser_path")
                    .remove("auto_shuffle_directory")
                    .remove("default_media_folder")
                    .remove("pref_cast_enabled")
                    .remove("pref_default_media_folder")
                    .apply();
        }
        //changes made in 3.0
        if (schema < 300) {
            getPrefs().edit()
                    //moved to their own file
                    .remove("only_on_wifi")
                    .remove("download_missing_artwork")
                    .remove("prefer_download_artwork")
                    .remove("download_missing_artist_images")
                    .remove("pref_cache_size")
                    .remove("pref_low_resolution")
                    //changed from component name to authority name
                    .remove(DISABLED_PLUGINS)
                    //managed per plugin now
                    .remove(ARTIST_SORT_ORDER)
                    .remove(ALBUM_SORT_ORDER)
                    .remove("song_sort_order")
                    .remove(GENRE_SORT_ORDER)
                    .remove(PLAYLIST_SORT_ORDER)
                    .remove(ARTIST_ALBUM_SORT_ORDER)
                    .remove("album_song_sort_order")
                    .remove("track_collection_sort_order")
                    .remove("song_collection_sort_order")
                    .remove(ARTIST_LAYOUT)
                    .remove(ALBUM_LAYOUT)
                    .remove(GENRE_LAYOUT)
                    .remove(PLAYLIST_LAYOUT)
                    .remove("start_page")
                    .remove("pref_home_pages")
                    //only opens now playing now
                    .remove("footer_click")
                    .remove("footer_long_click")
                    .remove("footer_thumb_click")
                    .remove("footer_thumb_long_click")
                    //new layout
                    .remove("now_playing_start_controls")
                    .apply();
        }
        if (schema < MY_VERSION) {
            putInt(VERSION, MY_VERSION);
        }
    }

    /*
     * Home pages
     */

    public final void saveGalleryPages(List<GalleryPage> pages) {
        try {
            Type type = new TypeToken<List<GalleryPage>>() {}.getType();
            putString(GALLERY_HOME_PAGES, gson.toJson(pages, type));
        } catch (Exception e) {
            remove(GALLERY_HOME_PAGES);
        }
    }

    public final List<GalleryPage> getGalleryPages() {
        String pgs = getString(GALLERY_HOME_PAGES, null);
        if (pgs != null) {
            try {
                Type type = new TypeToken<List<GalleryPage>>() {}.getType();
                return gson.fromJson(pgs, type);
            } catch (Exception ignored) {
                remove(GALLERY_HOME_PAGES);
            }
        }
        return Arrays.asList(GalleryPage.values());
    }

    /*
     * Plugins
     */

    public void setPluginEnabled(String authority) {
        List<String> disabledPlugins = new ArrayList<>(readDisabledPlugins());
        Iterator<String> ii = disabledPlugins.iterator();
        while (ii.hasNext()) {
            if (authority.equals(ii.next())) {
                ii.remove();
            }
        }
        writeDisabledPlugins(disabledPlugins);
    }

    public void setPluginDisabled(String authority) {
        List<String> disabledPlugins = new ArrayList<>(readDisabledPlugins());
        for (String cn : disabledPlugins) {
            if (authority.equals(cn)) {
                return;
            }
        }
        disabledPlugins.add(authority);
        writeDisabledPlugins(disabledPlugins);
    }

    public List<String> readDisabledPlugins() {
        String json = getString(DISABLED_PLUGINS, null);
        Timber.v("Read disabled plugins=" + json);
        if (json != null) {
            try {
                return gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
            } catch (Exception e) {
                Timber.w(e, "Unable to deserialize %s", json);
                remove(DISABLED_PLUGINS);
            }
        }
        return Collections.emptyList();
    }

    public void writeDisabledPlugins(List<String> authorities) {
        try {
            String json = gson.toJson(authorities, new TypeToken<List<String>>(){}.getType());
            Timber.v("Write disabled plugins=%s", json);
            putString(DISABLED_PLUGINS, json);
        } catch (Exception e) {
            Timber.w(e, "Unable to serialize %s", Arrays.toString(authorities.toArray()));
        }
    }

    public LibraryInfo getLibraryInfo(LibraryConfig pluginInfo, String pref) {
        String json = getString(makePluginPrefKey(pluginInfo, pref), null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, LibraryInfo.class);
        } catch (Exception e) {
            removeLibraryInfo(pluginInfo, pref);
            return null;
        }
    }

    public void setLibraryInfo(LibraryConfig pluginInfo, String pref, LibraryInfo libraryInfo) {
        String json = gson.toJson(libraryInfo);
        if (json != null) {
            putString(makePluginPrefKey(pluginInfo, pref), json);
        }
    }

    public void removeLibraryInfo(LibraryConfig pluginInfo, String pref) {
        remove(makePluginPrefKey(pluginInfo, pref));
    }

    public String makePluginPrefKey(LibraryConfig libraryConfig, String key) {
        return libraryConfig.authority+"."+key;
    }

    /*
     * Layouts
     */

    public boolean isGrid(String key, String def) {
        return StringUtils.equals(getPrefs().getString(key, def), GRID);
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
