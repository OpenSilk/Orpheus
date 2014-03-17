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
package org.opensilk.music.ui.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.IApolloService;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.MusicStateListener;
import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageCache;
import com.andrew.apollo.loaders.NowPlayingCursor;
import com.andrew.apollo.loaders.QueueLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastStatusCodes;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.opensilk.cast.helpers.CastServiceConnectionCallback;
import org.opensilk.cast.helpers.RemoteCastServiceManager;
import org.opensilk.music.cast.CastUtils;
import org.opensilk.music.cast.dialogs.StyledMediaRouteDialogFactory;
import org.opensilk.music.ui.fragments.QueueFragment;
import org.opensilk.music.widgets.AudioVisualizationView;
import org.opensilk.music.widgets.HeaderOverflowButton;
import org.opensilk.music.widgets.PanelHeaderLayout;
import org.opensilk.music.widgets.PlayPauseButton;
import org.opensilk.music.widgets.QueueButton;
import org.opensilk.music.widgets.RepeatButton;
import org.opensilk.music.widgets.RepeatingImageButton;
import org.opensilk.music.widgets.ShuffleButton;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import hugo.weaving.DebugLog;

import static android.media.audiofx.AudioEffect.ERROR_BAD_VALUE;
import static com.andrew.apollo.utils.MusicUtils.sService;
import static org.opensilk.cast.CastMessage.*;
import static org.opensilk.music.cast.CastUtils.sCastService;

/**
 * A base {@link FragmentActivity} used to update the bottom bar and
 * bind to Apollo's service.
 * <p>
 * {@link HomeSlidingActivity} extends from this skeleton.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public abstract class BaseSlidingActivity extends ActionBarActivity implements
        ServiceConnection,
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = BaseSlidingActivity.class.getSimpleName();
    // Message to refresh the time
    private static final int REFRESH_TIME = 1;

    /** Playstate and meta change listener */
    private final ArrayList<MusicStateListener> mMusicStateListener = Lists.newArrayList();

    /** The service token */
    private ServiceToken mToken;

    /** Broadcast receiver */
    private PlaybackStatus mPlaybackStatus;

    /** Handler used to update the current time */
    private TimeHandler mTimeHandler;

    // Background art
    private ImageView mArtBackground;

    /** Panel Header */
    private PanelHeaderLayout mPanelHeader;
    //Visualizer object
    private Visualizer mVisualizer;
    //Visualization view
    private AudioVisualizationView mVisualizerView;
    // Previous button
    private RepeatingImageButton mHeaderPrevButton;
    //play/pause
    private PlayPauseButton mHeaderPlayPauseButton;
    // Next button
    private RepeatingImageButton mHeaderNextButton;
    // Album art
    private ImageView mHeaderAlbumArt;
    // queue switch button
    private QueueButton mHeaderQueueButton;
    // overflow btn
    private HeaderOverflowButton mHeaderOverflow;
    // Track name
    private TextView mHeaderTrackName;
    // Artist name
    private TextView mHeaderArtistName;
    //media router btn
    private MediaRouteButton mHeaderMediaRouteButton;

    /** Panel Footer */
    // Play and pause button
    private PlayPauseButton mFooterPlayPauseButton;
    // Repeat button
    private RepeatButton mFooterRepeatButton;
    // Shuffle button
    private ShuffleButton mFooterShuffleButton;
    // Previous button
    private RepeatingImageButton mFooterPreviousButton;
    // Next button
    private RepeatingImageButton mFooterNextButton;
    // Progess
    private SeekBar mFooterProgress;
    // Current time
    private TextView mFooterCurrentTime;
    // Total time
    private TextView mFooterTotalTime;

    /** Sliding panel */
    private SlidingUpPanelLayout mSlidingPanel;

    /** Whether the queue is showing */
    private boolean mQueueShowing;

    private long mPosOverride = -1;
    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private long mLastShortSeekEventTime;
    private boolean mIsPaused = false;
    private boolean mFromTouch = false;

    /**
     * Cast stuff
     */
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    /** Determines whether the cast buttons should be shown */
    private boolean mCastDeviceAvailable = false;
    private RemoteCastServiceManager mCastServiceHelper;
    private boolean mTransientNetworkDisconnection = false;

    // Theme resourses
    private ThemeHelper mThemeHelper;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set our theme
        mThemeHelper = ThemeHelper.getInstance(this);
        setTheme(mThemeHelper.getPanelTheme());

        // Set the layout
        setContentView(R.layout.activity_base_sliding);

        // Setup action bar
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        // Fade it in
        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Control the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Bind Apollo's service
        mToken = MusicUtils.bindToService(this, this);

        // Initialize the broadcast receiver
        mPlaybackStatus = new PlaybackStatus(this);

        // Initialize the handler used to update the current time
        mTimeHandler = new TimeHandler(this);

        // Bind cast service
        mCastServiceHelper = new RemoteCastServiceManager(this, new Messenger(mCastManagerCallbackHandler));
        mCastServiceHelper.setCallback(mCastServiceConnectionCallback);
        mCastServiceHelper.bind();

        // Initialize the media router
        mMediaRouter = MediaRouter.getInstance(this);
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(getString(R.string.cast_id)))
                //.addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                .build();

        mSlidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingPanel.setDragView(findViewById(R.id.track_artist_info));
        mSlidingPanel.setPanelSlideListener(mPanelSlideListener);

        // Initialze the panel
        initPanel();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        startPlayback();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        sService = IApolloService.Stub.asInterface(service);

        startPlayback();
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // Update the favorites icon
        invalidateOptionsMenu();

        // Setup visualizer
        initVisualizer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceDisconnected(final ComponentName name) {
        sService = null;
        if (mVisualizer != null) {
            mVisualizer.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Media router
        getMenuInflater().inflate(R.menu.cast_mediarouter_button, menu);
        // init router button
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider)
                MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        mediaRouteActionProvider.setDialogFactory(new StyledMediaRouteDialogFactory());

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Start scanning for routes
        if (PreferenceUtils.getInstance(this).isCastEnabled()) {
            mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        }
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // Refresh the queue
        refreshQueue();
        // Make sure we dont overlap the panel
        if (mSlidingPanel.isExpanded()
                && getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
        }
        // update visualizer
        updateVisualizerState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // stop scanning for routes
        mMediaRouter.removeCallback(mMediaRouterCallback);
        //Disable visualizer
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        super.onStart();
        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Shuffle and repeat changes
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicPlaybackService.REFRESH);
        registerReceiver(mPlaybackStatus, filter);
        // Refresh the current time
        final long next = refreshCurrentTime();
        queueNextRefresh(next);
        MusicUtils.notifyForegroundStateChanged(this, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();
        MusicUtils.notifyForegroundStateChanged(this, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsPaused = false;
        mTimeHandler.removeMessages(REFRESH_TIME);
        // Unbind from the service
        if (mToken != null) {

            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }

        //Unbind from cast service
        mCastServiceHelper.unbind();
        sCastService = null;

        // Unregister the receiver
        try {
            unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
            //$FALL-THROUGH$
        }

        // Remove any music status listeners
        mMusicStateListener.clear();
        // Kill the visualizer
        if (mVisualizer != null) {
            mVisualizer.release();
        }

        //Free cache
        ImageCache.getInstance(this).evictAll();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("panel_open", false)) {
                mPanelSlideListener.onPanelExpanded(null);
                if (savedInstanceState.getBoolean("queue_showing", false)) {
                    onQueueVisibilityChanged(true);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("panel_open", mSlidingPanel.isExpanded());
        outState.putBoolean("queue_showing", mQueueShowing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        if (mSlidingPanel.isExpanded()) {
            mSlidingPanel.collapsePane();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (MusicUtils.isRemotePlayback()) {
            double increment = 0;
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                increment = 0.05;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                increment = -0.05;
            }
            if (increment != 0) {
                CastUtils.changeRemoteVolume(increment);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProgressChanged(final SeekBar bar, final int progress, final boolean fromuser) {
        if (!fromuser || sService == null) {
            return;
        }
        final long now = SystemClock.elapsedRealtime();
        if (now - mLastSeekEventTime > 250) {
            mLastSeekEventTime = now;
            mLastShortSeekEventTime = now;
            mPosOverride = MusicUtils.duration() * progress / 1000;
            MusicUtils.seek(mPosOverride);
            if (!mFromTouch) {
                // refreshCurrentTime();
                mPosOverride = -1;
            }
        } else if (now - mLastShortSeekEventTime > 5) {
            mLastShortSeekEventTime = now;
            mPosOverride = MusicUtils.duration() * progress / 1000;
            refreshCurrentTimeText(mPosOverride);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartTrackingTouch(final SeekBar bar) {
        mLastSeekEventTime = 0;
        mFromTouch = true;
        if (!mQueueShowing) {
            mFooterCurrentTime.setVisibility(View.VISIBLE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopTrackingTouch(final SeekBar bar) {
        if (mPosOverride != -1) {
            MusicUtils.seek(mPosOverride);
        }
        mPosOverride = -1;
        mFromTouch = false;
    }

    /**
     * Initializes the items in sliding panel.
     */
    private void initPanel() {
        //Header
        mPanelHeader = (PanelHeaderLayout) findViewById(R.id.panel_header);

        // Background art
        mArtBackground = (ImageView) findViewById(R.id.panel_background_art);

        //Visualizer view
        mVisualizerView = (AudioVisualizationView) findViewById(R.id.visualizer_view);

        // Previous button
        mHeaderPrevButton = (RepeatingImageButton) findViewById(R.id.header_action_button_previous);
        // Set the repeat listener for the previous button
        mHeaderPrevButton.setRepeatListener(mRewindListener);
        // Play and pause button
        mHeaderPlayPauseButton = (PlayPauseButton)findViewById(R.id.header_action_button_play);
        // Next button
        mHeaderNextButton = (RepeatingImageButton) findViewById(R.id.header_action_button_next);
        // Set the repeat listner for the next button
        mHeaderNextButton.setRepeatListener(mFastForwardListener);
        // Track name
        mHeaderTrackName = (TextView)findViewById(R.id.header_track_info);
        // Artist name
        mHeaderArtistName = (TextView)findViewById(R.id.header_artist_info);
        // Album art
        mHeaderAlbumArt = (ImageView)findViewById(R.id.header_album_art);
        // Open to the currently playing album profile
        mHeaderAlbumArt.setOnClickListener(mOpenCurrentAlbumProfile);
        // Used to show and hide the queue fragment
        mHeaderQueueButton = (QueueButton) findViewById(R.id.header_switch_queue);
        mHeaderQueueButton.setOnClickListener(mToggleHiddenPanel);
        // Set initial queue button drawable
        mHeaderQueueButton.setQueueShowing(mQueueShowing);


        // overflow
        mHeaderOverflow = (HeaderOverflowButton) findViewById(R.id.header_overflow);
        mHeaderOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(BaseSlidingActivity.this, mHeaderOverflow);
                popupMenu.getMenuInflater().inflate(R.menu.panel, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(mPanelOverflowMenuClickListener);
                popupMenu.show();
            }
        });

        // init router button
        mHeaderMediaRouteButton = (MediaRouteButton) findViewById(R.id.panel_mediarouter);
        mHeaderMediaRouteButton.setRouteSelector(mMediaRouteSelector);
        mHeaderMediaRouteButton.setDialogFactory(new StyledMediaRouteDialogFactory());

        if (!mSlidingPanel.isExpanded()) {
            mPanelHeader.makeBackgroundSolid();
            mHeaderQueueButton.setVisibility(View.GONE);
            mHeaderOverflow.setVisibility(View.GONE);
            mHeaderMediaRouteButton.setVisibility(View.GONE);
        } else {
            mPanelHeader.makeBackgroundTransparent();
        }

        // Play and pause button
        mFooterPlayPauseButton = (PlayPauseButton)findViewById(R.id.footer_action_button_play);
        // Shuffle button
        mFooterShuffleButton = (ShuffleButton)findViewById(R.id.footer_action_button_shuffle);
        // Repeat button
        mFooterRepeatButton = (RepeatButton)findViewById(R.id.footer_action_button_repeat);
        // Previous button
        mFooterPreviousButton = (RepeatingImageButton)findViewById(R.id.footer_action_button_previous);
        // Set the repeat listner for the previous button
        mFooterPreviousButton.setRepeatListener(mRewindListener);
        // Next button
        mFooterNextButton = (RepeatingImageButton)findViewById(R.id.footer_action_button_next);
        // Set the repeat listner for the next button
        mFooterNextButton.setRepeatListener(mFastForwardListener);
        // Current time
        mFooterCurrentTime = (TextView)findViewById(R.id.footer_player_current_time);
        // Total time
        mFooterTotalTime = (TextView)findViewById(R.id.footer_player_total_time);
        // Progress
        mFooterProgress = (SeekBar)findViewById(android.R.id.progress);
        // Update the progress
        mFooterProgress.setOnSeekBarChangeListener(this);
    }

    /**
     * Initializes visualizer
     */
    private void initVisualizer() {
        if (MusicUtils.getAudioSessionId() != ERROR_BAD_VALUE) {
            try {
                mVisualizer = new Visualizer(MusicUtils.getAudioSessionId());
                mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
                mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                        mVisualizerView.updateVisualizer(bytes);
                        //Log.d("VisualizerView", "Visualizer bytes:" + bytes.toString());
                    }

                    public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) { }
                }, Visualizer.getMaxCaptureRate() / 2, true, false);
                updateVisualizerState();
            } catch (RuntimeException e) {
                // Go without.
                e.printStackTrace();
            }
        } //else wait for service bind
    }

    /**
     * Enables or disables visualizer depending on playback state
     */
    private void updateVisualizerState() {
        if (mVisualizer != null && mVisualizerView != null) {
            if (MusicUtils.isPlaying() && !MusicUtils.isRemotePlayback() &&
                    PreferenceUtils.getInstance(this).showVisualizations()) {
                mVisualizer.setEnabled(true);
                mVisualizerView.setVisibility(View.VISIBLE);
            } else {
                mVisualizer.setEnabled(false);
                mVisualizerView.setVisibility(View.INVISIBLE);
            }
        } //else wait for create and service bind
    }

    /**
     * Possibly shows media route button
     */
    private void maybeShowHeaderMediaRouteButton(){
        if (mCastDeviceAvailable && mSlidingPanel.isExpanded()) {
            mHeaderMediaRouteButton.setVisibility(View.VISIBLE);
        } else {
            mHeaderMediaRouteButton.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the track name, album name, and album art.
     */
    private void updateNowPlayingInfo() {
        // Set the track name
        mHeaderTrackName.setText(MusicUtils.getTrackName());
        // Set the artist name
        mHeaderArtistName.setText(MusicUtils.getArtistName());
        // Set the album art
        ApolloUtils.getImageFetcher(this).loadCurrentArtwork(mHeaderAlbumArt);
        // Set the total time
        mFooterTotalTime.setText(MusicUtils.makeTimeString(this, MusicUtils.duration() / 1000));
        // Set the album art
        ApolloUtils.getImageFetcher(this).loadCurrentLargeArtwork(mArtBackground);
        // Update the current time
        queueNextRefresh(1);
    }

    /**
     * Sets the correct drawable states for the playback controls.
     */
    private void updatePlaybackControls() {
        // Set the play and pause image
        mHeaderPlayPauseButton.updateState();
        // Set the play and pause image
        mFooterPlayPauseButton.updateState();
        // Set the shuffle image
        mFooterShuffleButton.updateShuffleState();
        // Set the repeat image
        mFooterRepeatButton.updateRepeatState();
    }

    /**
     * Checks whether the passed intent contains a playback request,
     * and starts playback if that's the case
     */
    private void startPlayback() {
        Intent intent = getIntent();

        if (intent == null || sService == null) {
            return;
        }

        Uri uri = intent.getData();
        String mimeType = intent.getType();
        boolean handled = false;

        if (uri != null && uri.toString().length() > 0) {
            MusicUtils.playFile(this, uri);
            handled = true;
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            long id = intent.getLongExtra("playlistId", -1);
            if (id < 0) {
                String idString = intent.getStringExtra("playlist");
                if (idString != null) {
                    try {
                        id = Long.parseLong(idString);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
            if (id >= 0) {
                MusicUtils.playPlaylist(this, id);
                handled = true;
            }
        }

        if (handled) {
            // Make sure to process intent only once
            setIntent(new Intent());
            // Refresh the queue
            refreshQueue();
        }
    }

    /**
     * @param delay When to update
     */
    private void queueNextRefresh(final long delay) {
        if (!mIsPaused) {
            final Message message = mTimeHandler.obtainMessage(REFRESH_TIME);
            mTimeHandler.removeMessages(REFRESH_TIME);
            mTimeHandler.sendMessageDelayed(message, delay);
        }
    }

    /**
     * Used to scan backwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param delta The long press duration
     */
    private void scanBackward(final int repcnt, long delta) {
        if (sService == null) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos - delta;
            if (newpos < 0) {
                // move to previous track
                MusicUtils.previous(this);
                final long duration = MusicUtils.duration();
                mStartSeekPos += duration;
                newpos += duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    /**
     * Used to scan forwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param delta The long press duration
     */
    private void scanForward(final int repcnt, long delta) {
        if (sService == null) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos + delta;
            final long duration = MusicUtils.duration();
            if (newpos >= duration) {
                // move to next track
                MusicUtils.next();
                mStartSeekPos -= duration; // is OK to go negative
                newpos -= duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    private void refreshCurrentTimeText(final long pos) {
        mFooterCurrentTime.setText(MusicUtils.makeTimeString(this, pos / 1000));
    }

    /* Used to update the current time string */
    private long refreshCurrentTime() {
        if (sService == null) {
            return 500;
        }
        try {
            final long pos = mPosOverride < 0 ? MusicUtils.position() : mPosOverride;
            if (pos >= 0 && MusicUtils.duration() > 0) {
                refreshCurrentTimeText(pos);
                final int progress = (int)(1000 * pos / MusicUtils.duration());
                mFooterProgress.setProgress(progress);

                if (mFromTouch) {
                    return 500;
                } else if (MusicUtils.isPlaying()) {
                    if (!mQueueShowing) {
                        mFooterCurrentTime.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (!mQueueShowing) {
                        // blink the counter
                        final int vis = mFooterCurrentTime.getVisibility();
                        mFooterCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE
                                : View.INVISIBLE);
                        return 500;
                    }
                }
            } else {
                mFooterCurrentTime.setText("--:--");
                mFooterProgress.setProgress(1000);
            }
            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            final long remaining = 1000 - pos % 1000;
            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mFooterProgress.getWidth();
            if (width == 0) {
                width = 320;
            }
            final long smoothrefreshtime = MusicUtils.duration() / width;
            if (smoothrefreshtime > remaining) {
                return remaining;
            }
            if (smoothrefreshtime < 20) {
                return 20;
            }
            return smoothrefreshtime;
        } catch (final Exception ignored) {

        }
        return 500;
    }

    public void refreshQueue() {
        if (mQueueShowing) {
            QueueFragment queue = (QueueFragment) getSupportFragmentManager().findFragmentByTag("queue");
            if (queue != null) {
                queue.refreshQueue();
            }
        }
    }

    public void maybeClosePanel() {
        if (mSlidingPanel.isExpanded()) {
            mSlidingPanel.collapsePane();
        }
    }

    private void pushQueueFragment() {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.panel_middle_content, new QueueFragment(), "queue")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        onQueueVisibilityChanged(true);
    }

    private void popQueueFragment() {
        getSupportFragmentManager().beginTransaction()
                .remove(getSupportFragmentManager().findFragmentByTag("queue"))
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        onQueueVisibilityChanged(false);
    }

    private void onQueueVisibilityChanged(boolean visible) {
        if (visible) {
            mQueueShowing = true;
            mArtBackground.setVisibility(View.INVISIBLE);
            mFooterCurrentTime.setVisibility(View.INVISIBLE);
            mFooterTotalTime.setVisibility(View.INVISIBLE);
        } else {
            mQueueShowing = false;
            mArtBackground.setVisibility(View.VISIBLE);
            refreshCurrentTime();
            mFooterTotalTime.setVisibility(View.VISIBLE);
        }
        mHeaderQueueButton.setQueueShowing(mQueueShowing);
    }

    /**
     * Used to scan backwards through the track
     */
    private final RepeatingImageButton.RepeatListener mRewindListener = new RepeatingImageButton.RepeatListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onRepeat(final View v, final long howlong, final int repcnt) {
            scanBackward(repcnt, howlong);
        }
    };

    /**
     * Used to scan ahead through the track
     */
    private final RepeatingImageButton.RepeatListener mFastForwardListener = new RepeatingImageButton.RepeatListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onRepeat(final View v, final long howlong, final int repcnt) {
            scanForward(repcnt, howlong);
        }
    };

    /**
     * Switches from the large album art screen to show the queue and lyric
     * fragments, then back again
     */
    private final View.OnClickListener mToggleHiddenPanel = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (!mQueueShowing) {
                pushQueueFragment();
            } else {
                popQueueFragment();
            }

        }
    };

    private final SlidingUpPanelLayout.PanelSlideListener mPanelSlideListener =
            new SlidingUpPanelLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
            if (slideOffset < 0.2) {
                if (getSupportActionBar().isShowing()) {
                    getSupportActionBar().hide();
                }
            } else {
                if (!getSupportActionBar().isShowing()) {
                    getSupportActionBar().show();
                }
            }
        }

        @Override
        public void onPanelExpanded(View panel) {
            mHeaderQueueButton.setVisibility(View.VISIBLE);
            mHeaderOverflow.setVisibility(View.VISIBLE);
            maybeShowHeaderMediaRouteButton();
            mHeaderPrevButton.setVisibility(View.GONE);
            mHeaderPlayPauseButton.setVisibility(View.GONE);
            mHeaderNextButton.setVisibility(View.GONE);
            mPanelHeader.makeBackgroundTransparent();// .setBackgroundResource(R.color.app_background_light_transparent);
        }

        @Override
        public void onPanelCollapsed(View panel) {
            Log.i(TAG, "onPanelCollapsed");
            mHeaderQueueButton.setVisibility(View.GONE);
            mHeaderOverflow.setVisibility(View.GONE);
            mHeaderMediaRouteButton.setVisibility(View.GONE);
            mHeaderPrevButton.setVisibility(View.VISIBLE);
            mHeaderPlayPauseButton.setVisibility(View.VISIBLE);
            mHeaderNextButton.setVisibility(View.VISIBLE);
            if (mQueueShowing) {
                popQueueFragment();
            }
            mPanelHeader.makeBackgroundSolid();// .setBackgroundResource(R.color.app_background_light);
        }

        @Override
        public void onPanelAnchored(View panel) {

        }
    };

    /**
     * Opens the album profile of the currently playing album
     */
    private final View.OnClickListener mOpenCurrentAlbumProfile = new View.OnClickListener() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onClick(final View v) {
            if (MusicUtils.getCurrentAudioId() != -1) {
                Album album = MusicUtils.getCurrentAlbum(BaseSlidingActivity.this);
                if (album != null) {
                    NavUtils.openAlbumProfile(BaseSlidingActivity.this, album);
                }
            } else {
                MusicUtils.shuffleAll(BaseSlidingActivity.this);
            }
        }
    };

    /**
     * Handle mediarouter callbacks, responsible for keeping our mediarouter instance
     * in sync with the cast managers instance.
     */
    private final MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback() {

        @DebugLog
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            if (!CastUtils.notifyRouteSelected(BaseSlidingActivity.this, route)) {
                // If we couldnt notify the service we need to reset the router so
                // the user can try again
                router.selectRoute(router.getDefaultRoute());
            }
        }

        @DebugLog
        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            //
            if (!mTransientNetworkDisconnection) {
                if (MusicUtils.isPlaying()) {
                    MusicUtils.playOrPause();
                }
                CastUtils.notifyRouteUnselected();
            }
        }

        @DebugLog
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            // Show the router buttons
            if (!router.getDefaultRoute().equals(route)) {
                mCastDeviceAvailable = true;
                maybeShowHeaderMediaRouteButton();
            }
        }

        @DebugLog
        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
            // Hide the router buttons
            if (!router.getDefaultRoute().equals(route)) {
                mCastDeviceAvailable = false;
                maybeShowHeaderMediaRouteButton();
            }
        }

        @DebugLog
        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            // make sure the router buttons are showing
            // *this gets called alot
            if (!router.getDefaultRoute().equals(route)) {
                mCastDeviceAvailable = true;
                maybeShowHeaderMediaRouteButton();
            }
        }

    };

    /**
     * Handle messages sent from CastService notifying of CastManager events
     */
    private final Handler mCastManagerCallbackHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CAST_APPLICATION_CONNECTION_FAILED:
                    final String errorMsg;
                    switch (msg.arg1) {
                        case CastStatusCodes.APPLICATION_NOT_FOUND:
                            errorMsg = "ERROR_APPLICATION_NOT_FOUND";
                            break;
                        case CastStatusCodes.TIMEOUT:
                            errorMsg = "ERROR_TIMEOUT";
                            break;
                        default:
                            errorMsg = "UNKNOWN ERROR: " + msg.arg1;
                            break;
                    }
                    Log.d(TAG, "onApplicationConnectionFailed(): failed due to: " + errorMsg);
                    resetDefaultMediaRoute();
                    // notify if possible
                    if (MusicUtils.isForeground()) {
                        new AlertDialog.Builder(BaseSlidingActivity.this)
                                .setTitle(R.string.cast_error)
                                .setMessage(String.format(Locale.getDefault(),
                                        getString(R.string.cast_failed_to_connect), errorMsg))
                                .setNeutralButton(android.R.string.ok, null)
                                .show();
                    }
                    break;
                case CAST_APPLICATION_DISCONNECTED:
                    // This is just in case
                    resetDefaultMediaRoute();
                    break;
                case CAST_CONNECTION_SUSPENDED:
                    mTransientNetworkDisconnection = true;
                    break;
                case CAST_CONNECTIVITY_RECOVERED:
                    mTransientNetworkDisconnection = false;
                    break;
                case CAST_DISCONNECTED:
                    mTransientNetworkDisconnection = false;
                    break;
                case CAST_FAILED:
                    // notify if possible
                    if (MusicUtils.isForeground()) {
                        switch (msg.arg1) {
                            case (R.string.failed_load):
                                new AlertDialog.Builder(BaseSlidingActivity.this)
                                        .setTitle(R.string.cast_error)
                                        .setMessage(R.string.failed_load)
                                        .setNeutralButton(android.R.string.ok, null)
                                        .show();
                                break;
                        }
                    }
                    break;
            }
        }

        /**
         * We only do this to reset the mediarouter buttons, the cast manager
         * will have already done this, but our buttons dont know about it
         */
        private void resetDefaultMediaRoute() {
            // Reset the route
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
    };

    /**
     * Service connection listener for cast service bind
     */
    private final CastServiceConnectionCallback mCastServiceConnectionCallback = new CastServiceConnectionCallback() {
        @Override
        public void onCastServiceConnected() {
            sCastService = mCastServiceHelper.getService();
        }

        @Override
        public void onCastServiceDisconnected() {
            sCastService = null;
        }
    };

    /**
     * Handles panel overflow menu
     */
    private final PopupMenu.OnMenuItemClickListener mPanelOverflowMenuClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.panel_menu_share:
                    // Share the current meta data
                    if (MusicUtils.getTrackName() != null && MusicUtils.getArtistName() != null) {
                        final Intent shareIntent = new Intent();
                        final String shareMessage = getString(R.string.now_listening_to,
                                MusicUtils.getTrackName(), MusicUtils.getArtistName());
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_track_using)));
                    }
                    return true;
                case R.id.panel_menu_equalizer:
                    // Sound effects
                    NavUtils.openEffectsPanel(BaseSlidingActivity.this);
                    return true;
                case R.id.panel_menu_use_ringtone:
                    // Set the current track as a ringtone
                    MusicUtils.setRingtone(BaseSlidingActivity.this, MusicUtils.getCurrentAudioId());
                    return true;
                case R.id.panel_menu_delete:
                    // Delete current song
                    DeleteDialog.newInstance(MusicUtils.getTrackName(), new long[]{
                            MusicUtils.getCurrentAudioId()
                    }, null).show(getSupportFragmentManager(), "DeleteDialog");
                    return true;
                case R.id.panel_menu_save_queue:
                    NowPlayingCursor queue = (NowPlayingCursor) QueueLoader
                            .makeQueueCursor(BaseSlidingActivity.this);
                    CreateNewPlaylist.getInstance(MusicUtils.getSongListForCursor(queue)).show(
                            getSupportFragmentManager(), "CreatePlaylist");
                    queue.close();
                    return true;
                case R.id.panel_menu_clear_queue:
                    MusicUtils.clearQueue();
                    mSlidingPanel.collapsePane();
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    /**
     * Used to update the current time string
     */
    private static final class TimeHandler extends Handler {

        private final WeakReference<BaseSlidingActivity> mActivity;

        /**
         * Constructor of <code>TimeHandler</code>
         */
        public TimeHandler(final BaseSlidingActivity player) {
            mActivity = new WeakReference<BaseSlidingActivity>(player);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REFRESH_TIME:
                    final long next = mActivity.get().refreshCurrentTime();
                    mActivity.get().queueNextRefresh(next);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Used to monitor the state of playback
     */
    private final static class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<BaseSlidingActivity> mReference;

        /**
         * Constructor of <code>PlaybackStatus</code>
         */
        public PlaybackStatus(final BaseSlidingActivity activity) {
            mReference = new WeakReference<BaseSlidingActivity>(activity);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action.equals(MusicPlaybackService.META_CHANGED)) {
                // Current info
                mReference.get().updateNowPlayingInfo();
                // Update the favorites icon
                mReference.get().invalidateOptionsMenu();
                // Let the listener know to the meta chnaged
                for (final MusicStateListener listener : mReference.get().mMusicStateListener) {
                    if (listener != null) {
                        listener.onMetaChanged();
                    }
                }
            } else if (action.equals(MusicPlaybackService.PLAYSTATE_CHANGED)) {
                // Set the play and pause image
                mReference.get().mHeaderPlayPauseButton.updateState();
                mReference.get().mFooterPlayPauseButton.updateState();
                // Refresh the queue
                mReference.get().refreshQueue();
                // update visualizer
                mReference.get().updateVisualizerState();
            } else if (action.equals(MusicPlaybackService.REFRESH)) {
                // Let the listener know to update a list
                for (final MusicStateListener listener : mReference.get().mMusicStateListener) {
                    if (listener != null) {
                        listener.restartLoader();
                    }
                }
                // Cancel the broadcast so we aren't constantly refreshing
                context.removeStickyBroadcast(intent);
            } else if (action.equals(MusicPlaybackService.REPEATMODE_CHANGED)
                    || action.equals(MusicPlaybackService.SHUFFLEMODE_CHANGED)) {
                // Set the repeat image
                mReference.get().mFooterRepeatButton.updateRepeatState();
                // Set the shuffle image
                mReference.get().mFooterShuffleButton.updateShuffleState();
            }
        }
    }

    /**
     * @param status The {@link MusicStateListener} to use
     */
    public void setMusicStateListenerListener(final MusicStateListener status) {
        if (status != null) {
            mMusicStateListener.add(status);
        }
    }

}
