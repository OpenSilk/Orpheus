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

package org.opensilk.music.library.gallery;

import android.net.Uri;
import android.os.Bundle;

/**
 * Created by drew on 12/26/15.
 */
public class GalleryExtras {

    static final String ARG_OK = "argok";
    static final String ARG_ERROR = "argerror";
    static final String ARG_URI = "arguri";

    public static boolean getOk(Bundle extras) {
        return extras.getBoolean(ARG_OK);
    }

    public static String getError(Bundle extras) {
        return extras.getString(ARG_ERROR);
    }

    public static Uri getUri(Bundle extras) {
        return extras.getParcelable(ARG_URI);
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

        public Builder putUri(Uri uri) {
            bundle.putParcelable(ARG_URI, uri);
            return this;
        }

        public Bundle get() {
            return bundle;
        }

    }

}
