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
import java.util.List;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.opensilk.music.playback.PlaybackQueueTestUtil.*;

/*
 * Initial conditions
 * Queue: yes
 * Repeat: all
 * Shuffle: off
 * Non zero start pos
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class PlaybackQueueTest2 {

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
        mInitialPosition = 4;
        mClient = mock(IndexClient.class);
        when(mClient.getLastQueue()).thenReturn(mInitialQueue);
        when(mClient.getDescriptions(mInitialQueue)).thenReturn(Observable.just(descriptions));
        when(mClient.getLastQueuePosition()).thenReturn(mInitialPosition);
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
        verify(mClient).getDescriptions(mInitialQueue);
        assertThat(mPlaybackQueue.get()).isEqualTo(mInitialQueue);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(mInitialPosition + 1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(mInitialPosition - 1);
        assertThat(mPlaybackQueue.getRepeatMode()).isEqualTo(PlaybackConstants.REPEAT_ALL);
        assertThat(mPlaybackQueue.getShuffleMode()).isEqualTo(PlaybackConstants.SHUFFLE_NONE);
        verify(mListener).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testAddNext() {
        List<Uri> queue = new ArrayList<>(5);
        List<MediaDescriptionCompat> descriptions = new ArrayList<>(5);
        for (int ii=0; ii<5; ii++) {
            Uri uri = Uri.parse("content://test2/track/" + ii);
            queue.add(uri);
            MediaDescriptionCompat desc = mock(MediaDescriptionCompat.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));

        mPlaybackQueue.addNext(queue);

        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 15);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition);
        assertThat(mPlaybackQueue.get().get(mInitialPosition)).isEqualTo(mInitialQueue.get(mInitialPosition));
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(mInitialPosition + 1);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getNextPos())).isEqualTo(queue.get(0));
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(mInitialPosition - 1);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos())).isEqualTo(mInitialQueue.get(mInitialPosition - 1));
        verify(mListener).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testAddEnd() {
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
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition);
        assertThat(mPlaybackQueue.get().get(mInitialPosition)).isEqualTo(mInitialQueue.get(mInitialPosition));
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(mInitialPosition + 1);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getNextPos())).isEqualTo(mInitialQueue.get(mInitialPosition + 1));
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(mInitialPosition - 1);
        assertThat(mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos())).isEqualTo(mInitialQueue.get(mInitialPosition - 1));

        verify(mListener).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testMoveToNext_wraps() {
        for (int ii=mInitialPosition+1; ii<10; ii++) {
            mPlaybackQueue.moveToNext();
            assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(ii);
        }
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(1);
    }

    @Test
    public void testReplace_updatesCurrentToNewPos() {
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
        mPlaybackQueue.replace(queue, 4);

        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 10);
        assertThat(mPlaybackQueue.get()).isEqualTo(queue);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(4);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(5);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(3);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testReplaceWithEmptylist_resetsQueue() {
        mPlaybackQueue.replace(Collections.<Uri>emptyList(), 4);
        assertThat(mPlaybackQueue.get()).isEmpty();
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 0);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(-1);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(-1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testRemoveEntireList_resetsQueue() {
        mPlaybackQueue.remove(mInitialQueue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 0);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(-1);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(-1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(-1);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testRemoveNext_doesntChangeCurrent() {
        Uri currentUri = mPlaybackQueue.getCurrentUri();
        Uri nextUri = mPlaybackQueue.getNextUri();
        mPlaybackQueue.remove(nextUri);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 9);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition);
        assertThat(mPlaybackQueue.getCurrentUri()).isEqualTo(currentUri);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(mInitialPosition + 1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(mInitialPosition - 1);
        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testRemoveCurrent_nextBecomesCurrent() {
        Uri nextUri = mPlaybackQueue.getNextUri();
        Uri prevUri = mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos());
        mPlaybackQueue.remove(mInitialPosition);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 9);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition);
        //make sure we move to next
        assertThat(mPlaybackQueue.getCurrentUri()).isEqualTo(nextUri);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(mInitialPosition + 1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(mInitialPosition - 1);
        assertThat( mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos())).isEqualTo(prevUri);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testRemoveBeforeCurrent_updatesPointersWithoutChangingCurrent() {
        Uri uri = mInitialQueue.get(mInitialPosition - 1);
        mPlaybackQueue.remove(uri);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 9);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition - 1);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(mInitialPosition);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(mInitialPosition - 2);
        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testRemoveNonexistent_doesNothing() {
        Uri uri = Uri.parse("content://test/track/notinlist");
        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
        mPlaybackQueue.remove(uri);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 10);
        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testMoveItemAroundCurrent_updatesPointers() {
        Uri uri = mPlaybackQueue.getCurrentUri();
        Uri nextUri = mPlaybackQueue.getNextUri();
        Uri prevUri = mPlaybackQueue.get().get(mPlaybackQueue.getPreviousPos());
        int start = mInitialPosition - 2;
        int end = mInitialPosition + 2;
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
    public void testMoveItemBeforeCurrent_doesntAlterPointers() {
        int start = mInitialPosition - 3;
        int end = mInitialPosition - 1;
        mPlaybackQueue.moveItem(start, end);
        assertThat(mPlaybackQueue.get().get(end)).isEqualTo(mInitialQueue.get(start));
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 10);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(mInitialPosition+1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(mInitialPosition-1);
        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testMoveItemAfterCurrent_doesntAlterPointers() {
        int start = mInitialPosition + 2;
        int end = mInitialPosition + 4;
        mPlaybackQueue.moveItem(start, end);
        assertThat(mPlaybackQueue.get().get(end)).isEqualTo(mInitialQueue.get(start));
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 10);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(mInitialPosition);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(mInitialPosition + 1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(mInitialPosition-1);
        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testMoveItemCurrent_updatesPointer_changesNextPrev() {
        int start = mInitialPosition;
        int end = mInitialPosition + 2;
        mPlaybackQueue.moveItem(start, end);
        assertThat(mPlaybackQueue.get().get(end)).isEqualTo(mInitialQueue.get(mInitialPosition));
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems(), 10);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(end);
        assertThat(mPlaybackQueue.getNextPos()).isEqualTo(end + 1);
        assertThat(mPlaybackQueue.getPreviousPos()).isEqualTo(end - 1);
        verify(mListener, times(1)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testGetPosOfId_works() {
        MediaSessionCompat.QueueItem item = mPlaybackQueue.getQueueItems().get(4);
        int pos = mPlaybackQueue.getPosOfId(item.getQueueId());
        assertThat(pos).isEqualTo(4);
    }

}
