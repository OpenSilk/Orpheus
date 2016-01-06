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
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opensilk.music.playback.PlaybackQueueTestUtil.checkListsMatch;

/*
 * Initial conditions
 * Queue: yes
 * Repeat: all
 * Shuffle: on
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class PlaybackQueueTest3 {

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
        when(mClient.getLastQueueRepeatMode()).thenReturn(-1);
        when(mClient.getLastQueueShuffleMode()).thenReturn(PlaybackConstants.SHUFFLE_NORMAL);
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
        assertThat(mPlaybackQueue.getNextPos()).isGreaterThan(-1);
        assertThat(mPlaybackQueue.getNextPos()).isLessThan(10);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);
        assertThat(mPlaybackQueue.getRepeatMode()).isEqualTo(PlaybackConstants.REPEAT_ALL);
        assertThat(mPlaybackQueue.getShuffleMode()).isEqualTo(PlaybackConstants.SHUFFLE_NORMAL);
        verify(mListener).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }


    @Test
    public void testAddNext() {
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

        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 20);

        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition);
        assertThat(mPlaybackQueue.get().get(mInitialPosition)).isEqualTo(mInitialQueue.get(mInitialPosition));
        assertThat(mPlaybackQueue.getNextPos()).isGreaterThan(-1);
        assertThat(mPlaybackQueue.getNextPos()).isLessThan(20);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);

        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testAddEnd() {
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

        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 20);

        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition);
        assertThat(mPlaybackQueue.get().get(mInitialPosition)).isEqualTo(mInitialQueue.get(mInitialPosition));
        assertThat(mPlaybackQueue.getNextPos()).isGreaterThan(-1);
        assertThat(mPlaybackQueue.getNextPos()).isLessThan(20);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);

        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testMoveToNextRepeatOne_repeatOverridesShuffle() {
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
        mPlaybackQueue.setShuffleMode(PlaybackConstants.SHUFFLE_NORMAL);
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

    @Test
    public void testRemoveNext_currentAndPrevShouldNotChange() {
        mPlaybackQueue.moveToNext();
        Uri uri = mPlaybackQueue.getCurrentUri();
        Uri nextUri = mPlaybackQueue.getNextUri();
        Uri prevUri = mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos());
        mPlaybackQueue.remove(nextUri);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 9);
        assertThat(mPlaybackQueue.getCurrentUri()).isEqualTo(uri);
        assertThat(mPlaybackQueue.getNextPos()).isGreaterThan(-1);
        assertThat(mPlaybackQueue.getNextPos()).isLessThan(9);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos())).isEqualTo(prevUri);
        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testRemoveCurrent_nextShouldBecomeCurrent() {
        mPlaybackQueue.moveToNext();
        Uri uri = mPlaybackQueue.getCurrentUri();
        Uri nextUri = mPlaybackQueue.getNextUri();
        Uri prevUri = mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos());
        mPlaybackQueue.remove(uri);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 9);
        assertThat(mPlaybackQueue.getCurrentUri()).isEqualTo(nextUri);
        assertThat(mPlaybackQueue.getNextPos()).isGreaterThan(-1);
        assertThat(mPlaybackQueue.getNextPos()).isLessThan(9);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos())).isEqualTo(prevUri);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testRemoveCurrentAndPrevious_nextShouldBecomeCurrent_prevBecomesInvalid() {
        mPlaybackQueue.moveToNext();
        Uri uri = mPlaybackQueue.getCurrentUri();
        Uri nextUri = mPlaybackQueue.getNextUri();
        Uri prevUri = mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos());
        List<Uri> lst = new ArrayList<>();
        lst.add(uri);
        lst.add(prevUri);
        mPlaybackQueue.remove(lst);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 8);
        assertThat(mPlaybackQueue.getCurrentUri()).isEqualTo(nextUri);
        assertThat(mPlaybackQueue.getNextPos()).isGreaterThan(-1);
        assertThat(mPlaybackQueue.getNextPos()).isLessThan(8);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testMoveNext_urisShouldNotChange() {
        mPlaybackQueue.moveToNext();
        Uri uri = mPlaybackQueue.getCurrentUri();
        Uri nextUri = mPlaybackQueue.getNextUri();
        Uri prevUri = mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos());
        int start = mPlaybackQueue.getNextPos();
        int end = start != 4 ? 4 : 6;
        mPlaybackQueue.moveItem(start, end);
        assertThat(mPlaybackQueue.get().get(end)).isEqualTo(mInitialQueue.get(start));
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 10);
        assertThat(mPlaybackQueue.getCurrentUri()).isEqualTo(uri);
        assertThat(mPlaybackQueue.getNextUri()).isEqualTo(nextUri);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos())).isEqualTo(prevUri);
        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testMoveToNext_resets() {
        int current = mPlaybackQueue.getCurrentPos();
        int next = mPlaybackQueue.getNextPos();
        for (int ii=mInitialPosition+1; ii<10; ii++) {
            mPlaybackQueue.moveToNext();
            assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(next);
            assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(current);
            current = mPlaybackQueue.getCurrentPos();
            next = mPlaybackQueue.getNextPos();
        }
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        current = mPlaybackQueue.getCurrentPos();
        next = mPlaybackQueue.getNextPos();
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(next);
    }

}
