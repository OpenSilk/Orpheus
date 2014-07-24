/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.andrew.apollo.R;

import org.opensilk.cast.util.Utils;
import org.opensilk.music.cast.CastUtils;
import org.opensilk.music.ui.home.MusicFragment;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 * A collection of helpers designed to get and set various preferences across
 * Apollo.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class PreferenceUtils {

    // Sort order for the artist song list
    public static final String ARTIST_SONG_SORT_ORDER = "artist_song_sort_order";

    // Sort order for the artist album list
    public static final String ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order";

    // Sort order for the album song list
    public static final String ALBUM_SONG_SORT_ORDER = "album_song_sort_order";

    // Sets the type of layout to use for the recent list
    public static final String RECENT_LAYOUT = "recent_layout";

    // Key used to download images only on Wi-Fi
    public static final String ONLY_ON_WIFI = "only_on_wifi";

    // Key that gives permissions to download missing album covers
    public static final String DOWNLOAD_MISSING_ARTWORK = "download_missing_artwork";

    // Key to determine whether to try the network before local storage
    public static final String PREFER_DOWNLOAD_ARTWORK = "prefer_download_artwork";

    // Key that gives permissions to download missing artist images
    public static final String DOWNLOAD_MISSING_ARTIST_IMAGES = "download_missing_artist_images";

    // Key used to set image cache size
    public static final String PREF_CACHE_SIZE = "pref_cache_size";

    // Key used to set the overall theme color
    public static final String DEFAULT_THEME_COLOR = "default_theme_color";

    // Key used to set current theme
    public static final String THEME_STYLE = "theme_style";

    // Boolean to use dark themes
    public static final String PREF_DARK_THEME = "pref_dark_theme";

    // Key used to determine if casting should be enabled
    public static final String KEY_CAST_ENABLED = "pref_cast_enabled";

    // Key to decide whether we prefer high quality art
    public static final String USE_LOW_RESOLUTION_ART = "pref_low_resolution";

    //Key whether or not to show visualizations.
    public static final String SHOW_VISUALIZATIONS = "pref_visualizations";



    private static PreferenceUtils sInstance;

    private final SharedPreferences mPreferences;
    private final Context mContext;

    /**
     * Constructor for <code>PreferenceUtils</code>
     * 
     * @param context The {@link Context} to use.
     */
    public PreferenceUtils(final Context context) {
        mContext = context.getApplicationContext();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    /**
     * @param context The {@link Context} to use.
     * @return A singleton of this class
     */
    public static final PreferenceUtils getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new PreferenceUtils(context);
        }
        return sInstance;
    }

    public void putBoolean(String key, boolean value) {
        mPreferences.edit().putBoolean(key, value).apply();
    }

    public void putLong(String key, long value) {
        mPreferences.edit().putLong(key, value).apply();
    }

    public void putInt(String key, int value) {
        mPreferences.edit().putInt(key, value).apply();
    }

    public boolean getBoolean(String key, boolean def) {
        return mPreferences.getBoolean(key, def);
    }

    public long getLong(String key, long def) {
        return mPreferences.getLong(key, def);
    }

    public int getInt(String key, int def) {
        return mPreferences.getInt(key, def);
    }

    /**
     * Sets theme used by themehelper to choose resources
     * @param themeStyle
     */
    public void setThemeStyle(final ThemeStyle themeStyle) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(THEME_STYLE, themeStyle.toString());
        editor.commit();
    }

    /**
     * gets active theme
     * @return
     */
    public final ThemeStyle getThemeStyle() {
        return ThemeStyle.valueOf(mPreferences.getString(THEME_STYLE, ThemeStyle.ORPHEUS.toString()));
    }

    /**
     * Sets the new theme color.
     *
     * @param value The new theme color to use.
     */
    public void setDefaultThemeColor(final int value) {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putInt(DEFAULT_THEME_COLOR, value);
                editor.apply();

                return null;
            }
        }, (Void[])null);
    }

    public boolean wantDarkTheme() {
        return mPreferences.getBoolean(PREF_DARK_THEME, false);
    }

    /**
     * Returns the current theme color.
     * 
     * @param context The {@link Context} to use.
     * @return The default theme color.
     */
    @Deprecated
    public final int getDefaultThemeColor(final Context context) {
        return mPreferences.getInt(DEFAULT_THEME_COLOR,
                context.getResources().getColor(R.color.holo_blue_light));
    }

    /**
     * @return True if the user has checked to only download images on Wi-Fi,
     *         false otherwise
     */
    public final boolean onlyOnWifi() {
        return mPreferences.getBoolean(ONLY_ON_WIFI, true);
    }

    /**
     * @return True if the user has checked to download missing album covers,
     *         false otherwise.
     */
    public final boolean downloadMissingArtwork() {
        return mPreferences.getBoolean(DOWNLOAD_MISSING_ARTWORK, true);
    }

    /**
     * @return True if user has checked to try network first before local storage,
     *         false if they want to try local first
     */
    public final boolean preferDownloadArtwork() {
        return mPreferences.getBoolean(PREFER_DOWNLOAD_ARTWORK, false);
    }

    /**
     * @return True if the user has checked to download missing artist images,
     *         false otherwise.
     */
    public final boolean downloadMissingArtistImages() {
        return mPreferences.getBoolean(DOWNLOAD_MISSING_ARTIST_IMAGES, true);
    }

    /**
     * @return Prefered upper limit of image cache in bytes
     */
    public final int imageCacheSizeBytes() {
        String size = mPreferences.getString(PREF_CACHE_SIZE, "60");
        return Integer.decode(size) * 1024 * 1024;
    }

    /**
     * Saves the sort order for a list.
     * 
     * @param key Which sort order to change
     * @param value The new sort order
     */
    private void setSortOrder(final String key, final String value) {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(key, value);
                editor.apply();

                return null;
            }
        }, (Void[])null);
    }

    /**
     * Sets the sort order for the artist song list.
     * 
     * @param value The new sort order
     */
    public void setArtistSongSortOrder(final String value) {
        setSortOrder(ARTIST_SONG_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the artist song list in
     *         {@link ArtistSongFragment}
     */
    public final String getArtistSongSortOrder() {
        return mPreferences.getString(ARTIST_SONG_SORT_ORDER,
                SortOrder.ArtistSongSortOrder.SONG_A_Z);
    }

    /**
     * Sets the sort order for the artist album list.
     * 
     * @param value The new sort order
     */
    public void setArtistAlbumSortOrder(final String value) {
        setSortOrder(ARTIST_ALBUM_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the artist album list in
     *         {@link ArtistAlbumFragment}
     */
    public final String getArtistAlbumSortOrder() {
        return mPreferences.getString(ARTIST_ALBUM_SORT_ORDER,
                SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
    }

    /**
     * Sets the sort order for the album song list.
     * 
     * @param value The new sort order
     */
    public void setAlbumSongSortOrder(final String value) {
        setSortOrder(ALBUM_SONG_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the album song in
     *         {@link AlbumSongFragment}
     */
    public final String getAlbumSongSortOrder() {
        return mPreferences.getString(ALBUM_SONG_SORT_ORDER,
                SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
    }

    /**
     * Saves the layout type for a list
     * 
     * @param key Which layout to change
     * @param value The new layout type
     */
    private void setLayoutType(final String key, final String value) {
        ApolloUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(key, value);
                editor.apply();

                return null;
            }
        }, (Void[])null);
    }

    /**
     * Sets the layout type for the recent list
     * 
     * @param value The new layout type
     */
    public void setRecentLayout(final String value) {
        setLayoutType(RECENT_LAYOUT, value);
    }

    /**
     * @param context The {@link Context} to use.
     * @param which Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isSimpleLayout(final String which, final Context context) {
        final String simple = "simple";
        final String defaultValue = "grid";
        return mPreferences.getString(which, defaultValue).equals(simple);
    }

    /**
     * @param context The {@link Context} to use.
     * @param which Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isDetailedLayout(final String which, final Context context) {
        final String detailed = "detailed";
        final String defaultValue = "grid";
        return mPreferences.getString(which, defaultValue).equals(detailed);
    }

    /**
     * @param context The {@link Context} to use.
     * @param which Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isGridLayout(final String which, final Context context) {
        final String grid = "grid";
        final String defaultValue = "simple";
        return mPreferences.getString(which, defaultValue).equals(grid);
    }

    /**
     * Saves cast enabled pref to cast prefs
     * @param enabled
     */
    public void setCastEnabled(boolean enabled) {
        Utils.saveBooleanToPreference(mContext, KEY_CAST_ENABLED, enabled);
    }

    /**
     * @return true if casting is enabled
     */
    public boolean isCastEnabled() {
        return Utils.getBooleanFromPreference(mContext, KEY_CAST_ENABLED, true);
    }

    /**
     * Static method for service
     * @return true if casting is enabled
     */
    public static boolean isCastEnabled(Context context) {
        return Utils.getBooleanFromPreference(context.getApplicationContext(), KEY_CAST_ENABLED, true);
    }

    /**
     * @return true if we want mega sized art
     */
    public boolean wantHighResolutionArt() {
        return !mPreferences.getBoolean(USE_LOW_RESOLUTION_ART, false);
    }

    /**
     * @return true if we want to show visualizations
     */
    public boolean showVisualizations() {
        return mPreferences.getBoolean(SHOW_VISUALIZATIONS, true);
    }

}
