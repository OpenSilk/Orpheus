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

package org.opensilk.music.playback.player;

import android.os.Handler;

/**
 * Created by drew on 5/7/15.
 */
public class PlayerCallbackHelper implements PlayerCallback {
    PlayerCallback callback;
    Handler callbackHandler;

    public PlayerCallbackHelper() {
    }

    public void setCallback(PlayerCallback callback, Handler callbackHandler) {
        this.callback = callback;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public void onPlayerEvent(final PlayerEvent event) {
        if (callback == null) {
            return;
        }
        if (callbackHandler != null) {
            callbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onPlayerEvent(event);
                }
            });
        } else {
            callback.onPlayerEvent(event);
        }
    }

    @Override
    public void onPlayerStatus(final PlayerStatus status) {
        if (callback == null) {
            return;
        }
        if (callbackHandler != null) {
            callbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onPlayerStatus(status);
                }
            });
        } else {
            callback.onPlayerStatus(status);
        }
    }
}
