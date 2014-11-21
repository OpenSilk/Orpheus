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

import com.andrew.apollo.model.Genre;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
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

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/19/14.
 */
@Layout(R.layout.profile_recycler)
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
                    ProfilePortraitView.class,
                    ProfileLandscapeView.class,
                    ProfileAdapter.class
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
        public BasePresenter<ProfilePortraitView> providePortraitPresenter(PresenterPortrait p) {
            return p;
        }

        @Provides
        public BasePresenter<ProfileLandscapeView> provideLandscapePresenter(PresenterLandscape p) {
            return p;
        }
    }

    static abstract class GenrePresenter<V extends View> extends BasePresenter<V> {
        final OverflowHandlers.Genres genreOverflowHandler;
        final Genre genre;
        final Observable<List<Object>> loader;

        public GenrePresenter(ActionBarOwner actionBarOwner,
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

        @Override
        ProfileAdapter makeAdapter(Context context) {
            return new ProfileAdapter(context);
        }

        @Override
        boolean isGrid() {
            return true;
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

    @Singleton
    public static class PresenterPortrait extends GenrePresenter<ProfilePortraitView> {

        @Inject
        public PresenterPortrait(ActionBarOwner actionBarOwner,
                         ArtworkRequestManager requestor,
                         OverflowHandlers.Genres genreOverflowHandler,
                         Genre genre,
                         LocalGenresProfileLoader loader) {
            super(actionBarOwner, requestor, genreOverflowHandler, genre, loader);
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);

            loadMultiArtwork(requestor,
                    genre.mAlbumIds,
                    getView().mArtwork,
                    getView().mArtwork2,
                    getView().mArtwork3,
                    getView().mArtwork4
            );

            if (isSubscribed(loaderSubscription)) loaderSubscription.unsubscribe();
            loaderSubscription = loader.subscribe(new SimpleObserver<List<Object>>() {
                @Override
                public void onNext(List<Object> objects) {
                    if (getView() != null) {
                        getView().mAdapter.addAll(objects);
                    }
                }
            });
        }

    }

    @Singleton
    public static class PresenterLandscape extends GenrePresenter<ProfileLandscapeView> {

        @Inject
        public PresenterLandscape(ActionBarOwner actionBarOwner,
                         ArtworkRequestManager requestor,
                         OverflowHandlers.Genres genreOverflowHandler,
                         Genre genre,
                         LocalGenresProfileLoader loader) {
            super(actionBarOwner, requestor, genreOverflowHandler, genre, loader);
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);

            loadMultiArtwork(requestor,
                    genre.mAlbumIds,
                    getView().mArtwork,
                    getView().mArtwork2,
                    getView().mArtwork3,
                    getView().mArtwork4
            );

            if (isSubscribed(loaderSubscription)) loaderSubscription.unsubscribe();
            loaderSubscription = loader.subscribe(new SimpleObserver<List<Object>>() {
                @Override
                public void onNext(List<Object> objects) {
                    if (getView() != null) {
                        getView().mAdapter.addAll(objects);
                    }
                }
            });
        }

    }
}
