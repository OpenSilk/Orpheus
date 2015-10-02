/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.playback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.artwork.service.ArtworkProviderHelper;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.playback.service.PlaybackService;

import javax.inject.Inject;

import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.HandlerScheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 9/25/15.
 */
public class NotificationHelper2 extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    private static final String MYPKG = BuildConfig.APPLICATION_ID;
    public static final String ACTION_PAUSE = MYPKG + ".pause";
    public static final String ACTION_PLAY = MYPKG + ".play";
    public static final String ACTION_PREV = MYPKG + ".prev";
    public static final String ACTION_NEXT = MYPKG + ".next";
    public static final String ACTION_STOP = MYPKG + ".stop";

    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final ArtworkProviderHelper mArtworkHelper;

    private final PlaybackService mService;
    private MediaSession.Token mSessionToken;
    private MediaController mController;
    private MediaController.TransportControls mTransportControls;

    private PlaybackState mPlaybackState;
    private MediaMetadata mMetadata;

    private PendingIntent mPauseIntent;
    private PendingIntent mPlayIntent;
    private PendingIntent mPreviousIntent;
    private PendingIntent mNextIntent;
    private PendingIntent mStopIntent;

    private int mNotificationColor;
    private Subscription mArtworkSubscription;
    private boolean mAnyActivityInForeground;

    private boolean mStarted = false;

    @Inject
    public NotificationHelper2(
            @ForApplication Context context,
            NotificationManager notificationManager,
            ArtworkProviderHelper artworkHelper,
            PlaybackService service
    ) {
        mContext = context;
        mNotificationManager = notificationManager;
        mArtworkHelper = artworkHelper;
        mService = service;

        String pkg = mService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(mContext, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mContext, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mContext, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mContext, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mStopIntent = PendingIntent.getBroadcast(mContext, REQUEST_CODE,
                new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!mStarted) {
            updateSessionToken();
            mMetadata = mController.getMetadata();
            mPlaybackState = mController.getPlaybackState();
            // The notification must be updated after setting started to true
            mController.registerCallback(mCb);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_NEXT);
            filter.addAction(ACTION_PAUSE);
            filter.addAction(ACTION_PLAY);
            filter.addAction(ACTION_PREV);
            filter.addAction(ACTION_STOP);
            mContext.registerReceiver(this, filter);
            mStarted = true;
            buildNotification();
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void killNotification() {
        if (mStarted) {
            mStarted = false;
            mController.unregisterCallback(mCb);
            try {
                mContext.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            mService.stopForeground(true);
        }
        if (mArtworkSubscription != null) {
            mArtworkSubscription.unsubscribe();
        }
    }

    public void setActivityInForeground(boolean yes) {
        if (mStarted && mAnyActivityInForeground != yes) {
            mAnyActivityInForeground = yes;
            buildNotification();
        } else {
            mAnyActivityInForeground = yes;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Timber.d("Received intent with action %s", action);
        switch (action) {
            case ACTION_PAUSE:
                mTransportControls.pause();
                break;
            case ACTION_PLAY:
                mTransportControls.play();
                break;
            case ACTION_NEXT:
                mTransportControls.skipToNext();
                break;
            case ACTION_PREV:
                mTransportControls.skipToPrevious();
                break;
            case ACTION_STOP:
                mTransportControls.stop();
                break;
            default:
                Timber.w("Unknown intent ignored. Action=%s", action);
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() {
        MediaSession.Token freshToken = mService.getSessionToken();
        if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCb);
            }
            mSessionToken = freshToken;
            mController = new MediaController(mContext, mSessionToken);
            mTransportControls = mController.getTransportControls();
            if (mStarted) {
                mController.registerCallback(mCb, mService.getHandler());
            }
        }
    }

    private PendingIntent createContentIntent() {
        Intent openUI = NavUtils.makeLauncherIntent(mContext);
        return PendingIntent.getActivity(mContext, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private final MediaController.Callback mCb = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            PlaybackState oldstate = mPlaybackState;
            mPlaybackState = state;
            if (oldstate != null && oldstate.getState() == state.getState()) {
                Timber.d("Ignoring playback state update: no change");
            } else {
                Timber.d("Received new playback state %s", state);
                if (PlaybackStateHelper.isStoppedOrInactive(state)) {
                    killNotification();
                } else {
                    buildNotification();
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mMetadata = metadata;
            Timber.d("Received new metadata %s", metadata);
            buildNotification();
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Timber.d("Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }
    };

    private void notifyNotification(Notification notification) {
        if (notification != null) {
            mService.startForeground(NOTIFICATION_ID, notification);
//            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * Call this to build the {@link Notification}.
     */
    public void buildNotification() {
        if (mMetadata == null) {
            return;
        }

        //if activity is showing and we arent playing kill the notification
        //if we are playing we continue showing the notification so it will
        //still show on the lockscreen
        if (mAnyActivityInForeground &&
                !PlaybackStateHelper.isPlaying(mController.getPlaybackState())) {
            killNotification();
            return;
        }

        final Uri artUri = MediaMetadataHelper.getIconUri(mMetadata);
        if (mArtworkSubscription != null) {
            mArtworkSubscription.unsubscribe();
        }
        mArtworkSubscription = mArtworkHelper.getArtwork(artUri)
                .observeOn(mService.getScheduler())
                .subscribe(new Action1<Bitmap>() {
                    @Override
                    public void call(Bitmap bitmap) {
                        buildNotificationInternal(bitmap);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.w(throwable, "getArtwork");
                        mArtworkSubscription = null;
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        mArtworkSubscription = null;
                    }
                });
    }

    private void buildNotificationInternal(Bitmap icon) {
        if (mMetadata == null || mPlaybackState == null) {
            return;
        }

        final boolean isPlaying = PlaybackStateHelper.shouldShowPauseButton(mPlaybackState);

        // Default notfication layout
        RemoteViews mNotificationTemplate = new RemoteViews(mContext.getPackageName(),
                    R.layout.notification_template_base);

        // Set up the content view
        initCollapsedLayout(mNotificationTemplate, MediaMetadataHelper.getDisplayName(mMetadata),
                MediaMetadataHelper.getArtistName(mMetadata), icon);
        initPlaybackActions(mNotificationTemplate, isPlaying);

        Bundle extras = new Bundle();
        if (VersionUtils.hasApi21()) {
            //tells system we have a mediasession since we dont use mediastyle notification
            extras.putParcelable(NotificationCompat.EXTRA_MEDIA_SESSION, mSessionToken);
        }
        // Notification Builder
        Notification notification = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.stat_notify_music)
                .setContentIntent(createContentIntent())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setOngoing(true)
                .addExtras(extras)
                .setContent(mNotificationTemplate)
                .build();

        if (VersionUtils.hasJellyBean()) {
            // Expanded notifiction style
            RemoteViews mExpandedView = new RemoteViews(mContext.getPackageName(),
                        R.layout.notification_template_expanded_base);
            notification.bigContentView = mExpandedView;
            // Set up the expanded content view
            initExpandedLayout(mExpandedView, MediaMetadataHelper.getDisplayName(mMetadata),
                    MediaMetadataHelper.getAlbumName(mMetadata),
                    MediaMetadataHelper.getArtistName(mMetadata), icon);
            // Control playback from the notification
            initExpandedPlaybackActions(mExpandedView, isPlaying);
        }

        notifyNotification(notification);
    }

    /**
     * Lets the buttons in the remote view control playback in the expanded
     * layout
     */
    private void initExpandedPlaybackActions(RemoteViews mExpandedView, boolean isPlaying) {
        // Play and pause
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_play,
                isPlaying ? mPauseIntent : mPlayIntent);

        // Update the play button image
        mExpandedView.setImageViewResource(R.id.notification_expanded_base_play,
                getPlayPauseIcon(isPlaying));

        // Skip tracks
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_next,
                mNextIntent);

        // Previous tracks
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_previous,
                mPreviousIntent);

        // Stop and collapse the notification
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_collapse,
                mStopIntent);
    }

    /**
     * Lets the buttons in the remote view control playback in the normal layout
     */
    private void initPlaybackActions(RemoteViews mNotificationTemplate, boolean isPlaying) {
        // Play and pause
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_base_play,
                isPlaying ? mPauseIntent : mPlayIntent);

        // Update the play button image
        mNotificationTemplate.setImageViewResource(R.id.notification_base_play,
                getPlayPauseIcon(isPlaying));

        // Skip tracks
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_base_next,
                mNextIntent);
    }

    /**
     * Sets the track name, artist name, and album art in the normal layout
     */
    private void initCollapsedLayout(RemoteViews mNotificationTemplate, final String trackName,
                                     final String artistName, final Bitmap albumArt) {
        // Track name (line one)
        mNotificationTemplate.setTextViewText(R.id.notification_base_line_one, trackName);
        // Artist name (line two)
        mNotificationTemplate.setTextViewText(R.id.notification_base_line_two, artistName);
        // Album art
        mNotificationTemplate.setImageViewBitmap(R.id.notification_base_image, albumArt);
    }

    /**
     * Sets the track name, album name, artist name, and album art in the
     * expanded layout
     */
    private void initExpandedLayout(RemoteViews mExpandedView, final String trackName,
                                    final String artistName, final String albumName, final Bitmap albumArt) {
        // Track name (line one)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_one, trackName);
        // Album name (line two)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_two, albumName);
        // Artist name (line three)
        mExpandedView.setTextViewText(R.id.notification_expanded_base_line_three, artistName);
        // Album art
        mExpandedView.setImageViewBitmap(R.id.notification_expanded_base_image, albumArt);
    }

    private int getPlayPauseIcon(boolean isPlaying) {
        return isPlaying ? R.drawable.ic_pause_white_36dp : R.drawable.ic_play_arrow_white_36dp;
    }

}
