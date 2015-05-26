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
import android.media.audiofx.AudioEffect;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.*;
import android.support.annotation.NonNull;
import android.view.KeyEvent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.artwork.service.ArtworkProviderHelper;
import org.opensilk.music.model.Track;
import org.opensilk.music.playback.AlarmManagerHelper;
import org.opensilk.music.playback.AudioManagerHelper;
import org.opensilk.music.playback.BundleHelper;
import org.opensilk.music.playback.LibraryHelper;
import org.opensilk.music.playback.MediaMetadataHelper;
import org.opensilk.music.playback.NotificationHelper;

import javax.inject.Inject;

import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.PlaybackConstants.CMD;
import org.opensilk.music.playback.PlaybackConstants.EVENT;
import org.opensilk.music.playback.PlaybackPreferences;
import org.opensilk.music.playback.PlaybackQueue;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.mediaplayer.MultiPlayer;
import org.opensilk.music.playback.player.IPlayer;
import org.opensilk.music.playback.player.IPlayerCallback;

import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

import static org.opensilk.music.playback.PlaybackConstants.*;

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
    @Inject PlaybackStateHelper mPlaybackStateHelper;
    @Inject MediaMetadataHelper mMediaMetaHelper;
    @Inject LibraryHelper mLibraryHelper;
    @Inject PlaybackPreferences mSettings;
    @Inject PowerManager.WakeLock mWakeLock;
    @Inject ArtworkProviderHelper mArtworkProviderHelper;

    int mAudioSessionId;
    Handler mHandler;
    IPlayer mPlayer;

    //currently playing track
    Track mCurrentTrack;
    Uri mCurrentUri;
    //next track to load
    Track mNextTrack;
    Uri mNextUri;
    //trou if we should start playing again when we regain audio focus
    boolean mPausedByTransientLossOfFocus;
    //true if we should start playing when loading finishes
    boolean mPlayWhenReady;
    //
    boolean mQueueReloaded;
    //
    boolean mAnyActivityInForeground;
    int mConnectedClients;

    @Override
    public void onCreate() {
        super.onCreate();

        PlaybackServiceComponent component = DaggerService.getDaggerComponent(getApplicationContext());
        component.inject(this);

        acquireWakeLock();

        //fire up thread and init handler
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        //tell everyone about ourselves
        mNotificationHelper.setService(this, mHandler);
        mAudioManagerHelper.setChangeListener(mAudioFocusChangeListener, mHandler);
        mQueue.setListener(mQueueChangeListener, mHandler);
        mMediaSession.setCallback(mMediaSessionCallback, mHandler);
        mMediaSession.setPlaybackState(mPlaybackStateHelper.getState());

        mMediaMetaHelper.setMediaSession(mMediaSession, mHandler);

        mAudioSessionId = mAudioManagerHelper.getAudioSessionId();
        //init default player
        mPlayer = new MultiPlayer(this, mAudioSessionId);
        mPlayer.setCallback(mPlayerCallback, mHandler);

        mHandler.post(mLoadQueueRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveState();

        mMediaSession.release();

        mNotificationHelper.killNotification();
        mAlarmManagerHelper.cancelDelayedShutdown();
        mAudioManagerHelper.abandonFocus();

        mPlayer.release();

        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.getLooper().quit();

        // Remove any sound effects
        final Intent audioEffectsIntent = new Intent(
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(audioEffectsIntent);

        releaseWakeLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mConnectedClients++;
        mAlarmManagerHelper.cancelDelayedShutdown();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mConnectedClients--;
        if (!mPlaybackStateHelper.isPlaying()
                && !mPlayWhenReady
                && !mPausedByTransientLossOfFocus) {
            mAlarmManagerHelper.scheduleDelayedShutdown();
            if (!mAnyActivityInForeground) {
                updateNotification();
            }
        }
        saveState();
        return false;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_COMPLETE) {
            mArtworkProviderHelper.evictL1();
            saveState();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            acquireWakeLock();

            String action = intent.getAction();

            if (SHUTDOWN.equals(action)) {
                mAlarmManagerHelper.onRecievedShutdownIntent();
                maybeStopService();
                return START_NOT_STICKY;
            }

            if (intent.hasExtra(NOW_IN_FOREGROUND)) {
                mAnyActivityInForeground = intent.getBooleanExtra(NOW_IN_FOREGROUND, false);
                updateNotification();
            }

            if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                mMediaSession.getController().dispatchMediaButtonEvent(
                        intent.<KeyEvent>getParcelableExtra(Intent.EXTRA_KEY_EVENT));
            } else {
                handleIntentCommand(intent);
            }

            if (intent.getBooleanExtra(FROM_MEDIA_BUTTON, false)) {
                MediaButtonIntentReceiver.completeWakefulIntent(intent);
            }
        }
        return START_STICKY;
    }

    void handleIntentCommand(@NonNull Intent intent) {
        final String action = intent.getAction();
        final String command = SERVICECMD.equals(action) ? intent.getStringExtra(CMDNAME) : null;
        Timber.v("handleIntentCommand: action = %s, command = %s", action, command);
        MediaController controller = mMediaSession.getController();
        MediaController.TransportControls controls = controller.getTransportControls();
        if (CMDNEXT.equals(command) || NEXT_ACTION.equals(action)) {
            controls.skipToNext();
        } else if (CMDPREVIOUS.equals(command) || PREVIOUS_ACTION.equals(action)) {
            controls.skipToPrevious();
        } else if (CMDTOGGLEPAUSE.equals(command) || TOGGLEPAUSE_ACTION.equals(action)) {
            controls.sendCustomAction(CMD.TOGGLE_PLAYBACK, null);
        } else if (CMDPAUSE.equals(command) || PAUSE_ACTION.equals(action)) {
            controls.pause();
        } else if (CMDPLAY.equals(command)) {
            controls.play();
        } else if (CMDSTOP.equals(command) || STOP_ACTION.equals(action)) {
            controls.stop();
        } else if (REPEAT_ACTION.equals(action)) {
            controls.sendCustomAction(CMD.CYCLE_REPEAT, null);
        } else if (SHUFFLE_ACTION.equals(action)) {
            controls.sendCustomAction(CMD.SHUFFLE_QUEUE, null);
        }
    }

    void maybeStopService() {
        if (mPlaybackStateHelper.isPlaying() || mPlayWhenReady || mPausedByTransientLossOfFocus) {
            return;
        }
        if (!mAnyActivityInForeground) {
            mNotificationHelper.killNotification();
        }
        if (mConnectedClients <= 0) {
            stopSelf();
        }
    }

    void updateMeta() {
        mMediaMetaHelper.updateMeta(mCurrentTrack, mCurrentUri);
        updateNotification();
        mMediaSession.setPlaybackState(mPlaybackStateHelper.getState());
    }

    void updateNotification() {
        //we show notification when playing even if activity is showing
        //this is for lockscreen so it doesnt disappear when whe turn on the screen
        if (!mAnyActivityInForeground || mPlaybackStateHelper.isPlaying()) {
            mNotificationHelper.buildNotification(mCurrentTrack,
                    mPlaybackStateHelper.shouldShowPauseButton(), mMediaSession.getSessionToken());
        } else {
            mNotificationHelper.killNotification();
        }
    }

    void updatePlaybackState() {
        mMediaSession.setPlaybackState(mPlaybackStateHelper.getState());
        mNotificationHelper.updatePlayState(mPlaybackStateHelper.isActive());
    }

    void saveState() {
        final PlaybackQueue.Snapshot qSnapshot = mQueue.snapshot();
        final long seekPos = mPlaybackStateHelper.getPosition();
        //Use async to avoid making new thread
        new AsyncTask<Object, Void, Void>() {
            @Override
            @DebugLog
            protected Void doInBackground(Object... params) {
                mSettings.saveQueue(qSnapshot.q);
                if (qSnapshot.pos != -1) {
                    mSettings.putInt(PlaybackPreferences.CURRENT_POS, qSnapshot.pos);
                }
                if (seekPos > 0) {
                    mSettings.putLong(PlaybackPreferences.SEEK_POS, seekPos);
                }
                return null;
            }
        }.execute();
    }

    void resetState() {
        mCurrentTrack = null;
        mCurrentUri = null;
        mNextTrack = null;
        mNextUri = null;
        mPausedByTransientLossOfFocus = false;
        mPlayWhenReady = false;

        mPlaybackStateHelper.updateDuration(-1);
        mPlaybackStateHelper.updatePosition(-1);
        mPlaybackStateHelper.gotoStopped();
    }

    void acquireWakeLock() {
        releaseWakeLock();
        mWakeLock.acquire(30000);
    }

    void releaseWakeLock() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public MediaSession getMediaSession() {
        return mMediaSession;
    }

    public int getAudioSessionId() {
        return mAudioSessionId;
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
            if (mPlaybackStateHelper.isPaused()) {
                if (mAudioManagerHelper.requestFocus()) {
                    mPlayer.play();
                    mPlaybackStateHelper.gotoPlaying();
                    updatePlaybackState();
                    mHandler.removeCallbacks(mProgressCheckRunnable);
                    mHandler.post(mProgressCheckRunnable);
                }
                //update always
                mAlarmManagerHelper.cancelDelayedShutdown();
                updateNotification();
            } else if (mPlaybackStateHelper.isLoading()) {
                Timber.d("Player is loading. Request ignored and play when loading set");
                mPlayWhenReady = true;
            } else if (mQueue.notEmpty()) {
                Timber.e("In a bad state, current queue item not loaded");
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
            if (mPlaybackStateHelper.isLoadingOrSkipping()) {
                Timber.w("Ignoring onSkipToQueueItem while loading/skipping");
                return;
            }
            int qid = (int) id;
            if (mQueue.getCurrentPos() != qid) {
                if (mQueue.getNextPos() == qid) {
                    onSkipToNext();
                } else {
                    onPause();
                    mPlayWhenReady = true;
                    mQueue.goToItem(qid);
                }
            }
        }

        @Override
        @DebugLog
        public void onPause() {
            if (mPlaybackStateHelper.isPlaying()) {
                Timber.i("Pausing playback");
                mHandler.removeCallbacks(mProgressCheckRunnable);
                mPlayer.pause();
                mPlaybackStateHelper.gotoPaused();
                updatePlaybackState();
                mAudioManagerHelper.abandonFocus();
                saveState();
            } else if (mPlaybackStateHelper.isLoading()) {
                Timber.w("Player is still loading... Setting playWhenReady to false");
                mPlayWhenReady = false;
            } else {
                Timber.w("Ignoring onPause");
            }
        }

        @Override
        @DebugLog
        public void onSkipToNext() {
            if (mPlaybackStateHelper.isLoadingOrSkipping()) {
                Timber.w("Ignoring skipToNext while skipping/loading");
                return;
            }
            mPlaybackStateHelper.gotoSkippingNext();
            updatePlaybackState();
            //Will callback to WENT_TO_NEXT
            mPlayer.skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            if (mPlaybackStateHelper.isLoadingOrSkipping()) {
                Timber.w("Ignoring skipToPrevious while skipping/loading");
                return;
            }
            if (mPlaybackStateHelper.getPosition() > REWIND_INSTEAD_PREVIOUS_THRESHOLD) {
                onSeekTo(0);
            } else {
                mPlaybackStateHelper.gotoSkippingPrevious();
                updatePlaybackState();
                //will callback to onCurrentPosChanged
                mQueue.goToItem(mQueue.getPrevious());
            }
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
            if (mPlaybackStateHelper.isPlaying()) {
                mPlayer.stop();
            }
            mAudioManagerHelper.abandonFocus();
            resetState();
            updatePlaybackState();
            //Reload the current item
            mQueueChangeListener.onCurrentPosChanged();
            mAlarmManagerHelper.scheduleDelayedShutdown();
            saveState();
        }

        @Override
        public void onSeekTo(long pos) {
            mHandler.removeCallbacks(mProgressCheckRunnable);
            if (mPlayer.seekTo(pos)) {
                mPlaybackStateHelper.updatePosition(pos);
                updatePlaybackState();
                if (mPlaybackStateHelper.isPlaying()) {
                    mHandler.postDelayed(mProgressCheckRunnable, 2000);
                }
            } //else TODO
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
                    mQueue.toggleRepeat();
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
                    mPlaybackStateHelper.gotoConnecting();
                    updatePlaybackState();
                    List<Uri> list = BundleHelper.getList(extras);
                    int startpos = BundleHelper.getInt(extras);
                    mQueue.replace(list, startpos);
                    mPlayWhenReady = true;
                    break;
                }
                case CMD.PLAY_TRACKS_FROM: {
                    mPlaybackStateHelper.gotoConnecting();
                    updatePlaybackState();
                    Uri uri = BundleHelper.getUri(extras);
                    String sort = BundleHelper.getString(extras);
                    int startpos = BundleHelper.getInt(extras);
                    List<Uri> list = mLibraryHelper.getTracks(uri, sort);
                    mQueue.replace(list, startpos);
                    mPlayWhenReady = true;
                    break;
                }
                case CMD.SHUFFLE_QUEUE: {
                    mQueue.shuffle();
                    mMediaSession.sendSessionEvent(EVENT.QUEUE_SHUFFLED, null);
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
                case CMD.MOVE_QUEUE_ITEM: {
                    int from = BundleHelper.getInt(extras);
                    int to = BundleHelper.getInt2(extras);
                    mQueue.moveItem(from, to);
                    break;
                }
                case CMD.MOVE_QUEUE_ITEM_TO_NEXT: {
                    int pos = BundleHelper.getInt(extras);
                    mQueue.moveItem(pos, mQueue.getNextPos());
                    break;
                }
                case CMD.TOGGLE_PLAYBACK: {
                    if (mPlaybackStateHelper.isPlaying()) {
                        onPause();
                    } else if (mPlaybackStateHelper.isPaused()) {
                        onPlay();
                    } else if (mPlaybackStateHelper.isLoading()) {
                        mPlayWhenReady = !mPlayWhenReady;
                    } else if (mPlaybackStateHelper.isStopped()) {
                        Timber.w("Shouldnt be here");
                    }
                    break;
                }
            }
        }
    };

    final AudioManagerHelper.OnFocusChangedListener mAudioFocusChangeListener =
            new AudioManagerHelper.OnFocusChangedListener() {
                @Override
                public void onFocusLost() {
                    mPausedByTransientLossOfFocus = false;
                    mMediaSessionCallback.onPause();
                }

                @Override
                public void onFocusLostTransient() {
                    mPausedByTransientLossOfFocus = true;
                    mMediaSessionCallback.onPause();
                }

                @Override
                public void onFocusLostDuck() {
                    if (mPlaybackStateHelper.isPlaying()) {
                        mPlayer.duck(true);
                    }
                }

                @Override
                public void onFocusGain() {
                    if (mPausedByTransientLossOfFocus) {
                        mPausedByTransientLossOfFocus = false;
                        mMediaSessionCallback.onPlay();
                    } else {
                        mPlayer.duck(false);
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
                        Uri uri = mQueue.getCurrentUri();
                        Track track = mLibraryHelper.getTrack(uri);
                        if (track == null) {
                            //will callback in here
                            mQueue.remove(mQueue.getCurrentPos());
                        } else {
                            if (track.equals(mCurrentTrack)) {
                                Timber.w("Current track matches queue");
                            }
                            mCurrentTrack = track;
                            mCurrentUri = uri;
                            if (mPlaybackStateHelper.isPlaying()) {
                                mPlayWhenReady = true;
                            }
                            mPlaybackStateHelper.gotoConnecting();
                            mPlayer.setDataSource(track.dataUri);
                            mMediaSession.setQueue(mQueue.getQueueItems());
                            updateMeta();
                        }
                    } else if (mPlaybackStateHelper.isPlaying()) {
                        stopAndResetState();
                    }
                }

                @Override
                @DebugLog
                public void onQueueChanged() {
                    if (mQueue.notEmpty()) {
                        setNextTrack();
                        mMediaSession.setQueue(mQueue.getQueueItems());
                    } else if (mPlaybackStateHelper.isPlaying()) {
                        Timber.e("Got onQueueChanged with empty queue");
                        stopAndResetState();
                    }
                }

                @Override
                @DebugLog
                public void wentToNext() {
                    mCurrentTrack = mNextTrack;
                    mNextTrack = null;
                    mCurrentUri = mNextUri;
                    mNextUri = null;
                    if (!mPlaybackStateHelper.isPlaying()) {
                        mPlaybackStateHelper.gotoPlaying();
                    }
                    updateMeta();
                    setNextTrack();
                }

                private void setNextTrack() {
                    Uri uri = mQueue.getNextUri();
                    Track track = mLibraryHelper.getTrack(uri);
                    if (track == null) {
                        //will callback into onQueueChanged
                        mQueue.remove(mQueue.getNextPos());
                    } else if (!track.equals(mNextTrack)) {
                        mNextTrack = track;
                        mNextUri = uri;
                        mPlayer.setNextDataSource(track.dataUri);
                    } else {
                        Timber.i("Next track is still up to date");
                    }
                }

                private void stopAndResetState() {
                    Timber.i("Queue is gone. stopping playback");
                    mMediaSessionCallback.onStop();
                    mNotificationHelper.killNotification();
                }
            };

    final IPlayerCallback mPlayerCallback = new IPlayerCallback() {

        @Override
        @DebugLog
        public void onLoading() {
            mPlaybackStateHelper.gotoBuffering();
            updatePlaybackState();
        }

        @Override
        @DebugLog
        public void onReady() {
            long duration = mPlayer.getDuration();
            mPlaybackStateHelper.updateDuration(duration);
            if (mQueueReloaded) {
                mQueueReloaded = false;
                long pos = mPlaybackStateHelper.getPosition();
                if (pos > 0 && pos < duration) {
                    mPlayer.seekTo(pos);//TODO what to do on failure?
                }
            }
            mPlaybackStateHelper.gotoPaused();
            if (mPlayWhenReady) {
                mPlayWhenReady = false;
                mMediaSessionCallback.onPlay();
            } else {
                updatePlaybackState();
            }
            //Will load the next track
            mQueueChangeListener.onQueueChanged();
        }

        @Override
        @DebugLog
        public void onPlaying() {
            if (!mPlaybackStateHelper.isPlaying()) {
                Timber.e("Player started playing unexpectedly");
                mMediaSessionCallback.onPause();
            }
        }

        @Override
        @DebugLog
        public void onPaused() {
            if (!mPlaybackStateHelper.isPaused()) {
                Timber.e("Player paused unexpectedly");
                //TODO
            }
        }

        @Override
        @DebugLog
        public void onStopped() {
            if (mPlaybackStateHelper.isSkippingNext()) {
                //player failed to go next, manually move it
                mQueue.goToItem(mQueue.getNextPos());
            } else if (!mPlaybackStateHelper.isStopped()) {
                Timber.e("Player stopped unexpectedly");
                resetState();
                updatePlaybackState();
                //reload the current and see what happens
                mQueueChangeListener.onCurrentPosChanged();
            } else {
                //TODO anything?
            }
        }

        @Override
        @DebugLog
        public void onWentToNext() {
            //will call into wentToNext
            mQueue.wentToNext();
        }

        @Override
        @DebugLog
        public void onErrorOpenCurrentFailed(String msg) {
            //will call into onCurrentPosChanged
            mQueue.remove(mQueue.getCurrentPos());
        }

        @Override
        @DebugLog
        public void onErrorOpenNextFailed(String msg) {
            //will call into onQueueChanged
            mQueue.remove(mQueue.getNextPos());
        }

    };

    final Runnable mLoadQueueRunnable = new Runnable() {
        @Override
        public void run() {
            resetState();
            mPlaybackStateHelper.updatePosition(mSettings.getLong(PlaybackPreferences.SEEK_POS, -1));
            mQueue.load();
            mQueueReloaded = true;
            mQueueChangeListener.onCurrentPosChanged();
            mMediaSession.setActive(true);
        }
    };

    final Runnable mProgressCheckRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.removeCallbacks(this);
            long pos = mPlayer.getPosition();
            mPlaybackStateHelper.updatePosition(pos);
            updatePlaybackState();
            if (mPlaybackStateHelper.isPlaying()) {
                mHandler.postDelayed(this, 2000);
            }
        }
    };

}
