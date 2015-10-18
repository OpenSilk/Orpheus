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

package org.opensilk.music.index.model;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.opensilk.music.model.Model;

/**
 * Created by drew on 10/18/15.
 */
public class BioContent implements Model {

    private final Uri uri;
    private final Uri parentUri;
    private final String name;
    private final String content;
    private final String mbid;
    private final String url;

    public BioContent(Uri uri, Uri parentUri, String name, String content, String mbid, String url) {
        this.uri = uri;
        this.parentUri = parentUri;
        this.name = name;
        this.content = content;
        this.mbid = mbid;
        this.url = url;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return uri;
    }

    @NonNull
    @Override
    public Uri getParentUri() {
        return parentUri;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String getSortName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getMbid() {
        return mbid;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, BioContent.class.getName());
        b.putParcelable("_1", uri);
        b.putParcelable("_2", parentUri);
        b.putString("_3", name);
        b.putString("_4", content);
        b.putString("_5", mbid);
        b.putString("_6", url);
        return b;
    }

    public static final BundleCreator<BioContent> BUNDLE_CREATOR = new BundleCreator<BioContent>() {
        @Override
        public BioContent fromBundle(Bundle b) throws IllegalArgumentException {
            if (!BioContent.class.getName().equals(b.getString(CLZ))) {
                throw new IllegalArgumentException("Wrong clazz for BioContent " + b.getString(CLZ));
            }
            final Uri uri = b.getParcelable("_1");
            final Uri parentUri = b.getParcelable("_2");
            final String name = b.getString("_3");
            final String content = b.getString("_4");
            final String mbid = b.getString("_5");
            final String url = b.getString("_6");
            return new BioContent( uri, parentUri, name, content, mbid, url);
        }
    };

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Uri uri;
        private Uri parentUri;
        private String name;
        private String content;
        private String mbid;
        private String url;

        public Builder setUri(Uri uri) {
            this.uri = uri;
            return this;
        }

        public Builder setParentUri(Uri parentUri) {
            this.parentUri = parentUri;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public Builder setMbid(String mbid) {
            this.mbid = mbid;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public BioContent build() {
            return new BioContent(uri, parentUri, name, content, mbid, url);
        }
    }
}
