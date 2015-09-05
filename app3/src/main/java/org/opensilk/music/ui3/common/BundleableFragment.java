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

package org.opensilk.music.ui3.common;

import android.os.Bundle;

import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;

/**
 * Created by drew on 5/5/15.
 */
public abstract class BundleableFragment extends MortarFragment {

    protected LibraryConfig mLibraryConfig;
    protected LibraryInfo mLibraryInfo;
    protected String mTitle;

    protected static Bundle makeCommonArgsBundle(LibraryConfig config, LibraryInfo info, String title) {
        return makeCommonArgsBundle(config.dematerialize(), info, title);
    }

    protected static Bundle makeCommonArgsBundle(Bundle config, LibraryInfo info, String title) {
        Bundle b = new Bundle();
        b.putBundle("config", config);
        b.putParcelable("info", info);
        b.putString("title", title);
        return b;
    }

    protected void extractCommonArgs() {
        mLibraryConfig = LibraryConfig.materialize(getArguments().getBundle("config"));
        mLibraryInfo = getArguments().getParcelable("info");
    }

}
