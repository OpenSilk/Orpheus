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

package org.opensilk.music;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.andrew.apollo.MusicPlaybackService;

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.music.appwidgets.MusicWidget;
import org.opensilk.music.appwidgets.MusicWidgetService;
import org.opensilk.music.muzei.MuzeiService;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.android.apps.muzei.api.internal.ProtocolConstants.ACTION_HANDLE_COMMAND;
import static com.google.android.apps.muzei.api.internal.ProtocolConstants.EXTRA_COMMAND_ID;

/**
 * Handles broadcasts send by the music service
 * for non persistent services that run in the ui process
 *
 * Created by drew on 4/22/14.
 */
public class ServiceBroadcastReceiver extends BroadcastReceiver {

    @dagger.Module(
            addsTo = AppModule.class,
            injects = ServiceBroadcastReceiver.class
    )
    public static class Module {}

    @Inject AppPreferences mSettings;

    @Override
    public void onReceive(Context context, Intent intent) {
        ((DaggerInjector) context.getApplicationContext()).getObjectGraph().plus(new Module()).inject(this);
        final String action = intent.getAction();
        Timber.d("Received action=%s", action);
        if (action != null) {
            switch (action) {
                case MusicPlaybackService.META_CHANGED:
                    updateWidgets(context);
                    maybeUpdateWallpaper(context);
                    break;
                case MusicPlaybackService.PLAYSTATE_CHANGED:
                    updateWidgets(context);
                    break;
                case MusicPlaybackService.REFRESH:
                    // Cancel the broadcast so we aren't constantly refreshing
//                    context.removeStickyBroadcast(intent);
                    break;
                case MusicPlaybackService.REPEATMODE_CHANGED:
                case MusicPlaybackService.SHUFFLEMODE_CHANGED:
                    // Large is only one with repeat/shuffle buttons
                    updateWidgets(context, MusicWidget.LARGE);
                    break;
                case MusicPlaybackService.QUEUE_CHANGED:
                    break;
            }
        }
    }

    /**
     * Updates all widget types
     * @param context
     */
    private void updateWidgets(Context context) {
        for (MusicWidget widget : MusicWidget.values()) {
            updateWidgets(context, widget);
        }
    }

    /**
     * Updates specified widget type
     * @param context
     * @param widget
     */
    private void updateWidgets(Context context, MusicWidget widget) {
        AppWidgetManager gm = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, widget.getWidgetClass());
        for (int id : gm.getAppWidgetIds(cn)) {
            Intent intent = new Intent(context, MusicWidgetService.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            intent.putExtra(MusicWidgetService.WIDGET_TYPE, widget.ordinal());
            context.startService(intent);
        }
    }

    /**
     * Updates muzei wallpaper if enabled
     * @param context
     */
    private void maybeUpdateWallpaper(Context context) {
        if (mSettings.getBoolean(MuzeiService.MUZEI_EXTENSION_ENABLED, false)) {
            context.startService(new Intent(context, MuzeiService.class)
                    .setAction(ACTION_HANDLE_COMMAND)
                    .putExtra(EXTRA_COMMAND_ID, MuzeiService.BUILTIN_COMMAND_ID_NEXT_ARTWORK));
        }
    }
}
