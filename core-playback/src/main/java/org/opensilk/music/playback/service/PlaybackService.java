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
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.artwork.service.ArtworkProviderHelper;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.library.client.TypedBundleableLoader;
import org.opensilk.music.model.Track;
import org.opensilk.music.playback.MediaMetadataHelper;
import org.opensilk.music.playback.NotificationHelper2;
import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.PlaybackConstants.ACTION;
import org.opensilk.music.playback.PlaybackConstants.CMD;
import org.opensilk.music.playback.PlaybackConstants.EVENT;
import org.opensilk.music.playback.PlaybackQueue;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.R;
import org.opensilk.music.playback.renderer.IMusicRenderer;
import org.opensilk.music.playback.renderer.LocalRenderer;
import org.opensilk.music.playback.renderer.PlaybackServiceAccessor;
import org.opensilk.music.playback.session.IMediaControllerProxy;
import org.opensilk.music.playback.session.IMediaSessionProxy;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.HandlerScheduler;
import rx.functions.Action1;
import rx.functions.Func1;
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
    private final MediaSessionHolder mSessionHolder;
    private final IndexClient mIndexClient;
    private final LocalRenderer mLocalRenderer;
    private final ArtworkProviderHelper mArtworkHelper;

    private HandlerThread mHandlerThread;
    private PlaybackServiceProxy mProxy;
    private RendererServiceConnection mRendererConnection;
    private volatile IMusicRenderer mPlayback;

    private int mAudioSessionId;
    private Handler mHandler;
    private Scheduler mHandlerScheduler;

    //currently playing track
    private Track mCurrentTrack;
    //next track to load
    private Track mNextTrack;
    //true if we should start playing when loading finishes
    private boolean mPlayWhenReady;
    //
    private boolean mQueueReloaded;
    private boolean mQueueReady;
    //
    private boolean mServiceStarted = false;
    private volatile int mConnectedClients = 0;
    private boolean mRendererChanged;
    private long mSeekForNewRenderer;
    private int mInternalState;

    Subscription mCurrentTrackSub;
    Subscription mNextTrackSub;
    Subscription mArtworkSubscription;
    Subscription mPlayFromMediaIdSubscription;

    @Inject
    public PlaybackService(
            @ForApplication Context mContext,
            NotificationHelper2 mNotificationHelper,
            DelayedShutdownHandler mDelayedShutdownHandler,
            PlaybackQueue mQueue,
            PowerManager.WakeLock mWakeLock,
            MediaSessionHolder mSessionHolder,
            IndexClient mIndexClient,
            LocalRenderer mLocalRenderer,
            ArtworkProviderHelper mArtworkHelper
    ) {
        this.mContext = mContext;
        this.mNotificationHelper = mNotificationHelper;
        this.mDelayedShutdownHandler = mDelayedShutdownHandler;
        this.mQueue = mQueue;
        this.mWakeLock = mWakeLock;
        this.mSessionHolder = mSessionHolder;
        this.mIndexClient = mIndexClient;
        this.mLocalRenderer = mLocalRenderer;
        this.mArtworkHelper = mArtworkHelper;
    }

    //main thread
    public void onCreate(PlaybackServiceProxy proxy) {
        mProxy = proxy;

        //fire up thread and init handler
        mHandlerThread = new HandlerThread(PlaybackService.NAME, Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandlerScheduler = HandlerScheduler.from(mHandler);

        //tell everyone about ourselves
        mQueue.setListener(new PlaybackQueueQueueChangeListener());
        mSessionHolder.setCallback(new MediaSessionCallback(), mHandler);
        proxy.setSessionToken(mSessionHolder.getSessionToken());

        setupLocalRenderer();

        updatePlaybackState(null);

        mHandler.post(mLoadQueueRunnable);
    }

    //main thread
    public void onDestroy() {
        saveState(true); //fire early as possible

        RxUtils.unsubscribe(mCurrentTrackSub);
        RxUtils.unsubscribe(mNextTrackSub);
        RxUtils.unsubscribe(mArtworkSubscription);
        RxUtils.unsubscribe(mPlayFromMediaIdSubscription);

        mNotificationHelper.killNotification();
        mDelayedShutdownHandler.cancelDelayedShutdown();

        mPlayback.stop(false);

        releaseRendererConnection();

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
        //todo schedule shutdown;
    }

    //main thread
    public void onTrimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            mArtworkHelper.evictL1();
            saveState(true);
        }
    }

    //main thread
    public int onStartCommand(Intent intent, int flags, int startId) {
        acquireTempWakeLock();
        if (intent != null) {
            String action = intent.getAction();

            if (intent.hasExtra(NOW_IN_FOREGROUND)) {
                mNotificationHelper.setActivityInForeground(intent.getBooleanExtra(NOW_IN_FOREGROUND, false));
            }

            if (Intent.ACTION_MEDIA_BUTTON.equals(action) && intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
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
            controls.sendCustomAction(ACTION.TOGGLE_PLAYBACK, null);
        } else if (CMDPAUSE.equals(command) || PAUSE_ACTION.equals(action)) {
            controls.pause();
        } else if (CMDPLAY.equals(command)) {
            controls.play();
        } else if (CMDSTOP.equals(command) || STOP_ACTION.equals(action)) {
            controls.stop();
        } else if (REPEAT_ACTION.equals(action)) {
            controls.sendCustomAction(ACTION.CYCLE_REPEAT, null);
        } else if (SHUFFLE_ACTION.equals(action)) {
            controls.sendCustomAction(ACTION.TOGGLE_SHUFFLE_MODE, null);
        }
    }

    @DebugLog //handler thread
    void updatePlaybackState(String error) {
        updatePlaybackState(error, false);
    }

    //handler thread
    @SuppressWarnings("ResourceType")
    void updatePlaybackState(String error, boolean fromChecker) {
        int state = mPlayback.getState();
        Timber.d("updatePlaybackState(%s) err=%s",
                PlaybackStateHelper.stringifyState(state), error);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
            mNotificationHelper.showError(error);
        }

        setAvailableActions(stateBuilder);

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

        //TODO stop doing this
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

        if (PlaybackStateHelper.isLoading(state) || PlaybackStateHelper.isPlaying(state)) {
            acquireWakeLock();
        } else {
            acquireTempWakeLock();
        }

        mHandler.removeCallbacks(mProgressCheckRunnable);
        if (PlaybackStateHelper.isPlaying(state) && (!fromChecker || duration <= 0)) {
            //if not a schedule update recheck in 2 sec in case duration wasnt ready
            mHandler.postDelayed(mProgressCheckRunnable, 2000);
        }
    }

    //handler thread
    private void setAvailableActions(PlaybackStateCompat.Builder builder) {
        long actions = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                //| PlaybackState.ACTION_PLAY_FROM_SEARCH
                ;
        if (mQueue.notEmpty()) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
            if (mPlayback.isPlaying()) {
                actions |= PlaybackStateCompat.ACTION_PAUSE;
                actions &= ~PlaybackStateCompat.ACTION_PLAY;
                actions |= PlaybackStateCompat.ACTION_SEEK_TO;
            }
            if (mQueue.getPrevious() >= 0) {
                actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
            }
            if (mQueue.getNextPos() >= 0) {
                actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
            }
            builder.addCustomAction(
                    ACTION.TOGGLE_SHUFFLE_MODE, mContext.getString(R.string.menu_shuffle),
                    R.drawable.action_shuffle_black_36dp);
        }
        builder.setActions(actions);
    }

    long getCurrentSeekPosition() {
        PlaybackStateCompat state = mSessionHolder.getPlaybackState();
        if (state != null) {
            return PlaybackStateHelper.getAdjustedSeekPos(state);
        } else {
            return 0;
        }
    }

    //handler thread
    void updateMeta() {
        RxUtils.unsubscribe(mArtworkSubscription);
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
                        }
                        @Override public void onError(Throwable e) {
                            Timber.w(e, "getArtwork");
                        }
                        @Override public void onNext(Bitmap bitmap) {
                            mSessionHolder.setMetadata(new MediaMetadataCompat.Builder(meta)
                                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap).build());
                        }
                    });
        }
        if (mIndexClient.broadcastMeta()) {
            Timber.d("Broadcasting meta %s", MediaMetadataHelper.getDisplayName(meta));
            //For SimpleLastFmScrobbler
            final Intent musicIntent = new Intent(PlaybackConstants.MUSIC_META_CHANGED);
            musicIntent.putExtra("artist", MediaMetadataHelper.getArtistName(meta));
            musicIntent.putExtra("album", MediaMetadataHelper.getAlbumName(meta));
            musicIntent.putExtra("track", MediaMetadataHelper.getDisplayName(meta));
            musicIntent.putExtra("player", mContext.getString(R.string.app_name));
            musicIntent.putExtra("package", mContext.getPackageName());
            try {
                mContext.sendStickyBroadcast(musicIntent);
            } catch (SecurityException ignored) {
                Timber.e("Unable to broadcast sticky, disabling meta broadcasting");
                mIndexClient.setBroadcastMeta(false);
            }
        }
    }

    //handler thread / main thread
    void saveState(final boolean full) {
        final PlaybackQueue.Snapshot qSnapshot = mQueue.snapshot();
        final long seekPos = getCurrentSeekPosition();
        //Use async to avoid making new thread
        new AsyncTask<Object, Void, Void>() {
            @Override
            @DebugLog
            protected Void doInBackground(Object... params) {
                mIndexClient.startBatch();
                if (full) {
                    mIndexClient.saveQueue(qSnapshot.q);
                }
                mIndexClient.saveQueuePosition(qSnapshot.pos);
                mIndexClient.saveQueueRepeatMode(qSnapshot.repeat);
                mIndexClient.saveQueueShuffleMode(qSnapshot.shuffle);
                mIndexClient.saveLastSeekPosition(seekPos);
                mIndexClient.endBatch();
                return null;
            }
        }.execute();
    }

    void resetState() {
        mCurrentTrack = null;
        mNextTrack = null;
        mPlayWhenReady = false;
    }

    //we use our own releaser so we dont constantly reacquire the lock
    void acquireTempWakeLock() {
        acquireWakeLock();
        mHandler.postDelayed(mWakeLockReleaser, 60 * 1000);
    }

    void acquireWakeLock() {
        mHandler.removeCallbacks(mWakeLockReleaser);
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    void releaseWakeLock() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    void applyAudioEffects() {
        if (mAudioSessionId <= 0) {
            removeAudioEffects();
            return;
        }
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

    private void releaseRendererConnection() {
        if (mRendererConnection != null) {
            mRendererConnection.close();
            mRendererConnection = null;
        }
    }

    //handler thread / main thread
    @DebugLog
    void setupLocalRenderer() {
        if (mPlayback != null) {
            mPlayback.stop(false);
        }
        releaseRendererConnection();
        mPlayback = mLocalRenderer;
        initPlayback();
    }

    //handler thread
    @DebugLog
    void setupRemoteRenderer(@NonNull ComponentName componentName) {
        try {
            if (mPlayback != null) {
                mPlayback.stop(false);
                mPlayback = null;
            }
            releaseRendererConnection();
            mRendererConnection = bindRendererService(componentName);
            mPlayback = mRendererConnection.getService();
            initPlayback();
        } catch (InterruptedException e) {
            Timber.e(e, "setupRemoteRenderer");
            mPlayback = null;
            mRendererConnection = null;
            setupLocalRenderer();
        }
    }

    @DebugLog
    void initPlayback() {
        mPlayback.setState(PlaybackStateCompat.STATE_NONE);
        mPlayback.setCallback(new PlaybackCallback());
        mPlayback.setAccessor(new Accessor(this));
        mPlayback.start();
        if (mPlayback.getState() != PlaybackStateCompat.STATE_ERROR) {
            if (mPlayback.isRemotePlayback()) {
                mSessionHolder.setPlaybackToRemote(mPlayback.getVolumeProvider());
            } else {
                mSessionHolder.setPlaybackToLocal();
            }
        }
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
        if (mCurrentTrack != null && mCurrentTrack.getUri().equals(uri) && mPlayback.hasCurrent()) {
            Timber.e("Current track is up to date");
            if (mPlayWhenReady && !mPlayback.isPlaying()) {
                mPlayWhenReady = false;
                mPlayback.play();
            }
        }
        mHandler.removeCallbacks(mProgressCheckRunnable);
        mPlayback.prepareForTrack();
        RxUtils.unsubscribe(mCurrentTrackSub);
        mCurrentTrackSub = mIndexClient.getTrack(uri)
                .first()
                .observeOn(getScheduler())
                .subscribe(new Subscriber<Track>() {
                    @Override public void onCompleted() {
                    }
                    @Override public void onError(Throwable e) {
                        //will callback in here
                        mQueue.remove(mQueue.getCurrentPos());
                    }
                    @Override public void onNext(Track track) {
                        mCurrentTrack = track;
                        if (mPlayback.loadTrack(track.toBundle())) {
                            long seek = 0;
                            if (mQueueReloaded) {
                                mQueueReloaded = false;
                                seek = mIndexClient.getLastSeekPosition();
                            } else if (mRendererChanged) {
                                mRendererChanged = false;
                                seek = mSeekForNewRenderer;
                            }
                            if (seek > 0) {
                                mPlayback.seekTo(seek);
                            }
                            if (mPlayWhenReady) {
                                mPlayWhenReady = false;
                                mPlayback.play();
                            }
                            updateMeta();
                            setNextTrack();
                        } else {
                            Timber.e("Player rejected track %s ... moving to next", track.getUri());
                            mQueueReloaded = false;
                            //will call into onCurrentPosChanged
                            mQueue.remove(mQueue.getCurrentPos());
                        }
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
        RxUtils.unsubscribe(mNextTrackSub);
        mNextTrackSub = mIndexClient.getTrack(uri)
                .first()
                .observeOn(getScheduler())
                .subscribe(new Subscriber<Track>() {
                    @Override public void onCompleted() {
                    }
                    @Override public void onError(Throwable e) {
                        //will callback into onQueueChanged
                        mQueue.remove(mQueue.getNextPos());
                    }
                    @Override public void onNext(Track track) {
                        mNextTrack = track;
                        if (!mPlayback.loadNextTrack(track.toBundle())) {
                            Timber.e("Player rejected track %s ... skipping", track.getUri());
                            //will call into onQueueChanged
                            mQueue.remove(mQueue.getNextPos());
                        }
                    }
                });
    }

    class MediaSessionCallback implements IMediaSessionProxy.Callback {
        @Override
        @DebugLog
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
                case CMD.SWITCH_TO_NEW_RENDERER: {
                    ComponentName cn = BundleHelper.getParcelable(args);
                    boolean wasPlaying = mPlayback.isPlaying();
                    onPause();
                    if (cn != null) {
                        setupRemoteRenderer(cn);
                    } else {
                        setupLocalRenderer();
                    }
                    if (!PlaybackStateHelper.isError(mPlayback.getState())) {
                        mPlayWhenReady = cn != null || wasPlaying;//dont auto start localrenderer
                        if (mQueueReady) {
                            if (mQueue.notEmpty()) {
                                mRendererChanged = true;
                                mSeekForNewRenderer = getCurrentSeekPosition();
                                setTrack();
                            } //else ?? TODO
                        } //else wait for queue
                    } else {
                        mPlayWhenReady = false;
                        //and wait for callback
                    }
                    break;
                }
                case CMD.GET_CURRENT_RENDERER: {
                    if (cb != null) {
                        BundleHelper.Builder bob = BundleHelper.b();
                        if (mRendererConnection != null) {
                            cb.send(0, bob.putParcleable(mRendererConnection.getComponent()).get());
                        } else {
                            cb.send(0, bob.get());
                        }
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
        @DebugLog
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            onPause();
            if (StringUtils.equals(PlaybackConstants.MEDIA_ID_RAW_URI, mediaId)) {
                //TODO
            } else if (StringUtils.startsWith(mediaId, PlaybackConstants.MEDIA_ID_CONTAINER)
                    && BundleHelper.getUri(extras) != null) {
                RxUtils.unsubscribe(mPlayFromMediaIdSubscription);
                mPlayFromMediaIdSubscription = TypedBundleableLoader.<Track>create(mContext)
                        .setUri(BundleHelper.getUri(extras))
                        .setSortOrder(BundleHelper.getString(extras))
                        .createObservable()
                        .map(new Func1<List<Track>, List<Uri>>() {
                            @Override
                            public List<Uri> call(List<Track> tracks) {
                                List<Uri> uris = new ArrayList<Uri>(tracks.size());
                                for (Track t : tracks) {
                                    uris.add(t.getUri());
                                }
                                return uris;
                            }
                        })
                        .observeOn(getScheduler())
                        .subscribe(new Action1<List<Uri>>() {
                            @Override
                            public void call(List<Uri> uris) {
                                mPlayWhenReady = true;
                                mQueue.replace(uris);
                            }
                        });
            } else {
                //TODO
            }
        }

        @Override
        @DebugLog
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
                mInternalState = PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
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
            onPause();
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
                case ACTION.CYCLE_REPEAT: {
                    mQueue.toggleRepeat();
                    mSessionHolder.sendSessionEvent(EVENT.REPEAT_CHANGED,
                            BundleHelper.b().putInt(mQueue.getRepeatMode()).get());
                    updatePlaybackState(null);
                    break;
                }
                case ACTION.TOGGLE_SHUFFLE_MODE: {
                    mQueue.toggleShuffle();
                    mSessionHolder.sendSessionEvent(EVENT.QUEUE_SHUFFLED,
                            BundleHelper.b().putInt(mQueue.getShuffleMode()).get());
                    updatePlaybackState(null);
                    break;
                }
                case ACTION.ENQUEUE: {
                    int where = BundleHelper.getInt(extras);
                    List<Uri> list = BundleHelper.getList(extras);
                    if (where == PlaybackConstants.ENQUEUE_LAST) {
                        mQueue.addEnd(list);
                    } else if (where == PlaybackConstants.ENQUEUE_NEXT) {
                        mQueue.addNext(list);
                    }
                    break;
                }
                case ACTION.PLAY_ALL: {
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
                case ACTION.REMOVE_QUEUE_ITEM: {
                    Uri uri = BundleHelper.getUri(extras);
                    mQueue.remove(uri);
                    break;
                }
                case ACTION.REMOVE_QUEUE_ITEM_AT: {
                    int pos = BundleHelper.getInt(extras);
                    mQueue.remove(pos);
                    break;
                }
                case ACTION.CLEAR_QUEUE: {
                    mQueue.clear();
                    break;
                }
                case ACTION.MOVE_QUEUE_ITEM_TO: {
                    Uri uri = BundleHelper.getUri(extras);
                    int pos = BundleHelper.getInt(extras);
                    mQueue.moveItem(uri, pos);
                    break;
                }
                case ACTION.MOVE_QUEUE_ITEM: {
                    int from = BundleHelper.getInt(extras);
                    int to = BundleHelper.getInt2(extras);
                    mQueue.moveItem(from, to);
                    break;
                }
                case ACTION.MOVE_QUEUE_ITEM_TO_NEXT: {
                    int pos = BundleHelper.getInt(extras);
                    mQueue.moveItem(pos, mQueue.getNextPos());
                    break;
                }
                case ACTION.TOGGLE_PLAYBACK: {
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

    class PlaybackCallback implements IMusicRenderer.Callback {
        @Override
        public void onPlaybackStatusChanged(final int state) {
            if (Looper.myLooper() == getHandler().getLooper()) {
                updatePlaybackState(null);
            } else {
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        PlaybackCallback.this.onPlaybackStatusChanged(state);
                    }
                });
            }
        }

        @Override
        @DebugLog
        public void onCompletion() {
            if (Looper.myLooper() == getHandler().getLooper()) {
                mPlayWhenReady = false;
                if (mQueue.notEmpty()) {
                    //we've finished the list, go back to start
                    mQueue.goToItem(0);
                    updatePlaybackState(null);
                } else {
                    handleStop();
                }
            } else {
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        PlaybackCallback.this.onCompletion();
                    }
                });
            }
        }

        @Override
        @DebugLog
        public void onWentToNext() {
            if (Looper.myLooper() == getHandler().getLooper()) {
                //will call into moveToNext
                mQueue.moveToNext();
            } else {
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        PlaybackCallback.this.onWentToNext();
                    }
                });
            }
        }

        @Override
        @DebugLog
        public void onError(final String error) {
            if (Looper.myLooper() == getHandler().getLooper()) {
                updatePlaybackState(error);
                setupLocalRenderer();
                handleStop();
                //TODO better handle
            } else {
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        PlaybackCallback.this.onError(error);
                    }
                });
            }
        }

        @Override
        public void onAudioSessionId(final int audioSessionId) {
            if (Looper.myLooper() == getHandler().getLooper()) {
                mAudioSessionId = audioSessionId;
                applyAudioEffects();
                mSessionHolder.sendSessionEvent(EVENT.NEW_AUDIO_SESSION_ID,
                        BundleHelper.b().putInt(audioSessionId).get());
            } else {
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        PlaybackCallback.this.onAudioSessionId(audioSessionId);
                    }
                });
            }
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

    final Runnable mWakeLockReleaser = new Runnable() {
        @Override
        public void run() {
            releaseWakeLock();
        }
    };

    static final class RendererServiceConnection implements Closeable {
        private final Context context;
        private final ServiceConnection serviceConnection;
        private final IMusicRenderer service;
        private final ComponentName componentName;
        private RendererServiceConnection(
                Context context,
                ServiceConnection serviceConnection,
                IMusicRenderer service,
                ComponentName componentName
        ) {
            this.context = new ContextWrapper(context);
            this.serviceConnection = serviceConnection;
            this.service = service;
            this.componentName = componentName;
        }
        @Override public void close() {
            context.unbindService(serviceConnection);
        }
        public IMusicRenderer getService() {
            return service;
        }
        public ComponentName getComponent() {
            return componentName;
        }
    }

    RendererServiceConnection bindRendererService(ComponentName componentName) throws InterruptedException {
        ensureNotOnMainThread(mContext);
        final BlockingQueue<IMusicRenderer> q = new LinkedBlockingQueue<>(1);
        ServiceConnection keyChainServiceConnection = new ServiceConnection() {
            volatile boolean mConnectedAtLeastOnce = false;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (!mConnectedAtLeastOnce) {
                    mConnectedAtLeastOnce = true;
                    //Always one space available
                    q.offer((IMusicRenderer) service);
                }
            }
            @Override public void onServiceDisconnected(ComponentName name) {}
        };
        //noinspection ConstantConditions
        boolean isBound = mContext.bindService(new Intent().setComponent(componentName),
                keyChainServiceConnection,
                Context.BIND_AUTO_CREATE);
        if (!isBound) {
            throw new AssertionError("could not bind to KeyChainService");
        }
        return new RendererServiceConnection(mContext, keyChainServiceConnection, q.take(), componentName);
    }

    private static void ensureNotOnMainThread(Context context) {
        Looper looper = Looper.myLooper();
        if (looper != null && looper == context.getMainLooper()) {
            throw new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
        }
    }

    static class Accessor implements PlaybackServiceAccessor {
        final WeakReference<PlaybackService> mService;

        public Accessor(PlaybackService mService) {
            this.mService = new WeakReference<PlaybackService>(mService);
        }

        @Override
        public ParcelFileDescriptor getArtwork(Uri uri) {
            return mService.get().mArtworkHelper.getParcelFileDescriptior(uri);
        }

        @Override
        public MediaMetadataCompat convertTrackToMediaMetadata(Bundle trackBundle) {
            return mService.get().mIndexClient.convertToMediaMetadata(Track.BUNDLE_CREATOR.fromBundle(trackBundle));
        }
    }
}
