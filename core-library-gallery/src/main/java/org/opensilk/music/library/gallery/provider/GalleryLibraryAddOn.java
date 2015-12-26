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

package org.opensilk.music.library.gallery.provider;

import android.net.Uri;
import android.os.Bundle;

import org.opensilk.music.library.gallery.GalleryExtras;

import java.net.URI;

/**
 * Created by drew on 12/26/15.
 */
public class GalleryLibraryAddOn {


    final Handler handler;

    public GalleryLibraryAddOn(Handler handler) {
        this.handler = handler;
    }

    public Reply handleCall(String method, String arg, Bundle extras) {

        final GalleryExtras.Builder ok = GalleryExtras.b();

        if (method == null) method = "_";
        switch (method) {
            case GalleryMethods.GET_ARTISTS_URI: {
                Uri uri = handler.getGalleryArtistsUri();
                return new Reply(ok.putOk(uri != null).putUri(uri).get());
            }
            case GalleryMethods.GET_ALBUMS_URI: {
                Uri uri = handler.getGalleryAlbumsUri();
                return new Reply(ok.putOk(uri != null).putUri(uri).get());
            }
            case GalleryMethods.GET_GENRES_URI: {
                Uri uri = handler.getGalleryGenresUri();
                return new Reply(ok.putOk(uri != null).putUri(uri).get());
            }
            case GalleryMethods.GET_TRACKS_URI: {
                Uri uri = handler.getGalleryTracksUri();
                return new Reply(ok.putOk(uri != null).putUri(uri).get());
            }
            case GalleryMethods.GET_INDEXT_FOLDERS_URI: {
                Uri uri = handler.getGalleryIndexedFolders();
                return new Reply(ok.putOk(uri != null).putUri(uri).get());
            }
            default: {
                return new Reply();
            }
        }
    }

    public interface Handler {
        Uri getGalleryArtistsUri();
        Uri getGalleryAlbumsUri();
        Uri getGalleryGenresUri();
        Uri getGalleryTracksUri();
        Uri getGalleryIndexedFolders();
    }

    public static class Reply {
        private final boolean handled;
        private final Bundle reply;

        private Reply() {
            this.handled = false;
            this.reply = null;
        }

        private Reply(Bundle reply) {
            this.handled = true;
            this.reply = reply;
        }

        public boolean isHandled() {
            return handled;
        }

        public Bundle getReply() {
            return reply;
        }
    }

}
