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

package org.opensilk.common.glide;

import android.support.v7.graphics.Palette;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.transition.NoTransition;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.transition.TransitionFactory;

/**
 * Created by drew on 10/9/15.
 */
public class PaletteableDrawableCrossFadeFactory implements TransitionFactory<Palette> {
    private final int duration;
    private PaletteableDrawableCrossFadeTransition firstResourceTransition;
    private PaletteableDrawableCrossFadeTransition secondResourceTransition;

    private PaletteableDrawableCrossFadeFactory(int duration) {
        this.duration = duration;
    }

    public static PaletteableDrawableCrossFadeFactory create() {
        return new PaletteableDrawableCrossFadeFactory(PalettableUtils.DEFAULT_DURATION_MS);
    }

    public static PaletteableDrawableCrossFadeFactory create(int duration) {
        return new PaletteableDrawableCrossFadeFactory(duration);
    }

    @Override
    public Transition<Palette> build(DataSource dataSource, boolean isFirstResource) {
        if (dataSource == DataSource.MEMORY_CACHE) {
            return NoTransition.get();
        } else if (isFirstResource) {
            return getFirstResourceTransition(dataSource);
        } else {
            return getSecondResourceTransition(dataSource);
        }
    }

    private Transition<Palette> getFirstResourceTransition(DataSource dataSource) {
        if (firstResourceTransition == null) {
            firstResourceTransition = new PaletteableDrawableCrossFadeTransition(duration);
        }
        return firstResourceTransition;
    }

    private Transition<Palette> getSecondResourceTransition(DataSource dataSource) {
        if (secondResourceTransition == null) {
            secondResourceTransition = new PaletteableDrawableCrossFadeTransition(duration);
        }
        return secondResourceTransition;
    }
}
