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

package org.opensilk.music.playback;

import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.net.Uri;

import org.opensilk.music.artwork.coverartarchive.Metadata;

import static android.media.MediaMetadata.METADATA_KEY_ALBUM;
import static android.media.MediaMetadata.METADATA_KEY_ALBUM_ART;
import static android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST;
import static android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI;
import static android.media.MediaMetadata.METADATA_KEY_ART;
import static android.media.MediaMetadata.METADATA_KEY_ARTIST;
import static android.media.MediaMetadata.METADATA_KEY_ART_URI;
import static android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON;
import static android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI;
import static android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE;
import static android.media.MediaMetadata.METADATA_KEY_DURATION;
import static android.media.MediaMetadata.METADATA_KEY_TITLE;

/**
 * Created by drew on 5/8/15.
 */
public class MediaMetadataHelper {

    private MediaMetadataHelper(){}

    public static long getDuration(MediaMetadata meta) {
        return meta.getLong(METADATA_KEY_DURATION);
    }

    public static String getDisplayName(MediaMetadata meta) {
        String name = meta.getString(METADATA_KEY_DISPLAY_TITLE);
        if (name == null) {
            name = meta.getString(METADATA_KEY_TITLE);
        }
        return name;
    }

    public static String getArtistName(MediaMetadata meta) {
        return meta.getString(METADATA_KEY_ARTIST);
    }

    public static String getAlbumName(MediaMetadata meta) {
        return meta.getString(METADATA_KEY_ALBUM);
    }

    public static String getAlbumArtistName(MediaMetadata meta) {
        return meta.getString(METADATA_KEY_ALBUM_ARTIST);
    }

    public static Uri getIconUri(MediaMetadata meta) {
        String uri = meta.getString(METADATA_KEY_DISPLAY_ICON_URI);
        if (uri == null) {
            uri = meta.getString(METADATA_KEY_ART_URI);
        }
        if (uri == null) {
            uri = meta.getString(METADATA_KEY_ALBUM_ART_URI);
        }
        return uri != null ? Uri.parse(uri) : null;
    }

    public static Bitmap getIcon(MediaMetadata meta) {
        Bitmap bitmap = meta.getBitmap(METADATA_KEY_DISPLAY_ICON);
        if (bitmap == null) {
            bitmap = meta.getBitmap(METADATA_KEY_ART);
        }
        if (bitmap == null) {
            bitmap = meta.getBitmap(METADATA_KEY_ALBUM_ART);
        }
        return bitmap;
    }
}
