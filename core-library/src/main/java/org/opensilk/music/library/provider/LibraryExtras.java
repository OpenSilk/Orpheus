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

package org.opensilk.music.library.provider;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.sort.BundleableSortOrder;

import java.lang.reflect.Method;

import static org.opensilk.music.library.provider.LibraryMethods.Extras.*;

/**
 * Created by drew on 5/14/15.
 */
public class LibraryExtras {

    public static Uri getUri(Bundle extras) {
        return extras.getParcelable(URI);
    }

    public static String getSortOrder(Bundle extras) {
        return extras.getString(SORTORDER, BundleableSortOrder.A_Z);
    }

    public static boolean getOk(Bundle extras) {
        return extras.getBoolean(OK);
    }

    public static LibraryException getCause(Bundle extras) {
        Bundle b = extras.getBundle("wrappedcause");
        b.setClassLoader(LibraryException.class.getClassLoader());
        return b.getParcelable(CAUSE);
    }

    public static Builder b() {
        return new Builder();
    }

    public static class Builder {
        final Bundle b = new Bundle();

        private Builder() {
        }

        public Builder putUri(Uri uri) {
            b.putParcelable(URI, uri);
            return this;
        }

        public Builder putSortOrder(String sortorder) {
            b.putString(SORTORDER, sortorder);
            return this;
        }

        public Builder putOk(boolean ok) {
            b.putBoolean(OK, ok);
            return this;
        }

        public Builder putCause(LibraryException e) {
            //HAX since the bundle is returned (i guess)
            //the system classloader remarshals the bundle before we
            //can set our classloader...causing ClassNotFoundException.
            //To remedy nest the cause in another bundle.
            Bundle b2 = new Bundle();
            b2.putParcelable(CAUSE, e);
            b.putBundle("wrappedcause", b2);
            return this;
        }

        private Method _getIBinder = null;
        private IBinder getBinderCallbackFromBundle(Bundle b) {
            if (Build.VERSION.SDK_INT >= 18) {
                return b.getBinder(LibraryMethods.Extras.CALLBACK);
            } else {
                try {
                    synchronized (this) {
                        if (_getIBinder == null) {
                            _getIBinder = Bundle.class.getDeclaredMethod("getIBinder", String.class);
                        }
                    }
                    return (IBinder) _getIBinder.invoke(b, LibraryMethods.Extras.CALLBACK);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public Bundle get() {
            return b;
        }
    }
}
