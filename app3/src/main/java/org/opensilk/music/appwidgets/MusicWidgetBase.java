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

package org.opensilk.music.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;
import android.widget.RemoteViews;

import org.opensilk.music.R;
import org.opensilk.music.playback.MediaMetadataHelper;
import org.opensilk.music.playback.NavUtils;
import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.appwidget.AppWidgetService;
import org.opensilk.music.playback.service.IntentHelper;

import java.util.Arrays;

import timber.log.Timber;

/**
 * Created by drew on 11/20/15.
 */
public class MusicWidgetBase extends AppWidgetProvider {

    protected AppWidgetService mService;

    @Override
    public void onReceive(Context context, Intent intent) {
        mService = AppWidgetService.getService(context);
        if (AppWidgetService.WIDGET_UPDATE_ACTION.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int ids[] = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context.getPackageName(), getClass().getName()));
            if (ids != null && ids.length > 0) {
                onUpdate(context, appWidgetManager, ids);
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    protected void setupArtwork(Context context, RemoteViews views) {
        Bitmap bitmap = null;
        if (mService.getMeta() != null && (bitmap = MediaMetadataHelper.getIcon(mService.getMeta())) != null) {
            int maxMem = computeMaximumWidgetBitmapMemory(context);
            if (getBitmapSize(bitmap) > maxMem) {
                bitmap = scaleBitmap(bitmap, context.getResources()
                        .getDimensionPixelSize(R.dimen.notification_bitmap_resize));
            }
            views.setImageViewBitmap(R.id.widget_album_art, bitmap);
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.default_artwork);
        }
    }

    protected void setupArtworkIntent(Context context, RemoteViews views) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 5,
                NavUtils.makeLauncherIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_album_art, pendingIntent);
    }

    protected void setupPlayBtn(Context context, RemoteViews views) {
        int playbtnRes = R.drawable.ic_play_white_36dp;
        if (mService.getPlaybackState() != null && PlaybackStateHelper.shouldShowPauseButton(mService.getPlaybackState())) {
            playbtnRes = R.drawable.ic_pause_white_36dp;
        }
        views.setImageViewResource(R.id.widget_play, playbtnRes);
        views.setOnClickPendingIntent(R.id.widget_play, buildPendingIntent(context, PlaybackConstants.TOGGLEPAUSE_ACTION));
    }

    protected void setupNextBtnIntent(Context context, RemoteViews views) {
        views.setOnClickPendingIntent(R.id.widget_next, buildPendingIntent(context, PlaybackConstants.NEXT_ACTION));
    }

    protected void setupPreviousBtnIntent(Context context, RemoteViews views) {
        views.setOnClickPendingIntent(R.id.widget_previous, buildPendingIntent(context, PlaybackConstants.PREVIOUS_ACTION));
    }

//    protected void setupShuffleBtnIntent(Context context, RemoteViews views) {
//        views.setOnClickPendingIntent(R.id.widget_shuffle, buildPendingIntent(context, PlaybackConstants.SHUFFLE_ACTION));
//    }
//
//    protected void setupRepeatBtnIntent(Context context, RemoteViews views) {
//        views.setOnClickPendingIntent(R.id.widget_repeat, buildPendingIntent(context, PlaybackConstants.REPEAT_ACTION));
//    }

    protected void setupTrackTitle(Context context, RemoteViews views) {
        if (mService.getMeta() != null) {
            views.setTextViewText(R.id.widget_song_title, MediaMetadataHelper.getDisplayName(mService.getMeta()));
        }
    }

    protected void setupArtistName(Context context, RemoteViews views) {
        if (mService.getMeta() != null) {
            views.setTextViewText(R.id.widget_artist_name, MediaMetadataHelper.getArtistName(mService.getMeta()));
        }
    }

    protected void setupArtistPlusAlbumName(Context context, RemoteViews views) {
        if (mService.getMeta() != null) {
            String text = context.getString(R.string.something_circle_something,
                    MediaMetadataHelper.getArtistName(mService.getMeta()),
                    MediaMetadataHelper.getAlbumName(mService.getMeta()));
            views.setTextViewText(R.id.widget_artist_and_album_names, text);
        }
    }

    protected void postUpdate(AppWidgetManager appWidgetManager, int[] appWidgetIds, RemoteViews views) {
        try {
            appWidgetManager.updateAppWidget(appWidgetIds, views);
        } catch (IllegalArgumentException e) {
            Timber.w(e, "Failed to update widgets %s", Arrays.toString(appWidgetIds));
        }
    }

    protected PendingIntent buildPendingIntent(Context context, String action) {
        Intent intent = new Intent(action).setComponent(IntentHelper.getComponent(context));
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Bitmap scaleBitmap(Bitmap bmp, int maxSize) {
        float maxSizeF = maxSize;
        float widthScale = maxSizeF / bmp.getWidth();
        float heightScale = maxSizeF / bmp.getHeight();
        float scale = Math.min(widthScale, heightScale);
        int height = (int) (bmp.getHeight() * scale);
        int width = (int) (bmp.getWidth() * scale);
        return Bitmap.createScaledBitmap(bmp, width, height, true);
    }

    private int computeMaximumWidgetBitmapMemory(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
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
