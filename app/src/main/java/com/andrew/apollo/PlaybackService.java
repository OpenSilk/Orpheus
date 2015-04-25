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

package com.andrew.apollo;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;

import com.andrew.apollo.helper.AlarmManagerHelper;
import com.andrew.apollo.helper.AudioManagerHelper;
import com.andrew.apollo.helper.BundleHelper;
import com.andrew.apollo.helper.PlaybackStateHelper;
import com.andrew.apollo.player.IPlayer;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.artwork.ArtworkProviderUtil;

import javax.inject.Inject;

import dagger.ObjectGraph;
import timber.log.Timber;

import static com.andrew.apollo.PlaybackConstants.*;

/**
 * Created by drew on 4/22/15.
 */
public class PlaybackService extends Service {

    ObjectGraph mGraph;
    @Inject PlaybackServiceBinder mBinder;
    @Inject AlarmManagerHelper mAlarmManagerHelper;
    @Inject AudioManagerHelper mAudioManagerHelper;
    NotificationHelper mNotificationHelper;
    MediaSessionHelper mMediaSessionHelper;
    ArtworkProviderUtil mArtworkUtil;

    @Inject PlaybackQueue mQueue;
    @Inject PlaybackStateHelper mPlaybackStateHelper;

    MediaSession mMediaSession;

    private IPlayer mPlayer;

    private int mConnectedClients = 0;
    private boolean mAnyActivityInForeground = false;
    private PowerManager.WakeLock mWakeLock;
    private boolean mPausedByTransientLossOfFocus = false;
    private boolean mIsSupposedToBePlaying;

    public PlaybackService() {

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
        if (mIsSupposedToBePlaying || mPausedByTransientLossOfFocus) {
            // Something is currently playing, or will be playing once
            // an in-progress action requesting audio focus ends, so don't stop
            // the service now.

            //Do nothing
        } else if (mQueue.playlistLength() > 0) {
            // If there is a playlist but playback is paused, then wait a while
            // before stopping the service, so that pause/resume isn't slow.
            mAlarmManagerHelper.scheduleDelayedShutdown();
        } else {
            stopSelf();
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Injection
        mGraph = ObjectGraph.create(new PlaybackModule(this));
        mGraph.inject(this);

        //Init MediaSession
        final ComponentName mediaButtonReceiverComponent = new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName());
        final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(mediaButtonReceiverComponent);
        final PendingIntent mediaButtonReceiverIntent = PendingIntent.getBroadcast(this,
                0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        mMediaSession = new MediaSessionCompat(this, getClass().getSimpleName(),
//                mediaButtonReceiverComponent, mediaButtonReceiverIntent);
        mMediaSession = new MediaSession(this, getClass().getSimpleName());
        mMediaSession.setMediaButtonReceiver(mediaButtonReceiverIntent);
        final PendingIntent activityIntent = PendingIntent.getActivity(this,
                0, NavUtils.makeLauncherIntent(this), PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSession.setSessionActivity(activityIntent);
//        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
//                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(new SessionCallback());
//        mMediaSession.setRatingType(RatingCompat.RATING_NONE);//TODO lastfm integration
        mMediaSession.setPlaybackState(getStateHelper().getState());
        //TODO set active after queue is loaded
        mMediaSession.setActive(true);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mAudioManagerHelper.abandonFocus();

        mAlarmManagerHelper.cancelDelayedShutdown();

        mMediaSession.setCallback(null);
        mMediaSession.release();

        mGraph = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                final String command = SERVICECMD.equals(action) ? intent.getStringExtra(CMDNAME) : null;
                handleIntentCommand(intent, action, command);
            }
        }
        return START_STICKY;
    }

    void handleIntentCommand(@NonNull Intent intent, String action, String command) {
        Timber.v("handleIntentCommand: action = %s, command = %s", action, command);
        MediaController controller = mMediaSession.getController();
        MediaController.TransportControls controls = controller.getTransportControls();
        PlaybackState state = controller.getPlaybackState();
        if (CMDNEXT.equals(command) || NEXT_ACTION.equals(action)) {
            controls.skipToNext();
        } else if (CMDPREVIOUS.equals(command) || PREVIOUS_ACTION.equals(action)) {
            if (state == null || state.getPosition() < REWIND_INSTEAD_PREVIOUS_THRESHOLD) {
                controls.skipToPrevious();
            } else {
                controls.seekTo(0);
                //TODO might need play
            }
        } else if (CMDTOGGLEPAUSE.equals(command) || TOGGLEPAUSE_ACTION.equals(action)) {
            if (state == null || state.getState() != PlaybackState.STATE_PLAYING) {
                controls.pause();
            } else {
                controls.play();
            }
        } else if (CMDPAUSE.equals(command) || PAUSE_ACTION.equals(action)) {
            controls.pause();
        } else if (CMDPLAY.equals(command)) {
            controls.play();
        } else if (CMDSTOP.equals(command) || STOP_ACTION.equals(action)) {
            controls.stop();
        } else if (REPEAT_ACTION.equals(action)) {
            controls.sendCustomAction(REPEAT_ACTION, null);
        } else if (SHUFFLE_ACTION.equals(action)) {
            controls.sendCustomAction(SHUFFLE_ACTION, null);
        }
        if (intent.getBooleanExtra(FROM_MEDIA_BUTTON, false)) {
            MediaButtonIntentReceiver.completeWakefulIntent(intent);
        }
    }

    public PlaybackQueue getQueue() {
        return mQueue;
    }

    public PlaybackStateHelper getStateHelper() {
        return mPlaybackStateHelper;
    }

    public IPlayer getPlayer() {
        return mPlayer;
    }

    public MediaController getController() {
        return mMediaSession.getController();
    }

    /*
     * AIDL
     */

    public MediaSession.Token getToken() {
        return mMediaSession.getSessionToken();
    }

    /*
     * MediaSession
     */

    class SessionCallback extends MediaSession.Callback {
        @Override
        public void onCommand(String command, Bundle args, ResultReceiver cb) {
            if (CMD.FOCUSCHANGE.equals(command)) {
                final int change = BundleHelper.getInt(args);
                switch (change) {
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    case AudioManager.AUDIOFOCUS_LOSS:
                        if (mIsSupposedToBePlaying) {
                            mPausedByTransientLossOfFocus = change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
                            mIsSupposedToBePlaying = false;
                        }
                        getPlayer().pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        getPlayer().duck();
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        if (!mIsSupposedToBePlaying && mPausedByTransientLossOfFocus) {
                            mPausedByTransientLossOfFocus = false;
                            getPlayer().play();
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        @Override
        public void onPlay() {
            if (!mAudioManagerHelper.requestFocus()){
                return;
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            if (mediaId == null) {
                return;
            }
            getPlayer().setDataSource(Uri.parse(mediaId));
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
        }

        @Override
        public void onSkipToQueueItem(long id) {
            if (mQueue.peekCurrent() == id) {
                //Nothing
            } else if (mQueue.peekNext() == id) {
                getPlayer().skipToNext();
            } else {
                //TODO
            }
        }

        @Override
        public void onPause() {
            getPlayer().pause();
        }

        @Override
        public void onSkipToNext() {
            getPlayer().skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
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
        public void onStop() {
            mAudioManagerHelper.abandonFocus();
            getPlayer().stop();
        }

        @Override
        public void onSeekTo(long pos) {
            getPlayer().seekTo(pos);
        }

        @Override
        public void onSetRating(Rating rating) {
            super.onSetRating(rating);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            if (REPEAT_ACTION.equals(action)) {

            } else if (SHUFFLE_ACTION.equals(action)) {

            }
        }
    }

}
