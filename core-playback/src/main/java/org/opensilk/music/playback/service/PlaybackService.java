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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.drm.DrmStore;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.audiofx.AudioEffect;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.artwork.service.ArtworkProviderHelper;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.model.Track;
import org.opensilk.music.playback.AudioManagerHelper;
import org.opensilk.music.playback.DefaultMediaPlayer;
import org.opensilk.music.playback.IMediaPlayer;
import org.opensilk.music.playback.MediaMetadataHelper;
import org.opensilk.music.playback.NotificationHelper2;
import org.opensilk.music.playback.Playback;
import org.opensilk.music.playback.PlaybackComponent;
import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.PlaybackConstants.CMD;
import org.opensilk.music.playback.PlaybackConstants.EVENT;
import org.opensilk.music.playback.PlaybackQueue;
import org.opensilk.music.playback.PlaybackStateHelper;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.HandlerScheduler;
import timber.log.Timber;

import static org.opensilk.music.playback.PlaybackConstants.CMDNAME;
import static org.opensilk.music.playback.PlaybackConstants.CMDNEXT;
import static org.opensilk.music.playback.PlaybackConstants.CMDPAUSE;
import static org.opensilk.music.playback.PlaybackConstants.CMDPLAY;
import static org.opensilk.music.playback.PlaybackConstants.CMDPREVIOUS;
import static org.opensilk.music.playback.PlaybackConstants.CMDSTOP;
import static org.opensilk.music.playback.PlaybackConstants.CMDTOGGLEPAUSE;
import static org.opensilk.music.playback.PlaybackConstants.FROM_MEDIA_BUTTON;
import static org.opensilk.music.playback.PlaybackConstants.NEXT_ACTION;
import static org.opensilk.music.playback.PlaybackConstants.NOW_IN_FOREGROUND;
import static org.opensilk.music.playback.PlaybackConstants.PAUSE_ACTION;
import static org.opensilk.music.playback.PlaybackConstants.PREVIOUS_ACTION;
import static org.opensilk.music.playback.PlaybackConstants.REPEAT_ACTION;
import static org.opensilk.music.playback.PlaybackConstants.REWIND_INSTEAD_PREVIOUS_THRESHOLD;
import static org.opensilk.music.playback.PlaybackConstants.SERVICECMD;
import static org.opensilk.music.playback.PlaybackConstants.SHUFFLE_ACTION;
import static org.opensilk.music.playback.PlaybackConstants.STOP_ACTION;
import static org.opensilk.music.playback.PlaybackConstants.TOGGLEPAUSE_ACTION;

/**
 * Created by drew on 5/6/15.
 */
public class PlaybackService extends MediaBrowserService {
    public static final String NAME = PlaybackService.class.getName();

    @Inject NotificationHelper2 mNotificationHelper;
    @Inject DelayedShutdownHandler mDelayedShutdownHandler;
    @Inject AudioManagerHelper mAudioManagerHelper;
    @Inject PlaybackQueue mQueue;
    @Inject HandlerThread mHandlerThread;
    @Inject PowerManager.WakeLock mWakeLock;
    @Inject ArtworkProviderHelper mArtworkProviderHelper;
    @Inject MediaSessionHolder mSessionHolder;
    @Inject IndexClient mIndexClient;
    @Inject Playback mPlayback;

    int mAudioSessionId;
    private Handler mHandler;
    private Scheduler mHandlerScheduler;

    //currently playing track
    Track mCurrentTrack;
    //next track to load
    Track mNextTrack;
    //true if we should start playing when loading finishes
    boolean mPlayWhenReady;
    //
    boolean mQueueReloaded;
    //
    boolean mServiceStarted = false;
    volatile int mConnectedClients = 0;

    Subscription mCurrentTrackSub;
    Subscription mNextTrackSub;
    Subscription mQueueListSub;

    @Override
    public void onCreate() {
        PlaybackComponent parent = DaggerService.getDaggerComponent(getApplicationContext());
        parent.playbackServiceComponent(PlaybackServiceModule.create(this)).inject(this);
        super.onCreate();

        acquireWakeLock();

        //fire up thread and init handler
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandlerScheduler = HandlerScheduler.from(mHandler);

        //tell everyone about ourselves
        mQueue.setListener(new PlaybackQueueQueueChangeListener());

        getMediaSession().setCallback(new MediaSessionCallback(), mHandler);
        setSessionToken(mSessionHolder.getSessionToken());
        updatePlaybackState(null);

        mPlayback.setState(PlaybackState.STATE_NONE);
        mPlayback.setCallback(new PlaybackCallback());
        mPlayback.start();

        mAudioSessionId = mAudioManagerHelper.getAudioSessionId();
        mHandler.post(mLoadQueueRunnable);
    }

    @Override
    public void onDestroy() {
        saveState(); //fire early as possible
        super.onDestroy();

        mNotificationHelper.killNotification();
        mDelayedShutdownHandler.cancelDelayedShutdown();

        mPlayback.stop(false);
        mSessionHolder.release();

        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.getLooper().quitSafely();

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
        mDelayedShutdownHandler.cancelDelayedShutdown();
        mConnectedClients++;
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        saveState();
        mConnectedClients--;
        return super.onUnbind(intent);
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

            if (intent.hasExtra(NOW_IN_FOREGROUND)) {
                mNotificationHelper.setActivityInForeground(intent.getBooleanExtra(NOW_IN_FOREGROUND, false));
            }

            if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                mSessionHolder.getController().dispatchMediaButtonEvent(
                        intent.<KeyEvent>getParcelableExtra(Intent.EXTRA_KEY_EVENT));
            } else {
                handleIntentCommand(intent);
            }

            if (intent.getBooleanExtra(FROM_MEDIA_BUTTON, false)) {
                MediaButtonIntentReceiver.completeWakefulIntent(intent);
            }
        }
        mServiceStarted = true;
        return START_STICKY;
    }

    void handleIntentCommand(@NonNull Intent intent) {
        final String action = intent.getAction();
        final String command = SERVICECMD.equals(action) ? intent.getStringExtra(CMDNAME) : null;
        Timber.v("handleIntentCommand: action = %s, command = %s", action, command);
        MediaController controller = mSessionHolder.getController();
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
            controls.sendCustomAction(CMD.TOGGLE_SHUFFLE_MODE, null);
        }
    }

    @DebugLog
    void updatePlaybackState(String error) {
        Timber.d("updatePlaybackState(%s) err=%s",
                PlaybackStateHelper.stringifyState(mPlayback.getState()), error);

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(getAvailableActions());

        int state = mPlayback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackState.STATE_ERROR;
        }

        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        long duration = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        if (PlaybackStateHelper.isPlayingOrPaused(mPlayback.getState())) {
            position = mPlayback.getCurrentStreamPosition();
            duration = mPlayback.getDuration();
        }

        if (duration > 0) {
            //make sure meta has the right duration
            MediaMetadata current = getMediaSession().getController().getMetadata();
            if (current != null) {
                if (MediaMetadataHelper.getDuration(current) != duration) {
                    current = new MediaMetadata.Builder(current)
                            .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                            .build();
                    getMediaSession().setMetadata(current);
                }
            }
        }

        stateBuilder.setState(state, position,
                PlaybackStateHelper.PLAYBACK_SPEED, SystemClock.elapsedRealtime());
        if (VersionUtils.hasApi22()) {
            stateBuilder.setExtras(BundleHelper.b()
                    .putLong(duration)
                    .putInt(mQueue.getRepeatMode())
                    .putInt2(mQueue.getShuffleMode()).get());
        } else {
            stateBuilder.setBufferedPosition(duration);
        }

        // Set the activeQueueItemId if the current index is valid.
        MediaSession.QueueItem item = mQueue.getCurrentQueueItem();
        if (item != null) {
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        getMediaSession().setPlaybackState(stateBuilder.build());

        if (PlaybackStateHelper.isPlayingOrPaused(state)) {
            mNotificationHelper.startNotification();
        }

        mHandler.removeCallbacks(mProgressCheckRunnable);
        if (PlaybackStateHelper.isPlaying(state)) {
            mHandler.postDelayed(mProgressCheckRunnable, 2000);
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY
                | PlaybackState.ACTION_PLAY_PAUSE
                //| PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                //| PlaybackState.ACTION_PLAY_FROM_SEARCH
                ;
        if (mQueue.notEmpty()) {
            actions |= (PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM);
            if (mPlayback.isPlaying()) {
                actions |= PlaybackState.ACTION_PAUSE;
                actions &= ~PlaybackState.ACTION_PLAY;
            }
            if (PlaybackStateHelper.isPlayingOrPaused(mPlayback.getState())) {
                actions |= PlaybackState.ACTION_STOP;
            }
            if (mQueue.getPrevious() >= 0) {
                actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
            }
            if (mQueue.getNextPos() >= 0) {
                actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
            }
        }
        return actions;
    }

    void updateMeta() {
        MediaMetadata meta = mIndexClient.convertToMediaMetadata(mCurrentTrack);
        getMediaSession().setMetadata(meta);
    }

    void saveState() {
        final PlaybackQueue.Snapshot qSnapshot = mQueue.snapshot();
        PlaybackState state = getMediaSession().getController().getPlaybackState();
        final long seekPos;
        if (state != null) {
            seekPos = state.getPosition();
        } else {
            seekPos = 0;
        }
        //Use async to avoid making new thread
        new AsyncTask<Object, Void, Void>() {
            @Override
            @DebugLog
            protected Void doInBackground(Object... params) {
                mIndexClient.saveQueue(qSnapshot.q);
                mIndexClient.saveQueuePosition(qSnapshot.pos);
                mIndexClient.saveQueueRepeatMode(qSnapshot.repeat);
                mIndexClient.saveQueueShuffleMode(qSnapshot.shuffle);
                mIndexClient.saveLastSeekPosition(seekPos);
                return null;
            }
        }.execute();
    }

    void resetState() {
        mCurrentTrack = null;
        mNextTrack = null;
        mPlayWhenReady = false;
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

    public Scheduler getScheduler() {
        return mHandlerScheduler;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public MediaSession getMediaSession() {
        return mSessionHolder.getSession();
    }

    public int getAudioSessionId() {
        return mAudioSessionId;
    }

    @Nullable @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        return mIndexClient.browserGetRoot(clientPackageName, clientUid, rootHints);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowser.MediaItem>> result) {
        result.detach();
        mIndexClient.browserLoadChildren(parentId, result);
    }

    private void handleStop() {
        mPlayback.stop(false);
        mHandler.removeCallbacks(mProgressCheckRunnable);
        mDelayedShutdownHandler.cancelDelayedShutdown();
        resetState();
        if (mConnectedClients > 0) {
            //we cant stop so try going to paused
            if (mQueue.notEmpty()) {
                mPlayWhenReady = false;
                setTrack();
            } else {
                mPlayback.setState(PlaybackState.STATE_NONE);
                updatePlaybackState(null);
            }
        } else {
            //we aren't bound by anyone so we can stop
            stopSelf();
        }
    }

    IMediaPlayer.Factory resolveMediaPlayer(Track track) {
        return new DefaultMediaPlayer.Factory();
    }

    private void setTrack() {
        if (!mQueue.notEmpty()){
            throw new IllegalStateException("Can't setTrack() with no queue");
        }
        final Uri uri = mQueue.getCurrentUri();
        if (mCurrentTrack != null && mCurrentTrack.getUri().equals(uri)) {
            Timber.e("Uris match, what should i do? reloading anyway");
        }
        mHandler.removeCallbacks(mProgressCheckRunnable);
        mPlayback.prepareForTrack();
        if (mCurrentTrackSub != null) {
            mCurrentTrackSub.unsubscribe();
        }
        mCurrentTrackSub = mIndexClient.getTrack(uri)
                .first()
                .observeOn(getScheduler())
                .subscribe(new Subscriber<Track>() {
                    @Override public void onCompleted() {
                        mCurrentTrackSub = null;
                    }
                    @Override public void onError(Throwable e) {
                        mCurrentTrackSub = null;
                        //will callback in here
                        mQueue.remove(mQueue.getCurrentPos());
                    }
                    @Override public void onNext(Track track) {
                        mCurrentTrack = track;
                        mPlayback.loadTrack(track.getResources().get(0),
                                resolveMediaPlayer(track));
                        if (mQueueReloaded) {
                            mQueueReloaded = false;
                            long seek = mIndexClient.getLastSeekPosition();
                            if (seek > 0) {
                                mPlayback.seekTo(0);
                            }
                        }
                        if (mPlayWhenReady) {
                            mPlayWhenReady = false;
                            mPlayback.play();
                        }
                        updateMeta();
                        setNextTrack();
                    }
                });
    }

    private void setNextTrack() {
        if (mQueue.getNextPos() < 0) {
            Timber.i("No next track in queue");
            if (mPlayback.hasNext()) {
                //removes the next player
                mPlayback.prepareForNextTrack();
            }
            return;
        }
        final Uri uri = mQueue.getNextUri();
        if (mNextTrack != null && mNextTrack.getUri().equals(uri) && mPlayback.hasNext()) {
            Timber.i("Next track is up to date");
            return;
        }
        mPlayback.prepareForNextTrack();
        if (mNextTrackSub != null) {
            mNextTrackSub.unsubscribe();
        }
        mNextTrackSub = mIndexClient.getTrack(uri)
                .first()
                .observeOn(getScheduler())
                .subscribe(new Subscriber<Track>() {
                    @Override
                    public void onCompleted() {
                        mNextTrackSub = null;
                    }

                    @Override
                    public void onError(Throwable e) {
                        //will callback into onQueueChanged
                        mQueue.remove(mQueue.getNextPos());
                        mNextTrackSub = null;
                    }

                    @Override
                    public void onNext(Track track) {
                        mNextTrack = track;
                        mPlayback.loadNextTrack(track.getResources().get(0),
                                resolveMediaPlayer(track));
                    }
                });
    }

    class MediaSessionCallback extends MediaSession.Callback {
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
            mHandler.removeCallbacks(mProgressCheckRunnable);
            mDelayedShutdownHandler.cancelDelayedShutdown();
            if (!mServiceStarted) {
                startService(new Intent(PlaybackService.this, PlaybackService.class));
                mServiceStarted = true;
            }
            if (!getMediaSession().isActive()) {
                getMediaSession().setActive(true);
            }
            if (mQueue.notEmpty()) {
                if (PlaybackStateHelper.isConnecting(mPlayback.getState())) {
                    //we are still fetching the current track, tell it to
                    //play when it arrives
                    mPlayWhenReady = true;
                } else if (!mPlayback.isPlaying()) {
                    mPlayback.play();
                }
            } else {
                //TODO make random queue
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
            int pos = mQueue.getPosOfId(id);
            if (pos >= 0) {
                if (mQueue.getCurrentPos() != pos) {
                    if (mQueue.getNextPos() == pos) {
                        onSkipToNext();
                    } else {
                        mPlayback.prepareForTrack();
                        mPlayWhenReady = true;
                        mQueue.goToItem(pos);
                    }
                }
            } else {
                //no longer in queue TODO
            }
        }

        @Override
        @DebugLog
        public void onPause() {
            mHandler.removeCallbacks(mProgressCheckRunnable);
            mPlayWhenReady = false;
            mPlayback.pause();
            saveState();
        }

        @Override
        @DebugLog
        public void onSkipToNext() {
            mHandler.removeCallbacks(mProgressCheckRunnable);
            if (mPlayback.hasNext()) {
                mPlayback.goToNext();
            } else {
                int next = mQueue.getNextPos();
                if (next >= 0) {
                    mPlayback.prepareForTrack();
                    mPlayWhenReady = true;
                    mQueue.goToItem(next);
                }//else ignore
            }
        }

        @Override
        public void onSkipToPrevious() {
            mHandler.removeCallbacks(mProgressCheckRunnable);
            if (mPlayback.getCurrentStreamPosition() > REWIND_INSTEAD_PREVIOUS_THRESHOLD) {
                onSeekTo(0);
            } else {
                int prev = mQueue.getPrevious();
                if (prev >= 0) {
                    mPlayback.prepareForTrack();
                    mPlayWhenReady = true;
                    //will callback to onCurrentPosChanged
                    mQueue.goToItem(prev);
                }//else ignore
            }
        }

        @Override
        @DebugLog
        public void onStop() {
            handleStop();
        }

        @Override
        public void onSeekTo(long pos) {
            mHandler.removeCallbacks(mProgressCheckRunnable);
            mPlayback.seekTo(pos);
        }

        @Override
        public void onSetRating(@NonNull Rating rating) {
            super.onSetRating(rating);
        }

        @Override
        @DebugLog
        public void onCustomAction(@NonNull String action, Bundle extras) {
            switch (action) {
                case CMD.CYCLE_REPEAT: {
                    mQueue.toggleRepeat();
                    getMediaSession().sendSessionEvent(EVENT.REPEAT_CHANGED,
                            BundleHelper.builder().putInt(mQueue.getRepeatMode()).get());
                    updatePlaybackState(null);
                    break;
                }
                case CMD.TOGGLE_SHUFFLE_MODE: {
                    mQueue.toggleShuffle();
                    getMediaSession().sendSessionEvent(EVENT.QUEUE_SHUFFLED,
                            BundleHelper.builder().putInt(mQueue.getShuffleMode()).get());
                    updatePlaybackState(null);
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
                    final int where = BundleHelper.getInt(extras);
                    if (mQueueListSub != null) {
                        mQueueListSub.unsubscribe();
                    }
                    mQueueListSub = mIndexClient.getTrackUris(uri, sort)
                            .first()
                            .observeOn(getScheduler())
                            .subscribe(new Subscriber<List<Uri>>() {
                                @Override public void onCompleted() {
                                    mQueueListSub = null;
                                }
                                @Override public void onError(Throwable e) {
                                    mQueueListSub = null;
                                    ///TODO
                                }
                                @Override public void onNext(List<Uri> uris) {
                                    if (where == PlaybackConstants.ENQUEUE_LAST) {
                                        mQueue.addEnd(uris);
                                    } else if (where == PlaybackConstants.ENQUEUE_NEXT) {
                                        mQueue.addNext(uris);
                                    }
                                }
                            });
                    break;
                }
                case CMD.PLAY_ALL: {
                    List<Uri> list = BundleHelper.getList(extras);
                    int startpos = BundleHelper.getInt(extras);
                    if (!list.equals(mQueue.get())) {
                        mPlayback.prepareForTrack();
                        mPlayWhenReady = true;
                        mQueue.replace(list, startpos);
                    } else if (startpos != mQueue.getCurrentPos()) {
                        if (startpos == mQueue.getNextPos()) {
                            onSkipToNext();
                        } else {
                            mPlayback.prepareForTrack();
                            mPlayWhenReady = true;
                            mQueue.goToItem(startpos);
                        }
                    } //else no change, ignore
                    break;
                }
                case CMD.PLAY_TRACKS_FROM: {
                    Uri uri = BundleHelper.getUri(extras);
                    String sort = BundleHelper.getString(extras);
                    final int startpos = BundleHelper.getInt(extras);
                    mPlayback.prepareForTrack();
                    if (mQueueListSub != null) {
                        mQueueListSub.unsubscribe();
                    }
                    mQueueListSub = mIndexClient.getTrackUris(uri, sort)
                            .first()
                            .observeOn(getScheduler())
                            .subscribe(new Subscriber<List<Uri>>() {
                                @Override public void onCompleted() {
                                    mQueueListSub = null;
                                }
                                @Override public void onError(Throwable e) {
                                    mQueueListSub = null;
                                    ///TODO
                                }
                                @Override public void onNext(List<Uri> uris) {
                                    mQueue.replace(uris, startpos);
                                    mPlayWhenReady = true;
                                }
                            });
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
                    if (mPlayback.isPlaying() || mPlayWhenReady) {
                        onPause();
                    } else {
                        onPlay();
                    }
                    break;
                }
            }
        }
    }

    class PlaybackQueueQueueChangeListener implements PlaybackQueue.QueueChangeListener {
        @Override
        public void onCurrentPosChanged() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onCurrentPosChangedReal();
                }
            });
        }

        @Override
        public void onQueueChanged() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onQueueChangedReal();
                }
            });
        }

        @Override
        public void onMovedToNext() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onMovedToNextReal();
                }
            });
        }

        @DebugLog
        void onCurrentPosChangedReal() {
            if (mQueue.notEmpty()) {
                getMediaSession().setQueue(mQueue.getQueueItems());
                if (mQueue.getCurrentPos() < 0) {
                    Timber.e(new IllegalStateException("Current pos is " + mQueue.getCurrentPos()),
                            "This should not happen while queue has items");
                    return;
                }
                setTrack();
            } else {
                Timber.i("Queue is gone. stopping playback");
                handleStop();
            }
        }

        @DebugLog
        void onQueueChangedReal() {
            if (mQueue.notEmpty()) {
                getMediaSession().setQueue(mQueue.getQueueItems());
                setNextTrack();
            } else {
                Timber.e(new IllegalStateException("Got onQueueChanged with empty queue but " +
                                "should have got onCurrentPosChanged"), "fix this");
                handleStop();
            }
        }

        @DebugLog
        void onMovedToNextReal() {
            mCurrentTrack = mNextTrack;
            mNextTrack = null;
            updateMeta();
            setNextTrack();
        }

    }

    class PlaybackCallback implements Playback.Callback {
        @Override
        public void onPlaybackStatusChanged(int state) {
            updatePlaybackState(null);
        }

        @Override
        @DebugLog
        public void onCompletion() {

        }

        @Override
        @DebugLog
        public void onWentToNext() {
            //will call into moveToNext
            mQueue.moveToNext();
            updatePlaybackState(null);
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

        @Override
        @DebugLog
        public void onError(String error) {

        }
    }

    final Runnable mLoadQueueRunnable = new Runnable() {
        @Override
        public void run() {
            resetState();
            mQueue.load();
            mQueueReloaded = true;
            updatePlaybackState(null);
        }
    };

    final Runnable mProgressCheckRunnable = new Runnable() {
        @Override
        public void run() {
            updatePlaybackState(null);
        }
    };

}
