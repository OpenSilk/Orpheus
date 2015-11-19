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
 * Created by drew on 10/8/15.
 */
public class BioSummary implements Model {

    public enum Kind {
        ALBUM,
        ARTIST,
    }

    private final Uri uri;
    private final Uri parentUri;
    private final String name;
    private final String summary;
    private final Kind kind;
    private final String mbid;

    public BioSummary(Uri uri, Uri parentUri, String name, String summary, Kind kind, String mbid) {
        this.uri = uri;
        this.parentUri = parentUri;
        this.name = name;
        this.summary = summary;
        this.kind = kind;
        this.mbid = mbid;
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

    @Override
    public long getFlags() {
        return 0;
    }

    public String getSummary() {
        return summary;
    }

    public Kind getKind() {
        return kind;
    }

    public String getMbid() {
        return mbid;
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, BioSummary.class.getName());
        b.putParcelable("_1", uri);
        b.putParcelable("_2", parentUri);
        b.putString("_3", name);
        b.putString("_4", summary);
        b.putSerializable("_5", kind);
        b.putString("_6", mbid);
        return b;
    }

    public static final BundleCreator<BioSummary> BUNDLE_CREATOR = new BundleCreator<BioSummary>() {
        @Override
        public BioSummary fromBundle(Bundle b) throws IllegalArgumentException {
            if (!BioSummary.class.getName().equals(b.getString(CLZ))) {
                throw new IllegalArgumentException("Wrong clazz for BioSummary " + b.getString(CLZ));
            }
            final Uri uri = b.getParcelable("_1");
            final Uri parentUri = b.getParcelable("_2");
            final String name = b.getString("_3");
            final String summayr = b.getString("_4");
            final Kind kind = (Kind) b.getSerializable("_5");
            final String bio = b.getString("_6");
            return new BioSummary( uri, parentUri, name, summayr, kind,bio);
        }
    };

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Uri uri;
        private Uri parentUri;
        private String name;
        private String summary;
        private Kind kind;
        private String mbid;

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

        public Builder setSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder setKind(Kind kind) {
            this.kind = kind;
            return this;
        }

        public Builder setMbid(String mbid) {
            this.mbid = mbid;
            return this;
        }

        public BioSummary build() {
            return new BioSummary(uri, parentUri, name, summary, kind, mbid);
        }
    }
}
