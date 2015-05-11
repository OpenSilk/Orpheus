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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import org.opensilk.music.playback.PlaybackConstants;

import timber.log.Timber;

public class MediaButtonIntentReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Timber.v("Received intent: " + intent);
        final String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            startService(context, makeCommandIntent(context, PlaybackConstants.CMDPAUSE));
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            startService(context, copyIntent(context, intent));
            if (isOrderedBroadcast()) {
                abortBroadcast();
            }
        }
    }

    private static Intent makeCommandIntent(Context context, String command) {
        final Intent i = new Intent(context, PlaybackService.class);
        i.setAction(PlaybackConstants.SERVICECMD);
        i.putExtra(PlaybackConstants.CMDNAME, command);
        i.putExtra(PlaybackConstants.FROM_MEDIA_BUTTON, true);
        return i;
    }

    private static Intent copyIntent(Context context, Intent o) {
        Intent i = new Intent();
        i.fillIn(o, 0);
        i.setComponent(new ComponentName(context, PlaybackService.class));
        i.putExtra(PlaybackConstants.FROM_MEDIA_BUTTON, true);
        return i;
    }

    private static void startService(Context context, Intent i) {
        startWakefulService(context, i);
    }
}
