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
import com.andrew.apollo.model.LocalSongGroup;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui.profile.adapter.SongCollectionAdapter;
import org.opensilk.music.ui.profile.loader.SongGroupLoader;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Layout;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by drew on 11/19/14.
 */
@Layout(R.layout.profile_list)
@WithModule(SongGroupScreen.Module.class)
public class SongGroupScreen extends Screen {

    final LocalSongGroup songGroup;

    public SongGroupScreen(LocalSongGroup songGroup) {
        this.songGroup = songGroup;
    }

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = {
                    ProfileView.class,
                    SongCollectionAdapter.class
            }
    )
    public static class Module {
        final SongGroupScreen screen;

        public Module(SongGroupScreen screen) {
            this.screen = screen;
        }

        @Provides @Singleton @Named("songgroup")
        public long[] provideSongIds() {
            return screen.songGroup.songIds;
        }

        @Provides @Singleton
        public LocalSongGroup provideSongGroup() {
            return screen.songGroup;
        }

        @Provides @Singleton
        public BasePresenter profileFrameViewBasePresenter(Presenter p) {
            return p;
        }
    }

    @Singleton
    public static class Presenter extends BasePresenter {

        final OverflowHandlers.LocalSongGroups songGroupOverflowHandler;
        final LocalSongGroup songGroup;
        final Observable<List<LocalSong>> loader;

        @Inject
        public Presenter(ActionBarOwner actionBarOwner,
                         ArtworkRequestManager requestor,
                         OverflowHandlers.LocalSongGroups songGroupOverflowHandler,
                         LocalSongGroup songGroup,
                         SongGroupLoader loader) {
            super(actionBarOwner, requestor);
            this.songGroupOverflowHandler = songGroupOverflowHandler;
            this.songGroup = songGroup;
            this.loader = loader.getListObservable().share().cache();
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            setupActionBar();

            final int num = Math.min(getNumArtwork(), 4);
            switch (num) {
                case 4:
                    if (getView().mArtwork4 != null) {
                        requestor.newAlbumRequest(getView().mArtwork4, null, songGroup.albumIds[3], ArtworkType.LARGE);
                    }
                    //fall
                case 3:
                    if (getView().mArtwork3 != null) {
                        requestor.newAlbumRequest(getView().mArtwork3, null, songGroup.albumIds[2], ArtworkType.LARGE);
                    }
                    //fall
                case 2:
                    if (getView().mArtwork2 != null) {
                        requestor.newAlbumRequest(getView().mArtwork2, null, songGroup.albumIds[1], ArtworkType.LARGE);
                    }
                    //fall
                case 1:
                    if (getView().mArtwork != null) {
                        requestor.newAlbumRequest(getView().mArtwork, null, songGroup.albumIds[0], ArtworkType.LARGE);
                    }
                    break;
                default:
                    if (getView().mArtwork != null) {
                        getView().mArtwork.setDefaultImage();
                    }
            }

            final SongCollectionAdapter adapter = new SongCollectionAdapter(getView().getContext(), false);
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
            return songGroup.parentName;
        }

        @Override
        String getSubtitle(Context context) {
            return songGroup.name;
        }

        @Override
        int getNumArtwork() {
            return songGroup.albumIds.length;
        }

        void setupActionBar() {
            actionBarOwner.setConfig(
                new ActionBarOwner.Config.Builder()
                    .upButtonEnabled(true)
                    .withMenuConfig(new ActionBarOwner.MenuConfig.Builder()
                        .withMenus(OverflowHandlers.LocalSongGroups.MENUS)
                        .setActionHandler(new Func1<Integer, Boolean>() {
                            @Override
                            public Boolean call(Integer integer) {
                                try {
                                    return songGroupOverflowHandler.handleClick(
                                            OverflowAction.valueOf(integer), songGroup);
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
