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

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.graphics.Palette;

import com.bumptech.glide.request.transition.Transition;

public class PaletteableDrawableCrossFadeTransition implements Transition<Palette> {
    protected final int duration;

    public PaletteableDrawableCrossFadeTransition(int duration) {
        this.duration = duration;
    }

    @Override
    public boolean transition(Palette current, ViewAdapter adapter) {
        if (adapter instanceof PaletteableTarget) {
            Drawable previous = adapter.getCurrentDrawable();
            Drawable next = resolveColorDrawable((PaletteableTarget) adapter, current);
            if (previous != null && next != null) {
                TransitionDrawable transitionDrawable =
                        new TransitionDrawable(new Drawable[] { previous, next });
                transitionDrawable.setCrossFadeEnabled(true);
                transitionDrawable.startTransition(duration);
                adapter.setDrawable(transitionDrawable);
                return true;
            }
        }
        return false;
    }

    protected ColorDrawable resolveColorDrawable(PaletteableTarget adapter, Palette palette) {
        Palette.Swatch swatch = PalettableUtils.getSwatchFor(adapter.getSwatchType(), palette);
        if (swatch == null && adapter.getFallbackSwatch() != null) {
            swatch = PalettableUtils.getSwatchFor(adapter.getFallbackSwatch(), palette);
        }
        if (swatch != null) {
            return new ColorDrawable(swatch.getRgb());
        } else {
            return null;
        }
    }
}

