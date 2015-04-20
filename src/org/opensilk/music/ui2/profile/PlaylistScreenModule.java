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

package org.opensilk.music.ui2.profile;

import com.andrew.apollo.model.Playlist;

import org.opensilk.music.ui2.LauncherActivityModule;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;

/**
 * Created by drew on 4/20/15.
 */
@dagger.Module(
        addsTo = LauncherActivityModule.class,
        injects = {
                PlaylistPortraitView.class,
                PlaylistLandscapeView.class,
                PlaylistDragSortView.class,
                PlaylistAdapter.class,
        }
)
public class PlaylistScreenModule {
    final PlaylistScreen screen;

    public PlaylistScreenModule(PlaylistScreen screen) {
        this.screen = screen;
    }

    @Provides
    @Singleton
    @Named("playlist")
    public long providePlaylistId() {
        return screen.playlist.mPlaylistId;
    }

    @Provides
    @Singleton
    public Playlist providePlaylist() {
        return screen.playlist;
    }

}
