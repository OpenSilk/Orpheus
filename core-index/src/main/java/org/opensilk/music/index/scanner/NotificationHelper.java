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

package org.opensilk.music.index.scanner;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.index.R;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 9/20/15.
 */
@ScannerScope
public class NotificationHelper {

    static int NOTIF_ID = 4395;

    public enum Status {
        SCANNING,
        COMPLETED
    }

    final ScannerService service;
    final Context appContext;
    final NotificationManagerCompat notificationManager;

    @Inject
    public NotificationHelper(
            ScannerService service
    ) {
        this.service = service;
        this.appContext = service;
        this.notificationManager = NotificationManagerCompat.from(service);
    }

    @DebugLog
    void updateNotification(boolean running) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext);
        builder.setSmallIcon(R.drawable.ic_sync_white_24dp);
        int title = service.status.get() == Status.COMPLETED ? R.string.scan_finished : R.string.scan_running;
        builder.setContentTitle(appContext.getString(title));
        builder.setContentText(appContext.getString(R.string.scan_progress,
                service.numProcessed.get(), service.numTotal.get(), service.numError.get()));
        Notification notification = builder.build();

        if (running) {
            service.startForeground(NOTIF_ID, notification);
        } else {
            service.stopForeground(false);
            notificationManager.notify(NOTIF_ID, notification);
        }
    }

}
