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

package org.opensilk.music.bus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.andrew.apollo.MusicPlaybackService;

import org.opensilk.music.bus.events.MetaChanged;
import org.opensilk.music.bus.events.PlaybackModeChanged;
import org.opensilk.music.bus.events.PlaystateChanged;
import org.opensilk.music.bus.events.QueueChanged;
import org.opensilk.music.bus.events.Refresh;

/**
 * Created by drew on 4/22/14.
 */
public class ServiceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action != null) {
            if (action.equals(MusicPlaybackService.META_CHANGED)) {
                EventBus.getInstance().post(new MetaChanged());
            } else if (action.equals(MusicPlaybackService.PLAYSTATE_CHANGED)) {
                EventBus.getInstance().post(new PlaystateChanged());
            } else if (action.equals(MusicPlaybackService.REFRESH)) {
                EventBus.getInstance().post(new Refresh());
                // Cancel the broadcast so we aren't constantly refreshing
                context.removeStickyBroadcast(intent);
            } else if (action.equals(MusicPlaybackService.REPEATMODE_CHANGED)
                    || action.equals(MusicPlaybackService.SHUFFLEMODE_CHANGED)) {
                EventBus.getInstance().post(new PlaybackModeChanged());
            } else if (action.equals(MusicPlaybackService.QUEUE_CHANGED)) {
                EventBus.getInstance().post(new QueueChanged());
            }
        }
    }
}
