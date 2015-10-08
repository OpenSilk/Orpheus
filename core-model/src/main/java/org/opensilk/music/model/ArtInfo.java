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

package org.opensilk.music.model;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Base64;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;

/**
 * Contains all the info the ArtworkFetcher needs to locate artwork
 *
 * Created by drew on 6/21/14.
 */
public class ArtInfo implements Parcelable, Comparable<ArtInfo> {
    public static final ArtInfo NULLINSTANCE = new ArtInfo(null, null, null);

    public final String artistName;
    public final String albumName;
    public final Uri artworkUri;
    //This is only used for sanity so the fetcher can be sure we really want an artist
    public final boolean forArtist;

    /**
     * Creates a new album request
     * Implied contract artistName and albumName must both be non null OR artworkUri must be non null,
     * ideally all three are non null
     *
     * @param artistName album artist name
     * @param albumName album name
     * @param artworkUri fallback uri
     */
    public static ArtInfo forAlbum(String artistName, String albumName, Uri artworkUri) {
        if (artistName == null && albumName == null && artworkUri == null) {
            return NULLINSTANCE;
        } else {
            return new ArtInfo(artistName, albumName, artworkUri, false);
        }
    }

    /**
     * Creates a new artist request
     *
     * @param artistName artist name
     * @param artworkUri currently unused
     */
    public static ArtInfo forArtist(String artistName, Uri artworkUri) {
        if (artistName == null && artworkUri == null) {
            return NULLINSTANCE;
        } else {
            return new ArtInfo(artistName, null, artworkUri, true);
        }
    }

    @Deprecated
    public ArtInfo(String artistName, String albumName, Uri artworkUri) {
        this(artistName, albumName, artworkUri, false);
    }

    private ArtInfo(String artistName, String albumName, Uri artworkUri, boolean forArtist) {
        this.artistName = artistName;
        this.albumName = albumName;
        this.artworkUri = (artworkUri != null) ? artworkUri : Uri.EMPTY;
        this.forArtist = forArtist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArtInfo artInfo = (ArtInfo) o;
        if (forArtist != artInfo.forArtist) return false;
        if (artistName != null ? !artistName.equals(artInfo.artistName) : artInfo.artistName != null)
            return false;
        if (albumName != null ? !albumName.equals(artInfo.albumName) : artInfo.albumName != null)
            return false;
        return !(artworkUri != null ? !artworkUri.equals(artInfo.artworkUri) : artInfo.artworkUri != null);
    }

    @Override
    public int hashCode() {
        int result = artistName != null ? artistName.hashCode() : 0;
        result = 31 * result + (albumName != null ? albumName.hashCode() : 0);
        result = 31 * result + (artworkUri != null ? artworkUri.hashCode() : 0);
        result = 31 * result + (forArtist ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ArtInfo{ artist="+artistName+" album="+albumName+" uri="+artworkUri+" }";
    }

    public String cacheKey() {
        StringBuilder sb = new StringBuilder("forArtist=").append(forArtist);
        if (forArtist) {
            if (!StringUtils.isEmpty(artistName)) {
                sb.append("+artist=").append(artistName);
            } else {
                throw new IllegalArgumentException("Invalid artInfo cannot make cache key from:\n " + toString());
            }
        } else {
            if (!StringUtils.isEmpty(artistName) && !StringUtils.isEmpty(albumName)) {
                //prefer using artist/album to reduce duplication
                sb.append("+artist=").append(artistName);
                sb.append("+album=").append(albumName);
            } else if (artworkUri != null) {
                //must use uri to prevent improper mapping
                sb.append("+uri=").append(artworkUri);
            } else {
                throw new IllegalArgumentException("Invalid artInfo cannot make cache key from:\n " + toString());
            }
        }
        return sb.toString();
    }

    public Uri asUri(String authority) {
        Uri.Builder ub = new Uri.Builder()
                .scheme("content")
                .authority(authority)
                .appendPath("artInfo");
        StringBuilder builder = new StringBuilder();
        builder.append("forArtist=").append(forArtist);
        if (!StringUtils.isEmpty(artistName)) {
            builder.append("&artistName=").append(encodeString(artistName));
        }
        if (!StringUtils.isEmpty(albumName)) {
            builder.append("&albumName=").append(encodeString(albumName));
        }
        if (artworkUri != null && !Uri.EMPTY.equals(artworkUri)) {
            builder.append("&arworkUri=").append(encodeString(artworkUri.toString()));
        }
        return ub.encodedQuery(builder.toString()).build();
    }

    public static ArtInfo fromUri(Uri uri) {
        String encUri = uri.getQueryParameter("artworkUri");
        Uri artworkUri = null;
        if (!StringUtils.isEmpty(encUri)) {
            artworkUri = Uri.parse(decodeString(encUri));
        }
        if (uri.getBooleanQueryParameter("forArtist", false)) {
            String encArtist = uri.getQueryParameter("artistName");
            if (!StringUtils.isEmpty(encArtist)) {
                return ArtInfo.forArtist(decodeString(encArtist), artworkUri);
            } else if (artworkUri != null) {
                return ArtInfo.forArtist(null, artworkUri);
            } else {
                return NULLINSTANCE;
            }
        } else {
            String encArtist = uri.getQueryParameter("artistName");
            String encAlbum = uri.getQueryParameter("albumName");
            if (!StringUtils.isEmpty(encArtist) && !StringUtils.isEmpty(encAlbum)) {
                return ArtInfo.forAlbum(decodeString(encArtist), decodeString(encAlbum), artworkUri);
            } else if (artworkUri != null) {
                return ArtInfo.forAlbum(null, null, artworkUri);
            } else {
                return NULLINSTANCE;
            }
        }
    }

    private static String encodeString(String string) {
        return Base64.encodeToString(string.getBytes(Charset.defaultCharset()),
                Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
    }

    private static String decodeString(String string) {
        return new String(Base64.decode(string, Base64.URL_SAFE), Charset.defaultCharset());
    }

    @Override
    public int compareTo(ArtInfo another) {
        //Idk just be consistent, and this way i don't have to do null checks
        //we only need this so libraries can add them in any order and we can be
        //sure we always display the same ones in the same order
        return toString().compareTo(another.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(artistName);
        dest.writeString(albumName);
        artworkUri.writeToParcel(dest, flags);
        dest.writeInt(forArtist ? 1 : 0);
    }

    public static final Creator<ArtInfo> CREATOR = new Creator<ArtInfo>() {
        @Override
        public ArtInfo createFromParcel(Parcel in) {
            return new ArtInfo(
                    in.readString(),
                    in.readString(),
                    Uri.CREATOR.createFromParcel(in),
                    in.readInt() == 1
            );
        }

        @Override
        public ArtInfo[] newArray(int size) {
            return new ArtInfo[size];
        }
    };
}
