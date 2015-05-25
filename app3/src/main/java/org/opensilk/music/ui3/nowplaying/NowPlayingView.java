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

package org.opensilk.music.ui3.nowplaying;

import android.content.Context;
import android.media.audiofx.AudioEffect;

import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.artwork.PaletteObserver;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 5/24/15.
 */
public interface NowPlayingView {
    void setProgress(int progress);
    void setTotalTime(CharSequence text);
    void setCurrentTime(CharSequence text);
    void setCurrentTimeVisibility(int visibility);
    int getCurrentTimeVisibility();
    void attachVisualizer(int id);
    void destroyVisualizer();
    void setVisualizerEnabled(boolean enabled);
    AnimatedImageView getArtwork();
    Context getContext();
    PaletteObserver getPaletteObserver();
    void setPlayChecked(boolean yes);
    void setCurrentTrack(CharSequence text);
    void setCurrentArtist(CharSequence text);
}
