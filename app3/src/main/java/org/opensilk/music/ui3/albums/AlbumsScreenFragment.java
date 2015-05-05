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

package org.opensilk.music.ui3.albums;

import android.content.Context;
import android.os.Bundle;

import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.ui3.common.BundleableFragment;

/**
 * Created by drew on 5/5/15.
 */
public class AlbumsScreenFragment extends BundleableFragment {
    public static final String NAME = AlbumsScreenFragment.class.getName();

    public static AlbumsScreenFragment ni(Context context, LibraryConfig config, LibraryInfo info) {
        Bundle args = makeCommonArgsBundle(config, info);
        return factory(context, NAME, args);
    }

    @Override
    protected Object getScreen() {
        extractCommonArgs();
        return new AlbumsScreen(mLibraryConfig, mLibraryInfo);
    }
}
