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

import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.music.R;
import org.opensilk.music.ui3.index.albums.AlbumsScreen;
import org.opensilk.music.ui3.index.artists.ArtistsScreen;
import org.opensilk.music.ui3.index.folders.FoldersScreen;
import org.opensilk.music.ui3.index.genres.GenresScreen;
import org.opensilk.music.ui3.index.tracks.TracksScreen;

import rx.functions.Func0;

/**
 * Created by drew on 10/3/14.
 */
public enum GalleryPage {
    ARTIST(R.string.title_artists, new Func0<Screen>() {
        @Override
        public Screen call() {
            return new ArtistsScreen();
        }
    }),
    ALBUM(R.string.title_albums, new Func0<Screen>() {
        @Override
        public Screen call() {
            return new AlbumsScreen();
        }
    }),
    GENRE(R.string.title_genres, new Func0<Screen>() {
        @Override
        public Screen call() {
            return new GenresScreen();
        }
    }),
    SONG(R.string.title_songs, new Func0<Screen>() {
        @Override
        public Screen call() {
            return new TracksScreen();
        }
    }),
    FOLDER(R.string.title_folders, new Func0<Screen>() {
        @Override
        public Screen call() {
            return new FoldersScreen();
        }
    });


    public final int titleRes;
    public final Func0<Screen> FACTORY;

    GalleryPage(int titleRes, Func0<Screen> FACTORY) {
        this.titleRes = titleRes;
        this.FACTORY = FACTORY;
    }

}
