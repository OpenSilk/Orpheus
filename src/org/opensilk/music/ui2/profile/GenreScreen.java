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

import com.andrew.apollo.model.Genre;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.LocalGenresProfileLoader;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Layout;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by drew on 11/19/14.
 */
@Layout(R.layout.profile_staggeredgrid)
@WithModule(GenreScreen.Module.class)
public class GenreScreen extends Screen {

    final Genre genre;

    public GenreScreen(Genre genre) {
        this.genre = genre;
    }

    @Override
    public String getName() {
        return super.getName() + genre.mGenreName;
    }

    @dagger.Module(
            addsTo = ProfileActivity.Module.class,
            injects = {
                    ProfileView.class,
                    GridAdapter.class
            }
    )
    public static class Module {
        final GenreScreen screen;

        public Module(GenreScreen screen) {
            this.screen = screen;
        }

        @Provides
        public Genre provideGenre() {
            return screen.genre;
        }

        @Provides
        public BasePresenter profileFrameViewBasePresenter(Presenter p) {
            return p;
        }
    }

    @Singleton
    public static class Presenter extends BasePresenter {

        final OverflowHandlers.Genres genreOverflowHandler;
        final Genre genre;
        final Observable<List<Object>> loader;

        @Inject
        public Presenter(ActionBarOwner actionBarOwner,
                         ArtworkRequestManager requestor,
                         OverflowHandlers.Genres genreOverflowHandler,
                         Genre genre,
                         LocalGenresProfileLoader loader) {
            super(actionBarOwner, requestor);
            this.genreOverflowHandler = genreOverflowHandler;
            this.genre = genre;
            this.loader = loader.getListObservable().cache();
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            setupActionBar();

            final int num = Math.min(getNumArtwork(), 4);
            switch (num) {
                case 4:
                    if (getView().mArtwork4 != null) {
                        requestor.newAlbumRequest(getView().mArtwork4, null, genre.mAlbumIds[3], ArtworkType.LARGE);
                    }
                    //fall
                case 3:
                    if (getView().mArtwork3 != null) {
                        requestor.newAlbumRequest(getView().mArtwork3, null, genre.mAlbumIds[2], ArtworkType.LARGE);
                    }
                    //fall
                case 2:
                    if (getView().mArtwork2 != null) {
                        requestor.newAlbumRequest(getView().mArtwork2, null, genre.mAlbumIds[1], ArtworkType.LARGE);
                    }
                    //fall
                case 1:
                    if (getView().mArtwork != null) {
                        requestor.newAlbumRequest(getView().mArtwork, null, genre.mAlbumIds[0], ArtworkType.LARGE);
                    }
                    break;
                default:
                    if (getView().mArtwork != null) {
                        getView().mArtwork.setDefaultImage();
                    }
            }

            final GridAdapter adapter = new GridAdapter(getView().getContext());
            getView().mList.setAdapter(adapter);
            loaderSubscription = loader.subscribe(new SimpleObserver<List<Object>>() {
                @Override
                public void onNext(List<Object> objects) {
                    if (getView() != null) {
                        adapter.addAll(objects);
                    }
                }
            });
        }

        @Override
        String getTitle(Context context) {
            return genre.mGenreName;
        }

        @Override
        String getSubtitle(Context context) {
            return MusicUtils.makeLabel(context, R.plurals.Nalbums, genre.mAlbumIds.length)
                    + ", " + MusicUtils.makeLabel(context, R.plurals.Nsongs, genre.mSongIds.length);
        }

        @Override
        int getNumArtwork() {
            return genre.mAlbumIds.length;
        }

        void setupActionBar() {
            actionBarOwner.setConfig(
                    new ActionBarOwner.Config.Builder()
                            .upButtonEnabled(true)
                            .withMenuConfig(
                                    new ActionBarOwner.MenuConfig.Builder()
                                            .withMenus(OverflowHandlers.Genres.MENUS)
                                            .setActionHandler(new Func1<Integer, Boolean>() {
                                                @Override
                                                public Boolean call(Integer integer) {
                                                    try {
                                                        return genreOverflowHandler.handleClick(
                                                                OverflowAction.valueOf(integer), genre);
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
