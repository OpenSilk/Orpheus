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
import android.view.View;

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.LauncherActivity;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui2.loader.LocalPlaylistSongLoader;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import flow.HasParent;
import flow.Layout;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/19/14.
 */
@Layout(R.layout.profile_playlist)
@WithModule(PlaylistScreen.Module.class)
public class PlaylistScreen extends Screen implements HasParent<GalleryScreen> {

    final Playlist playlist;

    public PlaylistScreen(Playlist playlist) {
        this.playlist = playlist;
    }

    @Override
    public String getName() {
        return super.getName() + playlist.mPlaylistName;
    }

    @Override
    public GalleryScreen getParent() {
        return new GalleryScreen();
    }

    @dagger.Module (
            addsTo = LauncherActivity.Module.class,
            injects = {
                    PlaylistPortraitView.class,
                    PlaylistLandscapeView.class,
                    PlaylistDragSortView.class,
                    PlaylistAdapter.class,
            }
    )
    public static class Module {
        final PlaylistScreen screen;

        public Module(PlaylistScreen screen) {
            this.screen = screen;
        }

        @Provides @Singleton @Named("playlist")
        public long providePlaylistId() {
            return screen.playlist.mPlaylistId;
        }

        @Provides @Singleton
        public Playlist providePlaylist() {
            return screen.playlist;
        }

    }

    @Singleton
    public static class Presenter extends BasePresenter {
        final OverflowHandlers.Playlists playlistOverflowHandler;
        final Playlist playlist;

        @Inject
        public Presenter(ActionBarOwner actionBarOwner,
                         ArtworkRequestManager requestor,
                         OverflowHandlers.Playlists playlistOverflowHandler,
                         Playlist playlist) {
            super(actionBarOwner, requestor);
            this.playlistOverflowHandler = playlistOverflowHandler;
            this.playlist = playlist;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            setupActionBar();
            loadMultiArtwork(requestor,
                    playlist.mAlbumIds,
                    getView().getHero(),
                    getView().getHero2(),
                    getView().getHero3(),
                    getView().getHero4()
            );
        }

        @Override
        String getTitle(Context context) {
            return playlist.mPlaylistName;
        }

        @Override
        String getSubtitle(Context context) {
            return MusicUtils.makeLabel(context, R.plurals.Nsongs, playlist.mSongNumber);
        }

        @Override
        int getNumArtwork() {
            return playlist.mAlbumIds.length;
        }

        @Override
        ProfileAdapter makeAdapter(Context context) {
            return null;//not used
        }

        @Override
        boolean isGrid() {
            return false;//not used
        }

        PlaylistAdapter makePlaylistAdapter(Context context) {
            return new PlaylistAdapter(context, playlist.mPlaylistId);
        }

        void setupActionBar() {
            actionBarOwner.setConfig(new ActionBarOwner.Config.Builder()
                            .setTitle(getTitle(getView().getContext()))
                            .setSubtitle(getSubtitle(getView().getContext()))
                            .upButtonEnabled(true)
                            .withMenuConfig(makeMenuConfig())
                            .build()
            );
        }

        ActionBarOwner.MenuConfig makeMenuConfig() {
            ActionBarOwner.MenuConfig.Builder b = new ActionBarOwner.MenuConfig.Builder();
            int[] c = OverflowHandlers.Playlists.MENUS_COMMON;
            int[] u = OverflowHandlers.Playlists.MENUS_USER;
            if (isLastAdded()) {
                b.withMenus(c);
            } else {
                int[] m = new int[c.length+u.length];
                System.arraycopy(c, 0, m, 0, c.length);
                System.arraycopy(u, 0, m, c.length, u.length);
                b.withMenus(m);
            }
            b.setActionHandler(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer integer) {
                    try {
                        return playlistOverflowHandler.handleClick(OverflowAction.valueOf(integer), playlist);
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                }
            });
            return b.build();
        }

        boolean isLastAdded() {
            return playlist.mPlaylistId == -2;
        }
    }

    @Singleton
    public static class PresenterDslv extends ViewPresenter<PlaylistDragSortView> {

        final Playlist playlist;
        final Observable<List<LocalSong>> loader;

        Subscription loaderSubscription;

        @Inject
        public PresenterDslv(Playlist playlist,
                             LocalPlaylistSongLoader loader) {
            this.playlist = playlist;
            this.loader = loader.getListObservable().cache();
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            loaderSubscription = loader.subscribe(new SimpleObserver<List<LocalSong>>() {
                @Override
                public void onNext(List<LocalSong> localSongs) {
                    if (getView() != null) {
                        getView().mAdapter.addAll(localSongs);
                    }
                }
            });
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
            if (isSubscribed(loaderSubscription)) {
                loaderSubscription.unsubscribe();
                loaderSubscription = null;
            }
        }

        boolean isLastAdded() {
            return playlist.mPlaylistId == -2;
        }

    }

}
