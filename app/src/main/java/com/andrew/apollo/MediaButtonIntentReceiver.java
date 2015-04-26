/*
 * Copyright (C) 2007 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import timber.log.Timber;

public class MediaButtonIntentReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Timber.v("Received intent: " + intent);
        final String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            startService(context, makeCommandIntent(context, MusicPlaybackService.CMDPAUSE));
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            startService(context, copyIntent(context, intent));
            if (isOrderedBroadcast()) {
                abortBroadcast();
            }
        }
    }

    private static Intent makeCommandIntent(Context context, String command) {
        final Intent i = new Intent(context, MusicPlaybackService.class);
        i.setAction(MusicPlaybackService.SERVICECMD);
        i.putExtra(MusicPlaybackService.CMDNAME, command);
        i.putExtra(MusicPlaybackService.FROM_MEDIA_BUTTON, true);
        return i;
    }

    private static Intent copyIntent(Context context, Intent o) {
        Intent i = new Intent();
        i.fillIn(o, 0);
        i.setComponent(new ComponentName(context, MusicPlaybackService.class));
        i.putExtra(MusicPlaybackService.FROM_MEDIA_BUTTON, true);
        return i;
    }

    private static void startService(Context context, Intent i) {
        startWakefulService(context, i);
    }
}
