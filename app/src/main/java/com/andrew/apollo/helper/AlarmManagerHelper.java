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

package com.andrew.apollo.helper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.PlaybackService;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

import static com.andrew.apollo.PlaybackConstants.*;

/**
 * Created by drew on 4/22/15.
 */
@Singleton
public class AlarmManagerHelper {
    final PlaybackService mService;
    final AlarmManager mAlarmManager;

    PendingIntent mShutdownIntent;
    boolean mShutdownScheduled;

    @Inject
    public AlarmManagerHelper(
            PlaybackService service,
            AlarmManager alarmManager
    ) {
        mService = service;
        mAlarmManager = alarmManager;
    }

    public void scheduleDelayedShutdown() {
        Timber.v("Scheduling shutdown in %d ms", IDLE_DELAY);
        final Intent shutdownIntent = new Intent(mService, MusicPlaybackService.class).setAction(SHUTDOWN);
        mShutdownIntent = PendingIntent.getService(mService, 0, shutdownIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + IDLE_DELAY, mShutdownIntent);
        mShutdownScheduled = true;
    }

    public void cancelDelayedShutdown() {
        Timber.d("Cancelling delayed shutdown, scheduled = %s", mShutdownScheduled);
        if (mShutdownIntent != null) {
            mAlarmManager.cancel(mShutdownIntent);
        }
        mShutdownScheduled = false;
    }
}
