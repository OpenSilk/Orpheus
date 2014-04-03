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

package org.opensilk.music.appwidgets;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.widget.RemoteViews;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.artwork.ArtworkProviderUtil;

import java.util.ArrayDeque;
import java.util.Deque;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 3/31/14.
 */
public class MusicWidgetService extends Service implements ServiceConnection {

    private Looper mUpdateLooper;
    private Handler mUpdateHandler;
    private Deque<Runnable> mPendingUpdates = new ArrayDeque<>(4);

    private boolean isBound = false;
    private MusicUtils.ServiceToken mToken;

    private AppWidgetManager mAppWidgetManager;
    private ArtworkProviderUtil mArtworkProvider;

    @Override
    public IBinder onBind(Intent intent) {
        return null; //Not bindable
    }

    @Override
    @DebugLog
    public void onCreate() {
        super.onCreate();
        mToken = MusicUtils.bindToService(this, this);
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mArtworkProvider = new ArtworkProviderUtil(this);
        HandlerThread thread = new HandlerThread("WidgetService", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        // All widget updates are posted here to prevent blocking the main thread,
        // This mostly has to be done for bootup and reinstalls since the widget
        // might request an update before our content provider is ready
        // if we call into the ArtworkProvider /from/ the main thread we will prevent
        // the ArtworkService from starting.
        mUpdateLooper = thread.getLooper();
        mUpdateHandler = new Handler(mUpdateLooper);
    }

    @Override
    @DebugLog
    public void onDestroy() {
        super.onDestroy();
        MusicUtils.unbindFromService(mToken);
        mUpdateLooper.quit();
    }

    @Override
    @DebugLog
    public synchronized int onStartCommand(Intent intent, int flags, final int startId) {
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            final int appId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            final int appCategory = intent.getIntExtra("widget_category", -1);
            if (appId != -1 && appCategory != -1) {
                final Runnable update = new Runnable() {
                    @Override
                    public void run() {
                        updateWidget(appId, appCategory, startId);
                    }
                };
                if (isBound) {
                    mUpdateHandler.post(update);
                } else {
                    mPendingUpdates.add(update);
                }
            }
        }
        return START_NOT_STICKY;
    }

    @DebugLog
    private void updateWidget(int appId, int appCategory, int startId) {
        RemoteViews views = null;
        switch (appCategory) {
            case MusicWidget.ULTRA_MINI:
                views = createUltraMiniView();
                break;
            case MusicWidget.MINI:
                views = createMiniView();
                break;
            case MusicWidget.SMALL:
                views = createSmallView();
                break;
            case MusicWidget.LARGE:
                views = createLargeView();
                break;
        }
        if (views != null) {
            mAppWidgetManager.updateAppWidget(appId, views);
        }
        stopSelf(startId); //Will shut us down when last item is processed
    }

    /*
     * Views
     */

    public RemoteViews createUltraMiniView() {
        String mAlbumName = MusicUtils.getAlbumName();
        String mAlbumArtistName = MusicUtils.getAlbumArtistName();
        String mArtistName = MusicUtils.getArtistName();
        String mTrackName = MusicUtils.getTrackName();
        int mShuffleMode = MusicUtils.getShuffleMode();
        int mRepeateMode = MusicUtils.getRepeatMode();
        long mAlbumId = MusicUtils.getCurrentAlbumId();
        boolean mIsPlaying = MusicUtils.isPlaying();

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.music_widget_ultra_mini);
        Bitmap artwork = mArtworkProvider.getArtworkThumbnail(mArtistName, mAlbumName, mAlbumId);
        ComponentName serviceName = new ComponentName(this, MusicPlaybackService.class);
        PendingIntent pendingIntent;

        /* Album artwork */
        views.setImageViewBitmap(R.id.widget_ultra_mini_album_art, artwork);

        /* Pause / Play */
        views.setImageViewResource(R.id.widget_ultra_mini_play, mIsPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.TOGGLEPAUSE_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_ultra_mini_mask, pendingIntent);

        return views;
    }

    public RemoteViews createMiniView() {
        String mAlbumName = MusicUtils.getAlbumName();
        String mAlbumArtistName = MusicUtils.getAlbumArtistName();
        String mArtistName = MusicUtils.getArtistName();
        String mTrackName = MusicUtils.getTrackName();
        int mShuffleMode = MusicUtils.getShuffleMode();
        int mRepeateMode = MusicUtils.getRepeatMode();
        long mAlbumId = MusicUtils.getCurrentAlbumId();
        boolean mIsPlaying = MusicUtils.isPlaying();

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.music_widget_mini);
        Bitmap artwork = mArtworkProvider.getArtworkThumbnail(mAlbumArtistName, mAlbumName, mAlbumId);
        ComponentName serviceName = new ComponentName(this, MusicPlaybackService.class);
        PendingIntent pendingIntent;

        // Album artwork
        views.setImageViewBitmap(R.id.widget_mini_album_art, artwork);

        // Pause / Play
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.TOGGLEPAUSE_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_mini_play, pendingIntent);
        views.setImageViewResource(R.id.widget_mini_play, mIsPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);

        // Next / Prev
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.PREVIOUS_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_mini_previous, pendingIntent);
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.NEXT_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_mini_next, pendingIntent);

        return views;
    }

    public RemoteViews createSmallView() {
        String mAlbumName = MusicUtils.getAlbumName();
        String mAlbumArtistName = MusicUtils.getAlbumArtistName();
        String mArtistName = MusicUtils.getArtistName();
        String mTrackName = MusicUtils.getTrackName();
        int mShuffleMode = MusicUtils.getShuffleMode();
        int mRepeateMode = MusicUtils.getRepeatMode();
        long mAlbumId = MusicUtils.getCurrentAlbumId();
        boolean mIsPlaying = MusicUtils.isPlaying();

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.music_widget_small);
        Bitmap artwork = mArtworkProvider.getArtwork(mAlbumArtistName, mAlbumName, mAlbumId);
        ComponentName serviceName = new ComponentName(this, MusicPlaybackService.class);
        PendingIntent pendingIntent;

        /* Artist name and song title */
        views.setTextViewText(R.id.widget_small_artist_name, mArtistName);
        views.setTextViewText(R.id.widget_small_song_title, mTrackName);

        /* Album artwork */
        views.setImageViewBitmap(R.id.widget_small_album_art, artwork);

        /* Pause / Play */
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.TOGGLEPAUSE_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_small_play, pendingIntent);
        views.setImageViewResource(R.id.widget_small_play, mIsPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);

        /* Next / Prev */
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.PREVIOUS_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_small_previous, pendingIntent);
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.NEXT_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_small_next, pendingIntent);


        return views;
    }

    public RemoteViews createLargeView() {
        String mAlbumName = MusicUtils.getAlbumName();
        String mAlbumArtistName = MusicUtils.getAlbumArtistName();
        String mArtistName = MusicUtils.getArtistName();
        String mTrackName = MusicUtils.getTrackName();
        int mShuffleMode = MusicUtils.getShuffleMode();
        int mRepeateMode = MusicUtils.getRepeatMode();
        long mAlbumId = MusicUtils.getCurrentAlbumId();
        boolean mIsPlaying = MusicUtils.isPlaying();

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.music_widget_large);
        Bitmap artwork = mArtworkProvider.getArtwork(mAlbumArtistName, mAlbumName, mAlbumId);
        ComponentName serviceName = new ComponentName(this, MusicPlaybackService.class);
        PendingIntent pendingIntent;

        /* Artist name and song title */
        views.setTextViewText(R.id.widget_large_artist_name, mArtistName);
        views.setTextViewText(R.id.widget_large_song_title, mTrackName);

        /* Album artwork */
        views.setImageViewBitmap(R.id.widget_large_album_art, artwork);

        /* Pause / Play */
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.TOGGLEPAUSE_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_large_play, pendingIntent);
        views.setImageViewResource(R.id.widget_large_play, mIsPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);

        /* Next / Prev */
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.PREVIOUS_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_large_previous, pendingIntent);
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.NEXT_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_large_next, pendingIntent);

        /* Shuffle / Repeat */
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.SHUFFLE_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_large_shuffle, pendingIntent);
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.REPEAT_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_large_repeat, pendingIntent);

        int resId = -1;

        switch (mShuffleMode) {
            case MusicPlaybackService.SHUFFLE_NONE:
                resId = R.drawable.btn_playback_shuffle;
                break;
            case MusicPlaybackService.SHUFFLE_AUTO:
                resId = R.drawable.btn_playback_shuffle_all;
                break;
            default:
                resId = R.drawable.btn_playback_shuffle_all;
                break;
        }
        views.setImageViewResource(R.id.widget_large_shuffle, resId);

        switch (mRepeateMode) {
            case MusicPlaybackService.REPEAT_ALL:
                resId = R.drawable.btn_playback_repeat_all;
                break;
            case MusicPlaybackService.REPEAT_CURRENT:
                resId = R.drawable.btn_playback_repeat_one;
                break;
            default:
                resId = R.drawable.btn_playback_repeat;
                break;
        }
        views.setImageViewResource(R.id.widget_large_repeat, resId);

        return views;
    }

    /*
     * Helpers
     */

    protected PendingIntent buildPendingIntent(Context context, String action,
                                               ComponentName serviceName) {
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);
        intent.putExtra(MusicPlaybackService.NOW_IN_FOREGROUND, false);
        return PendingIntent.getService(context, 0, intent, 0);
    }


    /*
     * Service Connection callbacks
     */
    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder service) {
        isBound = true;
        while (mPendingUpdates.peek() != null) {
            mUpdateHandler.post(mPendingUpdates.poll());
        }
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
        isBound = false;
        mToken = null;
        //TODO maybe rebind
    }

}