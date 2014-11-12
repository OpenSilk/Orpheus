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

package org.opensilk.music.api;

import android.content.Intent;
import android.os.Bundle;

import org.opensilk.music.api.Config;
import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.meta.LibraryInfo;

/**
 * @see RemoteLibraryService for doc
 *
 * Created by drew on 6/9/14.
 */
interface RemoteLibrary {

    /*
     * API_010
     */

    //Deprecated since API_020
    int getApiVersion();
    //Deprecated since API_020
    int getCapabilities();
    //Deprecated since API_020
    void getLibraryChooserIntent(out Intent i);
    //Deprecated since API_020
    void getSettingsIntent(out Intent i);

    void pause();

    void resume();

    //Deprecated since API_020
    void browseFolders(String libraryIdentity, String folderIdentity, int maxResults, in Bundle paginationBundle, in Result callback);
    //Deprecated since API_020
    void listSongsInFolder(String libraryIdentity, String folderIdentity, int maxResults, in Bundle paginationBundle, in Result callback);
    //Deprecated since API_020
    void search(String libraryIdentity, String query, int maxResults, in Bundle paginationBundle, in Result callback);

    /*
     * API_020
     */

    Config getConfig();
}
