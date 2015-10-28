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

package org.opensilk.music.playback.renderer;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by drew on 10/26/15.
 */
public class Headers extends HashMap<String, String> implements Parcelable {

    public Headers() {
    }

    public Headers(int capacity) {
        super(capacity);
    }

    public Headers(Map<? extends String, ? extends String> map) {
        super(map);
    }

    private Headers(Parcel source) {
        int size = source.readInt();
        while (size-- > 0) {
            put(source.readString(), source.readString());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(size());
        for (Entry<String, String> entry : entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }

    public static final Creator<Headers> CREATOR = new Creator<Headers>() {
        @Override
        public Headers createFromParcel(Parcel source) {
            return new Headers(source);
        }

        @Override
        public Headers[] newArray(int size) {
            return new Headers[size];
        }
    };
}
