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

package org.opensilk.music.ui2.profile;

import android.content.Context;
import android.os.Bundle;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalSong;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.LauncherActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui2.loader.LocalAlbumSongLoader;
import org.opensilk.music.util.SortOrder;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import flow.HasParent;
import flow.Layout;
import rx.functions.Func1;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/18/14.
 */
@Layout(R.layout.profile_recycler)
@WithModule(AlbumScreen.Module.class)
@WithTransitions(
        forward = { R.anim.shrink_fade_out, R.anim.slide_in_child_bottom },
        backward = { R.anim.slide_out_child_bottom, R.anim.grow_fade_in }
)
public class AlbumScreen extends Screen implements HasParent<GalleryScreen> {

    final LocalAlbum album;

    public AlbumScreen(LocalAlbum album) {
        this.album = album;
    }

    @Override
    public String getName() {
        return super.getName() + album.name;
    }

    @Override
    public GalleryScreen getParent() {
        return new GalleryScreen();
    }

    @dagger.Module (
            addsTo = LauncherActivity.Module.class,
            injects = {
                    ProfilePortraitView.class,
                    ProfileLandscapeView.class,
                    ProfileAdapter.class
            },
            library = true
    )
    public static class Module {
        final LocalAlbum album;

        public Module(AlbumScreen screen) {
            this.album = screen.album;
        }

        @Provides @Named("album")
        public long provideAlbumId() {
            return album.albumId;
        }

        @Provides
        public LocalAlbum provideAlbum() {
            return album;
        }

        @Provides
        public BasePresenter providePresenter(Presenter p) {
            return p;
        }
    }

    @Singleton
    public static class Presenter extends BasePresenter {

        final OverflowHandlers.LocalAlbums albumsOverflowHandler;
        final LocalAlbumSongLoader loader;
        final LocalAlbum album;

        @Inject
        public Presenter(ActionBarOwner actionBarOwner,
                         ArtworkRequestManager requestor,
                         AppPreferences settings,
                         OverflowHandlers.LocalAlbums albumsOverflowHandler,
                         LocalAlbumSongLoader loader,
                         LocalAlbum album) {
            super(actionBarOwner, requestor, settings);
            this.albumsOverflowHandler = albumsOverflowHandler;
            this.loader = loader;
            this.album = album;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            setupActionBar();
            requestor.newAlbumRequest(getView().getHero(), null,
                    new ArtInfo(album.artistName, album.name, album.artworkUri), ArtworkType.LARGE);
            load();
        }

        @Override
        String getTitle(Context context) {
            return album.name;
        }

        @Override
        String getSubtitle(Context context) {
            return album.artistName;
        }

        @Override
        int getNumArtwork() {
            return 1;
        }

        @Override
        ProfileAdapter makeAdapter(Context context) {
            return new ProfileAdapter(context, true);
        }

        @Override
        boolean isGrid() {
            return false;
        }

        void load() {
            if (isSubscribed(loaderSubscription)) loaderSubscription.unsubscribe();
            loader.setSortOrder(settings.getString(AppPreferences.ALBUM_SONG_SORT_ORDER, SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST));
            loaderSubscription = loader.getListObservable().subscribe(new SimpleObserver<List<LocalSong>>() {
                @Override
                public void onNext(List<LocalSong> localSongs) {
                    if (getView() != null) {
                        getView().getAdapter().addAll(localSongs);
                    }
                }
            });
        }

        void setNewSortOrder(String sortOrder) {
            settings.putString(AppPreferences.ALBUM_SONG_SORT_ORDER, sortOrder);
            loader.reset();
            if (getView() != null) {
                getView().getAdapter().clear();
                getView().prepareRefresh();
            }
            load();
        }

        void setupActionBar() {
            actionBarOwner.setConfig(
                    new ActionBarOwner.Config.Builder(getCommonConfig())
                            .setMenuConfig(
                                    new ActionBarOwner.MenuConfig.Builder()
                                            .withMenus(R.menu.album_song_sort_by)
                                            .withMenus(OverflowHandlers.LocalAlbums.MENUS)
                                            .setActionHandler(new Func1<Integer, Boolean>() {
                                                @Override
                                                public Boolean call(Integer integer) {
                                                    switch (integer) {
                                                        case R.id.menu_sort_by_track_list:
                                                            setNewSortOrder(SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
                                                            return true;
                                                        case R.id.menu_sort_by_az:
                                                            setNewSortOrder(SortOrder.AlbumSongSortOrder.SONG_A_Z);
                                                            return true;
                                                        case R.id.menu_sort_by_za:
                                                            setNewSortOrder(SortOrder.AlbumSongSortOrder.SONG_Z_A);
                                                            return true;
                                                        case R.id.menu_sort_by_duration:
                                                            setNewSortOrder(SortOrder.AlbumSongSortOrder.SONG_DURATION);
                                                            return true;
                                                        case R.id.menu_sort_by_filename:
                                                            setNewSortOrder(SortOrder.AlbumSongSortOrder.SONG_FILENAME);
                                                            return true;
                                                        case R.id.menu_sort_by_artist:
                                                            setNewSortOrder(SortOrder.AlbumSongSortOrder.SONG_ARTIST);
                                                            return true;
                                                        default:
                                                            try {
                                                                return albumsOverflowHandler.handleClick(
                                                                        OverflowAction.valueOf(integer), album);
                                                            } catch (IllegalArgumentException e) {
                                                                return false;
                                                            }
                                                    }
                                                }
                                            })
                                            .build()
                            )
                            .build()
            );
        }

    }

}
