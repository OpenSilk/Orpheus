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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by andrew on 4/3/14.
 */
public class MusicWidgetLarge extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        /* Enable the BroadcastReceiver */
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            pm.setComponentEnabledSetting(
                    new ComponentName(context.getPackageName(), MusicWidgetReceiver.class.getName()),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            Intent intent = new Intent(context, MusicWidgetService.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            intent.putExtra(MusicWidgetService.WIDGET_SIZE, MusicWidgetService.LARGE);
            context.startService(intent);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d("MusicWidgetLarge", "onDeleted()");
        SharedPreferences.Editor prefs = context.getSharedPreferences(
                MusicWidgetSettings.PREFS_NAME, Context.MODE_MULTI_PROCESS).edit();
        for (int id : appWidgetIds) {
            prefs.remove(MusicWidgetSettings.PREF_PREFIX_KEY + id);
            prefs.apply();
        }
    }

    @Override
    public void onDisabled(Context context) {
        context.sendBroadcast(new Intent(MusicWidgetReceiver.QUERY_DISABLE));
    }

}
