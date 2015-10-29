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

package org.opensilk.music.playback.renderer.client;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by drew on 10/28/15.
 */
public class RendererInfo implements Parcelable, Comparable<RendererInfo> {
    /**
     * Manifest attribute android:label
     */
    private final String title;
    /**
     * Manifest attribute android:description
     */
    private final String description;
    /**
     * Service ComponentName
     */
    private final ComponentName componentName;
    /**
     * Manifest attribute android:icon
     */
    private transient Drawable icon;
    /**
     * Internal use, true if plugin shows in drawer
     */
    private transient boolean isActive = true;
    /**
     *
     */
    private ComponentName activityComponent;

    public RendererInfo(
            @NonNull String title,
            @NonNull String description,
            @Nullable ComponentName componentName
    ) {
        this.title = title;
        this.description = description;
        this.componentName = componentName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public @Nullable ComponentName getComponentName() {
        return componentName;
    }

    public @Nullable Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable d) {
        icon = d;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setActivityComponent(ComponentName component) {
        this.activityComponent = component;
    }

    public @Nullable ComponentName getActivityComponent() {
        return activityComponent;
    }

    public boolean hasActivityComponent() {
        return activityComponent != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RendererInfo that = (RendererInfo) o;
        if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = (componentName != null ? componentName.hashCode() : 0);
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
        dest.writeParcelable(componentName, flags);
    }

    private static RendererInfo readParcel(Parcel in) {
        return new RendererInfo(
                in.readString(),
                in.readString(),
                in.<ComponentName>readParcelable(null)
        );
    }

    public static final Parcelable.Creator<RendererInfo> CREATOR = new Parcelable.Creator<RendererInfo>() {
        @Override
        public RendererInfo createFromParcel(Parcel source) {
            return readParcel(source);
        }

        @Override
        public RendererInfo[] newArray(int size) {
            return new RendererInfo[size];
        }
    };

    @Override
    public int compareTo(@NonNull RendererInfo another) {
        return this.title.compareTo(another.title);
    }
}
