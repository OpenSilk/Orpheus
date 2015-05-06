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

import org.opensilk.common.ui.mortar.HasName;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.ui3.albums.AlbumsScreen;
import org.opensilk.music.ui3.artists.ArtistsScreen;
import org.opensilk.music.ui3.common.BundleableScreen;

import rx.functions.Func2;

/**
 * Created by drew on 10/3/14.
 */
public enum GalleryPage {
//    PLAYLIST(new PlaylistsScreen()),
    ARTIST(R.string.page_artists, new Func2<LibraryConfig, LibraryInfo, BundleableScreen>() {
        @Override
        public BundleableScreen call(LibraryConfig config, LibraryInfo libraryInfo) {
            return new ArtistsScreen(config, libraryInfo);
        }
    }),
    ALBUM(R.string.page_albums, new Func2<LibraryConfig, LibraryInfo, BundleableScreen>() {
        @Override
        public BundleableScreen call(LibraryConfig config, LibraryInfo libraryInfo) {
            return new AlbumsScreen(config, libraryInfo);
        }
    }),
//    SONG(new SongsScreen()),
//    GENRE(new GenresScreen()),
    ;

    public final int titleRes;
    public final Func2<LibraryConfig, LibraryInfo, BundleableScreen> FACTORY;

    GalleryPage(int titleRes, Func2<LibraryConfig, LibraryInfo, BundleableScreen> FACTORY) {
        this.titleRes = titleRes;
        this.FACTORY = FACTORY;
    }

}
