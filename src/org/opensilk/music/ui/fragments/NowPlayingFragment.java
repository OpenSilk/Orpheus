/*
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

package org.opensilk.music.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.MediaRouteButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.provider.MusicProviderUtil;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.model.Album;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.artwork.ArtworkProvider;
import org.opensilk.music.bus.EventBus;
import org.opensilk.music.bus.events.IABQueryResult;
import org.opensilk.music.bus.events.MetaChanged;
import org.opensilk.music.bus.events.MusicServiceConnectionChanged;
import org.opensilk.music.bus.events.PanelStateChanged;
import org.opensilk.music.bus.events.PlaybackModeChanged;
import org.opensilk.music.bus.events.PlaystateChanged;
import org.opensilk.music.iab.IabUtil;
import org.opensilk.music.ui.activities.BaseSlidingActivity;
import org.opensilk.music.widgets.AudioVisualizationView;
import org.opensilk.music.widgets.FullScreenArtworkImageView;
import org.opensilk.music.widgets.HeaderOverflowButton;
import org.opensilk.music.widgets.PanelHeaderLayout;
import org.opensilk.music.widgets.PlayPauseButton;
import org.opensilk.music.widgets.QueueButton;
import org.opensilk.music.widgets.RepeatButton;
import org.opensilk.music.widgets.RepeatingImageButton;
import org.opensilk.music.widgets.ShuffleButton;
import org.opensilk.music.widgets.ThumbnailArtworkImageView;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.support.ActivityScopedDaggerFragment;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import static android.media.audiofx.AudioEffect.ERROR_BAD_VALUE;
import static com.andrew.apollo.utils.MusicUtils.sService;

/**
 * Created by drew on 4/10/14.
 */
public class NowPlayingFragment extends ActivityScopedDaggerFragment implements
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = NowPlayingFragment.class.getSimpleName();

    // Message to refresh the time
    private static final int REFRESH_TIME = 1;

    // Handler used to update the current time
    private TimeHandler mTimeHandler;

    // Background art
    private FullScreenArtworkImageView mArtBackground;

    /*
     * Panel Header
     */
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
    private ThumbnailArtworkImageView mHeaderAlbumArt;
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

    /*
     * Panel Footer
     */
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

    // Whether the queue is showing
    private boolean mQueueShowing;

    private long mPosOverride = -1;
    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private long mLastShortSeekEventTime;
    private boolean mFromTouch = false;

    protected BaseSlidingActivity mActivity;

    @Inject @ForActivity
    Bus mActivityBus;

    private GlobalBusMonitor mGlobalMonitor;
    private ActivityBusMonitor mActivityMonitor;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (BaseSlidingActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // register the busses
        mGlobalMonitor = new GlobalBusMonitor();
        EventBus.getInstance().register(mGlobalMonitor);
        mActivityMonitor = new ActivityBusMonitor();
        mActivityBus.register(mActivityMonitor);
        // Initialize the handler used to update the current time
        mTimeHandler = new TimeHandler(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.panel_fragment, container, false);
        initPanel(v);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        IabUtil.queryDonateAsync(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the current time
        final long next = refreshCurrentTime();
        queueNextRefresh(next);
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // update visualizer
        if (mVisualizer == null) {
            initVisualizer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //Disable visualizer
        destroyVisualizer();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        // Unregister the busses
        EventBus.getInstance().unregister(mGlobalMonitor);
        mActivityBus.unregister(mActivityMonitor);
        // clear messages so we won't prevent gc
        mTimeHandler.removeMessages(REFRESH_TIME);
        super.onDestroy();
    }

    /*
     * SeekBar.OnSeekBarChangeListener
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

    @Override
    public void onStartTrackingTouch(final SeekBar bar) {
        mLastSeekEventTime = 0;
        mFromTouch = true;
        if (!mQueueShowing) {
            mFooterCurrentTime.setVisibility(View.VISIBLE);
        }
    }

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
    private void initPanel(View v) {
        //Header
        mPanelHeader = (PanelHeaderLayout) v.findViewById(R.id.panel_header);

        // Background art
        mArtBackground = (FullScreenArtworkImageView) v.findViewById(R.id.panel_background_art);

        //Visualizer view
        mVisualizerView = (AudioVisualizationView) v.findViewById(R.id.visualizer_view);
        mVisualizerView.setVisibility(View.GONE);

        // Previous button
        mHeaderPrevButton = (RepeatingImageButton) v.findViewById(R.id.header_action_button_previous);
        // Set the repeat listener for the previous button
        mHeaderPrevButton.setRepeatListener(mRewindListener);
        // Play and pause button
        mHeaderPlayPauseButton = (PlayPauseButton) v.findViewById(R.id.header_action_button_play);
        // Next button
        mHeaderNextButton = (RepeatingImageButton) v.findViewById(R.id.header_action_button_next);
        // Set the repeat listner for the next button
        mHeaderNextButton.setRepeatListener(mFastForwardListener);
        // Track name
        mHeaderTrackName = (TextView) v.findViewById(R.id.header_track_info);
        // Artist name
        mHeaderArtistName = (TextView) v.findViewById(R.id.header_artist_info);
        // Album art
        mHeaderAlbumArt = (ThumbnailArtworkImageView) v.findViewById(R.id.header_album_art);
        // Open to the currently playing album profile
        mHeaderAlbumArt.setOnClickListener(mOpenCurrentAlbumProfile);
        // Used to show and hide the queue fragment
        mHeaderQueueButton = (QueueButton) v.findViewById(R.id.header_switch_queue);
        mHeaderQueueButton.setOnClickListener(mToggleHiddenPanel);
        // Set initial queue button drawable
        mHeaderQueueButton.setQueueShowing(mQueueShowing);


        // overflow
        mHeaderOverflow = (HeaderOverflowButton) v.findViewById(R.id.header_overflow);
        mHeaderOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(mActivity, mHeaderOverflow);
                if (isQueueShowing()) {
                    popupMenu.inflate(R.menu.panel_save_queue);
                    popupMenu.inflate(R.menu.panel_clear_queue);
                } else {
                    popupMenu.inflate(R.menu.panel_share);
                    if (MusicUtils.isFromSDCard()) {
                        popupMenu.inflate(R.menu.panel_set_ringtone);
                        popupMenu.inflate(R.menu.panel_delete);
                    }
                }
                popupMenu.setOnMenuItemClickListener(mPanelOverflowMenuClickListener);
                popupMenu.show();
            }
        });

        // init router button
//        mHeaderMediaRouteButton = (MediaRouteButton) v.findViewById(R.id.panel_mediarouter);
//        mHeaderMediaRouteButton.setRouteSelector(mMediaRouteSelector);
//        mHeaderMediaRouteButton.setDialogFactory(new StyledMediaRouteDialogFactory());
//        mHeaderMediaRouteButton.setVisibility(View.GONE);

        // Play and pause button
        mFooterPlayPauseButton = (PlayPauseButton) v.findViewById(R.id.footer_action_button_play);
        // Shuffle button
        mFooterShuffleButton = (ShuffleButton) v.findViewById(R.id.footer_action_button_shuffle);
        // Repeat button
        mFooterRepeatButton = (RepeatButton) v.findViewById(R.id.footer_action_button_repeat);
        // Previous button
        mFooterPreviousButton = (RepeatingImageButton) v.findViewById(R.id.footer_action_button_previous);
        // Set the repeat listner for the previous button
        mFooterPreviousButton.setRepeatListener(mRewindListener);
        // Next button
        mFooterNextButton = (RepeatingImageButton) v.findViewById(R.id.footer_action_button_next);
        // Set the repeat listner for the next button
        mFooterNextButton.setRepeatListener(mFastForwardListener);
        // Current time
        mFooterCurrentTime = (TextView) v.findViewById(R.id.footer_player_current_time);
        // Total time
        mFooterTotalTime = (TextView) v.findViewById(R.id.footer_player_total_time);
        // Progress
        mFooterProgress = (SeekBar) v.findViewById(android.R.id.progress);
        // Update the progress
        mFooterProgress.setOnSeekBarChangeListener(this);
    }

    /**
     * Initializes visualizer
     */
    //@DebugLog
    private void initVisualizer() {
        if (MusicUtils.getAudioSessionId() != ERROR_BAD_VALUE) {
            try {
                if (mVisualizer != null) {
                    Log.e(TAG, "initVisualizer() called with active visualizer");
                    destroyVisualizer();
                }
                mVisualizer = new Visualizer(MusicUtils.getAudioSessionId());
                mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
                mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                        mVisualizerView.updateVisualizer(bytes);
                        //Log.d("VisualizerView", "Visualizer bytes:" + bytes.toString());
                    }

                    public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) { }
                }, Math.min(Visualizer.getMaxCaptureRate()/2, 7000), true, false);
                updateVisualizerState();
            } catch (RuntimeException e) {
                // Go without.
                e.printStackTrace();
                mVisualizer = null;
            }
        } //else wait for service bind
    }

    /**
     * Releases the visualizer
     */
    //@DebugLog
    private void destroyVisualizer() {
        if (mVisualizer != null) {
            mVisualizer.release();
            mVisualizer = null;
        }
    }

    /**
     * Enables or disables visualizer depending on playback state
     */
    //@DebugLog
    private void updateVisualizerState() {
        if (mVisualizer != null && mVisualizerView != null) {
            if (!mQueueShowing && MusicUtils.isPlaying() && !MusicUtils.isRemotePlayback() &&
                    PreferenceUtils.getInstance(mActivity).showVisualizations()) {
                try {
                    if (!mVisualizer.getEnabled()) {
                        mVisualizer.setEnabled(true);
                    }
                    mVisualizerView.setVisibility(View.VISIBLE);
                } catch (IllegalStateException e) {
                    destroyVisualizer();
                }
            } else {
                try {
                    if (mVisualizer.getEnabled()) {
                        mVisualizer.setEnabled(false);
                    }
                } catch (IllegalStateException e) {
                    destroyVisualizer();
                }
                mVisualizerView.setVisibility(View.GONE);
            }
        } //else wait for create and service bind
    }

    public void pushQueueFragment() {
        mActivity.getSupportFragmentManager().beginTransaction()
                .add(R.id.panel_middle_content, new QueueFragment(), "queue")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
        onQueueVisibilityChanged(true);
    }

    public void popQueueFragment() {
        mActivity.getSupportFragmentManager().beginTransaction()
                .remove(mActivity.getSupportFragmentManager().findFragmentByTag("queue"))
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();
        onQueueVisibilityChanged(false);
    }

    public void onQueueVisibilityChanged(boolean visible) {
        if (visible) {
            mQueueShowing = true;
            //TODO any reason to set visiblity GONE?
            mArtBackground.animate().alpha(0.0f).setDuration(500).start();
            mFooterCurrentTime.setVisibility(View.INVISIBLE);
            mFooterTotalTime.setVisibility(View.INVISIBLE);
        } else {
            mQueueShowing = false;
            mArtBackground.animate().alpha(1.0f).setDuration(500).start();
            refreshCurrentTime();
            mFooterTotalTime.setVisibility(View.VISIBLE);
        }
        mHeaderQueueButton.setQueueShowing(mQueueShowing);
        updateVisualizerState();
    }

    /**
     * Used for saveInstanceState in activity
     * @return
     */
    public boolean isQueueShowing() {
        return mQueueShowing;
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
        ArtworkManager.loadCurrentArtwork(mHeaderAlbumArt);
        // Set the total time
        mFooterTotalTime.setText(MusicUtils.makeTimeString(mActivity, MusicUtils.duration() / 1000));
        // Set the album art
        ArtworkManager.loadCurrentArtwork(mArtBackground);
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
    public boolean startPlayback(Intent intent) {

        if (intent == null || sService == null) {
            return false;
        }

        Uri uri = intent.getData();
        String mimeType = intent.getType();
        boolean handled = false;

        if (uri != null && uri.toString().length() > 0) {
            MusicUtils.playFile(mActivity, uri);
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
                MusicUtils.playPlaylist(mActivity, id, false);
                handled = true;
            }
        }
        return handled;
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
                MusicUtils.previous(mActivity);
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

    /**
     * @param delay When to update
     */
    private void queueNextRefresh(final long delay) {
        if (isResumed()) {
            final Message message = mTimeHandler.obtainMessage(REFRESH_TIME);
            mTimeHandler.removeMessages(REFRESH_TIME);
            mTimeHandler.sendMessageDelayed(message, delay);
        }
    }

    private void refreshCurrentTimeText(final long pos) {
        mFooterCurrentTime.setText(MusicUtils.makeTimeString(mActivity, pos / 1000));
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


    /**
     * Used to scan backwards through the track
     */
    private final RepeatingImageButton.RepeatListener mRewindListener = new RepeatingImageButton.RepeatListener() {
        @Override
        public void onRepeat(final View v, final long howlong, final int repcnt) {
            scanBackward(repcnt, howlong);
        }
    };

    /**
     * Used to scan ahead through the track
     */
    private final RepeatingImageButton.RepeatListener mFastForwardListener = new RepeatingImageButton.RepeatListener() {
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

    /**
     * Handles panel overflow menu
     */
    private final PopupMenu.OnMenuItemClickListener mPanelOverflowMenuClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.panel_menu_share:
                    // Share the current meta data
                    String trackname = MusicUtils.getTrackName();
                    String artistname = MusicUtils.getArtistName();
                    if (trackname != null && artistname != null) {
                        final Intent shareIntent = new Intent();
                        final String shareMessage = getString(R.string.now_listening_to, trackname, artistname);
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                        String albumartist = MusicUtils.getAlbumArtistName();
                        if (albumartist == null) albumartist = artistname;
                        String albumname = MusicUtils.getAlbumName();
                        shareIntent.putExtra(Intent.EXTRA_STREAM,
                                ArtworkProvider.createArtworkUri(albumartist, albumname));
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_track_using)));
                    } else {
                        //TODO toast
                    }
                    return true;
                case R.id.panel_menu_use_ringtone:
                    // Set the current track as a ringtone
                    if (MusicUtils.isFromSDCard()) {
                        long id = MusicUtils.getCurrentAudioId();
                        long realid = -1;
                        if (id >= 0) {
                            realid = MusicProviderUtil.getRealId(mActivity, id);
                        }
                        if (realid >= 0) {
                            MusicUtils.setRingtone(mActivity, realid);
                        } else {
                            //TODo toast
                        }
                    } // else unsupported
                    return true;
                case R.id.panel_menu_delete:
                    // Delete current song
                    if (MusicUtils.isFromSDCard()) {
                        long id = MusicUtils.getCurrentAudioId();
                        long realid = -1;
                        if (id >= 0) {
                            realid = MusicProviderUtil.getRealId(mActivity, id);
                        }
                        if (realid >= 0) {
                            DeleteDialog.newInstance(MusicUtils.getTrackName(), new long[]{realid}, null)
                                    .show(mActivity.getSupportFragmentManager(), "DeleteDialog");
                        } else {
                            //TODo toast
                        }
                    } // else unsupported
                    return true;
                case R.id.panel_menu_save_queue:
                    long[] queue = MusicUtils.getQueue();
                    if (queue != null && queue.length > 0) {
                        long[] playlist = MusicProviderUtil.transformListToRealIds(mActivity, queue);
                        if (playlist.length > 0) {
                            CreateNewPlaylist.getInstance(playlist)
                                    .show(mActivity.getSupportFragmentManager(), "CreatePlaylist");
                        } else {
                            // TODO toast
                        }
                    }
                    return true;
                case R.id.panel_menu_clear_queue:
                    MusicUtils.clearQueue();
                    mActivity.maybeClosePanel();
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    /**
     * Opens the album profile of the currently playing album
     */
    private final View.OnClickListener mOpenCurrentAlbumProfile = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            long id = MusicUtils.getCurrentAudioId();
            if (id > 0) {
                if (MusicUtils.isFromSDCard()) {
                    long albumId = MusicProviderUtil.getAlbumId(mActivity, id);
                    if (albumId >= 0) {
                        LocalAlbum album = MusicUtils.makeLocalAlbum(mActivity, albumId);
                        if (album != null) {
                            NavUtils.openAlbumProfile(mActivity, album);
                            return;
                        }
                    }
                    // TODO toast
                }// else todo notify?
            } else {
                MusicUtils.shuffleAll(mActivity);
            }
        }
    };

    class GlobalBusMonitor {
        @Subscribe
        public void onMetaChanged(MetaChanged e) {
            // Current info
            updateNowPlayingInfo();
        }

        @Subscribe
        public void onPlaystateChanged(PlaystateChanged e) {
            // Set the play and pause image
            mHeaderPlayPauseButton.updateState();
            mFooterPlayPauseButton.updateState();
            // update visualizer
            updateVisualizerState();
        }

        @Subscribe
        public void onPlaybackModeChanged(PlaybackModeChanged e) {
            // Set the repeat image
            mFooterRepeatButton.updateRepeatState();
            // Set the shuffle image
            mFooterShuffleButton.updateShuffleState();
        }

        /**
         * Handle changes to music service connection
         * @param e
         */
        @Subscribe
        public void onMusicServiceConnectionChanged(MusicServiceConnectionChanged e) {
            if (e.isConnected()) {
                // Set the playback drawables
                updatePlaybackControls();
                // Current info
                updateNowPlayingInfo();
                // Setup visualizer
                if (mVisualizer == null) {
                    initVisualizer();
                }
            } else {
                destroyVisualizer();
            }
        }

        //@DebugLog
        @Subscribe
        public void onIABResult(IABQueryResult r) {
            if (r.error == IABQueryResult.Error.NO_ERROR) {
                if (!r.isApproved) {
                    IabUtil.maybeShowDonateDialog(getActivity());
                }
            }
            //TODO handle faliurs
        }
    }

    class ActivityBusMonitor {
        /**
         * Handle panel change events posted by activity
         * @param e
         */
        @Subscribe
        public void onPanelStateChanged(PanelStateChanged e) {
            PanelStateChanged.Action action = e.getAction();
            switch (action) {
                case USER_EXPAND:
                    mPanelHeader.transitionToOpen();
                    break;
                case USER_COLLAPSE:
                    if (mQueueShowing) {
                        popQueueFragment();
                    }
                    mPanelHeader.transitionToClosed();
                    break;
                case SYSTEM_EXPAND:
                    mPanelHeader.makeOpen();
                    break;
                case SYSTEM_COLLAPSE:
                    mPanelHeader.makeClosed();
                    break;
            }
        }
    }

    /**
     * Used to update the current time string
     */
    private static final class TimeHandler extends Handler {
        private final WeakReference<NowPlayingFragment> reference;

        private TimeHandler(NowPlayingFragment fragment) {
            this.reference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(final Message msg) {
            NowPlayingFragment f = reference.get();
            if (f == null) {
                return;
            }
            switch (msg.what) {
                case REFRESH_TIME:
                    final long next = f.refreshCurrentTime();
                    f.queueNextRefresh(next);
                    break;
                default:
                    break;
            }
        }
    }

}
