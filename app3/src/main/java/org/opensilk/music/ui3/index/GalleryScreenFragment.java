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

package org.opensilk.music.ui3.index;

import android.content.Context;
import android.os.Bundle;

import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.common.ui.mortarfragment.PauseAndResumeFragment;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.ui3.common.BundleableFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 5/5/15.
 */
public class GalleryScreenFragment extends PauseAndResumeFragment {
    public static final String NAME = GalleryScreenFragment.class.getName();

    public static GalleryScreenFragment ni(Context context) {
        return factory(context, NAME, null);
    }

    @Override
    protected Screen newScreen() {
        return new GalleryScreen();
    }

}
