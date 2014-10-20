/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2.gallery;

import org.opensilk.common.flow.Screen;
import org.opensilk.music.R;

/**
 * Created by drew on 10/3/14.
 */
public enum GalleryPage {
    PLAYLIST(new PlaylistsScreen(), R.string.page_playlists),
    //    RECENT(R.string.page_recent),
    ARTIST(new ArtistsScreen(), R.string.page_artists),
    ALBUM(new AlbumsScreen(), R.string.page_albums),
    SONG(new SongsScreen(), R.string.page_songs),
    GENRE(new GenresScreen(), R.string.page_genres);

    public final Screen screen;
    public final int titleResource;

    private GalleryPage(Screen screen, int titleResource) {
        this.screen = screen;
        this.titleResource = titleResource;
    }
}
