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
import org.opensilk.music.playback.service.PlaybackServiceProxy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.opensilk.music.playback.PlaybackQueueTestUtil.*;

/*
 * Initial conditions
 * Queue: no
 * Repeat: all
 * Shuffle: off
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class PlaybackQueueTest {

    IndexClient mClient;
    PlaybackServiceProxy mService;
    PlaybackQueue mPlaybackQueue;
    PlaybackQueue.QueueChangeListener mListener;

    @Before
    public void setup() {
        mClient = mock(IndexClient.class);
        when(mClient.getLastQueue()).thenReturn(Collections.<Uri>emptyList());
        when(mClient.getLastQueuePosition()).thenReturn(-1);
        when(mClient.getLastQueueRepeatMode()).thenReturn(-1);
        when(mClient.getLastQueueShuffleMode()).thenReturn(-1);
        mService = Mockito.mock(PlaybackServiceProxy.class);
        when(mService.getScheduler()).thenReturn(Schedulers.immediate());
        mPlaybackQueue = new PlaybackQueue(mClient, mService);
        mListener = mock(PlaybackQueue.QueueChangeListener.class);
        mPlaybackQueue.setListener(mListener);
        mPlaybackQueue.load();
        verify(mClient).getLastQueue();
        verify(mClient).getLastQueuePosition();
        verify(mClient).getLastQueueRepeatMode();
        verify(mClient).getLastQueueShuffleMode();
        verify(mClient, never()).getDescriptions(Collections.<Uri>emptyList());
        assertThat(mPlaybackQueue.get()).isEmpty();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(-1);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(-1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);
        assertThat(mPlaybackQueue.getRepeatMode()).isEqualTo(PlaybackConstants.REPEAT_ALL);
        assertThat(mPlaybackQueue.getShuffleMode()).isEqualTo(PlaybackConstants.SHUFFLE_NONE);
        verify(mListener).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testAddNext_wrapsPrev() {
        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescriptionCompat> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test/track/" + ii);
            queue.add(uri);
            MediaDescriptionCompat desc = mock(MediaDescriptionCompat.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));

        mPlaybackQueue.addNext(queue);
        verify(mClient).getDescriptions(queue);

        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 10);
        assertThat(mPlaybackQueue.get()).isEqualTo(queue);

        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        assertThat(mPlaybackQueue.get().get(0)).isEqualTo(queue.get(0));
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(1);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getNextPos())).isEqualTo(queue.get(1));
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(9);//wraps

        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testAddNextRepeatOff_doesntWrapPrev() {
        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescriptionCompat> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test/track/" + ii);
            queue.add(uri);
            MediaDescriptionCompat desc = mock(MediaDescriptionCompat.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));

        mPlaybackQueue.setRepeatMode(PlaybackConstants.REPEAT_NONE);

        mPlaybackQueue.addNext(queue);
        verify(mClient).getDescriptions(queue);

        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 10);
        assertThat(mPlaybackQueue.get()).isEqualTo(queue);

        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        assertThat(mPlaybackQueue.get().get(0)).isEqualTo(queue.get(0));
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(1);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getNextPos())).isEqualTo(queue.get(1));
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);//no wrap

        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testAddEnd_wrapsPrev() {
        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescriptionCompat> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test/track/" + ii);
            queue.add(uri);
            MediaDescriptionCompat desc = mock(MediaDescriptionCompat.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));

        mPlaybackQueue.addEnd(queue);
        verify(mClient).getDescriptions(queue);

        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.get()).isEqualTo(queue);

        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        assertThat(mPlaybackQueue.get().get(0)).isEqualTo(queue.get(0));
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(1);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getNextPos())).isEqualTo(queue.get(1));
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(9); //wrap

        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testAddEndRepeatOff_doesntWrapPrev() {
        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescriptionCompat> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test/track/" + ii);
            queue.add(uri);
            MediaDescriptionCompat desc = mock(MediaDescriptionCompat.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));

        mPlaybackQueue.setRepeatMode(PlaybackConstants.REPEAT_NONE);

        mPlaybackQueue.addEnd(queue);
        verify(mClient).getDescriptions(queue);

        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.get()).isEqualTo(queue);

        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        assertThat(mPlaybackQueue.get().get(0)).isEqualTo(queue.get(0));
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(1);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getNextPos())).isEqualTo(queue.get(1));
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1); //no wrap

        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testMoveToNextRepeatOne_doesntChangePointers() {
        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescriptionCompat> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test/track/" + ii);
            queue.add(uri);
            MediaDescriptionCompat desc = mock(MediaDescriptionCompat.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));


        mPlaybackQueue.setRepeatMode(PlaybackConstants.REPEAT_CURRENT);
        mPlaybackQueue.addEnd(queue);

        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(0);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(0);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(0);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);
    }


}
