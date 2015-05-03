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

package org.opensilk.music.model;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by drew on 5/2/15.
 */
public class Playlist extends TrackCollection {

    protected Playlist(
            @NonNull String identity,
            @NonNull String name,
            @NonNull List<Uri> trackUris,
            @NonNull List<Uri> albumUris,
            @NonNull List<ArtInfo> artInfos
    ) {
        super(identity, name, trackUris, albumUris, artInfos);
    }

    protected Playlist(Bundle b) {
        super(b);
    }

    @Override
    protected String getClz() {
        return Playlist.class.getName();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final BundleCreator<Playlist> BUNDLE_CREATOR = new BundleCreator<Playlist>() {
        @Override
        public Playlist fromBundle(Bundle b) throws IllegalArgumentException {
            if (!Playlist.class.getName().equals(b.getString("clz"))) {
                throw new IllegalArgumentException("Wrong class for Playlist: "+b.getString("clz"));
            }
            return new Playlist(b);
        }
    };

    public static final class Builder extends TrackCollection.Builder<Playlist> {
        @Override
        public Playlist build() {
            if (identity == null || name == null) {
                throw new NullPointerException("identity and name are required");
            }
            List<Uri> albums = Arrays.asList(albumUris.toArray(new Uri[albumUris.size()]));
            List<ArtInfo> artworks = Arrays.asList(artInfos.toArray(new ArtInfo[artInfos.size()]));
            Collections.sort(albums);
            Collections.sort(artworks);
            if (artworks.size() > 4) {
                artworks = artworks.subList(0, 3); //Only need 4;
            }
            return new Playlist(identity, name, trackUris, albums, artworks);
        }
    }
}
