/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.opensilk.music.playback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.session.MediaSession;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.UtilsArt;
import org.opensilk.music.artwork.service.ArtworkProviderHelper;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Track;
import org.opensilk.music.playback.service.PlaybackService;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Builds the notification for Apollo's service. Jelly Bean and higher uses the
 * expanded notification by default.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class NotificationHelper {

    /**
     * Notification ID
     */
    private static final int APOLLO_MUSIC_SERVICE = 1;

    /**
     * NotificationManager
     */
    private final NotificationManager mNotificationManager;

    /**
     * Context
     */
    private final Context mContext;

    private final ArtworkProviderHelper mArtworkHelper;

    /**
     * Custom notification layout
     */
    private RemoteViews mNotificationTemplate;

    /**
     * The Notification
     */
    private Notification mNotification = null;

    /**
     * API 16+ bigContentView
     */
    private RemoteViews mExpandedView;

    Subscription mArtworkSubscription;
    CurrentInfo mCurrentInfo;

    Service mService;

    @Inject
    public NotificationHelper(
            @ForApplication Context context,
            NotificationManager notificationManager,
            ArtworkProviderHelper artworkHelper
    ) {
        mContext = context;
        mNotificationManager = notificationManager;
        mArtworkHelper = artworkHelper;
    }

    private static class CurrentInfo {
        final Track track;
        final boolean isPlaying;
        final ArtInfo artInfo;
        final Bitmap bitmap;

        public CurrentInfo(Track track, boolean isPlaying, ArtInfo artInfo, Bitmap bitmap) {
            this.track = track;
            this.artInfo = artInfo;
            this.isPlaying = isPlaying;
            this.bitmap = bitmap;
        }

        public CurrentInfo withIsPlaying(boolean isPlaying) {
            return new CurrentInfo(track, isPlaying, artInfo, bitmap);
        }

        public CurrentInfo withBitmap(Bitmap bitmap) {
            return new CurrentInfo(track, isPlaying, artInfo, bitmap);
        }
    }

    public void setService(Service service) {
        mService = service;
    }

    /**
     * Call this to build the {@link Notification}.
     */
    public void buildNotification(Track track, boolean isPlaying, MediaSession.Token mediaToken) {

        final ArtInfo artInfo = UtilsArt.makeBestfitArtInfo(track.albumArtistName,
                track.artistName, track.albumName, track.artworkUri);

        if (mCurrentInfo != null && artInfo.equals(mCurrentInfo.artInfo)) {
            mCurrentInfo = new CurrentInfo(track, isPlaying, mCurrentInfo.artInfo, mCurrentInfo.bitmap);
            if (mCurrentInfo.bitmap != null) {
                buildNotificationInternal();
            }
        } else {
            mCurrentInfo = new CurrentInfo(track, isPlaying, artInfo, null);
            if (mArtworkSubscription != null) {
                mArtworkSubscription.unsubscribe();
            }
            mArtworkSubscription = mArtworkHelper.getArtwork(artInfo, ArtworkType.THUMBNAIL)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                            mCurrentInfo = mCurrentInfo.withBitmap(bitmap);
                            buildNotificationInternal();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Timber.w(throwable, "artworkSubscription");
                            mCurrentInfo = null;
                        }
                    }, new Action0() {
                        @Override
                        public void call() {
                            mArtworkSubscription = null;
                        }
                    });
        }
    }

    private void buildNotificationInternal() {
        // Default notfication layout
        mNotificationTemplate = new RemoteViews(mContext.getPackageName(),
                R.layout.notification_template_base);

        // Set up the content view
        initCollapsedLayout(mCurrentInfo.track.name,
                mCurrentInfo.track.artistName, mCurrentInfo.bitmap);

        // Notification Builder
        mNotification = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.stat_notify_music)
                .setContentIntent(getPendingIntent())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setContent(mNotificationTemplate)
                .build();
        // Control playback from the notification
        initPlaybackActions(mCurrentInfo.isPlaying);
        if (VersionUtils.hasJellyBean()) {
            // Expanded notifiction style
            mExpandedView = new RemoteViews(mContext.getPackageName(),
                    R.layout.notification_template_expanded_base);
            mNotification.bigContentView = mExpandedView;
            // Control playback from the notification
            initExpandedPlaybackActions(mCurrentInfo.isPlaying);
            // Set up the expanded content view
            initExpandedLayout(mCurrentInfo.track.name, mCurrentInfo.track.albumName,
                    mCurrentInfo.track.artistName, mCurrentInfo.bitmap);
        }

        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        if (mService != null) {
            mService.startForeground(APOLLO_MUSIC_SERVICE, mNotification);
        }
    }

    /**
     * Remove notification
     */
    public void killNotification() {
        if (mService != null) {
            mService.stopForeground(true);
        }
        mNotification = null;
    }

    /**
     * Changes the playback controls in and out of a paused state
     *
     * @param isPlaying True if music is playing, false otherwise
     */
    public void updatePlayState(final boolean isPlaying) {
        if (mCurrentInfo.isPlaying == isPlaying) {
            return;
        }
        mCurrentInfo = mCurrentInfo.withIsPlaying(isPlaying);

        if (mNotification == null || mNotificationManager == null) {
            return;
        }

        if (mNotificationTemplate != null) {
            mNotificationTemplate.setImageViewResource(R.id.notification_base_play, getPlayPauseIcon(isPlaying));
        }

        if (VersionUtils.hasJellyBean() && mExpandedView != null) {
            mExpandedView.setImageViewResource(R.id.notification_expanded_base_play, getPlayPauseIcon(isPlaying));
        }

        mNotificationManager.notify(APOLLO_MUSIC_SERVICE, mNotification);
    }

    /**
     * Open to the now playing screen
     */
    private PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(mContext, 0,
                NavUtils.makeLauncherIntent(mContext),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Lets the buttons in the remote view control playback in the expanded
     * layout
     */
    private void initExpandedPlaybackActions(boolean isPlaying) {
        // Play and pause
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_play,
                retreivePlaybackActions(1));

        // Skip tracks
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_next,
                retreivePlaybackActions(2));

        // Previous tracks
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_previous,
                retreivePlaybackActions(3));

        // Stop and collapse the notification
        mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_base_collapse,
                retreivePlaybackActions(4));

        // Update the play button image
        mExpandedView.setImageViewResource(R.id.notification_expanded_base_play, getPlayPauseIcon(isPlaying));
    }

    /**
     * Lets the buttons in the remote view control playback in the normal layout
     */
    private void initPlaybackActions(boolean isPlaying) {
        // Play and pause
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_base_play,
                retreivePlaybackActions(1));

        // Skip tracks
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_base_next,
                retreivePlaybackActions(2));

        // Stop and collapse the notification
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_base_collapse,
                retreivePlaybackActions(4));

        // Update the play button image
        mNotificationTemplate.setImageViewResource(R.id.notification_base_play, getPlayPauseIcon(isPlaying));
    }

    /**
     * @param which Which {@link PendingIntent} to return
     * @return A {@link PendingIntent} ready to control playback
     */
    private PendingIntent retreivePlaybackActions(final int which) {
        Intent action;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(mContext, PlaybackService.class);
        switch (which) {
            case 1:
                // Play and pause
                action = new Intent(PlaybackConstants.TOGGLEPAUSE_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mContext, 1, action, 0);
                return pendingIntent;
            case 2:
                // Skip tracks
                action = new Intent(PlaybackConstants.NEXT_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mContext, 2, action, 0);
                return pendingIntent;
            case 3:
                // Previous tracks
                action = new Intent(PlaybackConstants.PREVIOUS_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mContext, 3, action, 0);
                return pendingIntent;
            case 4:
                // Stop and collapse the notification
                action = new Intent(PlaybackConstants.STOP_ACTION);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(mContext, 4, action, 0);
                return pendingIntent;
            default:
                break;
        }
        return null;
    }

    /**
     * Sets the track name, artist name, and album art in the normal layout
     */
    private void initCollapsedLayout(final String trackName, final String artistName,
            final Bitmap albumArt) {
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
    private void initExpandedLayout(final String trackName, final String artistName,
            final String albumName, final Bitmap albumArt) {
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
        if (VersionUtils.hasLollipop()) {
            return isPlaying ? R.drawable.ic_pause_black_36dp : R.drawable.ic_play_arrow_black_36dp;
        } else {
            return isPlaying ? R.drawable.ic_pause_white_36dp : R.drawable.ic_play_arrow_white_36dp;
        }
    }

}
