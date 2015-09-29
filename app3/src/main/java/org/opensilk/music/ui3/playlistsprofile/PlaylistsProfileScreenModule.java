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
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.model.sort.TrackSortOrder;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.common.ActionBarMenuConfigWrapper;
import org.opensilk.music.ui3.common.OverflowHandler;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;

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

//    @Provides @ScreenScope
//    public TracksDragSwipePresenterConfig providePresenterConfig(
//            TrackItemClickListener itemClickListener,
//            OverflowClickListener overflowClickListener,
//            ActionBarMenuConfig menuConfig,
//            TrackDragSwipeEventListener eventListener
//    ) {
//        return TracksDragSwipePresenterConfig.builder()
//                .setItemClickListener(itemClickListener)
//                .setOverflowClickListener(overflowClickListener)
//                .setMenuConfig(menuConfig)
//                .setDragSwipeEventListener(eventListener)
//                .build();
//    }
//
//    @Provides @ScreenScope
//    public TrackItemClickListener provideItemClickListener(final ItemClickDelegate delegate) {
//        return new TrackItemClickListener() {
//            @Override
//            public void onItemClicked(TracksDragSwipePresenter presenter, Context context, Bundleable item) {
//                delegate.playAllItems(context, presenter.getItems(), item);
//            }
//        };
//    }

    @Provides @ScreenScope
    public OverflowClickListener provideOverflowClickListener(final OverflowHandler delegate) {
        return new OverflowClickListener() {
            @Override
            public void onBuildMenu(Context context, PopupMenu m, Bundleable item) {
                delegate.onBuildMenu(context, m, item);
                m.getMenu().removeItem(R.id.popup_delete); //No delete for playlists
            }

            @Override
            public boolean onItemClicked(Context context, OverflowAction action, Bundleable item) {
                return delegate.onItemClicked(context, action, item);
            }
        };
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig(
            ActionBarMenuConfigWrapper wrapper,
            final OverflowHandler playlistsOverflowHandler
    ) {

        Func2<Context, Integer, Boolean> handler = new Func2<Context, Integer, Boolean>() {
            @Override
            public Boolean call(Context context, Integer integer) {
                try {
                    return playlistsOverflowHandler.onItemClicked(context,
                            OverflowAction.valueOf(integer), screen.playlist);
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        };

        return wrapper.injectCommonItems(ActionBarMenuConfig.builder()
                .withMenus(ActionBarMenuConfig.toObject(OverflowHandler.PLAYLISTS))
                .setActionHandler(handler)
                .build());
    }

//    @Provides @ScreenScope
//    public TrackDragSwipeEventListener provideDragSwipeEventListener() {
//        return new TrackDragSwipeEventListener() {
//            @Override
//            public void onItemRemoved(Context context, Track track) {
//
//            }
//        };
//    }
}
