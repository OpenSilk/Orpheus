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

package org.opensilk.music.model.util;

import android.os.Parcel;
import android.os.Parcelable;

import org.opensilk.music.model.ex.BadBundleableException;
import org.opensilk.music.model.spi.Bundleable;

/**
 * Created by drew on 5/2/15.
 */
public class ParcelableBundleable implements Parcelable {

    final Bundleable bundleable;

    public ParcelableBundleable(Bundleable bundleable) {
        this.bundleable = bundleable;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(bundleable.toBundle());
    }

    public static final Creator<ParcelableBundleable> CREATOR = new Creator<ParcelableBundleable>() {
        @Override
        public ParcelableBundleable createFromParcel(Parcel source) {
            Bundleable b;
            try {
                b = BundleableUtil.materializeBundle(source.readBundle(ParcelableBundleable.class.getClassLoader()));
            } catch (BadBundleableException e) {
                throw new IllegalArgumentException(e);
            }
            return new ParcelableBundleable(b);
        }

        @Override
        public ParcelableBundleable[] newArray(int size) {
            return new ParcelableBundleable[size];
        }
    };
}
