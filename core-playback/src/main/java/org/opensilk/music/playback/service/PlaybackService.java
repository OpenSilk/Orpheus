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

package org.opensilk.music.playback.service;

import android.app.Service;
import android.content.Intent;
import android.media.Rating;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.*;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.AlarmManagerHelper;
import org.opensilk.music.playback.AudioManagerHelper;
import org.opensilk.music.playback.BundleHelper;
import org.opensilk.music.playback.NotificationHelper;

import javax.inject.Inject;

import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.PlaybackConstants.CMD;
import org.opensilk.music.playback.PlaybackQueue;
import org.opensilk.music.playback.PlaybackStatus;
import org.opensilk.music.playback.mediaplayer.MultiPlayer;
import org.opensilk.music.playback.player.IPlayer;
import org.opensilk.music.playback.player.PlayerCallback;
import org.opensilk.music.playback.player.PlayerEvent;
import org.opensilk.music.playback.player.PlayerStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by drew on 5/6/15.
 */
@SuppressWarnings("NewApi")
public class PlaybackService extends Service {
    public static final String NAME = PlaybackService.class.getName();

    final PlaybackServiceBinder mBinder = new PlaybackServiceBinder(this);

    @Inject NotificationHelper mNotificationHelper;
    @Inject AlarmManagerHelper mAlarmManagerHelper;
    @Inject AudioManagerHelper mAudioManagerHelper;
    @Inject PlaybackQueue mQueue;
    @Inject HandlerThread mHandlerThread;
    @Inject MediaSession mMediaSession;
    @Inject PlaybackStatus mPlaybackStatus;

    private Handler mHandler;
    private IPlayer mPlayer;

    @Override
    public void onCreate() {
        super.onCreate();

        PlaybackServiceComponent component = DaggerService.getDaggerComponent(getApplicationContext());
        component.inject(this);

        //fire up thread and init handler
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        //tell everyone about ourselves
        mNotificationHelper.setService(this);
        mAudioManagerHelper.setChangeListener(mAudioFocusChangeListener, mHandler);
        mQueue.setListener(mQueueChangeListener, mHandler);
        mMediaSession.setCallback(mMediaSessionCallback, mHandler);

        mPlayer = new MultiPlayer(this, mAudioManagerHelper.getAudioSessionId());
        mPlayer.setCallback(mPlayerCallback, mHandler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mMediaSession.release();

        mNotificationHelper.killNotification();
        mNotificationHelper.setService(null);

        mAlarmManagerHelper.cancelDelayedShutdown();

        mAudioManagerHelper.abandonFocus();

        mQueue.save(); //Todo async

        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.getLooper().quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mAlarmManagerHelper.cancelDelayedShutdown();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {

            }
        }
        return START_STICKY;
    }

    final MediaSession.Callback mMediaSessionCallback = new MediaSession.Callback() {
        @Override
        public void onCommand(String command, Bundle args, ResultReceiver cb) {
            super.onCommand(command, args, cb);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            return super.onMediaButtonEvent(mediaButtonIntent);
        }

        @Override
        @DebugLog
        public void onPlay() {
            if (mPlaybackStatus.isPlayerReady()) {
                if (mPlaybackStatus.isSupposedToBePlaying()) {
                    Timber.e("onPlay called while isSupposedToBePlaying");
                }
                if (mAudioManagerHelper.requestFocus()) {
                    Timber.i("Audio focus granted. Starting playback");
                    mPlaybackStatus.setIsSupposedToBePlaying(true);
                    mPlayer.play();
                    //TODO update playstate
                }
                //update always
                mAlarmManagerHelper.cancelDelayedShutdown();
                mNotificationHelper.buildNotification(mPlaybackStatus.getCurrentTrack(),
                        mPlaybackStatus.isSupposedToBePlaying(), mMediaSession.getSessionToken());
            } else if (mPlaybackStatus.isPlayerLoading()) {
                Timber.d("Player is loading. Ignoring play request");
            } else if (mQueue.notEmpty()) {
                Timber.e("In a bad state, nobody loaded the current queue item");
            } else {
                Timber.i("Queue is empty");
                //TODO start autoshuffle
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            //TODO make sure its a track uri
            mQueue.addNext(Collections.singletonList(Uri.parse(mediaId)));
            super.onPlayFromMediaId(mediaId, extras);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
        }

        @Override
        public void onSkipToQueueItem(long id) {
            mQueue.goToItem((int)id);
        }

        @Override
        @DebugLog
        public void onPause() {
            if (!mPlaybackStatus.isSupposedToBePlaying()) {
                Timber.e("onStop called when !isSupposedToBePlaying");
            }
            mPlaybackStatus.setIsSupposedToBePlaying(false);
            mAudioManagerHelper.abandonFocus();
            mPlayer.pause();
            //TODO update playstate
        }

        @Override
        @DebugLog
        public void onSkipToNext() {
            //Will callback to WENT_TO_NEXT
            mPlayer.skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            mQueue.goToItem(mQueue.getPrevious());
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
        }

        @Override
        public void onRewind() {
            super.onRewind();
        }

        @Override
        @DebugLog
        public void onStop() {
            if (!mPlaybackStatus.isSupposedToBePlaying()) {
                Timber.e("onStop called when !isSupposedToBePlaying");
            }
            mPlaybackStatus.setIsSupposedToBePlaying(false);
            mAudioManagerHelper.abandonFocus();
            mPlayer.stop();
            mAlarmManagerHelper.scheduleDelayedShutdown();
            //TODO update playstate
        }

        @Override
        public void onSeekTo(long pos) {
            mPlayer.seekTo(pos);
        }

        @Override
        public void onSetRating(Rating rating) {
            super.onSetRating(rating);
        }

        @Override
        @DebugLog
        public void onCustomAction(String action, Bundle extras) {
            if (action == null) return;
            switch (action) {
                case CMD.CYCLE_REPEAT: {
                    //TODO
                    break;
                }
                case CMD.ENQUEUE: {
                    int where = BundleHelper.getInt(extras);
                    List<Uri> list = BundleHelper.getList(extras);
                    if (where == PlaybackConstants.ENQUEUE_LAST) {
                        mQueue.addEnd(list);
                    } else if (where == PlaybackConstants.ENQUEUE_NEXT) {
                        mQueue.addNext(list);
                    }
                    break;
                }
                case CMD.ENQUEUE_TRACKS_FROM: {
                    Uri uri = BundleHelper.getUri(extras);
                    String sort = BundleHelper.getString(extras);
                    int where = BundleHelper.getInt(extras);
                    List<Uri> list = getTracks(uri, sort);
                    if (where == PlaybackConstants.ENQUEUE_LAST) {
                        mQueue.addEnd(list);
                    } else if (where == PlaybackConstants.ENQUEUE_NEXT) {
                        mQueue.addNext(list);
                    }
                    break;
                }
                case CMD.PLAY_ALL: {
                    List<Uri> list = BundleHelper.getList(extras);
                    int startpos = BundleHelper.getInt(extras);
                    mQueue.replace(list, startpos);
                    mPlaybackStatus.setPlayWhenReady(true);
                    break;
                }
                case CMD.PLAY_TRACKS_FROM: {
                    Uri uri = BundleHelper.getUri(extras);
                    String sort = BundleHelper.getString(extras);
                    int startpos = BundleHelper.getInt(extras);
                    List<Uri> list = getTracks(uri, sort);
                    mQueue.replace(list, startpos);
                    mPlaybackStatus.setPlayWhenReady(true);
                    break;
                }
                case CMD.SHUFFLE_QUEUE: {
                    mQueue.shuffle();
                    break;
                }
                case CMD.REMOVE_QUEUE_ITEM: {
                    Uri uri = BundleHelper.getUri(extras);
                    mQueue.remove(uri);
                    break;
                }
                case CMD.REMOVE_QUEUE_ITEM_AT: {
                    int pos = BundleHelper.getInt(extras);
                    mQueue.remove(pos);
                    break;
                }
                case CMD.CLEAR_QUEUE: {
                    mQueue.clear();
                    break;
                }
                case CMD.MOVE_QUEUE_ITEM_TO: {
                    Uri uri = BundleHelper.getUri(extras);
                    int pos = BundleHelper.getInt(extras);
                    mQueue.moveItem(uri, pos);
                    break;
                }
            }
        }
    };

    final AudioManagerHelper.OnFocusChangedListener mAudioFocusChangeListener =
            new AudioManagerHelper.OnFocusChangedListener() {
                @Override
                public void onFocusLost() {
                    mMediaSessionCallback.onPause();
                }

                @Override
                public void onFocusLostTransient() {
                    mPlaybackStatus.setPausedByTransientLossOfFocus(true);
                    mMediaSessionCallback.onPause();
                }

                @Override
                public void onFocusLostDuck() {
                    if (mPlaybackStatus.isSupposedToBePlaying()) {
                        mPlayer.duck();
                    }
                }

                @Override
                public void onFocusGain() {
                    if (mPlaybackStatus.isPausedByTransientLossOfFocus()) {
                        mPlaybackStatus.setPausedByTransientLossOfFocus(false);
                        mMediaSessionCallback.onPlay();
                    }
                }
            };

    final PlaybackQueue.QueueChangeListener mQueueChangeListener =
            new PlaybackQueue.QueueChangeListener() {
                @Override
                @DebugLog
                public void onCurrentPosChanged() {
                    if (mQueue.notEmpty()) {
                        if (mPlaybackStatus.getCurrentQueuePos() == mQueue.getCurrentPos()) {
                            Timber.w("Our position matches queue position. what should i do?");
                        }
                        Uri uri = mQueue.getCurrentUri();
                        Track track = getTrack(uri);
                        if (track == null) {
                            //will callback in here
                            mPlaybackStatus.setCurrentQueuePos(-2);
                            mQueue.remove(mQueue.getCurrentPos());
                        } else {
                            mPlaybackStatus.setCurrentTrack(track);
                            mPlaybackStatus.setCurrentQueuePos(mQueue.getCurrentPos());
                            if (mPlaybackStatus.isSupposedToBePlaying()) {
                                mPlaybackStatus.setPlayWhenReady(true);
                            }
                            mPlayer.setDataSource(track.dataUri);
                            //TODO update notificaiton
                        }
                    } else if (mPlaybackStatus.isSupposedToBePlaying()) {
                        Timber.i("Queue is gone. stopping playback");
                        mMediaSessionCallback.onStop();
                    }
                }

                @Override
                @DebugLog
                public void onQueueChanged() {
                    if (mQueue.notEmpty()) {
                        Uri uri = mQueue.getNextUri();
                        Track track = getTrack(uri);
                        if (track == null) {
                            //Will callback in here
                            mQueue.remove(mQueue.getNextPos());
                        } else if (!track.equals(mPlaybackStatus.getNextTrack())) {
                            mPlaybackStatus.setNextTrack(track);
                            mPlayer.setNextDataSource(track.dataUri);
                        } //else nothing. we should be good
                    } else if (mPlaybackStatus.isSupposedToBePlaying()) {
                        Timber.i("Queue is gone. stopping playback");
                        mMediaSessionCallback.onStop();
                    }

                }

                @Override
                @DebugLog
                public void wentToNext() {
                    Uri uri = mQueue.getNextUri();
                    Track track = getTrack(uri);
                    if (track == null) {
                        //will callback into onQueueChanged
                        mQueue.remove(mQueue.getNextPos());
                    } else {
                        mPlaybackStatus.setNextTrack(track);
                        mPlayer.setNextDataSource(track.dataUri);
                    }
                }
            };

    final PlayerCallback mPlayerCallback = new PlayerCallback() {
        @Override
        @DebugLog
        public void onPlayerEvent(PlayerEvent event) {
            switch (event.getEvent()) {
                case PlayerEvent.OPEN_NEXT_FAILED: {
                    //will call into onQueueChanged
                    mQueue.remove(mQueue.getNextPos());
                    break;
                }
                case PlayerEvent.WENT_TO_NEXT: {
                    //will call into wentToNext
                    mQueue.wentToNext();
                    break;
                }
                case PlayerEvent.DURATION: {
                    if (mPlaybackStatus.getCurrentDuration() != event.getLongExtra()) {
                        mPlaybackStatus.setCurrentDuration(event.getLongExtra());
                        //TODO notify
                    }
                    break;
                }
                case PlayerEvent.POSITION: {
                    if (mPlaybackStatus.getCurrentSeekPos() != event.getLongExtra()) {
                        mPlaybackStatus.setCurrentSeekPos(event.getLongExtra());
                        //TODO notify
                    }
                    break;
                }
            }
        }

        @Override
        @DebugLog
        public void onPlayerStatus(PlayerStatus status) {
            mPlaybackStatus.setPlayerState(status.getState());
            switch (status.getState()) {
                case PlayerStatus.NONE: {
                    break;
                }
                case PlayerStatus.LOADING: {
                    Timber.i("Player moved to loading");
                    //TODO notify
                    break;
                }
                case PlayerStatus.READY: {
                    if (mPlaybackStatus.shouldPlayWhenReady()) {
                        mPlaybackStatus.setPlayWhenReady(false);
                        mMediaSessionCallback.onPlay();
                    }
                    //Will load the next track
                    mQueueChangeListener.onQueueChanged();
                    break;
                }
                case PlayerStatus.PLAYING: {
                    if (!mPlaybackStatus.isSupposedToBePlaying()) {
                        Timber.e("Player started playing unexpectedly");
                        mMediaSessionCallback.onPause();
                    }
                    break;
                }
                case PlayerStatus.PAUSED: {
                    if (mPlaybackStatus.isSupposedToBePlaying()) {
                        Timber.e("Player paused unexpectedly");
                        //TODO
                    }
                    break;
                }
                case PlayerStatus.STOPPED: {
                    if (mPlaybackStatus.isSupposedToBePlaying()) {
                        Timber.e("Player stopped unexpectedly");
                        //TODO
                    }
                    break;
                }
                case PlayerStatus.ERROR: {
                    Timber.e("Player error %s", status.getErrorMsg());
                    //TODO
                    break;
                }
            }
        }
    };

    Track getTrack(Uri uri) {
        try {
            return new BundleableLoader(PlaybackService.this, uri, null)
                    .createObservable().flatMap(new Func1<List<Bundleable>, Observable<? extends Bundleable>>() {
                        @Override
                        public Observable<? extends Bundleable> call(List<Bundleable> bundleables) {
                            return Observable.from(bundleables);
                        }
                    }).cast(Track.class).toBlocking().first();
        } catch (Exception e) {
            Timber.e(e, "getTrack");
            return null;
        }
    }

    List<Uri> getTracks(final Uri uri, String sortorder) {
        try {
            final String authority = uri.getAuthority();
            final String library = uri.getPathSegments().get(0);
            return new BundleableLoader(PlaybackService.this, uri, sortorder)
                    .createObservable().flatMap(new Func1<List<Bundleable>, Observable<Bundleable>>() {
                        @Override
                        public Observable<Bundleable> call(List<Bundleable> bundleables) {
                            return Observable.from(bundleables);
                        }
                    }).collect(new ArrayList<Uri>(), new Action2<ArrayList<Uri>, Bundleable>() {
                        @Override
                        public void call(ArrayList<Uri> uris, Bundleable bundleable) {
                            uris.add(LibraryUris.track(authority, library, bundleable.getIdentity()));
                        }
                    }).toBlocking().first();
        } catch (Exception e) {
            Timber.e(e, "getTracks");
            return Collections.emptyList();
        }
    }
}
