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
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.audiofx.AudioEffect;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.PlaybackConstants.CMD;
import org.opensilk.music.playback.service.IPlaybackService;
import org.opensilk.music.playback.service.PlaybackService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_CONNECTING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_FAST_FORWARDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_REWINDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;

/**
 * Created by drew on 5/6/15.
 */
@Singleton
public class PlaybackController {

    final Context mAppContext;
    final Handler mCallbackHandler = new Handler(Looper.getMainLooper());

    int mForegroundActivities = 0;
    boolean mWaitingForService = false;
    IPlaybackService mPlaybackService;
    MediaController mMediaController;
    MediaController.TransportControls mTransportControls;

    MediaBrowser mMediaBrowser;

    @Inject
    public PlaybackController(@ForApplication Context mAppContext) {
        this.mAppContext = new ContextWrapper(mAppContext);
        this.mMediaBrowser = new MediaBrowser(
                this.mAppContext, new ComponentName(this.mAppContext, PlaybackService.class),
                mConnectionCallback, null
        );
    }

    public void notifyForegroundStateChanged(boolean inForeground) {
        int old = mForegroundActivities;
        if (inForeground) {
            mForegroundActivities++;
        } else {
            mForegroundActivities--;
        }

        if (old == 0 || mForegroundActivities == 0) {
            final Intent intent = new Intent(mAppContext, PlaybackService.class);
            intent.setAction(PlaybackConstants.FOREGROUND_STATE_CHANGED);
            intent.putExtra(PlaybackConstants.NOW_IN_FOREGROUND, mForegroundActivities != 0);
            mAppContext.startService(intent);
        }

        if (old == 0) {
            connect();
        } else if (mForegroundActivities == 0) {
            disconnect();
        }
    }

    public static boolean isActive(PlaybackStateCompat state) {
        switch (state.getState()) {
            case STATE_FAST_FORWARDING:
            case STATE_REWINDING:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_NEXT:
            case STATE_BUFFERING:
            case STATE_CONNECTING:
            case STATE_PLAYING:
                return true;
        }
        return false;
    }

    public static boolean isActive(PlaybackState state) {
        return isActive(PlaybackStateCompat.fromPlaybackState(state));
    }

    public static boolean isPlayingOrSimilar(PlaybackStateCompat state) {
        switch (state.getState()) {
            case STATE_FAST_FORWARDING:
            case STATE_REWINDING:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_NEXT:
            case STATE_PLAYING:
                return true;
        }
        return false;
    }

    public static boolean isPlayingOrSimilar(PlaybackState state) {
        return isPlayingOrSimilar(PlaybackStateCompat.fromPlaybackState(state));
    }

    public static boolean isPlaying(PlaybackStateCompat state) {
        return state.getState() == STATE_PLAYING;
    }

    /*
     * Transport controls
     */

    public void play() {
        if (hasController()) {
            getTransportControls().play();
        }
    }

    public void playFromMediaId(String mediaId, Bundle extras) {
        if (hasController()) {
            getTransportControls().playFromMediaId(mediaId, extras);
        }
    }

    public void playFromSearch(String query, Bundle extras) {
        if (hasController()) {
            getTransportControls().playFromSearch(query, extras);
        }
    }

    public void skipToQueueItem(long id) {
        if (hasController()) {
            getTransportControls().skipToQueueItem(id);
        }
    }

    public void pause() {
        if (hasController()) {
            getTransportControls().pause();
        }
    }

    public void stop() {
        if (hasController()) {
            getTransportControls().stop();
        }
    }

    public void seekTo(long pos) {
        if (hasController()) {
            getTransportControls().seekTo(pos);
        }
    }

    public void fastForward() {
        if (hasController()) {
            getTransportControls().fastForward();
        }
    }

    public void skipToNext() {
        if (hasController()) {
            getTransportControls().skipToNext();
        }
    }

    public void rewind() {
        if (hasController()) {
            getTransportControls().rewind();
        }
    }

    public void skipToPrevious() {
        if (hasController()) {
            getTransportControls().skipToPrevious();
        }
    }

    public void setRating(Rating rating) {
        if (hasController()) {
            getTransportControls().setRating(rating);
        }
    }

    public void sendCustomAction(PlaybackState.CustomAction customAction, Bundle args) {
        if (hasController()) {
            getTransportControls().sendCustomAction(customAction, args);
        }
    }

    public void sendCustomAction(String action, Bundle args) {
        if (hasController()) {
            getTransportControls().sendCustomAction(action, args);
        }
    }

    /*
     * End transport controls
     */

    /*
     * Custom commands
     */

    public void playorPause() {
        sendCustomAction(CMD.TOGGLE_PLAYBACK, null);
    }

    public void cycleRepeateMode() {
        sendCustomAction(CMD.CYCLE_REPEAT, null);
    }

    public void shuffleQueue() {
        sendCustomAction(CMD.TOGGLE_SHUFFLE_MODE, null);
    }

    public void enqueueAll(List<Uri> queue, int where) {
        sendCustomAction(CMD.ENQUEUE, BundleHelper.builder().putInt(where).putList(queue).get());
    }

    public void enqueueAllNext(List<Uri> list) {
        enqueueAll(list, PlaybackConstants.ENQUEUE_NEXT);
    }

    public void addAllToQueue(List<Uri> list) {
        enqueueAll(list, PlaybackConstants.ENQUEUE_LAST);
    }

    public void playAll(List<Uri> list, int startpos) {
        sendCustomAction(CMD.PLAY_ALL, BundleHelper.builder().putList(list).putInt(startpos).get());
    }

    public void shuffleAll(List<Uri> list) {
        playAll(list, -1);
    }

    public void enqueueTracksFrom(Uri uri, int where, String sortorder) {
        sendCustomAction(CMD.ENQUEUE_TRACKS_FROM, BundleHelper.builder()
                .putUri(uri).putInt(where).putString(sortorder).get());
    }

    public void enqueueTracksNextFrom(Uri uri, String sortorder) {
        enqueueTracksFrom(uri, PlaybackConstants.ENQUEUE_NEXT, sortorder);
    }

    public void addTracksToQueueFrom(Uri uri, String sortorder) {
        enqueueTracksFrom(uri, PlaybackConstants.ENQUEUE_LAST, sortorder);
    }

    public void playTracksFrom(Uri uri, int startpos, String sortorder) {
        sendCustomAction(CMD.PLAY_TRACKS_FROM, BundleHelper.builder()
                .putUri(uri).putInt(startpos).putString(sortorder).get());
    }

    public void shuffleTracksFrom(Uri uri) {
        playTracksFrom(uri, -1, null);
    }

    public void removeQueueItem(Uri uri) {
        sendCustomAction(CMD.REMOVE_QUEUE_ITEM, BundleHelper.builder().putUri(uri).get());
    }

    public void removeQueueItemAt(int pos) {
        sendCustomAction(CMD.REMOVE_QUEUE_ITEM_AT, BundleHelper.builder().putInt(pos).get());
    }

    public void clearQueue() {
        sendCustomAction(CMD.CLEAR_QUEUE, null);
    }

    public void moveQueueItemTo(Uri uri, int newPos) {
        sendCustomAction(CMD.MOVE_QUEUE_ITEM_TO, BundleHelper.builder().putUri(uri).putInt(newPos).get());
    }

    public void moveQueueItem(int from, int to) {
        sendCustomAction(CMD.MOVE_QUEUE_ITEM, BundleHelper.builder().putInt(from).putInt2(to).get());
    }

    public void moveQueueItemToNext(int pos) {
        sendCustomAction(CMD.MOVE_QUEUE_ITEM_TO_NEXT, BundleHelper.builder().putInt(pos).get());
    }

    /*
     * End custom commands
     */

    /*
     * Misc
     */

    public int getAudioSessionId() {
        if (hasController()) {
            try {
                return mPlaybackService.getAudioSessionId();
            } catch (RemoteException e) {
            }
        }
        return AudioEffect.ERROR_BAD_VALUE;
    }

    /*
     * Subscriptions
     */

    final BehaviorSubject<PlaybackStateCompat> mPlayStateSubject = BehaviorSubject.create();

    public Subscription subscribePlayStateChanges(Action1<PlaybackStateCompat> onNext) {
        return mPlayStateSubject.asObservable().subscribe(onNext);
    }

    final BehaviorSubject<MediaMetadataCompat> mMetaSubject = BehaviorSubject.create();

    public Subscription subscribeMetaChanges(Action1<MediaMetadataCompat> onNext) {
        return mMetaSubject.asObservable().subscribe(onNext);
    }

    final BehaviorSubject<List<MediaSessionCompat.QueueItem>> mQueueSubject = BehaviorSubject.create();

    public Subscription subscribeQueueChanges(Action1<List<MediaSessionCompat.QueueItem>> onNext) {
        return mQueueSubject.asObservable().subscribe(onNext);
    }

    /*
     * end subscriptions
     */

    final MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onSessionDestroyed() {
            onDisconnect();
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            mPlayStateSubject.onNext(PlaybackStateCompat.fromPlaybackState(state));
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mMetaSubject.onNext(MediaMetadataCompat.fromMediaMetadata(metadata));
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            List<MediaSessionCompat.QueueItem> list = new ArrayList<>(queue.size());
            for (MediaSession.QueueItem item : queue) {
                MediaSessionCompat.QueueItem qi = MediaSessionCompat.QueueItem.obtain(item);
                list.add(qi);
            }
            mQueueSubject.onNext(list);
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            super.onQueueTitleChanged(title);
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            super.onExtrasChanged(extras);
        }

        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
            super.onAudioInfoChanged(info);
        }
    };

    boolean hasController() {
        if (mMediaController == null) {
            connect();
            return false;
        } else {
            return true;
        }
    }

    MediaController.TransportControls getTransportControls() {
        if (!hasController()) {
            throw new IllegalStateException("called getTransportControls without checking hasController");
        }
        return mTransportControls;
    }

    public void connect() {
        if (mMediaBrowser.isConnected() || mWaitingForService) {
            return;
        }
        mWaitingForService = true;
        mMediaBrowser.connect();
    }

    public void disconnect() {
        mMediaBrowser.disconnect();
        onDisconnect();
    }

    void onDisconnect() {
        mPlaybackService = null;
        mMediaController = null;
        mTransportControls = null;
        mWaitingForService = false;
    }

    final MediaBrowser.ConnectionCallback mConnectionCallback = new MediaBrowser.ConnectionCallback() {
        @Override
        public void onConnected() {
            mMediaController = new MediaController(mAppContext, mMediaBrowser.getSessionToken());
            mMediaController.registerCallback(mCallback, mCallbackHandler);
            mTransportControls = mMediaController.getTransportControls();
            mWaitingForService = false;
            final PlaybackState state = mMediaController.getPlaybackState();
            if (state != null) {
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onPlaybackStateChanged(state);
                    }
                });
            }
            final MediaMetadata meta = mMediaController.getMetadata();
            if (meta != null) {
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onMetadataChanged(meta);
                    }
                });
            }
            final List<MediaSession.QueueItem> queue = mMediaController.getQueue();
            if (queue != null) {
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onQueueChanged(queue);
                    }
                });
            }
        }

        @Override
        public void onConnectionSuspended() {
            onDisconnect();
        }

        @Override
        public void onConnectionFailed() {
            mWaitingForService = false;
            Timber.e(new IllegalStateException("Shouldn't be here"), "onConnectionFailed()");
        }
    };

}
