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
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalSong;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.LocalAlbumSongLoader;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Layout;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by drew on 11/18/14.
 */
@Layout(R.layout.profile_list)
@WithModule(AlbumScreen.Module.class)
public class AlbumScreen extends Screen {

    final LocalAlbum album;

    public AlbumScreen(LocalAlbum album) {
        this.album = album;
    }

    @Override
    public String getName() {
        return super.getName() + album.name;
    }

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = {
                    ProfileView.class,
                    SongCollectionAdapter.class
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
        public BasePresenter profileFrameViewBasePresenter(Presenter p) {
            return p;
        }
    }

    @Singleton
    public static class Presenter extends BasePresenter {

        final OverflowHandlers.LocalAlbums albumsOverflowHandler;
        final Observable<List<LocalSong>> loader;
        final LocalAlbum album;

        @Inject
        public Presenter(ActionBarOwner actionBarOwner,
                         ArtworkRequestManager requestor,
                         OverflowHandlers.LocalAlbums albumsOverflowHandler,
                         LocalAlbumSongLoader loader,
                         LocalAlbum album) {
            super(actionBarOwner, requestor);
            this.albumsOverflowHandler = albumsOverflowHandler;
            this.loader = loader.getListObservable().cache();
            this.album = album;
        }

        @Override
        @DebugLog
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            setupActionBar();

            requestor.newAlbumRequest(getView().mArtwork, getView().mPaletteObserver,
                    new ArtInfo(album.artistName, album.name, album.artworkUri), ArtworkType.LARGE);

            final SongCollectionAdapter adapter = new SongCollectionAdapter(getView().getContext(), true);
            getView().mList.setAdapter(adapter);
            loaderSubscription = loader.subscribe(new SimpleObserver<List<LocalSong>>() {
                @Override
                public void onNext(List<LocalSong> localSongs) {
                    if (getView() != null) {
                        adapter.addAll(localSongs);
                    }
                }
            });
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

        void setupActionBar() {
            actionBarOwner.setConfig(
                    new ActionBarOwner.Config.Builder()
                            .upButtonEnabled(true)
                            .withMenuConfig(
                                    new ActionBarOwner.MenuConfig.Builder()
                                            .withMenus(OverflowHandlers.LocalAlbums.MENUS)
                                            .setActionHandler(new Func1<Integer, Boolean>() {
                                                @Override
                                                public Boolean call(Integer integer) {
                                                    try {
                                                        return albumsOverflowHandler.handleClick(
                                                                OverflowAction.valueOf(integer), album);
                                                    } catch (IllegalArgumentException e) {
                                                        return false;
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
