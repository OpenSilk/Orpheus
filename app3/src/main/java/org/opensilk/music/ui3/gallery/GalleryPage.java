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

import android.net.Uri;

import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.music.R;

import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Created by drew on 10/3/14.
 */
public enum GalleryPage {
    ARTIST(new Func1<Uri, GalleryPageScreen>() {
        @Override
        public GalleryPageScreen call(Uri uri) {
            return new ArtistsScreen(uri);
        }
    }),
    ALBUM(new Func1<Uri, GalleryPageScreen>() {
        @Override
        public GalleryPageScreen call(Uri uri) {
            return new AlbumsScreen(uri);
        }
    }),
    GENRE(new Func1<Uri, GalleryPageScreen>() {
        @Override
        public GalleryPageScreen call(Uri uri) {
            return new GenresScreen(uri);
        }
    }),
    SONG(new Func1<Uri, GalleryPageScreen>() {
        @Override
        public GalleryPageScreen call(Uri uri) {
            return new TracksScreen(uri);
        }
    }),
    FOLDER(new Func1<Uri, GalleryPageScreen>() {
        @Override
        public GalleryPageScreen call(Uri uri) {
            return new FoldersScreen(uri);
        }
    });

    public final Func1<Uri, GalleryPageScreen> FACTORY;

    GalleryPage(Func1<Uri, GalleryPageScreen> FACTORY) {
        this.FACTORY = FACTORY;
    }

}
