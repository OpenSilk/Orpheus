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

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui.profile.adapter.PlaylistAdapter;
import org.opensilk.music.ui.profile.loader.PlaylistSongLoader;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import butterknife.ButterKnife;
import dagger.Provides;
import flow.Layout;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/19/14.
 */
@Layout(R.layout.profile_playlist)
@WithModule(PlaylistScreen.Module.class)
public class PlaylistScreen extends Screen {

    final Playlist playlist;

    public PlaylistScreen(Playlist playlist) {
        this.playlist = playlist;
    }

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = {
                    ProfileView.class,
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

        @Provides @Singleton
        public BasePresenter profileFrameViewBasePresenter(Presenter p) {
            return p;
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

            final int num = Math.min(getNumArtwork(), 4);
            switch (num) {
                case 4:
                    if (getView().mArtwork4 != null) {
                        requestor.newAlbumRequest(getView().mArtwork4, null, playlist.mAlbumIds[3], ArtworkType.LARGE);
                    }
                    //fall
                case 3:
                    if (getView().mArtwork3 != null) {
                        requestor.newAlbumRequest(getView().mArtwork3, null, playlist.mAlbumIds[2], ArtworkType.LARGE);
                    }
                    //fall
                case 2:
                    if (getView().mArtwork2 != null) {
                        requestor.newAlbumRequest(getView().mArtwork2, null, playlist.mAlbumIds[1], ArtworkType.LARGE);
                    }
                    //fall
                case 1:
                    if (getView().mArtwork != null) {
                        requestor.newAlbumRequest(getView().mArtwork, null, playlist.mAlbumIds[0], ArtworkType.LARGE);
                    }
                    break;
                default:
                    if (getView().mArtwork != null) {
                        getView().mArtwork.setDefaultImage();
                    }
            }
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

        void setupActionBar() {
            actionBarOwner.setConfig(new ActionBarOwner.Config.Builder()
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
                             PlaylistSongLoader loader) {
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
