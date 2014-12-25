/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.*;
import android.os.Process;
import android.text.TextUtils;

import com.andrew.apollo.provider.MusicProviderUtil;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.common.util.VersionUtils;

import java.util.ArrayList;
import java.util.List;

import static com.andrew.apollo.MusicPlaybackService.PLAYSTATE_CHANGED;
import static com.andrew.apollo.MusicPlaybackService.POSITION_CHANGED;
import static com.andrew.apollo.MusicPlaybackService.META_CHANGED;
import static com.andrew.apollo.MusicPlaybackService.QUEUE_CHANGED;

/**
 * Created by drew on 10/29/14.
 */
public class MediaSessionHelper {

    private final Impl IMPL;

    public MediaSessionHelper(MusicPlaybackService service) {
        if (VersionUtils.hasLollipop()) {
            IMPL = new LollipopImpl(service);
        } else if (VersionUtils.hasJellyBeanMR2()) {
            IMPL = new JellybeanMR2Impl(service);
        } else {
            IMPL = new IceCreamSandwichImpl(service);
        }
    }

    public void setup() {
        IMPL.setup();
    }

    public void teardown() {
        IMPL.teardown();
    }

    public void updateMeta(String what) {
        IMPL.updateMeta(what);
    }

    public void ping() {
        IMPL.ping();
    }

    public MediaSession.Token getSessionToken() {
        return IMPL.getMediaToken();
    }

    static abstract class Impl {
        final MusicPlaybackService mService;

        protected Impl(MusicPlaybackService mService) {
            this.mService = mService;
        }

        abstract void setup();
        abstract void teardown();
        abstract void updateMeta(String what);
        abstract void ping();

        MediaSession.Token getMediaToken() {
            return null;
        }

    }

    @SuppressWarnings("deprecation")
    static class IceCreamSandwichImpl extends Impl {

        RemoteControlClient mRemoteControlClient;
        ComponentName mMediaButtonReceiverComponent;
        AudioManager mAudioManager;

        IceCreamSandwichImpl(MusicPlaybackService mService) {
            super(mService);
        }

        @Override
        void setup() {
            mAudioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
            mMediaButtonReceiverComponent = new ComponentName(mService.getPackageName(),
                    MediaButtonIntentReceiver.class.getName());
            mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);

            final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON)
                    .setComponent(mMediaButtonReceiverComponent);
            mRemoteControlClient = new RemoteControlClient(
                    PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mediaButtonIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT));
            mAudioManager.registerRemoteControlClient(mRemoteControlClient);
            mRemoteControlClient.setTransportControlFlags(getFlags());
        }

        @Override
        void teardown() {
            mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
            mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
        }

        @Override
        void ping() {
            mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
        }

        @Override
        void updateMeta(String what) {
            int playState = mService.isPlaying()
                    ? RemoteControlClient.PLAYSTATE_PLAYING
                    : RemoteControlClient.PLAYSTATE_PAUSED;

            if (what.equals(PLAYSTATE_CHANGED) || what.equals(POSITION_CHANGED)) {
                setPlaybackState(playState);
            } else if (what.equals(META_CHANGED)) {
                Bitmap albumArt;
                if (VersionUtils.hasKitkat()) {
                    //Kitkat has fullscreen artwork
                    albumArt = mService.getAlbumArt();
                } else {
                    albumArt = mService.getAlbumArtThumbnail();
                }
                if (albumArt != null) {
                    // RemoteControlClient wants to recycle the bitmaps thrown at it, so we need
                    // to make sure not to hand out our cache copy
                    Bitmap.Config config = albumArt.getConfig();
                    if (config == null) {
                        config = Bitmap.Config.ARGB_8888;
                    }
                    albumArt = albumArt.copy(config, false);
                }

                mRemoteControlClient
                        .editMetadata(true)
                        .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, mService.getArtistName())
                        .putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                                !TextUtils.isEmpty(mService.getAlbumArtistName()) ? mService.getAlbumArtistName() : mService.getArtistName())
                        .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, mService.getAlbumName())
                        .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, mService.getTrackName())
                        .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, mService.duration())
                        .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, albumArt)
                        .apply();

                setPlaybackState(playState);
            }
        }

        void setPlaybackState(int playstate) {
            mRemoteControlClient.setPlaybackState(playstate);
        }

        int getFlags() {
            return RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                    | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                    | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                    | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                    | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                    | RemoteControlClient.FLAG_KEY_MEDIA_STOP;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressWarnings("deprecation")
    static class JellybeanMR2Impl extends IceCreamSandwichImpl  {

        JellybeanMR2Impl(MusicPlaybackService mService) {
            super(mService);
        }

        @Override
        void setup() {
            super.setup();
            mRemoteControlClient.setOnGetPlaybackPositionListener(
                    new RemoteControlClient.OnGetPlaybackPositionListener() {
                        @Override
                        public long onGetPlaybackPosition() {
                            return mService.position();
                        }
                    });
            mRemoteControlClient.setPlaybackPositionUpdateListener(
                    new RemoteControlClient.OnPlaybackPositionUpdateListener() {
                        @Override
                        public void onPlaybackPositionUpdate(long newPositionMs) {
                            mService.seek(newPositionMs);
                        }
                    });
        }

        @Override
        void setPlaybackState(int playState) {
            mRemoteControlClient.setPlaybackState(playState, mService.position(), 1.0f);
        }

        @Override
        int getFlags() {
            return super.getFlags() | RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE;
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static class LollipopImpl extends Impl {
        MediaSession mMediaSession;
        HandlerThread mHandlerThread;

        LollipopImpl(MusicPlaybackService mService) {
            super(mService);
        }

        @Override
        void setup() {
            mHandlerThread = new HandlerThread(getClass().getName(), Process.THREAD_PRIORITY_BACKGROUND);
            mHandlerThread.start();
            mMediaSession = new MediaSession(mService, mService.getClass().getName());
            mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mMediaSession.setCallback(new Callback(), new Handler(mHandlerThread.getLooper()));
            mMediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(
                    mService,
                    1,
                    new Intent(mService, MediaButtonIntentReceiver.class),
                    PendingIntent.FLAG_UPDATE_CURRENT
            ));
            mMediaSession.setSessionActivity(PendingIntent.getActivity(
                    mService,
                    2,
                    NavUtils.makeLauncherIntent(mService),
                    PendingIntent.FLAG_UPDATE_CURRENT
            ));
        }

        @Override
        void teardown() {
            mMediaSession.release();
            mHandlerThread.getLooper().quit();
        }

        @Override
        void updateMeta(String what) {
            switch (what) {
                case PLAYSTATE_CHANGED:
                case POSITION_CHANGED:
                    PlaybackState state = new PlaybackState.Builder()
                            .setActions(
                                    PlaybackState.ACTION_PLAY
                                    |PlaybackState.ACTION_PAUSE
                                    |PlaybackState.ACTION_SEEK_TO
                                    |PlaybackState.ACTION_SKIP_TO_NEXT
                                    |PlaybackState.ACTION_SKIP_TO_PREVIOUS
                                    |PlaybackState.ACTION_STOP
//                                    |PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM
                            )
                            .setActiveQueueItemId(mService.getAudioId())
                            .setState(
                                    mService.isPlaying() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                                    mService.position(),
                                    1.0f
                            )
                            .build();
                    mMediaSession.setPlaybackState(state);
                    break;
                case QUEUE_CHANGED:
                    //mMediaSession.setQueue(MusicProviderUtil.buildQueueList(mService, mService.getQueue()));
                    break;
                case META_CHANGED:
                    mMediaSession.setMetadata(buildMeta());
                    break;
            }
        }

        @Override
        void ping() {
            mMediaSession.setActive(true);
        }

        @Override
        MediaSession.Token getMediaToken() {
            return mMediaSession.getSessionToken();
        }

        MediaMetadata buildMeta() {
            return new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, mService.getArtistName())
                    .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
                            !TextUtils.isEmpty(mService.getAlbumArtistName())
                                    ? mService.getAlbumArtistName() : mService.getArtistName())
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, mService.getAlbumName())
                    .putString(MediaMetadata.METADATA_KEY_TITLE, mService.getTrackName())
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, mService.duration())
                    .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                            mService.getArtworkUri() != null ? mService.getArtworkUri().toString() : null)
                    .build();
        }

        class Callback extends MediaSession.Callback  {
            @Override
            public void onPause() {
                mService.pause();
            }

            @Override
            public void onPlay() {
                mService.play();
            }

            @Override
            public void onSeekTo(long pos) {
                mService.seek(pos);
            }

            @Override
            public void onSkipToNext() {
                mService.gotoNext(true);
            }

            @Override
            public void onSkipToPrevious() {
                MusicUtils.previous(mService);
            }

            @Override
            public void onStop() {
                mService.startService(new Intent(mService, MusicPlaybackService.class)
                        .setAction(MusicPlaybackService.STOP_ACTION));
            }

            @Override
            public void onSkipToQueueItem(long id) {
                long[] queue = mService.getQueue();
                for (int ii=0; ii<queue.length; ii++) {
                    if (queue[ii] == id) {
                        mService.setQueuePosition(ii);
                        break;
                    }
                }
            }
        }

    }

}
