/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrew.apollo.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/**
 * Created by drew on 7/10/14.
 */
public class LocalSongGroup implements Parcelable {

    public final String name;
    public final String parentName;
    public final long[] songIds;
    public final long[] albumIds;

    public LocalSongGroup(String name, String parentName, long[] songIds, long[] albumIds) {
        this.name = name;
        this.parentName = parentName;
        this.songIds = songIds;
        this.albumIds = albumIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalSongGroup)) return false;

        LocalSongGroup that = (LocalSongGroup) o;

        if (!Arrays.equals(albumIds, that.albumIds)) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (parentName != null ? !parentName.equals(that.parentName) : that.parentName != null)
            return false;
        if (!Arrays.equals(songIds, that.songIds)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (parentName != null ? parentName.hashCode() : 0);
        result = 31 * result + (songIds != null ? Arrays.hashCode(songIds) : 0);
        result = 31 * result + (albumIds != null ? Arrays.hashCode(albumIds) : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(parentName);
        dest.writeLongArray(songIds);
        dest.writeLongArray(albumIds);
    }

    private LocalSongGroup(Parcel in) {
        this.name = in.readString();
        this.parentName = in.readString();
        this.songIds = in.createLongArray();
        this.albumIds = in.createLongArray();
    }

    public static final Creator<LocalSongGroup> CREATOR = new Creator<LocalSongGroup>() {
        @Override
        public LocalSongGroup createFromParcel(Parcel source) {
            return new LocalSongGroup(source);
        }

        @Override
        public LocalSongGroup[] newArray(int size) {
            return new LocalSongGroup[size];
        }
    };
}
