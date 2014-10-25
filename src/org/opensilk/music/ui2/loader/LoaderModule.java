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

package org.opensilk.music.ui2.loader;

import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 10/24/14.
 */
@Module(
        library = true,
        complete = false
)
public class LoaderModule {

    @Provides @Singleton
    public RxLoader<LocalAlbum> provideLocalAlbumsLoader(LocalAlbumsLoader l) {
        return l;
    }

    @Provides @Singleton
    public RxLoader<LocalArtist> provideLocalArtistsLoader(LocalArtistsLoader l) {
        return l;
    }

    @Provides @Singleton
    public RxLoader<Genre> provideLocalGenresLoader(LocalGenresLoader l) {
        return l;
    }

    @Provides @Singleton
    public RxLoader<Playlist> provideLocalPlaylistsLoader(LocalPlaylistsLoader l) {
        return l;
    }

    @Provides @Singleton
    public RxLoader<LocalSong> provideLocalSongsLoader(LocalSongsLoader l) {
        return l;
    }

}
