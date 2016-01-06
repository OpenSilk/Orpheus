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
import android.support.v4.media.MediaDescriptionCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.playback.service.PlaybackServiceProxy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opensilk.music.playback.PlaybackQueueTestUtil.checkListsMatch;

/*
 * Initial conditions
 * Queue: yes
 * Repeat: off
 * Shuffle: off
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class PlaybackQueueTest4 {

    IndexClient mClient;
    PlaybackServiceProxy mService;
    PlaybackQueue mPlaybackQueue;
    PlaybackQueue.QueueChangeListener mListener;
    List<Uri> mInitialQueue;
    int mInitialPosition;

    @Before
    public void setup() {
        mInitialQueue = new ArrayList<>(10);
        List<MediaDescriptionCompat> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test/track/" + ii);
            mInitialQueue.add(uri);
            MediaDescriptionCompat desc = mock(MediaDescriptionCompat.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        mInitialPosition = 0;
        mClient = mock(IndexClient.class);
        when(mClient.getLastQueue()).thenReturn(mInitialQueue);
        when(mClient.getDescriptions(mInitialQueue)).thenReturn(Observable.just(descriptions));
        when(mClient.getLastQueuePosition()).thenReturn(mInitialPosition);
        when(mClient.getLastQueueRepeatMode()).thenReturn(PlaybackConstants.REPEAT_NONE);
        when(mClient.getLastQueueShuffleMode()).thenReturn(PlaybackConstants.SHUFFLE_NONE);
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
        verify(mClient).getDescriptions(mInitialQueue);
        assertThat(mPlaybackQueue.get()).isEqualTo(mInitialQueue);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(mInitialPosition + 1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(mInitialPosition - 1);
        assertThat(mPlaybackQueue.getRepeatMode()).isEqualTo(PlaybackConstants.REPEAT_NONE);
        assertThat(mPlaybackQueue.getShuffleMode()).isEqualTo(PlaybackConstants.SHUFFLE_NONE);
        verify(mListener).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testAddNextAfterMovementToEndOfQueue_updatesNext() {
        for (int ii=mInitialPosition+1; ii<10; ii++) {
            mPlaybackQueue.moveToNext();
            assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(ii);
        }

        List<Uri> queue = new ArrayList<>(1);
        List<MediaDescriptionCompat> descriptions = new ArrayList<>(1);
        for (int ii=0; ii<1; ii++) {
            Uri uri = Uri.parse("content://test2/track/" + ii);
            queue.add(uri);
            MediaDescriptionCompat desc = mock(MediaDescriptionCompat.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));

        mPlaybackQueue.addNext(queue);

        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 11);

        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(9);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(10);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(8);

        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testAddEndAfterMovementToEndOfQueue_updatesNext() {
        for (int ii=mInitialPosition+1; ii<10; ii++) {
            mPlaybackQueue.moveToNext();
            assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(ii);
        }

        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescriptionCompat> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test2/track/" + ii);
            queue.add(uri);
            MediaDescriptionCompat desc = mock(MediaDescriptionCompat.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));

        mPlaybackQueue.addEnd(queue);

        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 20);

        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(9);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(10);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(8);

        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testMoveToNext_stopsAtEndOfQueue() {
        for (int ii=mInitialPosition+1; ii<10; ii++) {
            mPlaybackQueue.moveToNext();
            assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(ii);
        }
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(9);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(-1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(8);
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(9);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(-1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(8);
    }

}
