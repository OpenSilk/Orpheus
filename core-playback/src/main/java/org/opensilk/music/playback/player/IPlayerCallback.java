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

import android.net.Uri;
import android.os.Handler;

/**
 * Created by drew on 4/24/15.
 */
public interface IPlayerCallback {
    /**
     * Media was opened successfully and buffering has started
     */
    void onLoading();

    /**
     * Media is loaded and ready for {@link IPlayer#play()}
     */
    void onReady();

    /**
     * playback has started
     */
    void onPlaying();

    /**
     * playback has paused
     */
    void onPaused();

    /**
     * playback has stopped and player has gone to idle state
     * at this point it must be reinitilazed with {@link IPlayer#setDataSource(Uri)}
     */
    void onStopped();

    /**
     * player has started playing track loaded with {@link IPlayer#setNextDataSource(Uri)}
     */
    void onWentToNext();

    /**
     * player has failed to open uri given by {@link IPlayer#setDataSource(Uri)}
     * @param msg
     */
    void onErrorOpenCurrentFailed(String msg);

    /**
     * player has failed to open uri given by {@link IPlayer#setNextDataSource(Uri)}
     * @param msg
     */
    void onErrorOpenNextFailed(String msg);
}
