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

package org.opensilk.music.api.meta;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by drew on 6/21/14.
 */
public class ArtInfo implements Parcelable {

    public final String artistName;
    public final String albumName;
    public final Uri artworkUri;

    public ArtInfo(String artistName, String albumName, Uri artworkUri) {
        this.artistName = artistName;
        this.albumName = albumName;
        this.artworkUri = (artworkUri != null) ? artworkUri : Uri.EMPTY;
    }

    private ArtInfo(Parcel in) {
        this.artistName = in.readString();
        this.albumName = in.readString();
        this.artworkUri = Uri.parse(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(artistName);
        dest.writeString(albumName);
        dest.writeString(artworkUri.toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (artistName == null ? 0 : artistName.hashCode());
        result = prime * result + (albumName == null ? 0 : albumName.hashCode());
        result = prime * result + (artworkUri == null ? 0 : artworkUri.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) return true;
        if (obj == null || !(obj instanceof ArtInfo)) return false;
        ArtInfo o = (ArtInfo)obj;
        if(!TextUtils.equals(o.artistName, this.artistName)) return false;
        if (!TextUtils.equals(o.albumName, this.albumName)) return false;
        if (!TextUtils.equals(o.artworkUri.toString(), this.artworkUri.toString())) return false;
        return true;
    }

    @Override
    public String toString() {
        return "ArtInfo{ artist="+artistName+" album="+albumName+" uri="+artworkUri+" }";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ArtInfo> CREATOR = new Creator<ArtInfo>() {
        @Override
        public ArtInfo createFromParcel(Parcel source) {
            return new ArtInfo(source);
        }

        @Override
        public ArtInfo[] newArray(int size) {
            return new ArtInfo[size];
        }
    };
}
