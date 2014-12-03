/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrew.apollo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.provider.MusicProvider;
import com.andrew.apollo.provider.MusicProviderUtil;
import com.andrew.apollo.provider.MusicStore;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.cast.callbacks.IMediaCastConsumer;
import org.opensilk.cast.exceptions.NoConnectionException;
import org.opensilk.cast.exceptions.TransientNetworkDisconnectionException;
import org.opensilk.cast.helpers.CastServiceConnectionCallback;
import org.opensilk.cast.helpers.LocalCastServiceManager;
import org.opensilk.cast.manager.MediaCastManager;
import org.opensilk.cast.util.CastPreferences;
import org.opensilk.music.BuildConfig;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.artwork.ArtworkProviderUtil;
import org.opensilk.music.ServiceBroadcastReceiver;
import org.opensilk.music.cast.CastWebServer;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.R;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * A backbround {@link Service} used to keep music playing between activities
 * and when the user moves Apollo into the background.
 */
public class MusicPlaybackService extends Service {
    private static final String TAG = "MusicPlaybackService";
    private static final boolean D = BuildConfig.DEBUG;

    public static final String APOLLO_PACKAGE_NAME = BuildConfig.APPLICATION_ID;

    /**
     * Indicates that the music has paused or resumed
     */
    public static final String PLAYSTATE_CHANGED = APOLLO_PACKAGE_NAME+".playstatechanged";

    /**
     * Indicates that music playback position within
     * a title was changed
     */
    public static final String POSITION_CHANGED = APOLLO_PACKAGE_NAME+".positionchanged";

    /**
     * Indicates the meta data has changed in some way, like a track change
     */
    public static final String META_CHANGED = APOLLO_PACKAGE_NAME+".metachanged";

    /**
     * Indicates the queue has been updated
     */
    public static final String QUEUE_CHANGED = APOLLO_PACKAGE_NAME+".queuechanged";

    /**
     * Indicates the repeat mode chaned
     */
    public static final String REPEATMODE_CHANGED = APOLLO_PACKAGE_NAME+".repeatmodechanged";

    /**
     * Indicates the shuffle mode chaned
     */
    public static final String SHUFFLEMODE_CHANGED = APOLLO_PACKAGE_NAME+".shufflemodechanged";

    /**
     * For backwards compatibility reasons, also provide sticky
     * broadcasts under the music package
     */
    public static final String MUSIC_PACKAGE_NAME = "com.android.music";

    /**
     * Called to indicate a general service commmand. Used in
     * {@link MediaButtonIntentReceiver}
     */
    public static final String SERVICECMD = APOLLO_PACKAGE_NAME+".musicservicecommand";

    /**
     * Called to go toggle between pausing and playing the music
     */
    public static final String TOGGLEPAUSE_ACTION = APOLLO_PACKAGE_NAME+".togglepause";

    /**
     * Called to go to pause the playback
     */
    public static final String PAUSE_ACTION = APOLLO_PACKAGE_NAME+".pause";

    /**
     * Called to go to stop the playback
     */
    public static final String STOP_ACTION = APOLLO_PACKAGE_NAME+".stop";

    /**
     * Called to go to the previous track
     */
    public static final String PREVIOUS_ACTION = APOLLO_PACKAGE_NAME+".previous";

    /**
     * Called to go to the next track
     */
    public static final String NEXT_ACTION = APOLLO_PACKAGE_NAME+".next";

    /**
     * Called to change the repeat mode
     */
    public static final String REPEAT_ACTION = APOLLO_PACKAGE_NAME+".repeat";

    /**
     * Called to change the shuffle mode
     */
    public static final String SHUFFLE_ACTION = APOLLO_PACKAGE_NAME+".shuffle";

    /**
     * Called to update the service about the foreground state of Apollo's activities
     */
    public static final String FOREGROUND_STATE_CHANGED = APOLLO_PACKAGE_NAME+".fgstatechanged";

    public static final String NOW_IN_FOREGROUND = "nowinforeground";

    public static final String FROM_MEDIA_BUTTON = "frommediabutton";

    /**
     * Used to easily notify a list that it should refresh. i.e. A playlist
     * changes
     */
    public static final String REFRESH = APOLLO_PACKAGE_NAME+".refresh";

    /**
     * Used by the alarm intent to shutdown the service after being idle
     */
    private static final String SHUTDOWN = APOLLO_PACKAGE_NAME+".shutdown";

    /**
     * Called to update the remote control client
     */
    public static final String UPDATE_LOCKSCREEN = APOLLO_PACKAGE_NAME+".updatelockscreen";

    public static final String CMDNAME = "command";

    public static final String CMDTOGGLEPAUSE = "togglepause";

    public static final String CMDSTOP = "stop";

    public static final String CMDPAUSE = "pause";

    public static final String CMDPLAY = "play";

    public static final String CMDPREVIOUS = "previous";

    public static final String CMDNEXT = "next";

    public static final String CMDNOTIF = "buttonId";

    private static final int IDCOLIDX = 0;

    /**
     * Moves a list to the front of the queue
     */
    public static final int NOW = 1;

    /**
     * Moves a list to the next position in the queue
     */
    public static final int NEXT = 2;

    /**
     * Moves a list to the last position in the queue
     */
    public static final int LAST = 3;

    /**
     * Shuffles no songs, turns shuffling off
     */
    public static final int SHUFFLE_NONE = 0;

    /**
     * Shuffles all songs
     */
    public static final int SHUFFLE_NORMAL = 1;

    /**
     * Party shuffle
     */
    public static final int SHUFFLE_AUTO = 2;

    /**
     * Turns repeat off
     */
    public static final int REPEAT_NONE = 0;

    /**
     * Repeats the current track in a list
     */
    public static final int REPEAT_CURRENT = 1;

    /**
     * Repeats all the tracks in a list
     */
    public static final int REPEAT_ALL = 2;


    /**
     * Idle time before stopping the foreground notfication (1 minute)
     */
    private static final int IDLE_DELAY = 60000 * 2;

    /**
     * Song play time used as threshold for rewinding to the beginning of the
     * track instead of skipping to the previous track when getting the PREVIOUS
     * command
     */
    private static final long REWIND_INSTEAD_PREVIOUS_THRESHOLD = 3000;

    /**
     * The max size allowed for the track history
     */
    static final int MAX_HISTORY_SIZE = 100;

    /**
     * Keeps a mapping of the track history
     */
    private final LinkedList<Integer> mHistory = Lists.newLinkedList();
    private final LinkedList<Integer> mAutoHistory = Lists.newLinkedList();
    /**
     * Used to shuffle the tracks
     */
    private final Shuffler mShuffler = new Shuffler();

    /**
     * Used to save the queue as reverse hexadecimal numbers, which we can
     * generate faster than normal decimal or hexadecimal numbers, which in
     * turn allows us to save the playlist more often without worrying too
     * much about performance
     */
    private static final char HEX_DIGITS[] = new char[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Service stub
     */
    private final IBinder mBinder;

    /**
     * The media player
     */
    private LocalMusicPlayer mPlayer;

    /**
     * the cast media player
     */
    private CastMusicPlayer mCastPlayer;

    /**
     * Keeps the service running when the screen is off
     */
    private WakeLock mWakeLock;

    /**
     * Alarm intent for removing the notification when nothing is playing
     * for some time
     */
    private AlarmManager mAlarmManager;
    private PendingIntent mShutdownIntent;
    private boolean mShutdownScheduled;

    /**
     * The cursor used to retrieve info on the current track and run the
     * necessary queries to play audio files
     */
    private Cursor mCursor;

    /**
     * Monitors the audio state
     */
    private AudioManager mAudioManager;

    /**
     * Settings used to save and retrieve the queue and history
     */
    private SharedPreferences mPreferences;

    /**
     * Used to know when the service is active
     */
    private int mConnectedClients = 0;

    /**
     * Used to know if something should be playing or not
     */
    private boolean mIsSupposedToBePlaying = false;

    /**
     * Used to indicate if the queue can be saved
     */
    private boolean mQueueIsSaveable = true;

    /**
     * Used to track what type of audio focus loss caused the playback to pause
     */
    private boolean mPausedByTransientLossOfFocus = false;

    /**
     * Used to track whether any of Apollo's activities is in the foreground
     */
    private boolean mAnyActivityInForeground = false;

    // We use this to distinguish between different cards when saving/restoring
    // playlists
    private int mCardId;

    private int mPlayListLen = 0;

    private int mPlayPos = -1;

    private int mNextPlayPos = -1;

    private long mLastKnowPosition = 0;

    private int mOpenFailedCounter = 0;

    private int mMediaMountedCount = 0;

    private int mShuffleMode = SHUFFLE_NONE;

    private int mRepeatMode = REPEAT_NONE;

    private long[] mPlayList = null;

    private long[] mAutoShuffleList = null;

    private MusicPlayerHandler mPlayerHandler;

    private BroadcastReceiver mUnmountReceiver = null;

    /**
     * Used to build the notification
     */
    private NotificationHelper mNotificationHelper;

    /**
     * handles mediasession on L and Remotecontrolclient on older apis
     */
    private MediaSessionHelper mMediaSessionHelper;

    /**
     * Proxy for artwork provider
     */
    private ArtworkProviderUtil mArtworkUtil;

    /**
     *
     */
    private boolean isCastingEnabled;

    /**
     * Token for SilkCastService
     */
    private LocalCastServiceManager.ServiceToken mCastServiceToken;

    /**
     * Cast manager
     */
    private MediaCastManager mCastManager;

    /**
     * Cast manager callbacks
     */
    private IMediaCastConsumer mCastConsumer;

    /**
     * Http server for service cast devices
     */
    private CastWebServer mCastServer;

    /**
     * indicates whether we are doing a local or a remote playback
     */
    public static enum PlaybackLocation {
        LOCAL,
        REMOTE
    }

    /**
     * Current playback location
     */
    private PlaybackLocation mPlaybackLocation = PlaybackLocation.LOCAL;

    /**
     * Fetches remote progress while we are in background
     */
    private Subscription mRemoteProgressSubscription;

    /**
     * Worker used to process intents
     */
    Scheduler.Worker mHandleIntentWorker;

    public MusicPlaybackService() {
        super();
        mBinder = new ApolloServiceBinder(this);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        if (D) Log.d(TAG, "Service bound, intent = " + intent);
        cancelShutdown();
        mConnectedClients++;
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        if (D) Log.d(TAG, "Service unbound");
        mConnectedClients--;
        saveQueue(true);

        if (mIsSupposedToBePlaying || mPausedByTransientLossOfFocus) {
            // Something is currently playing, or will be playing once
            // an in-progress action requesting audio focus ends, so don't stop
            // the service now.
            return true;

            // If there is a playlist but playback is paused, then wait a while
            // before stopping the service, so that pause/resume isn't slow.
            // Also delay stopping the service if we're transitioning between
            // tracks.
        } else if (mPlayListLen > 0 || mPlayerHandler.hasMessages(MusicPlayerHandler.TRACK_ENDED)) {
            scheduleDelayedShutdown();
            return true;
        }
        stopSelf();
        return true;
    }

    @Override
    public void onRebind(final Intent intent) {
        cancelShutdown();
        mConnectedClients++;
    }

    @Override
    public void onCreate() {
        if (D) Log.d(TAG, "Creating service");
        super.onCreate();

        // Initialize the notification helper
        mNotificationHelper = new NotificationHelper(this);

        // Create artwork cache
        mArtworkUtil = new ArtworkProviderUtil(this);

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work will not disrupt the UI.
        final HandlerThread thread = new HandlerThread("MusicPlayerHandler",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Initialize the handler
        mPlayerHandler = new MusicPlayerHandler(this, thread.getLooper());

        mHandleIntentWorker = Schedulers.newThread().createWorker();

        // Initialize the audio manager and register any headset controls for
        // playback
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        mMediaSessionHelper = new MediaSessionHelper(this);
        mMediaSessionHelper.setup();

        // Initialize the preferences
        mPreferences = getSharedPreferences("Service", 0);
        mCardId = getCardId();

        registerExternalStorageListener();

        // Initialize the media player
        mPlayer = new LocalMusicPlayer(this);
        mPlayer.setHandler(mPlayerHandler);

        isCastingEnabled = CastPreferences.getBoolean(this, CastPreferences.KEY_CAST_ENABLED, true);
        if (isCastingEnabled) {
            // Bind to the cast service
            mCastServiceToken = LocalCastServiceManager.bindToService(this, mCastServiceConnectionCallback);
        }

        // Initialize the intent filter and each action
        final IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICECMD);
        filter.addAction(TOGGLEPAUSE_ACTION);
        filter.addAction(PAUSE_ACTION);
        filter.addAction(STOP_ACTION);
        filter.addAction(NEXT_ACTION);
        filter.addAction(PREVIOUS_ACTION);
        filter.addAction(REPEAT_ACTION);
        filter.addAction(SHUFFLE_ACTION);
        // Attach the broadcast listener
        registerReceiver(mIntentReceiver, filter);

        // Initialize the wake lock
        final PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.setReferenceCounted(false);

        // Initialize the delayed shutdown intent
        final Intent shutdownIntent = new Intent(this, MusicPlaybackService.class);
        shutdownIntent.setAction(SHUTDOWN);

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mShutdownIntent = PendingIntent.getService(this, 0, shutdownIntent, 0);

        // Listen for the idle state
        scheduleDelayedShutdown();

        // Bring the queue back
        reloadQueue();
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);
    }

    @Override
    public void onDestroy() {
        if (D) Log.d(TAG, "Destroying service");
        mConnectedClients = 0;

        // Remove any sound effects
        final Intent audioEffectsIntent = new Intent(
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(audioEffectsIntent);

        // remove any pending alarms
        mAlarmManager.cancel(mShutdownIntent);

        // Release the player
        mPlayer.release();
        mPlayer = null;

        // Remove the audio focus listener and lock screen controls
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mMediaSessionHelper.teardown();

        // Remove any callbacks from the handler
        mPlayerHandler.removeCallbacksAndMessages(null);
        // Kill player thread
        mPlayerHandler.getLooper().quit();

        //release the worker
        mHandleIntentWorker.unsubscribe();

        // Close the cursor
        closeCursor();

        // Unregister the mount listener
        unregisterReceiver(mIntentReceiver);
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
            mUnmountReceiver = null;
        }

        // Unregister with cast manager
        if (mCastManager != null) {
            // kill the remote app.
            try {
                mCastManager.stopApplication();
            } catch (Exception ignored) { /*pass*/ }
            mCastManager.removeCastConsumer(mCastConsumer);
            mCastManager = null;
        }

        if (mRemoteProgressSubscription != null) {
            mRemoteProgressSubscription.unsubscribe();
            mRemoteProgressSubscription = null;
        }

        if (mCastPlayer != null) {
            mCastPlayer.release();
            mCastPlayer = null;
        }

        if (mCastServiceToken != null) {
            // Unbind cast service
            LocalCastServiceManager.unbindFromService(mCastServiceToken);
        }

        // Stop http server
        stopCastServer();

        // Release the wake lock
        releaseWakeLock();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (D) Log.d(TAG, "Got new intent " + intent + ", startId = " + startId);

        if (intent != null) {
            final String action = intent.getAction();

            if (intent.hasExtra(NOW_IN_FOREGROUND)) {
                mAnyActivityInForeground = intent.getBooleanExtra(NOW_IN_FOREGROUND, false);
                updateNotification();
                      // look and see if we were just disconnected
//                    mCastManager.reconnectSessionIfPossible(this, false, 2);
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        updateCastManagerUiCounter(mAnyActivityInForeground);
                    }
                });
            }

            if (SHUTDOWN.equals(action)) {
                mShutdownScheduled = false;
                releaseServiceUiAndStop();
                return START_NOT_STICKY;
            }

            mHandleIntentWorker.schedule(new Action0() {
                @Override
                public void call() {
                    handleCommandIntent(intent);
                }
            });
        }

        // Make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        scheduleDelayedShutdown();

        if (intent != null && intent.getBooleanExtra(FROM_MEDIA_BUTTON, false)) {
            MediaButtonIntentReceiver.completeWakefulIntent(intent);
        }

        return START_STICKY;
    }

    private void updateCastManagerUiCounter(final boolean visible) {
        if (!isCastingEnabled) {
            return;
        }
        if (mCastManager != null) {
            if (visible) {
                mCastManager.incrementUiCounter();
                if (mRemoteProgressSubscription != null) {
                    mRemoteProgressSubscription.unsubscribe();
                    mRemoteProgressSubscription =null;
                }
            } else {
                mCastManager.decrementUiCounter();
                if (mRemoteProgressSubscription == null
                        || mRemoteProgressSubscription.isUnsubscribed()) {
                    mRemoteProgressSubscription = createRemoteProgressHandler();
                }
            }
        } else {
            // if we haven't bound to the cast service yet we will wait a little and try again.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateCastManagerUiCounter(visible);
                }
            }, 100);
        }
    }

    private void releaseServiceUiAndStop() {
        if (isPlaying()
                || mPausedByTransientLossOfFocus
                || mPlayerHandler.hasMessages(MusicPlayerHandler.TRACK_ENDED)) {
            return;
        }

        if (D) Log.d(TAG, "Nothing is playing anymore, releasing notification");
        mNotificationHelper.killNotification();
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        if (mRemoteProgressSubscription != null) {
            mRemoteProgressSubscription.unsubscribe();
            mRemoteProgressSubscription = null;
        }

        if (mConnectedClients <= 0) {
            saveQueue(true);
            stopSelf();
        }
    }

    private void handleCommandIntent(Intent intent) {
        final String action = intent.getAction();
        final String command = SERVICECMD.equals(action) ? intent.getStringExtra(CMDNAME) : null;

        if (D) Log.d(TAG, "handleCommandIntent: action = " + action + ", command = " + command);

        if (CMDNEXT.equals(command) || NEXT_ACTION.equals(action)) {
            gotoNext(true);
        } else if (CMDPREVIOUS.equals(command) || PREVIOUS_ACTION.equals(action)) {
            if (position() < REWIND_INSTEAD_PREVIOUS_THRESHOLD) {
                prev();
            } else {
                seekAndPlay(0);
            }
        } else if (CMDTOGGLEPAUSE.equals(command) || TOGGLEPAUSE_ACTION.equals(action)) {
            if (isPlaying()) {
                pause();
                setPausedByTransientLossOfFocus(false);
            } else {
                play();
            }
        } else if (CMDPAUSE.equals(command) || PAUSE_ACTION.equals(action)) {
            pause();
            setPausedByTransientLossOfFocus(false);
        } else if (CMDPLAY.equals(command)) {
            play();
        } else if (CMDSTOP.equals(command) || STOP_ACTION.equals(action)) {
            pause();
            seek(0);
            setPausedByTransientLossOfFocus(false);
            releaseServiceUiAndStop();
        } else if (REPEAT_ACTION.equals(action)) {
            cycleRepeat();
        } else if (SHUFFLE_ACTION.equals(action)) {
            cycleShuffle();
        }
    }

    /**
     * Updates the notification, considering the current play and activity state
     */
    private void updateNotification() {
        if (!mAnyActivityInForeground && isPlaying()) {
            mNotificationHelper.buildNotification(getAlbumName(), getArtistName(),
                    getTrackName(), getAlbumArtThumbnail(), isPlaying(), mMediaSessionHelper.getSessionToken());
        } else if (mAnyActivityInForeground) {
            mNotificationHelper.killNotification();
        }
    }

    /**
     * releases wakelock
     */
    void releaseWakeLock() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    /**
     * acquires wakelock
     */
    void acquireWakeLock() {
        mWakeLock.acquire();
    }

    void acquireWakeLock(long milli) {
        mWakeLock.acquire(milli);
    }

    /**
     * Set if received message with AUDIOFOCUS_LOSS_TRANSIENT
     * @param yes
     */
    void setPausedByTransientLossOfFocus(boolean yes) {
        synchronized (this) {
            mPausedByTransientLossOfFocus = yes;
        }
    }

    /**
     * Used by handler to determine if playback should resume after focus lost
     * @return
     */
    boolean isPausedByTransientLossOfFocus() {
        return mPausedByTransientLossOfFocus;
    }

    /**
     * Fetches the current player
     * @return
     */
    IMusicPlayer getPlayer() {
        if (isRemotePlayback()) {
            if (mCastPlayer != null) {
                return mCastPlayer;
            } else {
                Timber.e("Wanted remote playback but cast player is null!");
                setPlaybackLocation(PlaybackLocation.LOCAL);
            }
        }
        return mPlayer;
    }

    void switchToCastPlayer() {
        Timber.d("switchToCastPlayer");
        startCastServer();
        if (mPlayer.isInitialized()) {
            mLastKnowPosition = mPlayer.position();
        }
        if (isRemotePlayback()) {
            setPlaybackLocation(PlaybackLocation.LOCAL);
        }
        stop(false);
        setPlaybackLocation(PlaybackLocation.REMOTE);
        openCurrentAndNext();
        if (mLastKnowPosition > 0) {
            seekAndPlay(mLastKnowPosition);
        } else {
            play();
        }
    }

    void switchToLocalPlayer() {
        Timber.d("switchToLocalPlayer");
        if (getPlayer() == mPlayer) {
            Timber.w("Current player is local player");
            return;
        }
        if (mCastPlayer != null && mCastPlayer.isInitialized() && mCastPlayer.isConnected()) {
            mLastKnowPosition = mCastPlayer.position();
        }
        if (!isRemotePlayback()) {
            setPlaybackLocation(PlaybackLocation.REMOTE);
        }
        stop(false);
        setPlaybackLocation(PlaybackLocation.LOCAL);
        openCurrentAndNext();
        if (mLastKnowPosition > 0) {
            seek(mLastKnowPosition);
        }
        stopCastServer();
    }

    void setPlaybackLocation(PlaybackLocation location) {
        mPlaybackLocation = location;
    }

    PlaybackLocation getPlaybackLocation() {
        return mPlaybackLocation;
    }

    void setSupposedToBePlaying(boolean playing) {
        mIsSupposedToBePlaying = playing;
    }

    boolean isSupposedToBePlaying() {
        return mIsSupposedToBePlaying;
    }

    void recoverFromTransientDisconnect() {
        updatePlaybackLocation(PlaybackLocation.REMOTE);
        if (mCastServer == null) {
            startCastServer();
        }
        if (mCastManager.isConnected()) {
            try {
                if (mCastManager.isRemoteMediaLoaded() && mCastManager.isRemoteMediaPlaying()) {
                    Log.d(TAG, "onConnectivityRecovered: remote media currently playing");
                    position();// force update the local position
                    if (mRemoteProgressSubscription == null
                            || mRemoteProgressSubscription.isUnsubscribed()) {
                        // restart progress updater if needed
                        mRemoteProgressSubscription = createRemoteProgressHandler();
                    }
                } else {
                    switchToCastPlayer();
                }
            } catch (TransientNetworkDisconnectionException e) {
                Log.w(TAG, "onConnectivityRecovered(1) TransientNetworkDisconnection");
            } catch (NoConnectionException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "onConnectivityRecoverd: Whoa, we arent connected??");
        }
    }

    /**
     * @return A card ID used to save and restore playlists, i.e., the queue.
     */
    private int getCardId() {
        final ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(Uri.parse("content://media/external/fs_id"), null, null,
                null, null);
        int mCardId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            mCardId = cursor.getInt(0);
            cursor.close();
            cursor = null;
        }
        return mCardId;
    }

    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     *
     * @param storagePath The path to mount point for the removed media
     */
    public void closeExternalStorageFiles(final String storagePath) {
        stop(true);
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications. The
     * intent will call closeExternalStorageFiles() if the external media is
     * going to be ejected, so applications can clean up any files they have
     * open.
     */
    public void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    final String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        saveQueue(true);
                        mQueueIsSaveable = false;
                        closeExternalStorageFiles(intent.getData().getPath());
                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        mMediaMountedCount++;
                        mCardId = getCardId();
                        reloadQueue();
                        mQueueIsSaveable = true;
                        notifyChange(QUEUE_CHANGED);
                        notifyChange(META_CHANGED);
                    }
                }
            };
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_EJECT);
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addDataScheme("file");
            registerReceiver(mUnmountReceiver, filter);
        }
    }

    private void scheduleDelayedShutdown() {
        if (D) Log.v(TAG, "Scheduling shutdown in " + IDLE_DELAY + " ms");
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + IDLE_DELAY, mShutdownIntent);
        mShutdownScheduled = true;
    }

    private void cancelShutdown() {
        if (D) Log.d(TAG, "Cancelling delayed shutdown, scheduled = " + mShutdownScheduled);
        if (mShutdownScheduled) {
            mAlarmManager.cancel(mShutdownIntent);
            mShutdownScheduled = false;
        }
    }

    /**
     * Stops playback
     *
     * @param goToIdle True to go to the idle state, false otherwise
     */
    private void stop(final boolean goToIdle) {
        if (D) Log.d(TAG, "Stopping playback, goToIdle = " + goToIdle);
        final IMusicPlayer player = getPlayer();
        if (player != null && player.isInitialized()) {
            player.stop(goToIdle);
        }
        synchronized (this) {
            closeCursor();
            if (goToIdle) {
                scheduleDelayedShutdown();
                mIsSupposedToBePlaying = false;
            } else {
                stopForeground(false);
            }
        }
    }

    /**
     * Removes the range of tracks specified from the play list. If a file
     * within the range is the file currently being played, playback will move
     * to the next file after the range.
     *
     * @param first The first file to be removed
     * @param last The last file to be removed
     * @return the number of tracks deleted
     */
    private int removeTracksInternal(int first, int last) {
        synchronized (this) {
            if (last < first) return 0;
            if (first < 0) first = 0;
            if (last >= mPlayListLen) last = mPlayListLen - 1;

            boolean gotonext = false;
            if (first <= mPlayPos && mPlayPos <= last) {
                mPlayPos = first;
                gotonext = true;
            } else if (mPlayPos > last) {
                mPlayPos -= last - first + 1;
            }
            final int num = mPlayListLen - last - 1;
            for (int i = 0; i < num; i++) {
                mPlayList[first + i] = mPlayList[last + 1 + i];
            }
            mPlayListLen -= last - first + 1;

            if (gotonext) {
                if (mPlayListLen == 0) {
                    stop(true);
                    mPlayPos = -1;
                    closeCursor();
                } else {
                    if (mShuffleMode != SHUFFLE_NONE) {
                        mPlayPos = getNextPosition(true);
                        if (mShuffleMode == SHUFFLE_AUTO) {
                            mPlayPos--;//Removing current track will cause skip
                        }
                    } else if (mPlayPos >= mPlayListLen) {
                        mPlayPos = 0;
                    }
                    final boolean wasPlaying = isPlaying();
                    stop(false);
                    openCurrentAndNext();
                    if (wasPlaying) {
                        play();
                    }
                }
                notifyChange(META_CHANGED);
            } else {
                setNextTrack();
            }
            return last - first + 1;
        }
    }

    /**
     * Adds a list to the playlist
     *
     * @param list The list to add
     * @param position The position to place the tracks
     */
    private void addToPlayList(final long[] list, int position) {
        final int addlen = list.length;
        if (position < 0) {
            mPlayListLen = 0;
            position = 0;
        }
        ensurePlayListCapacity(mPlayListLen + addlen);
        if (position > mPlayListLen) {
            position = mPlayListLen;
        }

        final int tailsize = mPlayListLen - position;
        for (int i = tailsize; i > 0; i--) {
            mPlayList[position + i] = mPlayList[position + i - addlen];
        }

        for (int i = 0; i < addlen; i++) {
            mPlayList[position + i] = list[i];
        }
        mPlayListLen += addlen;
        if (mPlayListLen == 0) {
            closeCursor();
            notifyChange(META_CHANGED);
        }
    }

    private void ensureCursor() {
        if (mCursor == null || mCursor.isClosed()) {
            if (getAudioId() >= 0) {
                updateCursor(getAudioId());
            }
        }
    }

    /**
     * @param trackId The track ID
     */
    private void updateCursor(final long trackId) {
        updateCursor(MusicProvider.RECENTS_URI, BaseColumns._ID + "=?", new String[] {String .valueOf(trackId)});
    }

    private void updateCursor(final Uri uri, final String selection, final String[] selectionArgs) {
        if (MusicProvider.RECENTS_URI.equals(uri)) {
            synchronized (this) {
                closeCursor();
                mCursor = openCursorAndGoToFirst(uri, Projections.RECENT_SONGS, selection, selectionArgs);
            }
        } else {
            Cursor c = openCursorAndGoToFirst(uri, Projections.LOCAL_SONG, selection, selectionArgs);
            if (c != null) {
                try {
                    long id = MusicProviderUtil.insertSong(this, CursorHelpers.makeLocalSongFromCursor(c));
                    updateCursor(id);
                } finally {
                     c.close();
                }
            }
        }
    }

    private Cursor openCursorAndGoToFirst(Uri uri, String[] projection,
            String selection, String[] selectionArgs) {
        Cursor c = getContentResolver().query(uri, projection,
                selection, selectionArgs, null);
        if (c == null) {
            return null;
        }
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
     }

    private void closeCursor() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    /**
     * Called to open a new file as the current track and prepare the next for
     * playback
     */
    void openCurrentAndNext() {
        openCurrentAndMaybeNext(true);
    }

    /**
     * Called to open a new file as the current track and prepare the next for
     * playback
     *
     * @param openNext True to prepare the next track for playback, false
     *            otherwise.
     */
    void openCurrentAndMaybeNext(final boolean openNext) {
        synchronized (this) {
            closeCursor();

            if (mPlayListLen == 0) {
                return;
            }
            stop(false);

            updateCursor(mPlayList[mPlayPos]);
            while (true) {
                if (mCursor != null && openFile(getDataUri().toString())) { //TODO use Uri
                    break;
                }
                // if we get here then opening the file failed. We can close the
                // cursor now, because
                // we're either going to create a new one next, or stop trying
                closeCursor();
                if (mOpenFailedCounter++ < 10 && mPlayListLen > 1) {
                    final int pos = getNextPosition(false);
                    if (pos < 0) {
                        scheduleDelayedShutdown();
                        if (mIsSupposedToBePlaying) {
                            mIsSupposedToBePlaying = false;
                            notifyChange(PLAYSTATE_CHANGED);
                        }
                        return;
                    }
                    mPlayPos = pos;
                    stop(false);
                    mPlayPos = pos;
                    updateCursor(mPlayList[mPlayPos]);
                } else {
                    mOpenFailedCounter = 0;
                    Log.w(TAG, "Failed to open file for playback");
                    scheduleDelayedShutdown();
                    if (mIsSupposedToBePlaying) {
                        mIsSupposedToBePlaying = false;
                        notifyChange(PLAYSTATE_CHANGED);
                    }
                    return;
                }
            }
            if (openNext) {
                setNextTrack();
            }
        }
    }

    /**
     * @param force True to force the player onto the track next, false
     *            otherwise.
     * @return The next position to play.
     */
    private int getNextPosition(final boolean force) {
        if (mRepeatMode == REPEAT_CURRENT) {
            if (mPlayPos < 0) {
                return 0;
            }
            return mPlayPos;
        } else if (mShuffleMode == SHUFFLE_NORMAL) {
            if (mPlayPos >= 0) {
                mHistory.add(mPlayPos);
            }
            if (mHistory.size() > MAX_HISTORY_SIZE) {
                mHistory.remove(0);
            }
            final int numTracks = mPlayListLen;
            final int[] tracks = new int[numTracks];
            for (int i = 0; i < numTracks; i++) {
                tracks[i] = i;
            }

            final int numHistory = mHistory.size();
            int numUnplayed = numTracks;
            for (int i = 0; i < numHistory; i++) {
                final int idx = mHistory.get(i);
                if (idx < numTracks && tracks[idx] >= 0) {
                    numUnplayed--;
                    tracks[idx] = -1;
                }
            }
            if (numUnplayed <= 0) {
                if (mRepeatMode == REPEAT_ALL || force) {
                    numUnplayed = numTracks;
                    for (int i = 0; i < numTracks; i++) {
                        tracks[i] = i;
                    }
                } else {
                    return -1;
                }
            }
            int skip = mShuffler.nextInt(numUnplayed);
            int cnt = -1;
            while (true) {
                while (tracks[++cnt] < 0) {
                    ;
                }
                skip--;
                if (skip < 0) {
                    break;
                }
            }
            return cnt;
        } else if (mShuffleMode == SHUFFLE_AUTO) {
            doAutoShuffleUpdate();
            return mPlayPos + 1;
        } else {
            if (mPlayPos >= mPlayListLen - 1) {
                if (mRepeatMode == REPEAT_NONE && !force) {
                    return -1;
                } else if (mRepeatMode == REPEAT_ALL || force) {
                    return 0;
                }
                return -1;
            } else {
                return mPlayPos + 1;
            }
        }
    }

    /**
     * Sets the track track to be played
     */
    private void setNextTrack() {
        synchronized (this) {
            mNextPlayPos = getNextPosition(false);
            if (D) Log.d(TAG, "setNextTrack: next play position = " + mNextPlayPos);
            final IMusicPlayer player = getPlayer();
            if (mNextPlayPos >= 0 && mPlayList != null) {
                final long id = mPlayList[mNextPlayPos];
                if (player != null) player.setNextDataSource(id);
            } else {
                if (player != null) player.setNextDataSource(null);
            }
        }
    }

    /**
     * called when player has gone no the next track
     */
    void wentToNext() {
        synchronized (this) {
            mPlayPos = mNextPlayPos;
            closeCursor();
            if (mPlayPos >= 0 && mPlayPos < mPlayList.length) {
                updateCursor(mPlayList[mPlayPos]);
            }
            notifyChange(META_CHANGED);
            updateNotification();
            setNextTrack();
        }
    }

    /**
     * Creates a shuffled playlist used for party mode
     */
    private boolean makeAutoShuffleList() {
        Cursor cursor = null;
        try {
            cursor = CursorHelpers.getCursorForAutoShuffle(this);
            if (cursor == null || cursor.getCount() == 0) {
                return false;
            }
            final int len = cursor.getCount();
            final long[] list = new long[len];
            for (int i = 0; i < len; i++) {
                cursor.moveToNext();
                list[i] = cursor.getLong(0);
            }
            mAutoShuffleList = list;
            return true;
        } catch (final RuntimeException e) {
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return false;
    }

    /**
     * Creates the party shuffle playlist
     */
    private void doAutoShuffleUpdate() {
        boolean notify = false;
        if (mPlayPos > 10) {
            removeTracks(0, mPlayPos - 9);
            notify = true;
        }
        final int toAdd = 7 - (mPlayListLen - (mPlayPos < 0 ? -1 : mPlayPos));
        if (toAdd > 0) {
            final long[] list = new long[toAdd];
            for (int i = 0; i < toAdd; i++) {
                int lookback = mAutoHistory.size();
                int idx = -1;
                while (true) {
                    idx = mShuffler.nextInt(mAutoShuffleList.length);
                    if (!wasRecentlyUsed(idx, lookback)) {
                        break;
                    }
                    lookback /= 2;
                }
                mAutoHistory.add(idx);
                if (mAutoHistory.size() > MAX_HISTORY_SIZE) {
                    mAutoHistory.remove(0);
                }
                list[i] = mAutoShuffleList[idx];
            }
            ensurePlayListCapacity(mPlayListLen + list.length);
            Song[] songs = CursorHelpers.getSongsFromId(this, list);
            for (Song s : songs) {
                long id = MusicProviderUtil.insertSong(this, s);
                mPlayList[mPlayListLen++] = id;
            }
            notify = true;
        }

        if (notify) {
            notifyChange(QUEUE_CHANGED);
        }
    }

    /**/
    private boolean wasRecentlyUsed(final int idx, int lookbacksize) {
        if (lookbacksize == 0) {
            return false;
        }
        final int histsize = mAutoHistory.size();
        if (histsize < lookbacksize) {
            lookbacksize = histsize;
        }
        final int maxidx = histsize - 1;
        for (int i = 0; i < lookbacksize; i++) {
            final long entry = mAutoHistory.get(maxidx - i);
            if (entry == idx) {
                return true;
            }
        }
        return false;
    }

    /**
     * Makes sure the playlist has enough space to hold all of the songs
     *
     * @param size The size of the playlist
     */
    private void ensurePlayListCapacity(final int size) {
        if (mPlayList == null || size > mPlayList.length) {
            // reallocate at 2x requested size so we don't
            // need to grow and copy the array for every
            // insert
            final long[] newlist = new long[size * 2];
            if (mPlayList != null) {
                final int len = mPlayList.length;
                System.arraycopy(mPlayList, 0, newlist, 0, len);
            }
            mPlayList = newlist;
        }
        // FIXME: shrink the array when the needed size is much smaller
        // than the allocated size
    }

    /**
     * Notify the change-receivers that something has changed.
     */
    void notifyChange(final String what) {
        if (D) Log.d(TAG, "notifyChange: what = " + what);

        // Update the lockscreen controls
        mMediaSessionHelper.updateMeta(what);

        if (what.equals(POSITION_CHANGED)) {
            return;
        }

        //For activities
        final Intent intent = new Intent(what);
        intent.putExtra("id", getAudioId());
        intent.putExtra("artist", getArtistName());
        intent.putExtra("album", getAlbumName());
        intent.putExtra("track", getTrackName());
        intent.putExtra("playing", isPlaying());
        sendStickyBroadcast(intent);

        //For SimpleLastFmScrobbler
        final Intent musicIntent = new Intent(intent);
        musicIntent.setAction(what.replace(APOLLO_PACKAGE_NAME, MUSIC_PACKAGE_NAME));
        musicIntent.putExtra("player", getString(R.string.app_name));
        musicIntent.putExtra("package", getPackageName());
        sendStickyBroadcast(musicIntent);

        //notify widgets
        final Intent widgetIntent = new Intent(intent);
        widgetIntent.setComponent(new ComponentName(this, ServiceBroadcastReceiver.class));
        sendBroadcast(widgetIntent);

        if (what.equals(META_CHANGED)) {
            // Add the track to the recently played list.
            MusicProviderUtil.updatePlaycount(this, getAudioId());
        } else if (what.equals(QUEUE_CHANGED)) {
            saveQueue(true);
        } else {
            saveQueue(false);
        }

        if (what.equals(PLAYSTATE_CHANGED)) {
            mNotificationHelper.updatePlayState(isPlaying());
        }
    }

    /**
     * Saves the queue
     *
     * @param full True if the queue is full
     */
    private void saveQueue(final boolean full) {
        if (!mQueueIsSaveable) {
            return;
        }

        final SharedPreferences.Editor editor = mPreferences.edit();
        if (full) {
            final StringBuilder q = new StringBuilder();
            int len = mPlayListLen;
            for (int i = 0; i < len; i++) {
                long n = mPlayList[i];
                if (n < 0) {
                    continue;
                } else if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        final int digit = (int)(n & 0xf);
                        n >>>= 4;
                        q.append(HEX_DIGITS[digit]);
                    }
                    q.append(";");
                }
            }
            editor.putString("queue", q.toString());
            editor.putInt("cardid", mCardId);
            // save shuffle history
            len = mHistory.size();
            if (len > 0) {
                q.setLength(0);
                for (int i = 0; i < len; i++) {
                    int n = mHistory.get(i);
                    if (n == 0) {
                        q.append("0;");
                    } else {
                        while (n != 0) {
                            final int digit = n & 0xf;
                            n >>>= 4;
                            q.append(HEX_DIGITS[digit]);
                        }
                        q.append(";");
                    }
                }
                editor.putString("history", q.toString());
            } else {
                editor.remove("history");
            }
            // save autoshuffle history
            len = mAutoHistory.size();
            if (len > 0) {
                q.setLength(0);
                for (int i = 0; i < len; i++) {
                    int n = mAutoHistory.get(i);
                    if (n == 0) {
                        q.append("0;");
                    } else {
                        while (n != 0) {
                            final int digit = n & 0xf;
                            n >>>= 4;
                            q.append(HEX_DIGITS[digit]);
                        }
                        q.append(";");
                    }
                }
                editor.putString("autohistory", q.toString());
            } else {
                editor.remove("autohistory");
            }
        }
        editor.putInt("curpos", mPlayPos);
        final IMusicPlayer player = getPlayer();
        if (player != null && player.isInitialized()) {
            editor.putLong("seekpos", player.position());
        }
        editor.putInt("repeatmode", mRepeatMode);
        editor.putInt("shufflemode", mShuffleMode);
        editor.putInt("schemaversion", PREF_VERSION);
        editor.apply();
    }

    /**
     * Increment value when pref schema changes
     */
    private static final int PREF_VERSION = 2;

    /**
     * Reloads the queue as the user left it the last time they stopped using
     * Apollo
     */
    private void reloadQueue() {
        int ver = mPreferences.getInt("schemaversion", 0);
        if (ver < PREF_VERSION) {
            return;
        }
        String q = null;
        int id = mCardId;
        if (mPreferences.contains("cardid")) {
            id = mPreferences.getInt("cardid", ~mCardId);
        }
        if (id == mCardId) {
            q = mPreferences.getString("queue", "");
        }
        int qlen = q != null ? q.length() : 0;
        if (qlen > 1) {
            int plen = 0;
            int n = 0;
            int shift = 0;
            for (int i = 0; i < qlen; i++) {
                final char c = q.charAt(i);
                if (c == ';') {
                    ensurePlayListCapacity(plen + 1);
                    mPlayList[plen] = n;
                    plen++;
                    n = 0;
                    shift = 0;
                } else {
                    if (c >= '0' && c <= '9') {
                        n += c - '0' << shift;
                    } else if (c >= 'a' && c <= 'f') {
                        n += 10 + c - 'a' << shift;
                    } else {
                        plen = 0;
                        break;
                    }
                    shift += 4;
                }
            }
            mPlayListLen = plen;
            final int pos = mPreferences.getInt("curpos", 0);
            if (pos < 0 || pos >= mPlayListLen) {
                mPlayListLen = 0;
                return;
            }
            mPlayPos = pos;

            closeCursor();
            mOpenFailedCounter = 20;
            openCurrentAndNext();
            final IMusicPlayer player = getPlayer();
            if (player != null && !player.isInitialized()) {
                mPlayListLen = 0;
                return;
            }

            final long seekpos = mPreferences.getLong("seekpos", 0);
            seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);

            if (D) {
                Log.d(TAG, "restored queue, currently at position "
                        + position() + "/" + duration()
                        + " (requested " + seekpos + ")");
            }

            int repmode = mPreferences.getInt("repeatmode", REPEAT_NONE);
            if (repmode != REPEAT_ALL && repmode != REPEAT_CURRENT) {
                repmode = REPEAT_NONE;
            }
            mRepeatMode = repmode;

            int shufmode = mPreferences.getInt("shufflemode", SHUFFLE_NONE);
            if (shufmode != SHUFFLE_AUTO && shufmode != SHUFFLE_NORMAL) {
                shufmode = SHUFFLE_NONE;
            }
            // restore shuffle history
            q = mPreferences.getString("history", "");
            qlen = q != null ? q.length() : 0;
            if (qlen > 1) {
                plen = 0;
                n = 0;
                shift = 0;
                mHistory.clear();
                for (int i = 0; i < qlen; i++) {
                    final char c = q.charAt(i);
                    if (c == ';') {
                        if (n >= mPlayListLen) {
                            mHistory.clear();
                            break;
                        }
                        mHistory.add(n);
                        n = 0;
                        shift = 0;
                    } else {
                        if (c >= '0' && c <= '9') {
                            n += c - '0' << shift;
                        } else if (c >= 'a' && c <= 'f') {
                            n += 10 + c - 'a' << shift;
                        } else {
                            mHistory.clear();
                            break;
                        }
                        shift += 4;
                    }
                }
            }
            // restore auto shuffle history
            q = mPreferences.getString("autohistory", "");
            qlen = q != null ? q.length() : 0;
            if (qlen > 1) {
                plen = 0;
                n = 0;
                shift = 0;
                mAutoHistory.clear();
                for (int i = 0; i < qlen; i++) {
                    final char c = q.charAt(i);
                    if (c == ';') {
                        if (n >= mPlayListLen) {
                            mAutoHistory.clear();
                            break;
                        }
                        mAutoHistory.add(n);
                        n = 0;
                        shift = 0;
                    } else {
                        if (c >= '0' && c <= '9') {
                            n += c - '0' << shift;
                        } else if (c >= 'a' && c <= 'f') {
                            n += 10 + c - 'a' << shift;
                        } else {
                            mAutoHistory.clear();
                            break;
                        }
                        shift += 4;
                    }
                }
            }
            if (shufmode == SHUFFLE_AUTO) {
                if (!makeAutoShuffleList()) {
                    shufmode = SHUFFLE_NONE;
                }
            }
            mShuffleMode = shufmode;
        }
    }

    /**
     * Opens a file and prepares it for playback
     *
     * @param path The path of the file to open
     */
    public boolean openFile(final String path) {
        if (D) Log.d(TAG, "openFile: path = " + path);
        synchronized (this) {
            if (path == null) {
                return false;
            }

            // If mCursor is null, try to associate path with a database cursor
            if (mCursor == null || mCursor.isClosed()) {
                Uri uri;
                String where;
                String selectionArgs[];
                if (path.startsWith("content://media/")) {
                    uri = Uri.parse(path);
                    where = null;
                    selectionArgs = null;
                } else {
                    uri = MediaStore.Audio.Media.getContentUriForPath(path);
                    where = MediaStore.Audio.Media.DATA + "=?";
                    selectionArgs = new String[] {
                        path
                    };
                }
                try {
                    updateCursor(uri, where, selectionArgs);
                    if (mCursor != null) {
                        if (mCursor.getCount() == 0) {
                            closeCursor();
                        } else {
                            ensurePlayListCapacity(1);
                            mPlayListLen = 1;
                            mPlayList[0] = mCursor.getLong(IDCOLIDX);
                            mPlayPos = 0;
                        }
                    }
                } catch (final UnsupportedOperationException ex) {
                }
            }
            final IMusicPlayer player = getPlayer();
            if (mCursor != null) {
                if (player != null) player.setDataSource(mCursor);
            } else {
                if (player != null) player.setDataSource(path);
            }
            if (player != null && player.isInitialized()) {
                mOpenFailedCounter = 0;
                return true;
            }
            stop(true);
            return false;
        }
    }

    /**
     * Returns the audio session ID
     *
     * @return The current media player audio session ID
     */
    public int getAudioSessionId() {
        synchronized (this) {
            return mPlayer.getAudioSessionId();
        }
    }

    /**
     * Indicates if the media storeage device has been mounted or not
     *
     * @return 1 if Intent.ACTION_MEDIA_MOUNTED is called, 0 otherwise
     */
    public int getMediaMountedCount() {
        return mMediaMountedCount;
    }

    /**
     * Returns the shuffle mode
     *
     * @return The current shuffle mode (all, party, none)
     */
    public int getShuffleMode() {
        return mShuffleMode;
    }

    /**
     * Returns the repeat mode
     *
     * @return The current repeat mode (all, one, none)
     */
    public int getRepeatMode() {
        return mRepeatMode;
    }

    /**
     * Removes all instances of the track with the given ID from the playlist.
     *
     * @param id The id to be removed
     * @return how many instances of the track were removed
     */
    public int removeTrack(final long id) {
        int numremoved = 0;
        synchronized (this) {
            for (int i = 0; i < mPlayListLen; i++) {
                if (mPlayList[i] == id) {
                    numremoved += removeTracksInternal(i, i);
                    i--;
                }
            }
        }
        if (numremoved > 0) {
            notifyChange(QUEUE_CHANGED);
        }
        return numremoved;
    }

    /**
     * Removes the range of tracks specified from the play list. If a file
     * within the range is the file currently being played, playback will move
     * to the next file after the range.
     *
     * @param first The first file to be removed
     * @param last The last file to be removed
     * @return the number of tracks deleted
     */
    public int removeTracks(final int first, final int last) {
        final int numremoved = removeTracksInternal(first, last);
        if (numremoved > 0) {
            notifyChange(QUEUE_CHANGED);
        }
        return numremoved;
    }

    /**
     * Returns the position in the queue
     *
     * @return the current position in the queue
     */
    public int getQueuePosition() {
        synchronized (this) {
            return mPlayPos;
        }
    }

    /**
     * Returns the path to current song
     *
     * @return The path to the current song
     */
    public Uri getDataUri() {
        synchronized (this) {
            ensureCursor();
            final String uri = CursorHelpers.getStringOrNull(mCursor, MusicStore.Cols.DATA_URI);
            if (TextUtils.isEmpty(uri)) {
                return null;
            } else {
                return Uri.parse(uri);
            }
        }
    }

    /**
     * Returns the album name
     *
     * @return The current song album Name
     */
    public String getAlbumName() {
        synchronized (this) {
            ensureCursor();
            return CursorHelpers.getStringOrNull(mCursor, MusicStore.Cols.ALBUM_NAME);
        }
    }

    /**
     * Returns the song name
     *
     * @return The current song name
     */
    public String getTrackName() {
        synchronized (this) {
            ensureCursor();
            return CursorHelpers.getStringOrNull(mCursor, MusicStore.Cols.NAME);
        }
    }

    /**
     * Returns the artist name
     *
     * @return The current song artist name
     */
    public String getArtistName() {
        synchronized (this) {
            ensureCursor();
            return CursorHelpers.getStringOrNull(mCursor, MusicStore.Cols.ARTIST_NAME);
        }
    }

    /**
     * Returns the artist name
     *
     * @return The current song artist name
     */
    public String getAlbumArtistName() {
        synchronized (this) {
            ensureCursor();
            return CursorHelpers.getStringOrNull(mCursor, MusicStore.Cols.ALBUM_ARTIST_NAME);
        }
    }

    /**
     * Returns the album ID
     *
     * @return The current song album ID
     */
    public long getAlbumId() {
        synchronized (this) {
            ensureCursor();
            return CursorHelpers.getLongOrZero(mCursor, MusicStore.Cols.ALBUM_IDENTITY);
        }
    }

    /**
     * Returns the current audio ID
     *
     * @return The current track ID
     */
    public long getAudioId() {
        synchronized (this) {
            if (mPlayPos >= 0 && mPlayList.length > mPlayPos) {
                return mPlayList[mPlayPos];
            }
        }
        return -1;
    }

    /**
     * Returns the mimeType
     *
     * @return The current song mimeType
     */
    public String getMimeType() {
        synchronized (this) {
            ensureCursor();
            return CursorHelpers.getStringOrNull(mCursor, MusicStore.Cols.MIME_TYPE);
        }
    }

    /**
     *
     * @return
     */
    public Uri getArtworkUri() {
        synchronized (this) {
            ensureCursor();
            final String uri = CursorHelpers.getStringOrNull(mCursor, MusicStore.Cols.ARTWORK_URI);
            if (TextUtils.isEmpty(uri)) {
                return null;
            } else {
                return Uri.parse(uri);
            }
        }
    }

    /**
     *
     * @return
     */
    public ArtInfo getCurrentArtInfo() {
        String albumartist = getAlbumArtistName();
        if (TextUtils.isEmpty(albumartist)) {
            albumartist = getArtistName();
        }
        return new ArtInfo(albumartist, getAlbumName(), getArtworkUri());
    }

    /**
     *
     */
    public boolean isFromSDCard() {
        synchronized (this) {
            ensureCursor();
            return CursorHelpers.getIntOrZero(mCursor, MusicStore.Cols.ISLOCAL) == 1;
        }
    }

    /**
     * Seeks the current track and initiates playback if needed
     *
     */
    public long seekAndPlay(long position) {
        final IMusicPlayer player = getPlayer();
        return player != null ? player.seekAndPlay(position) : -1;
    }

    /**
     * Seeks the current track to a specific time
     *
     * @param position The time to seek to
     * @return The time to play the track at
     */
    //@DebugLog
    public long seek(long position) {
        final IMusicPlayer player = getPlayer();
        if (player != null && player.isInitialized()) {
            if (position < 0) {
                position = 0;
            } else {
                long duration = duration();
                if (duration > 0 && position > duration) {
                    position = duration;
                }
            }
            long ret = player.seek(position);
            if (ret >= 0) {
                notifyChange(POSITION_CHANGED);
            }
        }
        return -1;
    }

    /**
     * Returns the current position in time of the currenttrack
     *
     * @return The current playback position in miliseconds
     */
    public long position() {
        final IMusicPlayer player = getPlayer();
        if (player != null && player.isInitialized()) {
            return player.position();
        }
        return -1;
    }

    /**
     * Returns the full duration of the current track
     *
     * @return The duration of the current track in miliseconds
     */
    public long duration() {
        final IMusicPlayer player = getPlayer();
        if (player != null && player.isInitialized()) {
            return player.duration();
        }
        return -1;
    }

    /**
     * Returns the queue
     *
     * @return The queue as a long[]
     */
    public long[] getQueue() {
        synchronized (this) {
            final int len = mPlayListLen;
            final long[] list = new long[len];
            System.arraycopy(mPlayList, 0, list, 0, len);
            return list;
        }
    }

    /**
     * @return True if music is playing, false otherwise
     */
    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    /**
     * True if the current track is a "favorite", false otherwise
     */
    public boolean isFavorite() {
        return false;
    }

    /**
     * Opens a list for playback
     *
     * @param list The list of tracks to open
     * @param position The position to start playback at
     */
    public void open(final long[] list, final int position) {
        synchronized (this) {
            if (mShuffleMode == SHUFFLE_AUTO) {
                mShuffleMode = SHUFFLE_NORMAL;
            }
            final long oldId = getAudioId();
            final int listlength = list.length;
            boolean newlist = true;
            if (mPlayListLen == listlength) {
                newlist = false;
                for (int i = 0; i < listlength; i++) {
                    if (list[i] != mPlayList[i]) {
                        newlist = true;
                        break;
                    }
                }
            }
            if (newlist) {
                addToPlayList(list, -1);
                notifyChange(QUEUE_CHANGED);
            }
            if (position >= 0) {
                mPlayPos = position;
            } else {
                mPlayPos = mShuffler.nextInt(mPlayListLen);
            }
            mHistory.clear();
            openCurrentAndNext();
            if (oldId != getAudioId()) {
                notifyChange(META_CHANGED);
            }
        }
    }

    /**
     * Stops playback.
     */
    public void stop() {
        stop(true);
    }

    /**
     * Resumes or starts playback.
     */
    //@DebugLog
    public void play() {
        int hint = isRemotePlayback() ? AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK : AudioManager.AUDIOFOCUS_GAIN;
        int status = mAudioManager.requestAudioFocus(mAudioFocusListener,
                AudioManager.STREAM_MUSIC, hint);

        if (D) Log.d(TAG, "Starting playback: audio focus request status = " + status);

        if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        mMediaSessionHelper.ping();

        final IMusicPlayer player = getPlayer();
        if (player != null && player.isInitialized()) {
//                final long duration = duration();
            // if not repeating and only 2 seconds left in current track goToNext
// TODO revisit (causing Stack Overflow: circumstances unknown)
//                if (mRepeatMode != REPEAT_CURRENT && duration > 2000
//                        && player.position() >= duration - 2000) {
//                    gotoNext(true);
//                    return;
//                }

            player.play();
            mPlayerHandler.removeMessages(MusicPlayerHandler.FADEDOWN);
            mPlayerHandler.sendEmptyMessage(MusicPlayerHandler.FADEUP);

            synchronized (this) {
                if (!mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = true;
                    notifyChange(PLAYSTATE_CHANGED);
                }
                cancelShutdown();
                updateNotification();
            }

        } else if (mPlayListLen <= 0) {
            setShuffleMode(SHUFFLE_AUTO);
        } else {
            Log.e(TAG, "play() Player not initialized and no playlist");
            synchronized (this) {
                if (mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = false;
                    notifyChange(PLAYSTATE_CHANGED);
                }
            }
        }
    }

    /**
     * Temporarily pauses playback.
     */
    public void pause() {
        synchronized (this) {
            if (D) Log.d(TAG, "Pausing playback");
            mPlayerHandler.removeMessages(MusicPlayerHandler.FADEUP);
            if (mIsSupposedToBePlaying) {
                final IMusicPlayer player = getPlayer();
                if (player != null) player.pause();
                scheduleDelayedShutdown();
                mIsSupposedToBePlaying = false;
                notifyChange(PLAYSTATE_CHANGED);
            }
        }
    }

    /**
     * Changes from the current track to the next track
     */
    public void gotoNext(final boolean force) {
        synchronized (this) {
            if (D) Log.d(TAG, "Going to next track");
            final IMusicPlayer player = getPlayer();
            if (player != null && !player.canGoNext()) {
                Log.w(TAG, "Ignoring next() request");
                return; //Ignore request
            }
            if (mPlayListLen <= 0) {
                if (D) Log.d(TAG, "No play queue");
                scheduleDelayedShutdown();
                return;
            }
            final int pos = getNextPosition(force);
            if (pos < 0) {
                scheduleDelayedShutdown();
                if (mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = false;
                    notifyChange(PLAYSTATE_CHANGED);
                }
                return;
            }
            mPlayPos = pos;
            stop(false);
            mPlayPos = pos;
            openCurrentAndNext();
            play();
            notifyChange(META_CHANGED);
        }
    }

    /**
     * Changes from the current track to the previous played track
     */
    //@DebugLog
    public void prev() {
        synchronized (this) {
            if (D) Log.d(TAG, "Going to previous track");
            final IMusicPlayer player = getPlayer();
            if (player != null && !player.canGoPrev()) {
                Log.w(TAG, "Ignoring prev() request");
                return; //Ignore request
            }
            if (mShuffleMode == SHUFFLE_NORMAL) {
                // Go to previously-played track and remove it from the history
                final int histsize = mHistory.size();
                if (histsize == 0) {
                    return;
                }
                mPlayPos = mHistory.remove(histsize - 1);
            } else {
                if (mPlayPos > 0) {
                    mPlayPos--;
                } else {
                    mPlayPos = mPlayListLen - 1;
                }
            }
            stop(false);
            openCurrent();
            play();
            notifyChange(META_CHANGED);
        }
    }

    /**
     * We don't want to open the current and next track when the user is using
     * the {@code #prev()} method because they won't be able to travel back to
     * the previously listened track if they're shuffling.
     */
    private void openCurrent() {
        openCurrentAndMaybeNext(false);
    }

    /**
     * Toggles the current song as a favorite.
     */
    public void toggleFavorite() {

    }

    /**
     * Moves an item in the queue from one position to another
     *
     * @param from The position the item is currently at
     * @param to The position the item is being moved to
     */
    public void moveQueueItem(int index1, int index2) {
        synchronized (this) {
            if (index1 >= mPlayListLen) {
                index1 = mPlayListLen - 1;
            }
            if (index2 >= mPlayListLen) {
                index2 = mPlayListLen - 1;
            }
            if (index1 < index2) {
                final long tmp = mPlayList[index1];
                for (int i = index1; i < index2; i++) {
                    mPlayList[i] = mPlayList[i + 1];
                }
                mPlayList[index2] = tmp;
                if (mPlayPos == index1) {
                    mPlayPos = index2;
                } else if (mPlayPos >= index1 && mPlayPos <= index2) {
                    mPlayPos--;
                }
            } else if (index2 < index1) {
                final long tmp = mPlayList[index1];
                for (int i = index1; i > index2; i--) {
                    mPlayList[i] = mPlayList[i - 1];
                }
                mPlayList[index2] = tmp;
                if (mPlayPos == index1) {
                    mPlayPos = index2;
                } else if (mPlayPos >= index2 && mPlayPos <= index1) {
                    mPlayPos++;
                }
            }
            notifyChange(QUEUE_CHANGED);
            setNextTrack();
        }
    }

    /**
     * Sets the repeat mode
     *
     * @param repeatmode The repeat mode to use
     */
    public void setRepeatMode(final int repeatmode) {
        synchronized (this) {
            mRepeatMode = repeatmode;
            setNextTrack();
            saveQueue(false);
            notifyChange(REPEATMODE_CHANGED);
        }
    }

    /**
     * Sets the shuffle mode
     *
     * @param shufflemode The shuffle mode to use
     */
    public void setShuffleMode(final int shufflemode) {
        synchronized (this) {
            if (mShuffleMode == shufflemode && mPlayListLen > 0) {
                return;
            }
            mShuffleMode = shufflemode;
            if (mShuffleMode == SHUFFLE_AUTO) {
                if (makeAutoShuffleList()) {
                    mPlayListLen = 0;
                    doAutoShuffleUpdate();
                    mPlayPos = 0;
                    openCurrentAndNext();
                    play();
                    notifyChange(META_CHANGED);
                    return;
                } else {
                    mShuffleMode = SHUFFLE_NONE;
                }
            }
            saveQueue(false);
            notifyChange(SHUFFLEMODE_CHANGED);
        }
    }

    /**
     * Sets the position of a track in the queue
     *
     * @param index The position to place the track
     */
    public void setQueuePosition(final int index) {
        synchronized (this) {
            stop(false);
            mPlayPos = index;
            openCurrentAndNext();
            play();
            notifyChange(META_CHANGED);
            if (mShuffleMode == SHUFFLE_AUTO) {
                doAutoShuffleUpdate();
            }
        }
    }

    /**
     * Queues a new list for playback
     *
     * @param list The list to queue
     * @param action The action to take
     */
    public void enqueue(final long[] list, final int action) {
        synchronized (this) {
            if (action == NEXT && mPlayPos + 1 < mPlayListLen) {
                addToPlayList(list, mPlayPos + 1);
                notifyChange(QUEUE_CHANGED);
                setNextTrack();
            } else {
                addToPlayList(list, Integer.MAX_VALUE);
                notifyChange(QUEUE_CHANGED);
                if (action == NOW) {
                    mPlayPos = mPlayListLen - list.length;
                    openCurrentAndNext();
                    play();
                    notifyChange(META_CHANGED);
                    return;
                }
            }
            if (mPlayPos < 0) {
                mPlayPos = 0;
                openCurrentAndNext();
                play();
                notifyChange(META_CHANGED);
            }
        }
    }

    /**
     * Cycles through the different repeat modes
     */
    private void cycleRepeat() {
        if (mRepeatMode == REPEAT_NONE) {
            setRepeatMode(REPEAT_ALL);
        } else if (mRepeatMode == REPEAT_ALL) {
            setRepeatMode(REPEAT_CURRENT);
            if (mShuffleMode != SHUFFLE_NONE) {
                setShuffleMode(SHUFFLE_NONE);
            }
        } else {
            setRepeatMode(REPEAT_NONE);
        }
    }

    /**
     * Cycles through the different shuffle modes
     */
    private void cycleShuffle() {
        if (mShuffleMode == SHUFFLE_NONE) {
            setShuffleMode(SHUFFLE_NORMAL);
            if (mRepeatMode == REPEAT_CURRENT) {
                setRepeatMode(REPEAT_ALL);
            }
        } else if (mShuffleMode == SHUFFLE_NORMAL || mShuffleMode == SHUFFLE_AUTO) {
            setShuffleMode(SHUFFLE_NONE);
        }
    }

    /**
     * @return The album art for the current album.
     */
    public Bitmap getAlbumArt() {
        String artist = getAlbumArtistName();
        if (TextUtils.isEmpty(artist)) {
            artist = getArtistName();
        }
        return mArtworkUtil.getArtwork(artist, getAlbumName());
    }

    /**
     * @return thumbnail for the current album.
     */
    public Bitmap getAlbumArtThumbnail() {
        String artist = getAlbumArtistName();
        if (TextUtils.isEmpty(artist)) {
            artist = getArtistName();
        }
        return mArtworkUtil.getArtworkThumbnail(artist, getAlbumName());
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    public void refresh() {
        notifyChange(REFRESH);
    }

    /**
     * Updates current playback location
     * @param newLocation
     */
    private void updatePlaybackLocation(PlaybackLocation newLocation) {
        mPlaybackLocation = newLocation;
    }

    /**
     * Whether we are currently in a remote session
     * @return
     */
    public boolean isRemotePlayback() {
        return isCastingEnabled && mCastManager != null && mPlaybackLocation == PlaybackLocation.REMOTE;
    }

    /**
     * Starts cast http server, creating it if needed.
     * @return success of operation
     */
    private boolean startCastServer() {
        if (mCastServer == null) {
            try {
                mCastServer = new CastWebServer(this);
            } catch (UnknownHostException e) {
                return false;
            }
        }
        try {
            mCastServer.start();
        } catch (IOException e) {
            e.printStackTrace();
            // At the moment the only thing i can think
            // would case a fail is if its already running
            // causing the port to be bound
            stopCastServer();
            // XXX
            return startCastServer();
        }
        acquireWakeLock();
        return true;
    }

    /**
     * Stops cast http server if running
     */
    private void stopCastServer() {
        if (mCastServer != null) {
            mCastServer.stop();
        }
        mCastServer = null;
        releaseWakeLock();
    }

    /**
     * When the ui is running the progress bar updater task will
     * fetch the remote progress for us, when we are in the background
     * this will do it
     */
    private Subscription createRemoteProgressHandler() {
        return Observable.timer(1000, 3000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        try {
                            if (isRemotePlayback()) {
                                try {
                                    //Check first so we dont fill up the log with TransientDisconnects
                                    mCastManager.checkConnectivity();
                                    if (mCastPlayer != null) {
                                        long pos = mCastPlayer.position();
                                        synchronized (this) {
                                            mLastKnowPosition = pos;
                                        }
                                    }
                                } catch (TransientNetworkDisconnectionException|NoConnectionException ignored) { }
                            } else {
                                mRemoteProgressSubscription.unsubscribe();
                            }
                        } catch (Exception ignored) {
                            if (mRemoteProgressSubscription != null)
                                mRemoteProgressSubscription.unsubscribe();
                        }
                    }
                });
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            mHandleIntentWorker.schedule(new Action0() {
                @Override
                public void call() {
                    handleCommandIntent(intent);
                }
            });
        }
    };

    private final OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onAudioFocusChange(final int focusChange) {
            mPlayerHandler.obtainMessage(MusicPlayerHandler.FOCUSCHANGE, focusChange, 0).sendToTarget();
        }
    };

    /**
     * Service connection callback handler for cast service
     */
    private final CastServiceConnectionCallback mCastServiceConnectionCallback = new CastServiceConnectionCallback() {
        @Override
        public void onCastServiceConnected() {
            // Initialize the cast manager
            mCastManager = LocalCastServiceManager.sCastService.getCastManager();
            mCastPlayer = new CastMusicPlayer(MusicPlaybackService.this, mCastManager);
            mCastPlayer.setHandler(mPlayerHandler);
            mCastConsumer = new MusicCastConsumer(MusicPlaybackService.this, mCastManager);
            mCastManager.addCastConsumer(mCastConsumer);
            //Initialize the cast media player
        }

        @Override
        public void onCastServiceDisconnected() {
            mCastManager = null;
        }
    };

}
