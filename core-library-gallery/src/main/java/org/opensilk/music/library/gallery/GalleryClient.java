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

package org.opensilk.music.library.gallery;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import org.opensilk.music.library.client.LibraryClient;
import org.opensilk.music.library.gallery.provider.GalleryMethods;

/**
 * Created by drew on 12/26/15.
 */
public class GalleryClient {

    final LibraryClient client;

    public GalleryClient(LibraryClient client) {
        this.client = client;
    }

    public static GalleryClient acquire(Context context, String authority) {
        return new GalleryClient(new LibraryClient(context, authority));
    }

    public void release() {
        client.release();
    }

    public @Nullable Uri getArtistsUri() {
        Bundle reply = client.makeCall(GalleryMethods.GET_ARTISTS_URI, null);
        if (GalleryExtras.getOk(reply)) {
            return GalleryExtras.getUri(reply);
        }
        return null;
    }

    public @Nullable Uri getAlbumsUri() {
        Bundle reply = client.makeCall(GalleryMethods.GET_ALBUMS_URI, null);
        if (GalleryExtras.getOk(reply)) {
            return GalleryExtras.getUri(reply);
        }
        return null;
    }

    public @Nullable Uri getGenresUri() {
        Bundle reply = client.makeCall(GalleryMethods.GET_GENRES_URI, null);
        if (GalleryExtras.getOk(reply)) {
            return GalleryExtras.getUri(reply);
        }
        return null;
    }

    public @Nullable Uri getTracksUri() {
        Bundle reply = client.makeCall(GalleryMethods.GET_TRACKS_URI, null);
        if (GalleryExtras.getOk(reply)) {
            return GalleryExtras.getUri(reply);
        }
        return null;
    }

    public @Nullable Uri getIndexedFoldersUri() {
        Bundle reply = client.makeCall(GalleryMethods.GET_INDEXT_FOLDERS_URI, null);
        if (GalleryExtras.getOk(reply)) {
            return GalleryExtras.getUri(reply);
        }
        return null;
    }

}
