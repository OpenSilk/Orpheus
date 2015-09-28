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

package org.opensilk.music.playback.service;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSession;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.playback.NavUtils;

import javax.inject.Inject;

/**
 * Created by drew on 9/25/15.
 */
@PlaybackServiceScope
public class MediaSessionHolder {

    final MediaSession mSession;

    @Inject
    public MediaSessionHolder(@ForApplication Context context) {
        mSession = new MediaSession(context, PlaybackService.NAME);
        configureSession(context);
    }

    void configureSession(Context context) {
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setSessionActivity(PendingIntent.getActivity(
                context, 2, NavUtils.makeLauncherIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT));
        final ComponentName mediaButtonReceiverComponent
                = new ComponentName(context, MediaButtonIntentReceiver.class);
        final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(mediaButtonReceiverComponent);
        final PendingIntent mediaButtonReceiverIntent = PendingIntent.getBroadcast(
                context, 0, mediaButtonIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mSession.setMediaButtonReceiver(mediaButtonReceiverIntent);
    }

    public MediaSession getSession() {
        return mSession;
    }

    public MediaSession.Token getSessionToken() {
        return mSession.getSessionToken();
    }

    public MediaController getController() {
        return mSession.getController();
    }

    public void release() {
        mSession.release();
    }

}
