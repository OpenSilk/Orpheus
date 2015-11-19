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

package org.opensilk.common.core.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by drew on 11/19/15.
 */
public class ConnectionUtils {

    public static boolean hasInternetConnection(Context context) {
        return hasInternetConnection((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE));
    }

    public static boolean hasInternetConnection(ConnectivityManager cm) {
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    public static boolean hasWifiOrEthernetConnection(ConnectivityManager cm) {
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return (ni != null && (ni.getType() == ConnectivityManager.TYPE_WIFI || ni.getType() == ConnectivityManager.TYPE_ETHERNET));
    }

}
