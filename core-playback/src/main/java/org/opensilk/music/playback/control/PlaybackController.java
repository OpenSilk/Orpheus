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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.PlaybackConstants.ACTION;
import org.opensilk.music.playback.PlaybackConstants.CMD;
import org.opensilk.music.playback.service.PlaybackServiceK;
import org.opensilk.music.playback.service.PlaybackServiceL;
import org.opensilk.music.playback.session.IMediaControllerProxy;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

/**
 * Created by drew on 5/6/15.
 */
@Singleton
public class PlaybackController {

    private final Context mAppContext;
    private final Handler mCallbackHandler = new Handler(Looper.getMainLooper());
    private final IPlaybackController mImpl;

    int mForegroundActivities = 0;

    @Inject
    public PlaybackController(@ForApplication Context mAppContext) {
        this.mAppContext = new ContextWrapper(mAppContext);
        if (VersionUtils.hasLollipop()) {
            mImpl = new PlaybackControllerL(mAppContext, this);
        } else {
            mImpl = new PlaybackControllerK(mAppContext, this);
        }
    }

    public void notifyForegroundStateChanged(boolean inForeground) {
        int old = mForegroundActivities;
        if (inForeground) {
            mForegroundActivities++;
        } else {
            mForegroundActivities--;
        }

        if (old == 0 || mForegroundActivities == 0) {
            final Intent intent;
            if (VersionUtils.hasLollipop()) {
                intent = new Intent(mAppContext, PlaybackServiceL.class);
            } else {
                intent = new Intent(mAppContext, PlaybackServiceK.class);
            }
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

    public void setRating(RatingCompat rating) {
        if (hasController()) {
            getTransportControls().setRating(rating);
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
        sendCustomAction(ACTION.TOGGLE_PLAYBACK, null);
    }

    public void cycleRepeateMode() {
        sendCustomAction(ACTION.CYCLE_REPEAT, null);
    }

    public void shuffleQueue() {
        sendCustomAction(ACTION.TOGGLE_SHUFFLE_MODE, null);
    }

    public void enqueueAll(List<Uri> queue, int where) {
        sendCustomAction(ACTION.ENQUEUE, BundleHelper.b().putList(clampList(queue, 0)).putInt(where).get());
    }

    public void enqueueAllNext(List<Uri> list) {
        enqueueAll(list, PlaybackConstants.ENQUEUE_NEXT);
    }

    public void enqueueAllEnd(List<Uri> list) {
        enqueueAll(list, PlaybackConstants.ENQUEUE_LAST);
    }

    public void playAll(List<Uri> list, int startpos) {
        sendCustomAction(ACTION.PLAY_ALL, BundleHelper.b().putList(clampList(list, startpos)).putInt(startpos).get());
    }

    public void removeQueueItem(Uri uri) {
        sendCustomAction(ACTION.REMOVE_QUEUE_ITEM, BundleHelper.b().putUri(uri).get());
    }

    public void removeQueueItemAt(int pos) {
        sendCustomAction(ACTION.REMOVE_QUEUE_ITEM_AT, BundleHelper.b().putInt(pos).get());
    }

    public void clearQueue() {
        sendCustomAction(ACTION.CLEAR_QUEUE, null);
    }

    public void moveQueueItemTo(Uri uri, int newPos) {
        sendCustomAction(ACTION.MOVE_QUEUE_ITEM_TO, BundleHelper.b().putUri(uri).putInt(newPos).get());
    }

    public void moveQueueItem(int from, int to) {
        sendCustomAction(ACTION.MOVE_QUEUE_ITEM, BundleHelper.b().putInt(from).putInt2(to).get());
    }

    public void moveQueueItemToNext(int pos) {
        sendCustomAction(ACTION.MOVE_QUEUE_ITEM_TO_NEXT, BundleHelper.b().putInt(pos).get());
    }

    public void switchToNewRenderer(@Nullable ComponentName componentName) {
        if (hasController()) {
            mImpl.getMediaController().sendCommand(CMD.SWITCH_TO_NEW_RENDERER,
                    BundleHelper.b().putParcleable(componentName).get(), null);
        }
    }

    public void getCurrentRenderer(final Action1<ComponentName> onNext) {
        if (hasController()) {
            mImpl.getMediaController().sendCommand(CMD.GET_CURRENT_RENDERER, null,
                    new ResultReceiver(mCallbackHandler) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            onNext.call(BundleHelper.<ComponentName>getParcelable(resultData));
                        }
                    });
        }
    }

    /*
     * End custom commands
     */

    /*
     * Misc
     */

    //exposed for testing
    static final int maxListsize = 500;
    /*package*/ static List<Uri> clampList(List<Uri> list, int startpos) {
        if (list.size() > maxListsize) {
            //XXX flood prevention
            int start, end;
            if ((startpos + maxListsize / 2) > list.size()) {
                //start pos is too close to end of list
                start = max(0, list.size() - maxListsize);
                end = list.size();
            } else if ((startpos - maxListsize / 2) < 0) {
                //start pos is too close to start of list
                start = 0;
                end = min(list.size(), maxListsize);
            } else {
                //items surrounding are within list bounds (we clamp anyway to be safe)
                start = max(0, startpos - maxListsize / 2);
                end = min(list.size(), startpos + maxListsize / 2);
            }
            return list.subList(start, end);
        }
        return list;
    }

    private static int max(int i1, int i2) {
        return i1 > i2 ? i1 : i2;
    }

    private static int min(int i1, int i2) {
        return i1 < i2 ? i1 : i2;
    }

    private void fetchRepeatMode() {
        if (hasController()) {
            mImpl.getMediaController().sendCommand(CMD.REQUEST_REPEATMODE_UPDATE, null, new ResultReceiver(mCallbackHandler) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    mRepeatModeSubject.onNext(BundleHelper.getInt(resultData));
                }
            });
        }
    }

    private void fetchShuffleMode() {
        if (hasController()) {
            mImpl.getMediaController().sendCommand(CMD.REQUEST_SHUFFLEMODE_UPDATE, null, new ResultReceiver(mCallbackHandler) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    mShuffleModeSubject.onNext(BundleHelper.getInt(resultData));
                }
            });
        }
    }

    private void fetchAudioSessionId() {
        if (hasController()) {
            mImpl.getMediaController().sendCommand(CMD.REQUEST_AUDIOSESSION_ID, null, new ResultReceiver(mCallbackHandler) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    mAudioSessionIdSubject.onNext(BundleHelper.getInt(resultData));
                }
            });
        }
    }

    /*
     * End misc
     */

    /*
     * Subscriptions
     */

    private final BehaviorSubject<PlaybackStateCompat> mPlayStateSubject = BehaviorSubject.create();

    public Subscription subscribePlayStateChanges(Action1<PlaybackStateCompat> onNext) {
        return mPlayStateSubject.subscribe(onNext);
    }

    private final BehaviorSubject<MediaMetadataCompat> mMetaSubject = BehaviorSubject.create();

    public Subscription subscribeMetaChanges(Action1<MediaMetadataCompat> onNext) {
        return mMetaSubject.subscribe(onNext);
    }

    private final BehaviorSubject<List<MediaSessionCompat.QueueItem>> mQueueSubject = BehaviorSubject.create();

    public Subscription subscribeQueueChanges(Action1<List<MediaSessionCompat.QueueItem>> onNext) {
        return mQueueSubject.subscribe(onNext);
    }

    private final BehaviorSubject<Integer> mAudioSessionIdSubject = BehaviorSubject.create();

    public Subscription subscribeAudioSessionIdChanges(Action1<Integer> onNext) {
        return mAudioSessionIdSubject.subscribe(onNext);
    }

    private final BehaviorSubject<Integer> mRepeatModeSubject = BehaviorSubject.create();

    public Subscription subscribeRepeatModeChanges(Action1<Integer> onNext) {
        return mRepeatModeSubject.subscribe(onNext);
    }

    private final BehaviorSubject<Integer> mShuffleModeSubject = BehaviorSubject.create();

    public Subscription subscribeShuffleModeChanges(Action1<Integer> onNext) {
        return mShuffleModeSubject.subscribe(onNext);
    }

    /*
     * end subscriptions
     */

    public void connect() {
        mImpl.connect();
    }

    public void disconnect() {
        mImpl.disconnect();
    }

    /*
     *
     */

    private boolean hasController() {
        return mImpl.isConnected();
    }

    private IMediaControllerProxy.TransportControlsProxy getTransportControls() {
        return mImpl.getTransportControls();
    }

    /*
     * Hooks for impl
     */

    void onConnected() {
        mImpl.getMediaController().registerCallback(mCallback, mCallbackHandler);
        final PlaybackStateCompat state = mImpl.getMediaController().getPlaybackState();
        if (state != null) {
            mPlayStateSubject.onNext(state);
        }
        final MediaMetadataCompat meta = mImpl.getMediaController().getMetadata();
        if (meta != null) {
            mMetaSubject.onNext(meta);
        }
        final List<MediaSessionCompat.QueueItem> queue = mImpl.getMediaController().getQueue();
        mQueueSubject.onNext(queue != null ? queue : Collections.<MediaSessionCompat.QueueItem>emptyList());
        fetchRepeatMode();
        fetchShuffleMode();
        fetchAudioSessionId();
    }

    final IMediaControllerProxy.Callback mCallback = new IMediaControllerProxy.Callback() {
        @Override
        public void onSessionDestroyed() {
            mImpl.disconnect();
        }

        @Override
        @DebugLog
        public void onSessionEvent(@NonNull String event, Bundle extras) {
            switch (event) {
                case PlaybackConstants.EVENT.NEW_AUDIO_SESSION_ID:{
                    if (VersionUtils.hasMarshmallow()) {
                        mAudioSessionIdSubject.onNext(BundleHelper.getInt(extras));
                    } else{
                        //work around platform bug (extras not delivered)
                        fetchAudioSessionId();
                    }
                    break;
                }
                case PlaybackConstants.EVENT.REPEAT_CHANGED: {
                    if (VersionUtils.hasMarshmallow()) {
                        mRepeatModeSubject.onNext(BundleHelper.getInt(extras));
                    } else {
                        //work around platform bug (extras not delivered)
                        fetchRepeatMode();
                    }
                    break;
                }
                case PlaybackConstants.EVENT.QUEUE_SHUFFLED: {
                    if (VersionUtils.hasMarshmallow()) {
                        mShuffleModeSubject.onNext(BundleHelper.getInt(extras));
                    } else {
                        //work around platform bug (extras not delivered)
                        fetchShuffleMode();
                    }
                    break;
                }
            }
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            mPlayStateSubject.onNext(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mMetaSubject.onNext(metadata);
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            mQueueSubject.onNext(queue != null ? queue : Collections.<MediaSessionCompat.QueueItem>emptyList());
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
        }

        @Override
        public void onAudioInfoChanged(Object info) {
        }
    };
}
