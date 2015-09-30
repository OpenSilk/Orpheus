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

import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.playback.service.PlaybackService;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class PlaybackQueueTest {

    IndexClient mClient;
    PlaybackService mService;
    PlaybackQueue mPlaybackQueue;
    PlaybackQueue.QueueChangeListener mListener;

    @Before
    public void setup() {
        mClient = mock(IndexClient.class);
        when(mClient.getLastQueue()).thenReturn(Collections.<Uri>emptyList());
        when(mClient.getLastQueuePosition()).thenReturn(-1);
        when(mClient.getLastQueueRepeatMode()).thenReturn(-1);
        when(mClient.getLastQueueShuffleMode()).thenReturn(-1);
        mService = Mockito.mock(PlaybackService.class);
        when(mService.getScheduler()).thenReturn(Schedulers.immediate());
        mPlaybackQueue = new PlaybackQueue(mClient, mService) {
            @Override MediaSession.QueueItem makeNewQueueItem(Uri uri, List<MediaDescription> descriptions) {
                for (MediaDescription desc : descriptions) {
                    if (uri.toString().equals(desc.getMediaId())) {
                        MediaSession.QueueItem item = mock(MediaSession.QueueItem.class);
                        when(item.getDescription()).thenReturn(desc);
                        when(item.getQueueId()).thenReturn(mIdGenerator.incrementAndGet());
                        return item;
                    }
                }
                return null;
            }
        };
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
        assertThat(mPlaybackQueue.getRepeatMode()).isEqualTo(PlaybackConstants.REPEAT_ALL);
        assertThat(mPlaybackQueue.getShuffleMode()).isEqualTo(PlaybackConstants.SHUFFLE_NONE);
        verify(mListener).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    void checkListsMatch(List<Uri> uris, List<MediaSession.QueueItem> items) {
        assertThat(uris.size()).isEqualTo(items.size());
        Iterator<Uri> urisI = uris.iterator();
        Iterator<MediaSession.QueueItem> itemsI = items.iterator();
        while (urisI.hasNext() && itemsI.hasNext()) {
            assertThat(urisI.next().toString()).isEqualTo(
                    itemsI.next().getDescription().getMediaId()
            );
        }
    }

    @Test
    public void testAddNextOnEmptyQueue() {
        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescription> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test/track/" + ii);
            queue.add(uri);
            MediaDescription desc = mock(MediaDescription.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));
        mPlaybackQueue.addNext(queue);
        verify(mClient).getDescriptions(queue);
        assertThat(mPlaybackQueue.getQueueItems().size()).isEqualTo(queue.size());
        assertThat(mPlaybackQueue.get()).isEqualTo(queue);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testaddNextWithQueue() {
        //populate with some stuff
        testAddNextOnEmptyQueue();
        List<Uri> queue = new ArrayList<>(1);
        List<MediaDescription> descriptions = new ArrayList<>(1);
        for (int ii=0; ii<1; ii++) {
            Uri uri = Uri.parse("content://test2/track/" + ii);
            queue.add(uri);
            MediaDescription desc = mock(MediaDescription.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));
        mPlaybackQueue.addNext(queue);
        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getQueueItems().size()).isEqualTo(11);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(11);
        assertThat(mPlaybackQueue.getQueueItems().get(1).getDescription().getMediaId())
                .isEqualTo(queue.get(0).toString());
        assertThat(mPlaybackQueue.get().get(1)).isEqualTo(queue.get(0));
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testMoveToNextRepeatAll() {
        testAddNextOnEmptyQueue();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        for (int ii=1; ii<10; ii++) {
            mPlaybackQueue.moveToNext();
            assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(ii);
        }
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(1);
    }

    @Test
    public void testMoveToNextRepeatOne() {
        testAddNextOnEmptyQueue();
        mPlaybackQueue.setRepeatMode(PlaybackConstants.REPEAT_CURRENT);
        verify(mListener, times(1)).onQueueChanged();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
    }

    @Test
    public void testMoveToNextRepeatOff() {
        testAddNextOnEmptyQueue();
        mPlaybackQueue.setRepeatMode(PlaybackConstants.REPEAT_NONE);
        verify(mListener, times(1)).onQueueChanged();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        for (int ii=1; ii<10; ii++) {
            mPlaybackQueue.moveToNext();
            assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(ii);
        }
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(-1);
        mPlaybackQueue.moveToNext();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(-1);
    }

    @Test
    public void testaddNextInsert() {
        //populate with some stuff
        testAddNextOnEmptyQueue();
        mPlaybackQueue.goToItem(4);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(4);
        List<Uri> queue = new ArrayList<>(1);
        List<MediaDescription> descriptions = new ArrayList<>(1);
        for (int ii=0; ii<1; ii++) {
            Uri uri = Uri.parse("content://test2/track/" + ii);
            queue.add(uri);
            MediaDescription desc = mock(MediaDescription.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));
        mPlaybackQueue.addNext(queue);
        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getQueueItems().size()).isEqualTo(11);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(11);
        //should be 6th item
        assertThat(mPlaybackQueue.getQueueItems().get(5).getDescription().getMediaId())
                .isEqualTo(queue.get(0).toString());
        assertThat(mPlaybackQueue.get().get(5)).isEqualTo(queue.get(0));
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(4);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testaddNextAfterMovementPastEndOfQueue() {
        //populate with some stuff
        testMoveToNextRepeatOff();
        List<Uri> queue = new ArrayList<>(1);
        List<MediaDescription> descriptions = new ArrayList<>(1);
        for (int ii=0; ii<1; ii++) {
            Uri uri = Uri.parse("content://test2/track/" + ii);
            queue.add(uri);
            MediaDescription desc = mock(MediaDescription.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));
        mPlaybackQueue.addNext(queue);
        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getQueueItems().size()).isEqualTo(11);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(11);
        //should be first item
        assertThat(mPlaybackQueue.getQueueItems().get(0).getDescription().getMediaId())
                .isEqualTo(queue.get(0).toString());
        assertThat(mPlaybackQueue.get().get(0)).isEqualTo(queue.get(0));
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testAddEndOnEmptyQueue() {
        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescription> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test/track/" + ii);
            queue.add(uri);
            MediaDescription desc = mock(MediaDescription.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));
        mPlaybackQueue.addEnd(queue);
        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.get()).isEqualTo(queue);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testAddEndWithQueue() {
        testAddEndOnEmptyQueue();
        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescription> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test2/track/" + ii);
            queue.add(uri);
            MediaDescription desc = mock(MediaDescription.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));
        mPlaybackQueue.addEnd(queue);
        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.get().size()).isEqualTo(20);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testAddEndAfterMovementPastEndOfQueue() {
        testMoveToNextRepeatOff();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(-1);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener, times(1)).onQueueChanged();
        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescription> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test2/track/" + ii);
            queue.add(uri);
            MediaDescription desc = mock(MediaDescription.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));
        mPlaybackQueue.addEnd(queue);
        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.get().size()).isEqualTo(20);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(10);
        //one extra
        verify(mListener, times(3)).onCurrentPosChanged();
        //no extra
        verify(mListener, times(1)).onQueueChanged();
    }

    @Test
    public void testReplaceQueue() {
        //populate with some stuff
        testAddNextOnEmptyQueue();
        verify(mListener, times(2)).onCurrentPosChanged();
        List<Uri> queue = new ArrayList<>(10);
        List<MediaDescription> descriptions = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test2/track/" + ii);
            queue.add(uri);
            MediaDescription desc = mock(MediaDescription.class);
            when(desc.getMediaId()).thenReturn(uri.toString());
            descriptions.add(desc);
        }
        when(mClient.getDescriptions(queue)).thenReturn(Observable.just(descriptions));
        mPlaybackQueue.replace(queue, 4);
        verify(mClient).getDescriptions(queue);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.get()).isEqualTo(queue);
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(4);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testReplaceWithEmptylist() {
        //populate with some stuff
        testAddNextOnEmptyQueue();
        verify(mListener, times(2)).onCurrentPosChanged();
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.replace(Collections.<Uri>emptyList(), 4);
        assertThat(mPlaybackQueue.get()).isEmpty();
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(-1);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testRemoveEntireList() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        verify(mListener, times(2)).onCurrentPosChanged();
        List<Uri> queue = new ArrayList<>(10);
        for (int ii=0; ii<10; ii++) {
            Uri uri = Uri.parse("content://test/track/" + ii);
            queue.add(uri);
        }
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.remove(queue);
        assertThat(mPlaybackQueue.get()).isEmpty();
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(-1);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testRemoveBeforeCurrent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        verify(mListener, times(2)).onCurrentPosChanged();
        mPlaybackQueue.goToItem(5);
        verify(mListener, times(3)).onCurrentPosChanged();
        Uri uri =Uri.parse("content://test/track/3");
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.remove(uri);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(9);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(4);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testRemoveCurrent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        verify(mListener, times(2)).onCurrentPosChanged();
        mPlaybackQueue.goToItem(3);
        verify(mListener, times(3)).onCurrentPosChanged();
        Uri uri =Uri.parse("content://test/track/3");
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.remove(uri);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(9);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(3);
        verify(mListener, times(4)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testRemoveCurrentAtEndOfList() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        verify(mListener, times(2)).onCurrentPosChanged();
        mPlaybackQueue.goToItem(9);
        verify(mListener, times(3)).onCurrentPosChanged();
        Uri uri =Uri.parse("content://test/track/9");
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.remove(uri);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(9);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(8);
        verify(mListener, times(4)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testRemoveAfterCurrent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        verify(mListener, times(2)).onCurrentPosChanged();
        mPlaybackQueue.goToItem(3);
        verify(mListener, times(3)).onCurrentPosChanged();
        Uri uri =Uri.parse("content://test/track/5");
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.remove(uri);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(9);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(3);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testRemoveWithNoCurrent() {
        //populate with some stuff
        testMoveToNextRepeatOff();
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(-1);
        verify(mListener, times(2)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
        Uri uri =Uri.parse("content://test/track/5");
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.remove(uri);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(9);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(0);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testRemoveNonexistent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        mPlaybackQueue.goToItem(3);
        verify(mListener, times(3)).onCurrentPosChanged();
        Uri uri =Uri.parse("content://test/track/22");
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.remove(uri);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(10);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(3);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testRemoveIndexAfterCurrent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        mPlaybackQueue.goToItem(3);
        verify(mListener, times(3)).onCurrentPosChanged();
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.remove(4);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(9);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(3);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testRemoveIndexBeforeCurrent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        mPlaybackQueue.goToItem(3);
        verify(mListener, times(3)).onCurrentPosChanged();
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.remove(2);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(9);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(2);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testRemoveIndexCurrent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        mPlaybackQueue.goToItem(3);
        verify(mListener, times(3)).onCurrentPosChanged();
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.remove(3);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(9);
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(3);
        verify(mListener, times(4)).onCurrentPosChanged();
        verify(mListener, never()).onQueueChanged();
    }

    @Test
    public void testMoveItemAroundCurrent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        mPlaybackQueue.goToItem(3);
        verify(mListener, times(3)).onCurrentPosChanged();
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.moveItem(2, 5);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(10);
        assertThat(mPlaybackQueue.get().get(5)).isEqualTo(Uri.parse("content://test/track/2"));
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(2);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testMoveItemBeforeCurrent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        mPlaybackQueue.goToItem(6);
        verify(mListener, times(3)).onCurrentPosChanged();
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.moveItem(2, 5);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(10);
        assertThat(mPlaybackQueue.get().get(5)).isEqualTo(Uri.parse("content://test/track/2"));
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(6);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testMoveItemAfterCurrent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        mPlaybackQueue.goToItem(1);
        verify(mListener, times(3)).onCurrentPosChanged();
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.moveItem(2, 5);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(10);
        assertThat(mPlaybackQueue.get().get(5)).isEqualTo(Uri.parse("content://test/track/2"));
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(1);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testMoveItemCurrent() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        mPlaybackQueue.goToItem(2);
        verify(mListener, times(3)).onCurrentPosChanged();
        //make sure we dont try to get any descriptions
        verifyNoMoreInteractions(mClient);
        mPlaybackQueue.moveItem(2, 5);
        assertThat(mPlaybackQueue.get().size()).isEqualTo(10);
        assertThat(mPlaybackQueue.get().get(5)).isEqualTo(Uri.parse("content://test/track/2"));
        checkListsMatch(mPlaybackQueue.get(), mPlaybackQueue.getQueueItems());
        assertThat(mPlaybackQueue.getCurrentPos()).isEqualTo(5);
        verify(mListener, times(3)).onCurrentPosChanged();
        verify(mListener).onQueueChanged();
    }

    @Test
    public void testgetPosOfId() {
        //populate with some stuff
        testAddEndOnEmptyQueue();
        MediaSession.QueueItem item = mPlaybackQueue.getQueueItems().get(4);
        int pos = mPlaybackQueue.getPosOfId(item.getQueueId());
        assertThat(pos).isEqualTo(4);
    }

}
