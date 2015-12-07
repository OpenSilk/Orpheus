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

package org.opensilk.music.index.database;

import android.net.Uri;

import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 11/20/15.
 */
public class TestData {

    static final Uri URI_SFB = Uri.parse("content://sample/foo/bar");
    static final Uri URI_SFB_PARENT = Uri.parse("content://sample/foo");
    static final List<Uri> URI_SFB_CHILDREN_0_10 = new ArrayList<>();
    static {
        for (int ii=0; ii<10; ii++) {
            URI_SFB_CHILDREN_0_10.add(Uri.parse("content://sample/foo/bar/" + ii));
        }
    }
    static final List<Uri> URI_SFB_CHILDREN_0_125 = new ArrayList<>();
    static {
        for (int ii=0; ii<125; ii++) {
            URI_SFB_CHILDREN_0_125.add(Uri.parse("content://sample/foo/bar/" + ii));
        }
    }
    static final List<Track> TRACK_SFB_0_10 = new ArrayList<>();
    static final List<Metadata> METADATA_TRACK_SFB_0_10 = new ArrayList<>();
    static {
        for (int ii=0; ii<10; ii++) {
            Track track = Track.builder()
                    .setUri(Uri.parse("content://sample/foo/bar/track" + ii))
                    .setName("track" + ii)
                    .setParentUri(URI_SFB)
                    .addRes(Track.Res.builder().setUri(Uri.parse("content://sample/foo/bar/res"+ii)).build())
                    .build();
            TRACK_SFB_0_10.add(track);
            Metadata meta = Metadata.builder()
                    .putString(Metadata.KEY_TRACK_NAME, "metatrack"+ii)
                    .putString(Metadata.KEY_ALBUM_NAME, "album"+ii%2)
                    .putString(Metadata.KEY_ARTIST_NAME, "artist" + ii % 2)
                    .putString(Metadata.KEY_ALBUM_ARTIST_NAME, "artist"+ii%2)
                    .build();
            METADATA_TRACK_SFB_0_10.add(meta);
        }
    }
    static final List<Metadata> METADATA_TRACK_SFB_0_113 = new ArrayList<>();
    static {
        for (int ii=0; ii<113; ii++) {
            Track track = Track.builder()
                    .setUri(Uri.parse("content://sample/foo/bar/track" + ii))
                    .setName("track" + ii)
                    .setParentUri(URI_SFB)
                    .addRes(Track.Res.builder().setUri(Uri.parse("content://sample/foo/bar/res"+ii)).build())
                    .build();
            TRACK_SFB_0_10.add(track);
            Metadata meta = Metadata.builder()
                    .putString(Metadata.KEY_TRACK_NAME, "metatrack"+ii)
                    .putString(Metadata.KEY_ALBUM_NAME, "album"+ii%2)
                    .putString(Metadata.KEY_ARTIST_NAME, "artist" + ii % 2)
                    .putString(Metadata.KEY_ALBUM_ARTIST_NAME, "artist"+ii%2)
                    .build();
            METADATA_TRACK_SFB_0_113.add(meta);
        }
    }
}
