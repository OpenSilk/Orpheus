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

package org.opensilk.music.ui3.library;

import android.os.Bundle;

import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.library.LibraryConfig;

/**
 * Created by drew on 5/1/15.
 */
public class LandingScreenFragment extends MortarFragment {
    public static final String TAG = LandingScreenFragment.class.getName();

    public static LandingScreenFragment newInstance(LibraryConfig config) {
        LandingScreenFragment f = new LandingScreenFragment();
        f.setArguments(config.dematerialize());
        return f;
    }

    @Override
    protected Object newScreen() {
        return new LandingScreen(LibraryConfig.materialize(getArguments()));
    }
}
