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

package org.opensilk.music.library.upnp.provider;

import android.content.UriMatcher;
import android.net.Uri;

/**
 * Created by drew on 9/17/15.
 */
public class UpnpCDUris {

    public static final String DEFAULT_ROOT_FOLDER = "0";

    public static Uri makeUri(String authority, String device, String object) {
        if (object == null) {
            return new Uri.Builder().scheme("content").authority(authority).appendPath(device).build();
        }
        return new Uri.Builder().scheme("content").authority(authority).appendPath(device).appendPath(object).build();
    }

    static final int M_OBJECT = 1;
    static final int M_DEVICE_ROOT = 2;

    public static UriMatcher makeMatcher(String authority) {

        UriMatcher m = new UriMatcher(UriMatcher.NO_MATCH);
        m.addURI(authority, "*", M_DEVICE_ROOT);
        m.addURI(authority, "*/*", M_OBJECT);

        return m;
    }




}
