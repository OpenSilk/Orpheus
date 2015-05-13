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

package org.opensilk.music.playback;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.playback.service.PlaybackService;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

import static org.opensilk.music.playback.PlaybackConstants.*;

/**
 * Created by drew on 4/22/15.
 */
public class AlarmManagerHelper {
    final Context mContext;
    final AlarmManager mAlarmManager;

    PendingIntent mShutdownIntent;
    boolean mShutdownScheduled;

    @Inject
    public AlarmManagerHelper(
            @ForApplication Context context,
            AlarmManager alarmManager
    ) {
        mContext = context;
        mAlarmManager = alarmManager;
    }

    public void scheduleDelayedShutdown() {
        Timber.v("Scheduling shutdown in %d ms", IDLE_DELAY);
        final Intent shutdownIntent = new Intent(mContext, PlaybackService.class).setAction(SHUTDOWN);
        mShutdownIntent = PendingIntent.getService(mContext, 0, shutdownIntent, PendingIntent.FLAG_CANCEL_CURRENT);
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

    public boolean isShutdownScheduled() {
        return mShutdownScheduled;
    }

    public void onRecievedShutdownIntent() {
        mShutdownScheduled = false;
    }
}
