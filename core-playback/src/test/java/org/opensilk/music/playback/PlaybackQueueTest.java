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

import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.playback.service.PlaybackService;
import org.opensilk.music.playback.service.PlaybackServiceComponent;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import rx.schedulers.Schedulers;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class PlaybackQueueTest {
    
    PlaybackQueue mPlaybackQueue;

    @Before
    public void setup() {
        IndexClient client = Mockito.mock(IndexClient.class);
        PlaybackService service = Mockito.mock(PlaybackService.class);
        Mockito.when(service.getScheduler()).thenReturn(Schedulers.immediate());
        mPlaybackQueue = new PlaybackQueue(client, service);
        mPlaybackQueue.load();
    }

    @Test
    public void testAddNext() {
        List<Uri> toAdd = new ArrayList<>();
        for (int ii=0; ii<5; ii++) {
            toAdd.add(Uri.parse("content://someauthority2/lib1/id"+ii));
        }
        mPlaybackQueue.addNext(toAdd);
        for (int ii=0; ii<5; ii++) {
            assertThat(mPlaybackQueue.mQueue.get(mPlaybackQueue.mQueue.size() - (1 + ii))).isEqualTo(toAdd.get(5-ii));
        }
    }
}
