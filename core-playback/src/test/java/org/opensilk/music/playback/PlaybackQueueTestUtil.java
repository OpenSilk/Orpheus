/*
 * Copyright (c) 2016 OpenSilk Productions LLC
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

import android.net.Uri;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by drew on 1/5/16.
 */
public class PlaybackQueueTestUtil {

    static void checkListsMatch(List<Uri> uris, List<MediaSessionCompat.QueueItem> items) {
        assertThat(uris.size()).isEqualTo(items.size());
        Iterator<Uri> urisI = uris.iterator();
        Iterator<MediaSessionCompat.QueueItem> itemsI = items.iterator();
        while (urisI.hasNext() && itemsI.hasNext()) {
            assertThat(urisI.next().toString()).isEqualTo(
                    itemsI.next().getDescription().getMediaId()
            );
        }
    }

    static void checkListsMatch(List<Uri> uris, List<MediaSessionCompat.QueueItem> items, int size) {
        assertThat(uris.size()).isEqualTo(items.size());
        assertThat(uris.size()).isEqualTo(size);
        assertThat(items.size()).isEqualTo(size);
        Iterator<Uri> urisI = uris.iterator();
        Iterator<MediaSessionCompat.QueueItem> itemsI = items.iterator();
        while (urisI.hasNext() && itemsI.hasNext()) {
            assertThat(urisI.next().toString()).isEqualTo(
                    itemsI.next().getDescription().getMediaId()
            );
        }
    }

}
