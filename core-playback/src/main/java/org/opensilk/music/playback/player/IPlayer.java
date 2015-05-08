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

import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by drew on 4/24/15.
 */
public interface IPlayer {
    //Transport controls
    void play();
    void pause();
    void stop();
    void seekTo(long pos);
    //void fastForward();
    void skipToNext();
    //void rewind();
    //void skipToPrevious();
    //void setRating(Rating rating)

    void getPosition();
    void getDuration();

    void setDataSource(Uri uri);
    void setNextDataSource(Uri uri);

    void duck();

    void release();

    void setCallback(PlayerCallback callback, Handler handler);
}
