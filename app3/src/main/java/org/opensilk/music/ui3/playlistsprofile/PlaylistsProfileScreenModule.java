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

package org.opensilk.music.ui3.playlistsprofile;

import android.content.Context;
import android.net.Uri;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.model.sort.TrackSortOrder;
import org.opensilk.music.ui3.common.MenuHandlerImpl;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import rx.functions.Func2;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class PlaylistsProfileScreenModule {
    final PlaylistsProfileScreen screen;

    public PlaylistsProfileScreenModule(PlaylistsProfileScreen screen) {
        this.screen = screen;
    }

    @Provides
    public LibraryConfig provideLibraryConfig() {
        return screen.libraryConfig;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri() {
        return Uri.EMPTY;
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder() {
        return TrackSortOrder.PLAYORDER;
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig(
    ) {

        Func2<Context, Integer, Boolean> handler = new Func2<Context, Integer, Boolean>() {
            @Override
            public Boolean call(Context context, Integer integer) {
                return false;
            }
        };

        return ActionBarMenuConfig.builder()
                .withMenus(ActionBarMenuConfig.toObject(MenuHandlerImpl.PLAYLISTS))
                .setActionHandler(handler)
                .build();
    }

}
