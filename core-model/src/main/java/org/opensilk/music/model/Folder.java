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

package org.opensilk.music.model;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.opensilk.music.model.spi.Bundleable;

/**
 * Created by drew on 6/10/14.
 */
public class Folder extends Container {

    protected Folder(@NonNull Uri uri, @NonNull Uri parentUri,
                     @NonNull String name, @NonNull Metadata metadata) {
        super(uri, parentUri, name, metadata);
    }

    public int getChildCount() {
        return metadata.getInt(Metadata.KEY_CHILD_COUNT);
    }

    public String getDateModified() {
        return metadata.getString(Metadata.KEY_DATE_MODIFIED);
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, Folder.class.getName());
        b.putParcelable("_1", uri);
        b.putString("_2", name);
        b.putParcelable("_3", metadata);
        b.putParcelable("_4", parentUri);
        return b;
    }

    protected static Folder fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Folder.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for Folder: "+b.getString(CLZ));
        }
        b.setClassLoader(Folder.class.getClassLoader());
        return new Folder(
                b.<Uri>getParcelable("_1"),
                b.<Uri>getParcelable("_4"),
                b.getString("_2"),
                b.<Metadata>getParcelable("_3")
        );
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
        private Uri uri;
        private Uri parentUri;
        private String name;
        private Metadata.Builder bob = Metadata.builder();

        private Builder() {
        }

        public Builder setUri(Uri uri) {
            this.uri = uri;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDisplayName(String name) {
            bob.putString(Metadata.KEY_DISPLAY_NAME, name);
            return this;
        }

        public Builder setParentUri(Uri parentUri) {
            this.parentUri = parentUri;
            return this;
        }

        public Builder setChildCount(int childCount) {
            bob.putInt(Metadata.KEY_CHILD_COUNT, childCount);
            return this;
        }

        public Builder setDateModified(String date) {
            bob.putString(Metadata.KEY_DATE_MODIFIED, date);
            return this;
        }

        public Folder build() {
            if (uri == null || parentUri == null || name == null) {
                throw new NullPointerException("uri and name are required");
            }
            return new Folder(uri, parentUri, name, bob.build());
        }
    }
}
