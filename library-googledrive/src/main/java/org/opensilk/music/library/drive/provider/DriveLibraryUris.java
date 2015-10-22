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

package org.opensilk.music.library.drive.provider;

import android.content.UriMatcher;
import android.net.Uri;

/**
 * Created by drew on 10/20/15.
 */
public class DriveLibraryUris {

    static final String folder = "folder";
    static final String file = "file";
    static final String rootFolder = "rootFolder";

    static Uri.Builder baseUri(String authority, String account) {
        return new Uri.Builder().scheme("content").authority(authority).appendPath(account);
    }

    public static Uri folder(String authority, String account, String folderId) {
        return baseUri(authority, account).appendPath(folder).appendPath(folderId).build();
    }

    public static Uri file(String authority, String account, String parentId, String fileId) {
        return baseUri(authority, account).appendPath(file).appendPath(parentId).appendPath(fileId).build();
    }

    public static Uri rootFolder(String authority, String account) {
        return baseUri(authority, account).appendPath(rootFolder).build();
    }

    public static String extractAccount(Uri uri) {
        return uri.getPathSegments().get(0);
    }

    public static String extractFolderId(Uri uri) {
        return uri.getPathSegments().get(2);
    }

    public static String extractFileId(Uri uri) {
        return uri.getPathSegments().get(3);
    }

    public static Uri call(String authority) {
        return new Uri.Builder().scheme("content").authority(authority).build();
    }

    static final int M_ROOT_FOLDER = 1;
    static final int M_FOLDER = 2;
    static final int M_FILE = 3;

    static final String baseMatch = "*/";
    static final String slashWild = "/*";

    static UriMatcher matcher(String authority) {
        UriMatcher m = new UriMatcher(UriMatcher.NO_MATCH);

        m.addURI(authority, baseMatch + rootFolder, M_ROOT_FOLDER);
        m.addURI(authority, baseMatch + folder + slashWild, M_FOLDER);
        m.addURI(authority, baseMatch + file + slashWild + slashWild, M_FILE);

        return m;
    }
}
