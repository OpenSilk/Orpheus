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
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Base64;

import org.opensilk.common.core.app.PreferencesWrapper;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.ui.theme.OrpheusTheme;

import java.nio.charset.Charset;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 6/21/14.
 */
@Singleton
public class AppPreferences extends PreferencesWrapper {

    private static final String VERSION = "__version__";
    private static final int MY_VERSION = 303;

    //Theme
    public static final String WANT_DARK_THEME = "pref_dark_theme";
    public static final String ORPHEUS_THEME = "orpheus_theme";
    //launcher activity
    public static final String LAST_NAVIGATION_ITEM = "last_navigation_item";
    //intro
    public static final String WAS_SHOWN_CHANGES = "was_shown_changes";
    private static final int CHANGES_ORDINAL = 2; //increment when updated

    //Gallery pages
    public static final int DEFAULT_GALLERY_PAGE = 2;

    //Playlistpages
    public static final int DEFAULT_PLAYLISTS_PAGE = 0;
    public static final String PLAYLISTS_START_PAGE = "playlists.start_page";

    //Now playing
    public static final String NOW_PLAYING_KEEP_SCREEN_ON = "now_playing.keep_screen_on";
    public static final String NOW_PLAYING_VIEW = "now_playing.view";
    //values must mirror array
    public static final String NOW_PLAYING_VIEW_ARTWORK = "artwork";
    public static final String NOW_PLAYING_VIEW_VIS_CIRCLE = "vis_circle";
    public static final String NOW_PLAYING_VIEW_VIS_CIRCLE_BAR = "vis_circle_bar";
    public static final String NOW_PLAYING_VIEW_VIS_LINES = "vis_lines";

    // values for list styles
    public static final String SIMPLE = "simple";
    public static final String GRID = "grid";

    private final SharedPreferences prefs;

    @Inject
    public AppPreferences(@ForApplication Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
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
        if (schema < 302) {
            getPrefs().edit()
                    //moved to their own file
                    .remove("only_on_wifi")
                    .remove("download_missing_artwork")
                    .remove("prefer_download_artwork")
                    .remove("download_missing_artist_images")
                    .remove("pref_cache_size")
                    .remove("pref_low_resolution")
                    //no longer used
                    .remove("is_first_run")
                    .remove("default_library")
                    .remove("last_used_plugin")
                    .remove("disabled_plugins")
                    //managed per uri now
                    .remove("artist_sort_order")
                    .remove("album_sort_order")
                    .remove("song_sort_order")
                    .remove("genre_sort_order")
                    .remove("playlist_sort_order")
                    .remove("artist_album_sort_order")
                    .remove("album_song_sort_order")
                    .remove("track_collection_sort_order")
                    .remove("song_collection_sort_order")
                    .remove("track_sort_order")
                    .remove("album_track_sort_order")
                    .remove("genre_album_sort_order")
                    .remove("genre_track_sort_order")
                    .remove("artist_layout")
                    .remove("album_layout")
                    .remove("genre_layout")
                    .remove("playlist_layout")
                    .remove("artist_album_layout")
                    .remove("genre_album_layout")
                    //no longer used
                    .remove("start_page")
                    .remove("pref_home_pages")
                    .remove("gallery_home_pages")
                    //footer click always opens now playing
                    .remove("footer_click")
                    .remove("footer_long_click")
                    //no longer clickable
                    .remove("footer_thumb_click")
                    .remove("footer_thumb_long_click")
                    //no more multi action fab
                    .remove("fab_click")
                    .remove("fab_double_click")
                    .remove("fab_long_click")
                    .remove("fab_fling")
                    //only one layout now
                    .remove("now_playing_layout")
                    .remove("now_playing_start_controls")
                    .remove("now_playing_view")
                    .apply();
        }
        if (schema < 303) {
            prefs.edit()
                    //prefixed by authority now
                    .remove("gallery.start_page")
                    .apply();
        }
        if (schema < MY_VERSION) {
            putInt(VERSION, MY_VERSION);
        }
    }

    public String makePrefKey(String root, String key) {
        return root + "." + key;
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

    /*
     * Sort order
     */

    public String sortOrderKey(Uri uri) {
        return makePrefKey(encodeString(uri.toString()), ".sortorder");
    }

    public String getSortOrder(Uri uri, String defaultSort) {
        return getString(makePrefKey(encodeString(uri.toString()), ".sortorder"), defaultSort);
    }

    /*
     * Layouts
     */

    public String layoutKey(Uri uri) {
        return makePrefKey(encodeString(uri.toString()), ".layout");
    }

    public String getLayout(Uri uri, boolean defaultGrid) {
        return getString(makePrefKey(encodeString(uri.toString()), ".layout"), defaultGrid ? GRID : SIMPLE);
    }

    /*
     * gallery
     */

    public String galleryStartPageKey(String authority) {
        return makePrefKey(encodeString(authority), ".gallery_start_page");
    }

    public int getGalleryStartPage(String authority) {
        return getInt(galleryStartPageKey(authority), DEFAULT_GALLERY_PAGE);
    }


    /*
     * Changes dialog
     */

    public boolean shouldShowIntro() {
        return getInt(WAS_SHOWN_CHANGES, 0) < CHANGES_ORDINAL;
    }

    public void setWasShownIntro() {
        putInt(WAS_SHOWN_CHANGES, CHANGES_ORDINAL);
    }

    private static String encodeString(String string) {
        return Base64.encodeToString(string.getBytes(Charset.defaultCharset()),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private static String decodeString(String string) {
        return new String(Base64.decode(string, Base64.URL_SAFE), Charset.defaultCharset());
    }

}
