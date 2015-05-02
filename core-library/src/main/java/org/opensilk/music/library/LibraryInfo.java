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

package org.opensilk.music.library;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * Created by drew on 7/2/14.
 */
public class LibraryInfo implements Parcelable {

    /**
     * Unique identifier for this library
     */
    public final String libraryId;
    /**
     * Human readable name for library (used for display)
     */
    public final String libraryName;
    /**
     * Unique folder identifier for this request
     */
    public final String folderId;
    /**
     * Human readable name for folder;
     */
    public final String folderName;

    public LibraryInfo(@NonNull String libraryId, @Nullable String libraryName,
                       @Nullable String folderId, @Nullable String folderName) {
        this.libraryId = libraryId;
        this.libraryName = libraryName;
        this.folderId = folderId;
        this.folderName = folderName;
    }

    /**
     * @return a new instance retaining {@link #libraryId} and {@link #libraryName}
     */
    public LibraryInfo buildUpon(@Nullable String newFolderId, @Nullable String newFolderName) {
        return new LibraryInfo(this.libraryId, this.libraryName, newFolderId, newFolderName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof LibraryInfo)) return false;
        LibraryInfo that = (LibraryInfo) o;
        if (!TextUtils.equals(libraryId, that.libraryId)) return false;
        if (!TextUtils.equals(libraryName, that.libraryName)) return false;
        if (!TextUtils.equals(folderId, that.folderId)) return false;
        if (!TextUtils.equals(folderName, that.folderName)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = libraryId != null ? libraryId.hashCode() : 0;
        result = 31 * result + (libraryName != null ? libraryName.hashCode() : 0);
        result = 31 * result + (folderId != null ? folderId.hashCode() : 0);
        result = 31 * result + (folderName != null ? folderName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LibraryInfo{"+libraryId+"/"+libraryName+"::"+folderId+"/"+folderName+"}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(libraryId);
        dest.writeString(libraryName);
        dest.writeString(folderId);
        dest.writeString(folderName);
    }

    private static LibraryInfo fromParcel(Parcel source) {
        return new LibraryInfo(
                source.readString(),
                source.readString(),
                source.readString(),
                source.readString()
        );
    }

    public static final Creator<LibraryInfo> CREATOR = new Creator<LibraryInfo>() {
        @Override
        public LibraryInfo createFromParcel(Parcel source) {
            return LibraryInfo.fromParcel(source);
        }

        @Override
        public LibraryInfo[] newArray(int size) {
            return new LibraryInfo[size];
        }
    };
}
