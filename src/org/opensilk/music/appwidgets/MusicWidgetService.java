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
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.andrew.apollo.MusicPlaybackService;

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.music.AppModule;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;

import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkProviderUtil;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Func6;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 3/31/14.
 */
public class MusicWidgetService extends Service {

    @dagger.Module(addsTo = AppModule.class, injects = MusicWidgetService.class)
    public static class Module {
        @Provides @Singleton @Named("activity")
        public EventBus provideEventBus() {
            return EventBus.getDefault();
        }
    }

    private static class Meta {
        ArtInfo artInfo;
        String artistName;
        String trackName;
        int shuffleMode;
        int repeatMode;
        boolean isplaying;
        Bitmap artwork;
    }

    public static final String WIDGET_TYPE = "widget_type";

    public static final int STYLE_LARGE_ONE = 0;
    public static final int STYLE_LARGE_TWO = 1;

    @Inject MusicServiceConnection mMusicService;

    private Scheduler.Worker mUpdateWorker;

    private AppWidgetManager mAppWidgetManager;
    private ArtworkProviderUtil mArtworkProvider;
    private ThemeHelper mThemeHelper;

    private int mAllocUpperBound;

    @Override
    public IBinder onBind(Intent intent) {
        return null; //Not bindable
    }

    @Override
    //@DebugLog
    public void onCreate() {
        Timber.v("onCreate()");
        super.onCreate();
        ((DaggerInjector)getApplication()).getObjectGraph().plus(new Module()).inject(this);
        mMusicService.bind();
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mArtworkProvider = new ArtworkProviderUtil(this);
        mThemeHelper = ThemeHelper.getInstance(this);
        mUpdateWorker = Schedulers.newThread().createWorker();
        mAllocUpperBound = computeMaximumWidgetBitmapMemory();
    }

    @Override
    //@DebugLog
    public void onDestroy() {
        Timber.v("onDestroy()");
        super.onDestroy();
        mMusicService.unbind();
        mUpdateWorker.unsubscribe();
    }

    @Override
    //@DebugLog
    public int onStartCommand(Intent intent, int flags, final int startId) {
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            final int appId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            final int widgetType = intent.getIntExtra(WIDGET_TYPE, -1);
            if (appId != -1 && widgetType != -1) {
                mUpdateWorker.schedule(new Action0() {
                    @Override
                    public void call() {
                        updateWidget(appId, startId, widgetType);
                    }
                });
            }
        }
        return START_NOT_STICKY;
    }

    //@DebugLog
    private void updateWidget(int appId, int startId, int widgetType) {
        Meta meta;
        try {
            meta = Observable.zip(
                    mMusicService.getCurrentArtInfo(),
                    mMusicService.getArtistName(),
                    mMusicService.getTrackName(),
                    mMusicService.getShuffleMode(),
                    mMusicService.getRepeatMode(),
                    mMusicService.isPlaying(),
                    new Func6<ArtInfo, String, String, Integer, Integer, Boolean, Meta>() {
                        @Override
                        public Meta call(ArtInfo artInfo, String s, String s2,
                                         Integer integer, Integer integer2, Boolean aBoolean) {
                            Meta meta = new Meta();
                            meta.artInfo = artInfo;
                            meta.artistName = s;
                            meta.trackName = s2;
                            meta.shuffleMode = integer;
                            meta.repeatMode = integer2;
                            meta.isplaying = aBoolean;
                            meta.artwork = mArtworkProvider.getArtworkThumbnail(meta.artInfo.artistName, meta.artInfo.albumName);
                            return meta;
                        }
                    }
            ).toBlocking().first();
        } catch (Exception e) {
            stopSelf(startId); //Will shut us down when last item is processed
            return;
        }
        final int bitmapSize = getBitmapSize(meta.artwork);
        Timber.i("Artwork size = %d, allocSize = %d, free = %d", bitmapSize, mAllocUpperBound, mAllocUpperBound - bitmapSize);
        if (bitmapSize >= mAllocUpperBound) {
            Timber.i("Artwork too large: %d > %d", bitmapSize, mAllocUpperBound);
            try {
                Bitmap tmp = meta.artwork;
                meta.artwork = Bitmap.createScaledBitmap(tmp, meta.artwork.getWidth()/2, meta.artwork.getHeight()/2, false);
                Timber.i("Artwork scaled down: new size = %d", getBitmapSize(tmp));
                if (meta.artwork != tmp) tmp.recycle();
            } catch (OutOfMemoryError e) {
                stopSelf(startId); //Will shut us down when last item is processed
                return;
            }
        }
        RemoteViews views = createView(appId, widgetType, meta);
        if (views != null) {
            try {
                mAppWidgetManager.updateAppWidget(appId, views);
            } catch (IllegalArgumentException e) {
                Timber.e(e, "Failed to update widget %d", appId);
            }
        }
        stopSelf(startId); //Will shut us down when last item is processed
    }

    /*
     * Create views depending on size, and style
     */
    public RemoteViews createView(int appId, int widgetType, Meta meta) {
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

        /* Album artwork -- set for all widgets */
        if (meta.artwork != null) {
            views.setImageViewBitmap(R.id.widget_album_art, meta.artwork);
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.default_artwork);
        }
        if (widget.compareTo(MusicWidget.ULTRA_MINI) > 0) {
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    NavUtils.makeLauncherIntent(this), PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_album_art, pendingIntent);
        }

        /* Pause / Play -- set for all widgets */
        views.setImageViewResource(R.id.widget_play, meta.isplaying ?
                R.drawable.ic_action_playback_pause_white : R.drawable.ic_action_playback_play_white);
        views.setOnClickPendingIntent(R.id.widget_play, buildPendingIntent(MusicPlaybackService.TOGGLEPAUSE_ACTION));

        /* Next / Prev */
        if (widget.compareTo(MusicWidget.MINI) >= 0) { // Mini, Small, Large
            views.setOnClickPendingIntent(R.id.widget_previous, buildPendingIntent(MusicPlaybackService.PREVIOUS_ACTION));
            views.setOnClickPendingIntent(R.id.widget_next, buildPendingIntent(MusicPlaybackService.NEXT_ACTION));
        }

        /* Artist name and song title */
        if (widget.compareTo(MusicWidget.SMALL) >= 0) { //Small, Large
            views.setTextViewText(R.id.widget_artist_name, meta.artistName);
            views.setTextViewText(R.id.widget_song_title, meta.trackName);
        }

        /* Shuffle / Repeat */
        if (widget == MusicWidget.LARGE) {
            views.setOnClickPendingIntent(R.id.widget_shuffle, buildPendingIntent(MusicPlaybackService.SHUFFLE_ACTION));
            views.setOnClickPendingIntent(R.id.widget_repeat, buildPendingIntent(MusicPlaybackService.REPEAT_ACTION));

            Drawable drawable;

            switch (meta.shuffleMode) {
                case MusicPlaybackService.SHUFFLE_NONE:
                    drawable = getResources().getDrawable(R.drawable.ic_action_playback_shuffle_white);
                    break;
                case MusicPlaybackService.SHUFFLE_AUTO:
                default:
                    drawable = mThemeHelper.getPrimaryColorShuffleButtonDrawable();
                    break;
            }
            if (drawable != null && drawable instanceof BitmapDrawable) {
                views.setImageViewBitmap(R.id.widget_shuffle, ((BitmapDrawable) drawable).getBitmap());
                drawable = null;
            }

            switch (meta.repeatMode) {
                case MusicPlaybackService.REPEAT_ALL:
                    drawable = mThemeHelper.getPrimaryColorRepeatButtonDrawable();
                    break;
                case MusicPlaybackService.REPEAT_CURRENT:
                    drawable = mThemeHelper.getPrimaryColorRepeatOneButtonDrawable();
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

    protected PendingIntent buildPendingIntent(String action) {
        Intent intent = new Intent(action).setComponent(new ComponentName(this, MusicPlaybackService.class));
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private int computeMaximumWidgetBitmapMemory() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return 6 * size.x *size.y;
    }

    private static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        } else {
            return bitmap.getByteCount();
        }
    }

}