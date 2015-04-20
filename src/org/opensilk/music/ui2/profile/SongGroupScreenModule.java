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

import com.andrew.apollo.model.LocalSongGroup;

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
                ProfilePortraitView.class,
                ProfileLandscapeView.class,
                ProfileAdapter.class,
        }
)
public class SongGroupScreenModule {
    final SongGroupScreen screen;

    public SongGroupScreenModule(SongGroupScreen screen) {
        this.screen = screen;
    }

    @Provides
    @Singleton
    @Named("songgroup")
    public long[] provideSongIds() {
        return screen.songGroup.songIds;
    }

    @Provides
    @Singleton
    public LocalSongGroup provideSongGroup() {
        return screen.songGroup;
    }

    @Provides
    public ProfilePresenter providePresenter(SongGroupScreenPresenter p) {
        return p;
    }
}
