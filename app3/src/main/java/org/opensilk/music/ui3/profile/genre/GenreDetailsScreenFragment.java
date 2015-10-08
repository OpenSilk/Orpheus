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

package org.opensilk.music.ui3.profile.genre;

import android.content.Context;
import android.os.Bundle;

import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortarfragment.MortarFragment;

/**
 * Created by drew on 5/5/15.
 */
public class GenreDetailsScreenFragment extends MortarFragment {
    public static final String NAME = GenreDetailsScreenFragment.class.getName();

    public static GenreDetailsScreenFragment ni(Context context, GenreDetailsScreen screen) {
        Bundle args = new Bundle();
        args.putParcelable("screen", screen);
        return factory(context, NAME, args);
    }

    @Override
    protected Screen newScreen() {
        getArguments().setClassLoader(getClass().getClassLoader());
        return getArguments().<GenreDetailsScreen>getParcelable("screen");
    }
}
