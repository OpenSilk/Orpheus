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

package org.opensilk.music.ui3.gallery;

import android.content.Context;
import android.os.Bundle;

import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.ui3.common.BundleableFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 5/5/15.
 */
public class GalleryScreenFragment extends BundleableFragment {
    public static final String NAME = GalleryScreenFragment.class.getName();

    public static GalleryScreenFragment ni(
            Context context,
            LibraryConfig config,
            LibraryInfo libraryInfo,
            List<GalleryPage> pages
    ) {
        Bundle args = makeCommonArgsBundle(config, libraryInfo);
        String[] v = new String[pages.size()];
        for (int ii=0; ii<pages.size(); ii++) {
            v[ii] = pages.get(ii).toString();
        }
        args.putStringArray("pages", v);
        return factory(context, NAME, args);
    }

    @Override
    protected Screen newScreen() {
        extractCommonArgs();
        String[] v = getArguments().getStringArray("pages");
        List<GalleryPage> pages = new ArrayList<>(v.length);
        for (String p : v) {
            pages.add(GalleryPage.valueOf(p));
        }
        return new GalleryScreen(mLibraryConfig, mLibraryInfo, pages);
    }
}
