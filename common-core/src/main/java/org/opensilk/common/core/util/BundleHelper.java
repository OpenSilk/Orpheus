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

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for Bundles to allow easily inserting a couple items
 * in chainable fashion. Useful for cross process things
 *
 * Created by drew on 4/24/15.
 */
public class BundleHelper {
    public static final String INT_ARG = "intarg";
    public static final String INT_ARG2 = "intarg2";
    public static final String URI_ARG = "uriarg";
    public static final String URI_ARG2 = "uriarg2";
    public static final String STRING_ARG = "stringarg";
    public static final String STRING_ARG2 = "stringarg2";
    public static final String LONG_ARG = "longarg";
    public static final String LONG_ARG2 = "longarg2";
    public static final String LIST_ART = "listarg";
    public static final String PARCELABLE_ARG = "parcelablearg";

    public static int getInt(Bundle b) {
        return b.getInt(INT_ARG);
    }

    public static int getInt2(Bundle b) {
        return b.getInt(INT_ARG2);
    }

    public static Uri getUri(Bundle b) {
        b.setClassLoader(BundleHelper.class.getClassLoader());
        return b.getParcelable(URI_ARG);
    }

    public static Uri getUri2(Bundle b) {
        b.setClassLoader(BundleHelper.class.getClassLoader());
        return b.getParcelable(URI_ARG2);
    }

    public static String getString(Bundle b) {
        return b.getString(STRING_ARG);
    }

    public static String getString2(Bundle b) {
        return b.getString(STRING_ARG2);
    }

    public static long getLong(Bundle b) {
        return b.getLong(LONG_ARG);
    }

    public static long getLong2(Bundle b) {
        return b.getLong(LONG_ARG2);
    }

    public static <T extends Parcelable> List<T> getList(Bundle b) {
        b.setClassLoader(BundleHelper.class.getClassLoader());
        return b.getParcelableArrayList(LIST_ART);
    }

    public static <T extends Parcelable> T getParcelable(Bundle b) {
        b.setClassLoader(BundleHelper.class.getClassLoader());
        return b.<T>getParcelable(PARCELABLE_ARG);
    }

    public static Builder b() {
        return builder();
    }

    public static Builder from(Bundle b) {
        return new Builder(b);
    }

    @Deprecated //Cant refactor cause too common of name
    public static Builder builder() {
        return new Builder(new Bundle());
    }

    /**
     * Wraps bundle for chaining.
     */
    public static class Builder {
        final Bundle b;

        private Builder(Bundle b) {
            this.b = b;
        }

        public Builder putInt(int val) {
            b.putInt(INT_ARG, val);
            return this;
        }

        public Builder putInt2(int val) {
            b.putInt(INT_ARG2, val);
            return this;
        }

        public Builder putUri(Uri uri) {
            b.putParcelable(URI_ARG, uri);
            return this;
        }

        public Builder putUri2(Uri uri) {
            b.putParcelable(URI_ARG2, uri);
            return this;
        }

        public Builder putString(String string) {
            b.putString(STRING_ARG, string);
            return this;
        }

        public Builder putString2(String string) {
            b.putString(STRING_ARG2, string);
            return this;
        }

        public Builder putLong(long val) {
            b.putLong(LONG_ARG, val);
            return this;
        }

        public Builder putLong2(long val) {
            b.putLong(LONG_ARG2, val);
            return this;
        }

        public <T extends Parcelable> Builder putList(List<T> list) {
            b.putParcelableArrayList(LIST_ART, new ArrayList<T>(list));
            return this;
        }

        public Builder putParcleable(Parcelable p) {
            b.putParcelable(PARCELABLE_ARG, p);
            return this;
        }

        public Bundle get() {
            return b;
        }
    }




}
