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

package org.opensilk.music.core.model;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.opensilk.music.core.spi.Bundleable;

/**
 * Created by drew on 6/10/14.
 */
public class Folder implements Bundleable {

    public final String identity;
    public final String name;
    public final String parentIdentity;
    public final int childCount;
    public final String date;

    protected Folder(@NonNull String identity,
                  @NonNull String name,
                  @Nullable String parentIdentity,
                  int childCount,
                  @Nullable String date
    ) {
        this.identity = identity;
        this.name =name;
        this.parentIdentity = parentIdentity;
        this.childCount = childCount;
        this.date = date;
    }

    @Override
    public String getIdentity() {
        return identity;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(12); //2x
        b.putString("clz", Folder.class.getName());
        b.putString("_1", identity);
        b.putString("_2", name);
        b.putString("_3", parentIdentity);
        b.putInt("_4", childCount);
        b.putString("_5", date);
        return b;
    }

    protected static Folder fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Folder.class.getName().equals(b.getString("clz"))) {
            throw new IllegalArgumentException("Wrong class for Folder: "+b.getString("clz"));
        }
        return new Builder()
                .setIdentity(b.getString("_1"))
                .setName(b.getString("_2"))
                .setParentIdentity(b.getString("_3"))
                .setChildCount(b.getInt("_4"))
                .setDate(b.getString("_5"))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Folder)) return false;

        Folder folder = (Folder) o;

        if (childCount != folder.childCount) return false;
        if (date != null ? !date.equals(folder.date) : folder.date != null) return false;
        if (identity != null ? !identity.equals(folder.identity) : folder.identity != null)
            return false;
        if (name != null ? !name.equals(folder.name) : folder.name != null) return false;
        if (parentIdentity != null ? !parentIdentity.equals(folder.parentIdentity) : folder.parentIdentity != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = identity != null ? identity.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (parentIdentity != null ? parentIdentity.hashCode() : 0);
        result = 31 * result + childCount;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static BundleCreator<Folder> BUNDLE_CREATOR = new BundleCreator<Folder>() {
        @Override
        public Folder fromBundle(Bundle b) throws IllegalArgumentException {
            return Folder.fromBundle(b);
        }
    };

    public static final class Builder {
        private String identity;
        private String name;
        private String parentIdentity;
        private int childCount;
        private String date;

        public Builder setIdentity(String identity) {
            this.identity = identity;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setParentIdentity(String parentIdentity) {
            this.parentIdentity = parentIdentity;
            return this;
        }

        public Builder setChildCount(int childCount) {
            this.childCount = childCount;
            return this;
        }

        public Builder setDate(String date) {
            this.date = date;
            return this;
        }

        public Folder build() {
            if (identity == null || name == null) {
                throw new NullPointerException("identity and name are required");
            }
            return new Folder(identity, name, parentIdentity, childCount, date);
        }
    }
}
