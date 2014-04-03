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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.MusicPlaybackService;

import hugo.weaving.DebugLog;

/**
 * Created by andrew on 3/30/14.
 */
public class MusicWidget extends AppWidgetProvider {
    private static final String TAG = "MusicWidget";
    private static final boolean D = BuildConfig.DEBUG;

    static final int ULTRA_MINI = 1;
    static final int MINI = 2;
    static final int SMALL = 3;
    static final int LARGE = 4;

    @Override
    @DebugLog
    public void onReceive(Context context, Intent intent) {
        // Handle broadcasts from the music service
        if (MusicPlaybackService.PLAYSTATE_CHANGED.equals(intent.getAction())
                || MusicPlaybackService.META_CHANGED.equals(intent.getAction())
                || MusicPlaybackService.REPEATMODE_CHANGED.equals(intent.getAction())
                || MusicPlaybackService.SHUFFLEMODE_CHANGED.equals(intent.getAction())) {
            final AppWidgetManager manager = AppWidgetManager.getInstance(context);
            final int[] ids = manager.getAppWidgetIds(new ComponentName(context, MusicWidget.class));
            // Only post updates if we actually have widgets
            if (ids != null && ids.length > 0) {
                onUpdate(context, manager, ids);
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    @DebugLog
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appId : appWidgetIds) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appId);
            onAppWidgetOptionsChanged(context, appWidgetManager, appId, options);
        }
    }

    @Override
    @DebugLog
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        Intent intent = new Intent(context, MusicWidgetService.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra("widget_category", getWidgetCategory(newOptions));
        context.startService(intent);
    }

    @DebugLog
    private int getWidgetCategory(Bundle options) {
        final int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        final int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        final int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        final int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        if (D) Log.d(TAG, String.format("w=%d h=%d mw=%d mh=%d", width,height, minWidth, minHeight));
        // TODO not sure how to pick best one
        return Math.min(findBestMatch(minWidth, minHeight), findBestMatch(width, height));
    }

    /*
    <dimen name="music_widget_large_min_width">250dp</dimen>
    <dimen name="music_widget_large_min_height">110dp</dimen>

    <dimen name="music_widget_ultra_mini_min_width">40dp</dimen>
    <dimen name="music_widget_ultra_mini_min_height">40dp</dimen>

    <dimen name="music_widget_mini_min_width">110dp</dimen>
    <dimen name="music_widget_mini_min_height">110dp</dimen>

    <dimen name="music_widget_small_min_width">250dp</dimen>
    <dimen name="music_widget_small_min_height">56dp</dimen>
    <dimen name="music_widget_small_artwork_size">56dp</dimen>
     */

    @DebugLog
    private int findBestMatch(int w, int h) {
        if (w>=250) {
            if (h>=110) {
                return LARGE;
            } else {
                return SMALL;
            }
        } else {
            if (h<110 && w<110) {
                return ULTRA_MINI;
            } else {
                return MINI;
            }
        }
    }

}
