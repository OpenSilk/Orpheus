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

    public static final String WIDGET_SIZE = "widget_size";
    public static final String WIDGET_STYLE = "widget_style";

    public static final int ULTRA_MINI = 1;
    public static final int MINI = 2;
    public static final int SMALL = 3;
    public static final int LARGE = 4;

    public static final int STYLE_LARGE_ONE = 0;
    public static final int STYLE_LARGE_TWO = 1;

    private Looper mUpdateLooper;
    private Handler mUpdateHandler;
    private Deque<Runnable> mPendingUpdates = new ArrayDeque<>(4);

    private boolean isBound = false;
    private MusicUtils.ServiceToken mToken;

    private AppWidgetManager mAppWidgetManager;
    private ArtworkProviderUtil mArtworkProvider;

    private String mArtistName;
    private String mTrackName;
    private int mShuffleMode;
    private int mRepeatMode;
    private boolean mIsPlaying;
    private Bitmap mArtwork;

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
        if (mUpdateLooper != null) {
            mUpdateHandler = new Handler(mUpdateLooper);
        }
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
            final int widgetSize = intent.getIntExtra(WIDGET_SIZE, -1);
            final int widgetStyle = intent.getIntExtra(WIDGET_STYLE, STYLE_LARGE_ONE);
            if (appId != -1 && widgetSize != -1) {
                final Runnable update = new Runnable() {
                    @Override
                    public void run() {
                        updateWidget(appId, startId, widgetSize, widgetStyle);
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
    private void updateWidget(int appId, int startId, int widgetSize, int widgetStyle) {
        String albumName = MusicUtils.getAlbumName();
        String albumArtistName = MusicUtils.getAlbumArtistName();
        long albumId = MusicUtils.getCurrentAlbumId();
        mArtistName = MusicUtils.getArtistName();
        mTrackName = MusicUtils.getTrackName();
        mShuffleMode = MusicUtils.getShuffleMode();
        mRepeatMode = MusicUtils.getRepeatMode();
        mIsPlaying = MusicUtils.isPlaying();

        /* Only query the artwork for the first startId, it'll be cached until the last id is done */
        if (startId == 1) {
            mArtwork = mArtworkProvider.getArtworkThumbnail(albumArtistName, albumName, albumId);
        }

        RemoteViews views = createView(widgetSize, widgetStyle);
        if (views != null) {
            mAppWidgetManager.updateAppWidget(appId, views);
        }
        stopSelf(startId); //Will shut us down when last item is processed
    }

    /*
     * Create views depending on size, and style
     * TODO: FIX WHEN OTHER LARGE WIDGETS ARE ADDED
     */
    public RemoteViews createView(int widgetSize, int widgetStyle) {
        int layoutId = -1;
        String idInfix = "";
        switch (widgetSize) {
            case ULTRA_MINI:
                layoutId = R.layout.music_widget_ultra_mini;
                idInfix = "ultra_mini";
                break;
            case MINI:
                layoutId = R.layout.music_widget_mini;
                idInfix = "mini";
                break;
            case SMALL:
                layoutId = R.layout.music_widget_small;
                idInfix = "small";
                break;
            case LARGE:
                if (widgetStyle == STYLE_LARGE_ONE) {
                    layoutId = R.layout.music_widget_large_style_one;
                    idInfix = "large_style_one";
                } else if (widgetStyle == STYLE_LARGE_TWO) {
                    layoutId = R.layout.music_widget_large_style_two;
                    idInfix = "large_style_two";
                }
        }

        RemoteViews views = new RemoteViews(getPackageName(), layoutId);
        ComponentName serviceName = new ComponentName(this, MusicPlaybackService.class);
        PendingIntent pendingIntent;

        /* Album artwork -- set for all widgets */
        if (mArtwork != null) {
            views.setImageViewBitmap(getId(idInfix, "album_art"), mArtwork);
        } else {
            views.setImageViewResource(getId(idInfix, "album_art"), R.drawable.default_artwork);
        }

        /* Pause / Play -- set for all widgets */
        views.setImageViewResource(getId(idInfix, "play"), mIsPlaying ?
                R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.TOGGLEPAUSE_ACTION, serviceName);
        views.setOnClickPendingIntent(getId(idInfix, "play"), pendingIntent);

        if (widgetSize == ULTRA_MINI) { // Ultra Mini only
            views.setOnClickPendingIntent(getId(idInfix, "mask"), pendingIntent);
        }

        /* Next / Prev */
        if (widgetSize >= MINI) { // Mini, Small, Large
            pendingIntent = buildPendingIntent(this, MusicPlaybackService.PREVIOUS_ACTION, serviceName);
            views.setOnClickPendingIntent(getId(idInfix, "previous"), pendingIntent);
            pendingIntent = buildPendingIntent(this, MusicPlaybackService.NEXT_ACTION, serviceName);
            views.setOnClickPendingIntent(getId(idInfix, "next"), pendingIntent);
        }

        /* Artist name and song title */
        if (widgetSize >= SMALL) { //Small, Large

            views.setTextViewText(getId(idInfix, "artist_name"), mArtistName);
            views.setTextViewText(getId(idInfix, "song_title"), mTrackName);
        }

        /* Shuffle / Repeat */
        if (widgetSize == LARGE) {
            pendingIntent = buildPendingIntent(this, MusicPlaybackService.SHUFFLE_ACTION, serviceName);
            views.setOnClickPendingIntent(getId(idInfix, "shuffle"), pendingIntent);
            pendingIntent = buildPendingIntent(this, MusicPlaybackService.REPEAT_ACTION, serviceName);
            views.setOnClickPendingIntent(getId(idInfix, "repeat"), pendingIntent);

            int resId;

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
            views.setImageViewResource(getId(idInfix, "shuffle"), resId);

            switch (mRepeatMode) {
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
            views.setImageViewResource(getId(idInfix, "repeat"), resId);
        }

        return views;
    }

    /*
     * Helpers
     */

    private int getId(String infix, String suffix) {
        return getResources().getIdentifier("widget_" + infix + "_" + suffix,
                "id", getPackageName());
    }

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