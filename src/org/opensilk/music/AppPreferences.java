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
import android.util.JsonReader;
import android.util.JsonWriter;

import com.andrew.apollo.utils.Lists;

import org.opensilk.music.ui.home.MusicFragment;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 6/21/14.
 */
@Singleton
public class AppPreferences {

    public static final String PREF_DEFAULT_MEDIA_FOLDER = "default_media_folder";

    public static final String PREF_LAST_FOLDER_BROWSER_PATH = "last_folder_browser_path";

    public static final int DEFAULT_PAGE = 2;
    public static final String START_PAGE = "start_page";
    public static final String HOME_PAGES = "pref_home_pages";

    public static final String ARTIST_SORT_ORDER = "artist_sort_order";
    public static final String ALBUM_SORT_ORDER = "album_sort_order";
    public static final String SONG_SORT_ORDER = "song_sort_order";

    public static final String ARTIST_LAYOUT = "artist_layout";
    public static final String ALBUM_LAYOUT = "album_layout";
    public static final String SIMPLE = "simple";
    public static final String GRID = "grid";

    private SharedPreferences prefs;

    @Inject
    public AppPreferences(@ForApplication Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static AppPreferences get(Context context) {
        return new AppPreferences(context);
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


    /**
     * @return List of class names for home pager fragments
     */
    public final List<MusicFragment> getHomePages() {
        String pgs = getString(HOME_PAGES, null);
        if (pgs == null) {
            return null;
        }
        List<MusicFragment> pages = Lists.newArrayList();
        JsonReader jw = new JsonReader(new StringReader(pgs));
        try {
            jw.beginArray();
            while (jw.hasNext()) {
                pages.add(MusicFragment.valueOf(jw.nextString()));
            }
            jw.endArray();
        } catch (IOException |IllegalArgumentException e) {
            remove(HOME_PAGES);
            return null;
        } finally {
            try {
                jw.close();
            } catch (IOException ignored) { }
        }
        return pages;
    }

    /**
     * Saves fragment class names for home pager
     * @param pages
     */
    public final void setHomePages(List<MusicFragment> pages) {
        StringWriter sw = new StringWriter(400);
        JsonWriter jw = new JsonWriter(sw);
        try {
            jw.beginArray();
            for (MusicFragment p : pages) {
                jw.value(p.toString());
            }
            jw.endArray();
            putString(HOME_PAGES, sw.toString());
        } catch (IOException e) {
            remove(HOME_PAGES);
        } finally {
            try {
                jw.close();
            } catch (IOException ignored) { }
        }
    }
}
