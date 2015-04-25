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

package com.andrew.apollo;

import android.app.AlarmManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.browse.MediaBrowser;
import android.service.media.MediaBrowserService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 4/22/15.
 */
@Module(
        injects = PlaybackService.class,
        library = true
)
public class PlaybackModule {
    final PlaybackService service;

    public PlaybackModule(PlaybackService service) {
        this.service = service;
    }

    @Provides @Singleton
    public PlaybackService provideService() {
        return service;
    }

    @Provides @Singleton
    public AlarmManager provideAlarmManager(PlaybackService service) {
        return (AlarmManager) service.getSystemService(Context.ALARM_SERVICE);
    }

    @Provides @Singleton
    public AudioManager provideAudioManager(PlaybackService service) {
        return (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
    }
}
