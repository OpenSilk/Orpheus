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

import android.annotation.TargetApi;
import android.app.Notification;
import android.media.session.MediaSession;
import android.os.Bundle;

/**
 * Created by drew on 11/2/15.
 */
@TargetApi(21)
public class NotificationHelperL {
    //tells system we have a mediasession since we dont use mediastyle notification
    public static void applySessionExtra(Bundle extras, Object token) {
        extras.putParcelable(Notification.EXTRA_MEDIA_SESSION, (MediaSession.Token) token);
    }
}
