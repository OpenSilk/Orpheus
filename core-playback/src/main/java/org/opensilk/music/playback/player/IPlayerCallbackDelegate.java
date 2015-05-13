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
public class IPlayerCallbackDelegate implements IPlayerCallback {
    IPlayerCallback callback;
    Handler callbackHandler;

    public IPlayerCallbackDelegate() {
    }

    public void setCallback(IPlayerCallback callback, Handler callbackHandler) {
        this.callback = callback;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public void onLoading() {
        if (hasCallback()) {
            final IPlayerCallback c = callback;
            post(new Runnable() {
                @Override
                public void run() {
                    c.onLoading();
                }
            });
        }
    }

    @Override
    public void onReady() {
        if (hasCallback()) {
            final IPlayerCallback c = callback;
            post(new Runnable() {
                @Override
                public void run() {
                    c.onReady();
                }
            });
        }
    }

    @Override
    public void onPlaying() {
        if (hasCallback()) {
            final IPlayerCallback c = callback;
            post(new Runnable() {
                @Override
                public void run() {
                    c.onPlaying();
                }
            });
        }
    }

    @Override
    public void onPaused() {
        if (hasCallback()) {
            final IPlayerCallback c = callback;
            post(new Runnable() {
                @Override
                public void run() {
                    c.onPaused();
                }
            });
        }
    }

    @Override
    public void onStopped() {
        if (hasCallback()) {
            final IPlayerCallback c = callback;
            post(new Runnable() {
                @Override
                public void run() {
                    c.onStopped();
                }
            });
        }
    }

    @Override
    public void onWentToNext() {
        if (hasCallback()) {
            final IPlayerCallback c = callback;
            post(new Runnable() {
                @Override
                public void run() {
                    c.onWentToNext();
                }
            });
        }
    }

    @Override
    public void onErrorOpenCurrentFailed(final String msg) {
        if (hasCallback()) {
            final IPlayerCallback c = callback;
            post(new Runnable() {
                @Override
                public void run() {
                    c.onErrorOpenCurrentFailed(msg);
                }
            });
        }
    }

    @Override
    public void onErrorOpenNextFailed(final String msg) {
        if (hasCallback()) {
            final IPlayerCallback c = callback;
            post(new Runnable() {
                @Override
                public void run() {
                    c.onErrorOpenNextFailed(msg);
                }
            });
        }
    }

    private boolean hasCallback() {
        return callback != null;
    }

    private void post(Runnable r) {
        if (callbackHandler != null) {
            callbackHandler.post(r);
        } else {
            r.run();
        }
    }
}
