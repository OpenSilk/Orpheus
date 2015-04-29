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

package org.opensilk.music.core.exception;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by drew on 11/11/14.
 */
public class ParcelableException extends Exception implements Parcelable {

    private static final long serialVersionUID = 3941740512702819161L;

    /**
     * Unknown or non recoverable error. Orpheus will stop the service
     * and rebind in the hopes it will be corrected. (up to 4 times)
     */
    public static final int UNKNOWN = -1;
    /**
     * Temporary failure, Orpheus will retry request with exponential backoff
     */
    public static final int RETRY = 1;
    /**
     * Permanent auth failure, Orpheus will relaunch the library picker activity
     */
    public static final int AUTH_FAILURE = 2;
    /**
     * IO or Network error, Orpheus will check connectivity and retry with exponential backoff
     */
    public static final int NETWORK = 3;

    private int code;

    public ParcelableException(Throwable wrappedCause) {
        this(UNKNOWN, wrappedCause);
    }

    public ParcelableException(int code, Throwable wrappedCause) {
        super(wrappedCause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(code);
        dest.writeSerializable(getCause());
    }

    private static ParcelableException readParcel(Parcel in) {
        return new ParcelableException(
                in.readInt(),
                (Throwable) in.readSerializable()
        );
    }

    public static final Creator<ParcelableException> CREATOR = new Creator<ParcelableException>() {
        @Override public ParcelableException createFromParcel(Parcel source) {
            return readParcel(source);
        }

        @Override public ParcelableException[] newArray(int size) {
            return new ParcelableException[size];
        }
    };
}
