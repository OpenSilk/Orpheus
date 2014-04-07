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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import com.andrew.apollo.MusicPlaybackService;

/**
 * Created by andrew on 4/3/14.
 */
public class MusicWidgetReceiver extends BroadcastReceiver {

    public static final String QUERY_DISABLE = MusicPlaybackService.APOLLO_PACKAGE_NAME + ".QUERY_DISABLE";

    @Override
    public void onReceive(Context context, Intent recvIntent) {
        Log.d("MusicWidgetReceiver", "onReceiver: " + recvIntent.toString());
        boolean disable = true;

        AppWidgetManager gm = AppWidgetManager.getInstance(context);

        /*
         *  This can be simplified, but it works right now. The best route would
         *  probably be using one startService(), passing int arrays in an extra
         */
        ComponentName umini = new ComponentName(context, MusicWidgetUltraMini.class);
        for (int id : gm.getAppWidgetIds(umini)) {
            Intent intent = new Intent(context, MusicWidgetService.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            intent.putExtra(MusicWidgetService.WIDGET_SIZE, MusicWidgetService.ULTRA_MINI);
            context.startService(intent);
            disable = false;
        }

        ComponentName mini = new ComponentName(context, MusicWidgetMini.class);
        for (int id : gm.getAppWidgetIds(mini)) {
            Intent intent = new Intent(context, MusicWidgetService.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            intent.putExtra(MusicWidgetService.WIDGET_SIZE, MusicWidgetService.MINI);
            context.startService(intent);
            disable = false;
        }

        ComponentName small = new ComponentName(context, MusicWidgetSmall.class);
        for (int id : gm.getAppWidgetIds(small)) {
            Intent intent = new Intent(context, MusicWidgetService.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            intent.putExtra(MusicWidgetService.WIDGET_SIZE, MusicWidgetService.SMALL);
            context.startService(intent);
            disable = false;
        }

        ComponentName large = new ComponentName(context, MusicWidgetLarge.class);
        SharedPreferences prefs = context.getSharedPreferences(MusicWidgetSettings.PREFS_NAME,
                Context.MODE_MULTI_PROCESS);
        for (int id : gm.getAppWidgetIds(large)) {
            Intent intent = new Intent(context, MusicWidgetService.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            intent.putExtra(MusicWidgetService.WIDGET_SIZE, MusicWidgetService.LARGE);
            intent.putExtra(MusicWidgetService.WIDGET_STYLE, prefs.getInt(
                    MusicWidgetSettings.PREF_PREFIX_KEY + id, MusicWidgetService.STYLE_LARGE_ONE));
            context.startService(intent);
            disable = false;
        }
        /* If no widgets are left, disable the receiver */
        if (disable) {
            Log.i("MusicWidgetReceiver", "No widget Id's left, disabling the receiver");
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                pm.setComponentEnabledSetting(
                        new ComponentName(context.getPackageName(), getClass().getName()),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }

    }
}
