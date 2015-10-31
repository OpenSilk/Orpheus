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

package org.opensilk.common.core.dagger2;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 10/31/15.
 */
@Module
public class SystemServicesModule {
    @Provides @Singleton
    public NotificationManager provideNotificationManager(@ForApplication Context context) {
        return (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    @Provides @Singleton
    public AlarmManager provideAlarmManager(@ForApplication Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    @Provides @Singleton
    public AudioManager provideAudioManager(@ForApplication Context context) {
        return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
    @Provides @Singleton
    public PowerManager providePowerManager(@ForApplication Context context) {
        return (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }
    @Provides @Singleton
    public WifiManager provideWifiManager(@ForApplication Context context) {
        return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }
    @Provides @Singleton
    public ConnectivityManager provideConnectivityManager(@ForApplication Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
}
