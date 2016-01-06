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

package org.opensilk.music.playback.control;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by drew on 5/6/15.
 */
public interface PlaybackController {

    public void notifyForegroundStateChanged(boolean inForeground);

    /*
     * Transport controls
     */

    public void play();

    public void playFromMediaId(String mediaId, Bundle extras);

    public void playFromSearch(String query, Bundle extras);

    public void skipToQueueItem(long id);

    public void pause();

    public void stop();

    public void seekTo(long pos);

    public void fastForward();

    public void skipToNext();

    public void rewind();

    public void skipToPrevious();

    public void setRating(RatingCompat rating);

    public void sendCustomAction(String action, Bundle args);

    /*
     * End transport controls
     */

    /*
     * Custom commands
     */

    public void playorPause();

    public void cycleRepeateMode();

    public void cycleShuffleMode();

    public void shuffleQueue();

    public void enqueueAll(List<Uri> queue, int where);

    public void enqueueAllNext(List<Uri> list);

    public void enqueueAllEnd(List<Uri> list);

    public void playAll(List<Uri> list, int startpos);

    public void removeQueueItem(Uri uri);

    public void removeQueueItemAt(int pos);

    public void clearQueue();

    public void moveQueueItemTo(Uri uri, int newPos);

    public void moveQueueItem(int from, int to);

    public void moveQueueItemToNext(int pos);

    public void switchToNewRenderer(@Nullable ComponentName componentName);

    public void getCurrentRenderer(final Action1<ComponentName> onNext);

    public PlaybackInfoCompat getPlaybackInfo();

    public void setVolume(int volume);

    /*
     * End custom commands
     */

    /*
     * Subscriptions
     */

    public Subscription subscribePlayStateChanges(Action1<PlaybackStateCompat> onNext);

    public Subscription subscribeMetaChanges(Action1<MediaMetadataCompat> onNext);

    public Subscription subscribeQueueChanges(Action1<List<MediaSessionCompat.QueueItem>> onNext);

    public Subscription subscribeAudioSessionIdChanges(Action1<Integer> onNext);

    public Subscription subscribeRepeatModeChanges(Action1<Integer> onNext);

    public Subscription subscribeShuffleModeChanges(Action1<Integer> onNext);

    /*
     * end subscriptions
     */

    public void connect();

    public void disconnect();

    public boolean isConnected();

}
