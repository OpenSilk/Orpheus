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
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.artwork.service.ArtworkProviderHelper;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.model.Track;
import org.opensilk.music.playback.renderer.DefaultMediaPlayer;
import org.opensilk.music.playback.renderer.IMediaPlayer;
import org.opensilk.music.playback.MediaMetadataHelper;
import org.opensilk.music.playback.NotificationHelper2;
import org.opensilk.music.playback.renderer.LocalRenderer;
import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.PlaybackConstants.CMD;
import org.opensilk.music.playback.PlaybackConstants.EVENT;
import org.opensilk.music.playback.PlaybackQueue;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.session.IMediaControllerProxy;
import org.opensilk.music.playback.session.IMediaSessionProxy;

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
@PlaybackServiceScope
public class PlaybackService {
    public static final String NAME = PlaybackService.class.getName();

    private final Context mContext;
    private final NotificationHelper2 mNotificationHelper;
    private final DelayedShutdownHandler mDelayedShutdownHandler;
    private final PlaybackQueue mQueue;
    private final PowerManager.WakeLock mWakeLock;
    private final ArtworkProviderHelper mArtworkProviderHelper;
    private final MediaSessionHolder mSessionHolder;
    private final IndexClient mIndexClient;
    private final LocalRenderer mPlayback;
    private final ArtworkProviderHelper mArtworkHelper;

    private HandlerThread mHandlerThread;
    private PlaybackServiceProxy mProxy;

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
    boolean mQueueReady;
    //
    boolean mServiceStarted = false;
    volatile int mConnectedClients = 0;
    volatile boolean shouldReleaseClient;

    Subscription mCurrentTrackSub;
    Subscription mNextTrackSub;
    Subscription mQueueListSub;
    Subscription mArtworkSubscription;

    @Inject
    public PlaybackService(
            @ForApplication Context mContext,
            NotificationHelper2 mNotificationHelper,
            DelayedShutdownHandler mDelayedShutdownHandler,
            PlaybackQueue mQueue,
            PowerManager.WakeLock mWakeLock,
            ArtworkProviderHelper mArtworkProviderHelper,
            MediaSessionHolder mSessionHolder,
            IndexClient mIndexClient,
            LocalRenderer mPlayback,
            ArtworkProviderHelper mArtworkHelper
    ) {
        this.mContext = mContext;
        this.mNotificationHelper = mNotificationHelper;
        this.mDelayedShutdownHandler = mDelayedShutdownHandler;
        this.mQueue = mQueue;
        this.mWakeLock = mWakeLock;
        this.mArtworkProviderHelper = mArtworkProviderHelper;
        this.mSessionHolder = mSessionHolder;
        this.mIndexClient = mIndexClient;
        this.mPlayback = mPlayback;
        this.mArtworkHelper = mArtworkHelper;
    }

    //main thread
    public void onCreate(PlaybackServiceProxy proxy) {
        mProxy = proxy;

        acquireWakeLock();

        //fire up thread and init handler
        mHandlerThread = new HandlerThread(PlaybackService.NAME, Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandlerScheduler = HandlerScheduler.from(mHandler);

        //tell everyone about ourselves
        mQueue.setListener(new PlaybackQueueQueueChangeListener());

        mSessionHolder.setCallback(new MediaSessionCallback(), mHandler);
        proxy.setSessionToken(mSessionHolder.getSessionToken());

        mPlayback.setState(PlaybackStateCompat.STATE_NONE);
        mPlayback.setCallback(new PlaybackCallback());
        mPlayback.start();

        updatePlaybackState(null);

        mHandler.post(mLoadQueueRunnable);
    }

    //main thread
    public void onDestroy() {
        shouldReleaseClient = true;
        saveState(true); //fire early as possible

        if (mCurrentTrackSub != null) {
            mCurrentTrackSub.unsubscribe();
        }
        if (mNextTrackSub != null) {
            mNextTrackSub.unsubscribe();
        }
        if (mQueueListSub != null) {
            mQueueListSub.unsubscribe();
        }
        if (mArtworkSubscription != null) {
            mArtworkSubscription.unsubscribe();
        }

        mNotificationHelper.killNotification();
        mDelayedShutdownHandler.cancelDelayedShutdown();

        mPlayback.stop(false);
        mSessionHolder.release();

        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.getLooper().quit();

        removeAudioEffects();
        releaseWakeLock();
    }

    //binder thread?
    public void onBind() {
        mDelayedShutdownHandler.cancelDelayedShutdown();
        mConnectedClients++;
    }

    //binder thread?
    public void onUnbind() {
        saveState(true);
        mConnectedClients--;
    }

    //main thread
    public void onTrimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            mArtworkProviderHelper.evictL1();
            saveState(true);
        }
    }

    //main thread
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            acquireWakeLock();

            String action = intent.getAction();

            if (intent.hasExtra(NOW_IN_FOREGROUND)) {
                mNotificationHelper.setActivityInForeground(intent.getBooleanExtra(NOW_IN_FOREGROUND, false));
            }

            if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                mSessionHolder.dispatchMediaButtonEvent(
                        intent.<KeyEvent>getParcelableExtra(Intent.EXTRA_KEY_EVENT));
            } else {
                handleIntentCommand(intent);
            }

            if (intent.getBooleanExtra(FROM_MEDIA_BUTTON, false)) {
                MediaButtonIntentReceiver.completeWakefulIntent(intent);
            }
        }
        mServiceStarted = true;
        return Service.START_STICKY;
    }

    //main thread
    void handleIntentCommand(@NonNull Intent intent) {
        final String action = intent.getAction();
        final String command = SERVICECMD.equals(action) ? intent.getStringExtra(CMDNAME) : null;
        Timber.v("handleIntentCommand: action = %s, command = %s", action, command);
        IMediaControllerProxy.TransportControlsProxy controls = mSessionHolder.getTransportControls();
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

    @DebugLog //handler thread
    void updatePlaybackState(String error) {
        updatePlaybackState(error, false);
    }

    //handler thread
    void updatePlaybackState(String error, boolean fromChecker) {
        int state = mPlayback.getState();
        Timber.d("updatePlaybackState(%s) err=%s",
                PlaybackStateHelper.stringifyState(state), error);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());


        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }

        long position = mPlayback.getCurrentStreamPosition();
        long duration = mPlayback.getDuration();

        if (duration > 0) {
            //make sure meta has the right duration
            MediaMetadataCompat current = mSessionHolder.getMetadata();
            if (current != null) {
                long metaDuration = MediaMetadataHelper.getDuration(current);
                if (metaDuration != duration) {
                    Timber.d("Updating meta with proper duration old=%d, new=%d",
                            metaDuration, duration);
                    current = new MediaMetadataCompat.Builder(current)
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                            .build();
                    mSessionHolder.setMetadata(current);
                }
            }
        }

        stateBuilder.setState(state, position,
                PlaybackStateHelper.PLAYBACK_SPEED, SystemClock.elapsedRealtime());
        if (VersionUtils.hasApi22()) {
            stateBuilder.setExtras(BundleHelper.b()
                    .putLong(duration).get());
        } else {
            stateBuilder.setBufferedPosition(duration);
        }

        // Set the activeQueueItemId if the current index is valid.
        MediaSessionCompat.QueueItem item = mQueue.getCurrentQueueItem();
        if (item != null) {
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mSessionHolder.setPlaybackState(stateBuilder.build());

        if (PlaybackStateHelper.isPlayingOrPaused(state)) {
            mNotificationHelper.startNotification();
        }

        mHandler.removeCallbacks(mProgressCheckRunnable);
        if (PlaybackStateHelper.isPlaying(state)) {
            //if not a schedule update recheck in 2 sec in case duration wasnt ready
            mHandler.postDelayed(mProgressCheckRunnable, fromChecker ? 30000 : 2000);
        }
    }

    //handler thread
    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                //| PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                //| PlaybackState.ACTION_PLAY_FROM_SEARCH
                ;
        if (mQueue.notEmpty()) {
            actions |= (PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM);
            if (mPlayback.isPlaying()) {
                actions |= PlaybackStateCompat.ACTION_PAUSE;
                actions &= ~PlaybackStateCompat.ACTION_PLAY;
            }
            if (PlaybackStateHelper.isPlayingOrPaused(mPlayback.getState())) {
                actions |= PlaybackStateCompat.ACTION_STOP;
            }
            if (mQueue.getPrevious() >= 0) {
                actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
            }
            if (mQueue.getNextPos() >= 0) {
                actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
            }
        }
        return actions;
    }

    //handler thread
    void updateMeta() {
        if (mArtworkSubscription != null) {
            mArtworkSubscription.unsubscribe();
            mArtworkSubscription = null;
        }
        final MediaMetadataCompat meta = mIndexClient.convertToMediaMetadata(mCurrentTrack);
        final Uri artUri = MediaMetadataHelper.getIconUri(meta);
        ArtworkProviderHelper.CacheBitmap bitmap = mArtworkHelper.getCachedOrDefault(artUri);
        //Always build with default first to ensure it shows promptly
        mSessionHolder.setMetadata(new MediaMetadataCompat.Builder(meta).putBitmap(
                MediaMetadataCompat.METADATA_KEY_ART, bitmap.getBitmap()).build());
        if (!bitmap.fromCache() && artUri != null) {
            //Then go for artwork, since it could take a while
            mArtworkSubscription = mArtworkHelper.getArtwork(artUri)
                    .observeOn(getScheduler())
                    .subscribe(new Subscriber<Bitmap>() {
                        @Override public void onCompleted() {
                            mArtworkSubscription = null;
                        }
                        @Override public void onError(Throwable e) {
                            Timber.w(e, "getArtwork");
                            mArtworkSubscription = null;
                        }
                        @Override public void onNext(Bitmap bitmap) {
                            mSessionHolder.setMetadata(new MediaMetadataCompat.Builder(meta)
                                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap).build());
                        }
                    });
        }
    }

    //handler thread / main thread
    void saveState(final boolean full) {
        final PlaybackQueue.Snapshot qSnapshot = mQueue.snapshot();
        PlaybackStateCompat state = mSessionHolder.getPlaybackState();
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
                if (full) {
                    mIndexClient.saveQueue(qSnapshot.q);
                }
                mIndexClient.saveQueuePosition(qSnapshot.pos);
                mIndexClient.saveQueueRepeatMode(qSnapshot.repeat);
                mIndexClient.saveQueueShuffleMode(qSnapshot.shuffle);
                mIndexClient.saveLastSeekPosition(seekPos);
                if (shouldReleaseClient) {
                    mIndexClient.release();
                }
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

    void applyAudioEffects() {
        //apply audio effects to our new sessionId
        final Intent audioEffectsIntent = new Intent(
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mAudioSessionId);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mContext.getPackageName());
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
        mContext.sendBroadcast(audioEffectsIntent);
    }

    void removeAudioEffects() {
        // Remove any sound effects
        final Intent audioEffectsIntent = new Intent(
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mAudioSessionId);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mContext.getPackageName());
        mContext.sendBroadcast(audioEffectsIntent);
    }

    Scheduler getScheduler() {
        return mHandlerScheduler;
    }

    Handler getHandler() {
        return mHandler;
    }

    MediaSessionHolder getSessionHolder() {
        return mSessionHolder;
    }

    IndexClient getIndexClient() {
        return mIndexClient;
    }

    void handleStop() {
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
                mPlayback.setState(PlaybackStateCompat.STATE_NONE);
                updatePlaybackState(null);
            }
        } else {
            //we aren't bound by anyone so we can stop
            mProxy.stopSelf();
        }
    }

    IMediaPlayer.Factory resolveMediaPlayer(Track track) {
        return new DefaultMediaPlayer.Factory();
    }

    @DebugLog
    private void setTrack() {
        if (!mQueue.notEmpty()){
            throw new IllegalStateException("Can't setTrack() with no queue");
        }
        final Uri uri = mQueue.getCurrentUri();
        if (uri == null) {
            throw new IllegalStateException("Current uri is null for pos " + mQueue.getCurrentPos());
        }
        if (mCurrentTrack != null && mCurrentTrack.getUri().equals(uri) && mPlayback.hasPlayer()) {
            Timber.e("Current track is up to date");
            if (mPlayWhenReady && !mPlayback.isPlaying()) {
                mPlayWhenReady = false;
                mPlayback.play();
            }
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
                                mPlayback.seekTo(seek);
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

    @DebugLog
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
                    @Override public void onCompleted() {
                        mNextTrackSub = null;
                    }
                    @Override public void onError(Throwable e) {
                        //will callback into onQueueChanged
                        mQueue.remove(mQueue.getNextPos());
                        mNextTrackSub = null;
                    }
                    @Override public void onNext(Track track) {
                        mNextTrack = track;
                        mPlayback.loadNextTrack(track.getResources().get(0),
                                resolveMediaPlayer(track));
                    }
                });
    }

    class MediaSessionCallback implements IMediaSessionProxy.Callback {
        @Override
        public void onCommand(String command, Bundle args, ResultReceiver cb) {
            switch (command) {
                case CMD.REQUEST_REPEATMODE_UPDATE: {
                    Bundle reply = BundleHelper.b().putInt(mQueue.getRepeatMode()).get();
                    if (cb != null) {
                        cb.send(0, reply);
                    } else {
                        mSessionHolder.sendSessionEvent(EVENT.REPEAT_CHANGED, reply);
                    }
                    break;
                }
                case CMD.REQUEST_SHUFFLEMODE_UPDATE: {
                    Bundle reply = BundleHelper.b().putInt(mQueue.getShuffleMode()).get();
                    if (cb != null) {
                        cb.send(0, reply);
                    } else {
                        mSessionHolder.sendSessionEvent(EVENT.QUEUE_SHUFFLED, reply);
                    }
                    break;
                }
                case CMD.REQUEST_AUDIOSESSION_ID: {
                    Bundle reply = BundleHelper.b().putInt(mAudioSessionId).get();
                    if (cb != null) {
                        cb.send(0, reply);
                    } else if (mAudioSessionId != 0) {
                        mSessionHolder.sendSessionEvent(EVENT.NEW_AUDIO_SESSION_ID, reply);
                    }
                    break;
                }
            }
        }

        @Override
        @DebugLog
        public void onPlay() {
            mHandler.removeCallbacks(mProgressCheckRunnable);
            mDelayedShutdownHandler.cancelDelayedShutdown();
            if (!mServiceStarted) {
                mProxy.startSelf();
                mServiceStarted = true;
            }
            if (mQueueReady) {
                mSessionHolder.setActive(true);
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
            } else {
                //If we were started from a mediabutton
                //we will be called before the queue has a chance to
                //load, so tell it to go ahead and start
                mPlayWhenReady = true;
            }
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
            saveState(true);
        }

        @Override
        @DebugLog
        public void onSkipToNext() {
            mHandler.removeCallbacks(mProgressCheckRunnable);
            if (mPlayback.hasNext()) {
                mPlayback.goToNext();
            } else if (mQueue.notEmpty()) {
                int next = mQueue.getNextPos();
                if (next >= 0) {
                    mPlayback.prepareForTrack();
                    mPlayWhenReady = true;
                    mQueue.goToItem(next);
                }//else ignore
            } //else ignore
        }

        @Override
        @DebugLog
        public void onSkipToPrevious() {
            mHandler.removeCallbacks(mProgressCheckRunnable);
            if (mPlayback.getCurrentStreamPosition() > REWIND_INSTEAD_PREVIOUS_THRESHOLD) {
                onSeekTo(0);
            } else if (mQueue.notEmpty()) {
                int prev = mQueue.getPrevious();
                if (prev >= 0) {
                    mPlayback.prepareForTrack();
                    mPlayWhenReady = true;
                    //will callback to onCurrentPosChanged
                    mQueue.goToItem(prev);
                }//else ignore
            } //else ignore
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
        public void onSetRating(@NonNull RatingCompat rating) {
            //TODO
        }

        @Override
        @DebugLog
        public void onCustomAction(@NonNull String action, Bundle extras) {
            switch (action) {
                case CMD.CYCLE_REPEAT: {
                    mQueue.toggleRepeat();
                    mSessionHolder.sendSessionEvent(EVENT.REPEAT_CHANGED,
                            BundleHelper.b().putInt(mQueue.getRepeatMode()).get());
                    updatePlaybackState(null);
                    break;
                }
                case CMD.TOGGLE_SHUFFLE_MODE: {
                    mQueue.toggleShuffle();
                    mSessionHolder.sendSessionEvent(EVENT.QUEUE_SHUFFLED,
                            BundleHelper.b().putInt(mQueue.getShuffleMode()).get());
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
                mSessionHolder.setQueue(mQueue.getQueueItems());
                if (mQueueReloaded) {
                    mQueueReady = true;
                    updatePlaybackState(null);
                }
                if (mQueue.getCurrentPos() < 0) {
                    Timber.e(new IllegalStateException("Current pos is " + mQueue.getCurrentPos()),
                            "This should not happen while queue has items");
                    return;
                }
                setTrack();
            } else {
                Timber.i("Queue is gone. stopping playback");
                mSessionHolder.setQueue(null);
                handleStop();
            }
        }

        @DebugLog
        void onQueueChangedReal() {
            if (mQueue.notEmpty()) {
                mSessionHolder.setQueue(mQueue.getQueueItems());
                setNextTrack();
            } else {
                throw new IllegalStateException("Got onQueueChanged with empty queue but " +
                                "should have got onCurrentPosChanged");
            }
        }

        @DebugLog
        void onMovedToNextReal() {
            mCurrentTrack = mNextTrack;
            mNextTrack = null;
            updateMeta();
            updatePlaybackState(null);
            setNextTrack();
            saveState(false);
        }

    }

    class PlaybackCallback implements LocalRenderer.Callback {
        @Override
        @DebugLog
        public void onPlaybackStatusChanged(int state) {
            updatePlaybackState(null);
        }

        @Override
        @DebugLog
        public void onCompletion() {
            mPlayWhenReady = false;
            if (mQueue.notEmpty()) {
                //we've finished the list, go back to start
                mQueue.goToItem(0);
                updatePlaybackState(null);
            } else {
                handleStop();
            }
        }

        @Override
        @DebugLog
        public void onWentToNext() {
            //will call into moveToNext
            mQueue.moveToNext();
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
            updatePlaybackState(error);
        }

        @Override
        public void onAudioSessionId(int audioSessionId) {
            mAudioSessionId = audioSessionId;
            applyAudioEffects();
            mSessionHolder.sendSessionEvent(EVENT.NEW_AUDIO_SESSION_ID,
                    BundleHelper.b().putInt(audioSessionId).get());
        }
    }

    final Runnable mLoadQueueRunnable = new Runnable() {
        @Override
        public void run() {
            resetState();
            mQueue.load();
            mQueueReloaded = true;
        }
    };

    final Runnable mProgressCheckRunnable = new Runnable() {
        @Override
        public void run() {
            updatePlaybackState(null, true);
        }
    };

}
