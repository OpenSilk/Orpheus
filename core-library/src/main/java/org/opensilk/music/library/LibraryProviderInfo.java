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

package org.opensilk.music.library;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Information collected by querying ProviderInfos, used in Navigation Drawer
 *
 * Created by drew on 6/14/14.
 */
public class LibraryProviderInfo implements Parcelable, Comparable<LibraryProviderInfo> {

    /**
     * Manifest attribute android:label
     */
    public final String title;
    /**
     * Manifest attribute android:description
     */
    public final String description;
    /**
     * Manifest attribute android:authority
     */
    public final String authority;
    /**
     * Manifest attribute android:icon
     */
    public transient Drawable icon;
    /**
     * Internal use, true if plugin shows in drawer
     */
    public transient boolean isActive = true;


    public LibraryProviderInfo(
            @NonNull String title,
            @NonNull String description,
            @NonNull String authority
    ) {
        this.title = title;
        this.description = description;
        this.authority = authority;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthority() {
        return authority;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryProviderInfo that = (LibraryProviderInfo) o;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null)
            return false;
        return !(authority != null ? !authority.equals(that.authority) : that.authority != null);

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (authority != null ? authority.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(authority);
    }

    private static LibraryProviderInfo readParcel(Parcel in) {
        return new LibraryProviderInfo(
                in.readString(),
                in.readString(),
                in.readString()
        );
    }

    public static final Creator<LibraryProviderInfo> CREATOR = new Creator<LibraryProviderInfo>() {
        @Override
        public LibraryProviderInfo createFromParcel(Parcel source) {
            return readParcel(source);
        }

        @Override
        public LibraryProviderInfo[] newArray(int size) {
            return new LibraryProviderInfo[size];
        }
    };

    @Override
    public int compareTo(@NonNull LibraryProviderInfo another) {
        return this.title.compareTo(another.title);
    }

}
