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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadata;
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
import org.opensilk.music.playback.LibraryHelper;
import org.opensilk.music.playback.MediaMetadataHelper;
import org.opensilk.music.playback.NavUtils;
import org.opensilk.music.playback.NotificationHelper;

import javax.inject.Inject;

import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.PlaybackConstants.CMD;
import org.opensilk.music.playback.PlaybackConstants.EVENT;
import org.opensilk.music.playback.PlaybackConstants.EXTRA;
import org.opensilk.music.playback.PlaybackQueue;
import org.opensilk.music.playback.PlaybackStateHelper;
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
    @Inject PlaybackStateHelper mPlaybackStateHelper;
    @Inject MediaMetadataHelper mMediaMetaHelper;
    @Inject LibraryHelper mLibraryHelper;

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
        mNotificationHelper.setService(this, mHandler);
        mAudioManagerHelper.setChangeListener(mAudioFocusChangeListener, mHandler);
        mQueue.setListener(mQueueChangeListener, mHandler);
        mMediaSession.setCallback(mMediaSessionCallback, mHandler);
        mMediaMetaHelper.setMediaSession(mMediaSession, mHandler);

        //init default player
        mPlayer = new MultiPlayer(this, mAudioManagerHelper.getAudioSessionId());
        mPlayer.setCallback(mPlayerCallback, mHandler);

        mHandler.post(mLoadQueueRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaSession.release();
        mNotificationHelper.killNotification();
        mAlarmManagerHelper.cancelDelayedShutdown();
        mAudioManagerHelper.abandonFocus();
        mQueue.save();
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

    private void updateMeta() {
        mMediaMetaHelper.updateMeta(mPlaybackStatus.getCurrentTrack());
        updateNotification();
    }

    private void updateNotification() {
        mNotificationHelper.buildNotification(mPlaybackStatus.getCurrentTrack(),
                mPlaybackStatus.isSupposedToBePlaying(), mMediaSession.getSessionToken());
    }

    private void updatePlaybackState() {
        mNotificationHelper.updatePlayState(mPlaybackStateHelper.isActive());
        mMediaSession.setPlaybackState(mPlaybackStateHelper.getState());
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
                    mPlaybackStatus.setIsSupposedToBePlaying(true);
                    mPlayer.play();
                    mPlaybackStateHelper.gotoPlaying();
                    updatePlaybackState();
                }
                //update always
                mAlarmManagerHelper.cancelDelayedShutdown();
                updateNotification();
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
                Timber.e("onPause called when !isSupposedToBePlaying");
            }
            mPlaybackStatus.setIsSupposedToBePlaying(false);
            mAudioManagerHelper.abandonFocus();
            mPlayer.pause();
            mPlaybackStateHelper.gotoPaused();
            updatePlaybackState();
        }

        @Override
        @DebugLog
        public void onSkipToNext() {
            //Will callback to WENT_TO_NEXT
            mPlayer.skipToNext();
            mPlaybackStateHelper.gotoSkippingNext();
            updatePlaybackState();
        }

        @Override
        public void onSkipToPrevious() {
            mQueue.goToItem(mQueue.getPrevious());
            mPlaybackStateHelper.gotoSkippingPrevious();
            updatePlaybackState();
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
            mPlayer.stop();
            mAudioManagerHelper.abandonFocus();
            mPlaybackStateHelper.gotoStopped();
            updatePlaybackState();
            mAlarmManagerHelper.scheduleDelayedShutdown();
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
                    List<Uri> list = mLibraryHelper.getTracks(uri, sort);
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
                    List<Uri> list = mLibraryHelper.getTracks(uri, sort);
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
                    mPlaybackStatus.setPausedByTransientLossOfFocus(false);
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
                        mHandler.removeCallbacks(mProgressCheckRunnable);
                        if (mPlaybackStatus.getCurrentQueuePos() == mQueue.getCurrentPos()) {
                            Timber.w("Current position matches queue");
                        }
                        Uri uri = mQueue.getCurrentUri();
                        Track track = mLibraryHelper.getTrack(uri);
                        if (track == null) {
                            //will callback in here
                            mPlaybackStatus.setCurrentQueuePos(-2);
                            mQueue.remove(mQueue.getCurrentPos());
                        } else {
                            if (track.equals(mPlaybackStatus.getCurrentTrack())) {
                                Timber.w("Current track matches queue");
                            }
                            mPlaybackStatus.setCurrentTrack(track);
                            mPlaybackStatus.setCurrentQueuePos(mQueue.getCurrentPos());
                            if (mPlaybackStatus.isSupposedToBePlaying()) {
                                mPlaybackStatus.setPlayWhenReady(true);
                            }
                            mPlaybackStatus.setIsSupposedToBePlaying(false);
                            mPlayer.setDataSource(track.dataUri);
                            updateMeta();
                        }
                    } else if (mPlaybackStatus.isSupposedToBePlaying()) {
                        stopAndResetState();
                    }
                }

                @Override
                @DebugLog
                public void onQueueChanged() {
                    if (mQueue.notEmpty()) {
                        setNextTrack();
                    } else if (mPlaybackStatus.isSupposedToBePlaying()) {
                        Timber.e("Got onQueueChanged with empty queue");
                        stopAndResetState();
                    }
                }

                @Override
                @DebugLog
                public void wentToNext() {
                    mPlaybackStatus.setNextTrackToCurrent();
                    mPlaybackStatus.setIsSupposedToBePlaying(true);
                    setNextTrack();
                    updateMeta();
                }

                private void setNextTrack() {
                    Uri uri = mQueue.getNextUri();
                    Track track = mLibraryHelper.getTrack(uri);
                    if (track == null) {
                        //will callback into onQueueChanged
                        mQueue.remove(mQueue.getNextPos());
                    } else if (!track.equals(mPlaybackStatus.getNextTrack())) {
                        mPlaybackStatus.setNextTrack(track);
                        mPlayer.setNextDataSource(track.dataUri);
                    } else {
                        Timber.i("Next track is still up to date");
                    }
                }

                private void stopAndResetState() {
                    Timber.i("Queue is gone. stopping playback");
                    mMediaSessionCallback.onStop();
                    mPlaybackStatus.reset();
                    mNotificationHelper.killNotification();
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
                        notifyProgress();
                    }
                    break;
                }
                case PlayerEvent.POSITION: {
                    if (mPlaybackStatus.getCurrentSeekPos() != event.getLongExtra()) {
                        mPlaybackStatus.setCurrentSeekPos(event.getLongExtra());
                        notifyProgress();
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
                    mPlaybackStateHelper.gotoBuffering();
                    updatePlaybackState();
                    break;
                }
                case PlayerStatus.READY: {
                    if (mPlaybackStatus.shouldPlayWhenReady()) {
                        mPlaybackStatus.setPlayWhenReady(false);
                        mMediaSessionCallback.onPlay();
                    }
                    //will kickoff progress subscription
                    mPlayer.getDuration();
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

        private void notifyProgress() {
            mPlaybackStateHelper.updatePosition(mPlaybackStatus.getCurrentSeekPos());
            mPlaybackStateHelper.updateDuration(mPlaybackStatus.getCurrentDuration());
            updatePlaybackState();
            mHandler.removeCallbacks(mProgressCheckRunnable);
            mHandler.postDelayed(mProgressCheckRunnable, 2000);
        }
    };

    final Runnable mLoadQueueRunnable = new Runnable() {
        @Override
        public void run() {
            mQueue.load();
            mQueueChangeListener.onCurrentPosChanged();
            mMediaSession.setActive(true);
        }
    };

    final Runnable mProgressCheckRunnable = new Runnable() {
        @Override
        public void run() {
            mPlayer.getPosition();
        }
    };

}
