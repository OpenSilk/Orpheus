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

package org.opensilk.music.ui3.folders;

import android.os.Bundle;

import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;

/**
 * Created by drew on 5/2/15.
 */
public class FoldersScreenFragment extends MortarFragment {
    public static final String TAG = FoldersScreenFragment.class.getName();

    public static FoldersScreenFragment newInstance(LibraryConfig config, LibraryInfo info) {
        FoldersScreenFragment f = new FoldersScreenFragment();
        Bundle b = new Bundle();
        b.putBundle("config", config.dematerialize());
        b.putParcelable("info", info);
        f.setArguments(b);
        return f;
    }

    @Override
    protected Object getScreen() {
        return new FoldersScreen(LibraryConfig.materialize(getArguments().getBundle("config")),
                getArguments().<LibraryInfo>getParcelable("info"));
    }

    @Override
    protected String getScopeName() {
        return super.getScopeName() + getScreen().toString();
    }
}
