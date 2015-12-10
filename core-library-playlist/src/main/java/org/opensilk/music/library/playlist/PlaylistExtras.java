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

package org.opensilk.music.library.playlist;

import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import org.opensilk.music.library.internal.ResultReceiverWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 12/10/15.
 */
public class PlaylistExtras {

    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_ERROR = 2;

    static final String ARG_OK = "argok";
    static final String ARG_ERROR = "argerror";
    static final String ARG_NAME = "argname";
    static final String ARG_URI = "arguri";
    static final String ARG_URI_LIST = "argurilist";
    static final String ARG_INT = "argint";
    static final String ARG_RESULT_RECEIVER = "argresultreceiver";

    public static boolean getOk(Bundle extras) {
        return extras.getBoolean(ARG_OK);
    }

    public static String getError(Bundle extras) {
        return extras.getString(ARG_ERROR);
    }

    public static String getName(Bundle extras) {
        return extras.getString(ARG_NAME);
    }

    public static Uri getUri(Bundle extras) {
        return extras.getParcelable(ARG_URI);
    }

    public static List<Uri> getUriList(Bundle extras) {
        return extras.getParcelableArrayList(ARG_URI_LIST);
    }

    public static int getInt(Bundle extras) {
        return extras.getInt(ARG_INT);
    }

    public static ResultReceiver getResultReceiver(Bundle extras) {
        extras.setClassLoader(PlaylistExtras.class.getClassLoader());
        ResultReceiverWrapper rrw = extras.getParcelable(ARG_RESULT_RECEIVER);
        if (rrw == null) throw new NullPointerException("No resultReceiver in bundle");
        return rrw.get();
    }

    public static Bundle sanitize(Bundle extras) {
        extras.remove(ARG_RESULT_RECEIVER);
        return extras;
    }

    public static Builder b() {
        return new Builder();
    }

    public static class Builder {

        private Bundle bundle = new Bundle();

        public Builder putOk(boolean ok) {
            bundle.putBoolean(ARG_OK, ok);
            return this;
        }

        public Builder putError(String error) {
            bundle.putString(ARG_ERROR, error);
            return this;
        }

        public Builder putName(String name) {
            bundle.putString(ARG_NAME, name);
            return this;
        }

        public Builder putUri(Uri uri) {
            bundle.putParcelable(ARG_URI, uri);
            return this;
        }

        public Builder putUriList(List<Uri> uriList) {
            bundle.putParcelableArrayList(ARG_URI_LIST, new ArrayList<Uri>(uriList));
            return this;
        }

        public Builder putInt(int num) {
            bundle.putInt(ARG_INT, num);
            return this;
        }

        public Builder putResultReceiver(ResultReceiver resultReceiver) {
            bundle.putParcelable(ARG_RESULT_RECEIVER, new ResultReceiverWrapper(resultReceiver));
            return this;
        }

        public Bundle get() {
            return bundle;
        }

    }
}
