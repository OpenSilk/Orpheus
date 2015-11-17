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

package org.opensilk.music;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 11/16/15.
 */
public class TestData {

    public static final List<MediaSessionCompat.QueueItem> QUEUE_1 = new ArrayList<>(10);
    static {
        for (int ii=1; ii<11; ii++) {
            QUEUE_1.add(new MediaSessionCompat.QueueItem(
                    new MediaDescriptionCompat.Builder()
                            .setTitle("track"+ii)
                            .setMediaId("content://foo/track"+ii)
                            .setSubtitle("artist1")
                            .build(),
                    1));
        }
    }
}
