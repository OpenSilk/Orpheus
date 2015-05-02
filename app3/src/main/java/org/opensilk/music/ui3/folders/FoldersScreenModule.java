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

import android.net.Uri;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.ui3.common.BundleableAdapter;
import org.opensilk.music.ui3.common.BundleablePresenter;

import javax.inject.Named;

import dagger.Provides;

/**
 * Created by drew on 5/2/15.
 */
public class FoldersScreenModule {
    final FoldersScreen screen;

    public FoldersScreenModule(FoldersScreen screen) {
        this.screen = screen;
    }

    @Provides
    public FoldersScreen provideScreen() {
        return screen;
    }

    @Provides
    public BundleablePresenter provideBundleablePresenter(FoldersScreenPresenter p) {
        return p;
    }

    @Provides @Named("loaderuri")
    public Uri provideLoaderUri() {
        return LibraryUris.folders(screen.libraryConfig.authority, screen.libraryInfo.libraryId, screen.libraryInfo.folderId);
    }
}
