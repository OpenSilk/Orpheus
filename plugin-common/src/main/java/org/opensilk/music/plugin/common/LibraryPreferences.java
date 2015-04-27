/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.plugin.common;

import android.content.Context;
import android.content.SharedPreferences;

import org.opensilk.common.dagger.qualifier.ForApplication;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 11/6/14.
 */
@Singleton
public class LibraryPreferences {

    public static final String ROOT_FOLDER = "root_folder_identity";
    public static final String ROOT_FOLDER_NAME = "root_folder_title";
    public static final String SEARCH_FOLDER = "search_folder_identity";
    public static final String SEARCH_FOLDER_NAME = "search_folder_title";

    final Context appContext;

    @Inject
    public LibraryPreferences(@ForApplication Context appContext) {
        this.appContext = appContext;
    }

    public String getRootFolder(String libraryId) {
        return obtainPrefs(libraryId).getString(ROOT_FOLDER, null);
    }

    public void setRootFolder(String libraryId, String folderId) {
        obtainPrefs(libraryId).edit().putString(ROOT_FOLDER, folderId).apply();
    }

    public String getRootFolderName(String libraryId) {
        return obtainPrefs(libraryId).getString(ROOT_FOLDER_NAME, null);
    }

    public void setRootFolderName(String libraryId, String folderName) {
        obtainPrefs(libraryId).edit().putString(libraryId, folderName).apply();
    }

    public String getSearchFolder(String libraryId) {
        return obtainPrefs(libraryId).getString(SEARCH_FOLDER, null);
    }

    public void setSearchFolder(String libraryId, String folderId) {
        obtainPrefs(libraryId).edit().putString(libraryId, folderId).apply();
    }

    public String getSearchFolderName(String libraryId) {
        return obtainPrefs(libraryId).getString(SEARCH_FOLDER_NAME, null);
    }

    public void setSearchFolderName(String libraryId, String folderName) {
        obtainPrefs(libraryId).edit().putString(libraryId, folderName).apply();
    }

    final Map<String, SharedPreferences> PREFS = new HashMap<>();

    SharedPreferences obtainPrefs(String libraryId) {
        SharedPreferences prefs = PREFS.get(libraryId);
        if (prefs == null) {
            prefs = appContext.getSharedPreferences(PluginUtil.posixSafe(libraryId), Context.MODE_PRIVATE);
            PREFS.put(libraryId, prefs);
        }
        return prefs;
    }
}
