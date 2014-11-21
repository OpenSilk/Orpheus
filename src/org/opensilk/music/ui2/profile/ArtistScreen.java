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

import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.LocalArtistProfileLoader;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Layout;
import mortar.MortarScope;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by drew on 11/19/14.
 */
@Layout(R.layout.profile_staggeredgrid)
@WithModule(ArtistScreen.Module.class)
public class ArtistScreen extends Screen {

    final LocalArtist artist;

    public ArtistScreen(LocalArtist artist) {
        this.artist = artist;
    }

    @Override
    public String getName() {
        return super.getName() + artist.name;
    }

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = {
                    ProfilePortraitView.class,
                    GridAdapter.class,
                    ProfileAdapter.class
            }
    )
    public static class Module {
        final ArtistScreen screen;

        public Module(ArtistScreen screen) {
            this.screen = screen;
        }

        @Provides
        public LocalArtist provideArtist() {
            return screen.artist;
        }

        @Provides
        public BasePresenter<ProfilePortraitView> profileFrameViewBasePresenter(Presenter p) {
            return p;
        }
    }

    @Singleton
    public static class Presenter extends BasePresenter<ProfilePortraitView> {

        final OverflowHandlers.LocalArtists artistsOverflowHandler;
        final LocalArtist artist;
        final Observable<List<Object>> loader;

        @Inject
        public Presenter(ActionBarOwner actionBarOwner,
                         ArtworkRequestManager requestor,
                         OverflowHandlers.LocalArtists artistsOverflowHandler,
                         LocalArtist artist,
                         LocalArtistProfileLoader loader) {
            super(actionBarOwner, requestor);
            this.artistsOverflowHandler = artistsOverflowHandler;
            this.artist = artist;
            this.loader = loader.getListObservable().cache();
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            setupActionbar();

            requestor.newArtistRequest(getView().mArtwork, getView().mPaletteObserver,
                    new ArtInfo(artist.name, null, null), ArtworkType.LARGE);

            loaderSubscription = loader.subscribe(new SimpleObserver<List<Object>>() {
                @Override
                public void onNext(List<Object> objects) {
                    if (getView() != null) {
                        getView().mAdapter.addAll(objects);
                    }
                }
            });
        }

        @Override
        String getTitle(Context context) {
            return artist.name;
        }

        @Override
        String getSubtitle(Context context) {
            return MusicUtils.makeLabel(context, R.plurals.Nalbums, artist.albumCount)
                    + ", " + MusicUtils.makeLabel(context, R.plurals.Nsongs, artist.songCount);
        }

        @Override
        int getNumArtwork() {
            return 1;
        }

        @Override
        ProfileAdapter makeAdapter(Context context) {
            return new ProfileAdapter(context);
        }

        @Override
        boolean isGrid() {
            return true;
        }

        void setupActionbar() {
            actionBarOwner.setConfig(
                new ActionBarOwner.Config.Builder()
                    .upButtonEnabled(true)
                    .withMenuConfig(
                            new ActionBarOwner.MenuConfig.Builder()
                                    .withMenus(OverflowHandlers.LocalArtists.MENUS)
                                    .setActionHandler(new Func1<Integer, Boolean>() {
                                        @Override
                                        public Boolean call(Integer integer) {
                                            try {
                                                return artistsOverflowHandler.handleClick(
                                                        OverflowAction.valueOf(integer), artist);
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
