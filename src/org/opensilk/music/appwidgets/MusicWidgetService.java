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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.widget.RemoteViews;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.artwork.ArtworkProviderUtil;
import org.opensilk.music.ui.activities.HomeSlidingActivity;

import java.util.ArrayDeque;
import java.util.Deque;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 3/31/14.
 */
public class MusicWidgetService extends Service implements ServiceConnection {

    public static final String WIDGET_TYPE = "widget_type";

    public static final int STYLE_LARGE_ONE = 0;
    public static final int STYLE_LARGE_TWO = 1;

    private Looper mUpdateLooper;
    private Handler mUpdateHandler;
    private Deque<Runnable> mPendingUpdates = new ArrayDeque<>(4);

    private boolean isBound = false;
    private MusicUtils.ServiceToken mToken;

    private AppWidgetManager mAppWidgetManager;
    private ArtworkProviderUtil mArtworkProvider;
    private ThemeHelper mThemeHelper;

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
        mThemeHelper = ThemeHelper.getInstance(this);
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
    public int onStartCommand(Intent intent, int flags, final int startId) {
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            final int appId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            final int widgetType = intent.getIntExtra(WIDGET_TYPE, -1);
            if (appId != -1 && widgetType != -1) {
                final Runnable update = new Runnable() {
                    @Override
                    public void run() {
                        updateWidget(appId, startId, widgetType);
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
    private void updateWidget(int appId, int startId, int widgetType) {
        String albumName = MusicUtils.getAlbumName();
        String albumArtistName = MusicUtils.getAlbumArtistName();
        long albumId = MusicUtils.getCurrentAlbumId();
        mArtistName = MusicUtils.getArtistName();
        mTrackName = MusicUtils.getTrackName();
        mShuffleMode = MusicUtils.getShuffleMode();
        mRepeatMode = MusicUtils.getRepeatMode();
        mIsPlaying = MusicUtils.isPlaying();
        mArtwork = mArtworkProvider.getArtworkThumbnail(albumArtistName, albumName, albumId);

        RemoteViews views = createView(appId, widgetType);
        if (views != null) {
            mAppWidgetManager.updateAppWidget(appId, views);
        }
        stopSelf(startId); //Will shut us down when last item is processed
    }

    /*
     * Create views depending on size, and style
     */
    public RemoteViews createView(int appId, int widgetType) {
        final MusicWidget widget = MusicWidget.valueOf(widgetType);
        int layoutId = -1;
        switch (widget) {
            case ULTRA_MINI:
                layoutId = R.layout.music_widget_ultra_mini;
                break;
            case MINI:
                layoutId = R.layout.music_widget_mini;
                break;
            case SMALL:
                layoutId = R.layout.music_widget_small;
                break;
            case LARGE:
                SharedPreferences prefs = getSharedPreferences(MusicWidgetSettings.PREFS_NAME,
                        Context.MODE_MULTI_PROCESS);
                int widgetStyle = prefs.getInt(MusicWidgetSettings.PREF_PREFIX_KEY + appId,
                        MusicWidgetService.STYLE_LARGE_ONE);
                switch (widgetStyle) {
                    case STYLE_LARGE_TWO:
                        layoutId = R.layout.music_widget_large_style_two;
                        break;
                    case STYLE_LARGE_ONE:
                    default:
                        layoutId = R.layout.music_widget_large_style_one;
                        break;
                }
        }

        RemoteViews views = new RemoteViews(getPackageName(), layoutId);
        ComponentName serviceName = new ComponentName(this, MusicPlaybackService.class);
        PendingIntent pendingIntent;

        /* Album artwork -- set for all widgets */
        if (mArtwork != null) {
            views.setImageViewBitmap(R.id.widget_album_art, mArtwork);
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.default_artwork);
        }
        if (widget.compareTo(MusicWidget.ULTRA_MINI) > 0) {
            pendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, HomeSlidingActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_album_art, pendingIntent);
        }

        /* Pause / Play -- set for all widgets */
        views.setImageViewResource(R.id.widget_play, mIsPlaying ?
                R.drawable.ic_action_playback_pause_white : R.drawable.ic_action_playback_play_white);
        pendingIntent = buildPendingIntent(this, MusicPlaybackService.TOGGLEPAUSE_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_play, pendingIntent);

        if (widget == MusicWidget.ULTRA_MINI) { // Ultra Mini only
            views.setOnClickPendingIntent(R.id.widget_mask, pendingIntent);
        }

        /* Next / Prev */
        if (widget.compareTo(MusicWidget.MINI) >= 0) { // Mini, Small, Large
            pendingIntent = buildPendingIntent(this, MusicPlaybackService.PREVIOUS_ACTION, serviceName);
            views.setOnClickPendingIntent(R.id.widget_previous, pendingIntent);
            pendingIntent = buildPendingIntent(this, MusicPlaybackService.NEXT_ACTION, serviceName);
            views.setOnClickPendingIntent(R.id.widget_next, pendingIntent);
        }

        /* Artist name and song title */
        if (widget.compareTo(MusicWidget.SMALL) >= 0) { //Small, Large

            views.setTextViewText(R.id.widget_artist_name, mArtistName);
            views.setTextViewText(R.id.widget_song_title, mTrackName);
        }

        /* Shuffle / Repeat */
        if (widget == MusicWidget.LARGE) {
            pendingIntent = buildPendingIntent(this, MusicPlaybackService.SHUFFLE_ACTION, serviceName);
            views.setOnClickPendingIntent(R.id.widget_shuffle, pendingIntent);
            pendingIntent = buildPendingIntent(this, MusicPlaybackService.REPEAT_ACTION, serviceName);
            views.setOnClickPendingIntent(R.id.widget_repeat, pendingIntent);

            Drawable drawable;

            switch (mShuffleMode) {
                case MusicPlaybackService.SHUFFLE_NONE:
                    drawable = getResources().getDrawable(R.drawable.ic_action_playback_shuffle_white);
                    break;
                case MusicPlaybackService.SHUFFLE_AUTO:
                default:
                    drawable = mThemeHelper.getShuffleButtonDrawable();
                    break;
            }
            if (drawable != null && drawable instanceof BitmapDrawable) {
                views.setImageViewBitmap(R.id.widget_shuffle, ((BitmapDrawable) drawable).getBitmap());
                drawable = null;
            }

            switch (mRepeatMode) {
                case MusicPlaybackService.REPEAT_ALL:
                    drawable = mThemeHelper.getRepeatButtonDrawable();
                    break;
                case MusicPlaybackService.REPEAT_CURRENT:
                    drawable = mThemeHelper.getRepeatOneButtonDrawable();
                    break;
                default:
                    drawable = getResources().getDrawable(R.drawable.ic_action_playback_repeat_white);
                    break;
            }
            if (drawable != null && drawable instanceof BitmapDrawable) {
                views.setImageViewBitmap(R.id.widget_repeat, ((BitmapDrawable) drawable).getBitmap());
                drawable = null;
            }
        }

        return views;
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
    public void onServiceConnected(ComponentName name, IBinder service) {
        isBound = true;
        while (mPendingUpdates.peek() != null) {
            mUpdateHandler.post(mPendingUpdates.poll());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isBound = false;
        mToken = null;
        //TODO maybe rebind
    }

}