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

package org.opensilk.music.index.scanner;

import android.net.Uri;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by drew on 11/17/15.
 */
public class TestData {
    static final Uri URI_FOLDER1 = Uri.parse("content://foo/folder1");
    static final List<Bundleable> TRACKS_FOLDER1 = new ArrayList<>(5);
    static {
        for (int ii = 0; ii < 5; ii++) {
            TRACKS_FOLDER1.add(Track.builder()
                    .setName("track" + ii)
                    .setUri(Uri.parse("content://foo/folder1/track" + ii))
                    .setParentUri(URI_FOLDER1)
                    .addRes(Track.Res.builder()
                            .setUri(Uri.parse("content://foo/folder1/track" + ii + "/res")).build()).build());
        }
    }
    static final Map<Uri, Metadata> TRACK_META_FOLDER1 = new HashMap<>();
    static {
        for (int ii= 0; ii< 5; ii++) {
            TRACK_META_FOLDER1.put(Uri.parse("content://foo/folder1/track" + ii + "/res"),
                    Metadata.builder()
                        .putString(Metadata.KEY_TRACK_NAME, "track"+ii)
                        .build());
        }
    }
}
