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
import org.opensilk.music.ui2.loader.LocalArtistProfileLoader;
import org.opensilk.music.util.SortOrder;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.HasParent;
import flow.Layout;
import rx.functions.Func1;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/19/14.
 */
@Layout(R.layout.profile_recycler)
@WithModule(ArtistScreen.Module.class)
@WithTransitions(
        forward = { R.anim.shrink_fade_out, R.anim.slide_in_child_bottom },
        backward = { R.anim.slide_out_child_bottom, R.anim.grow_fade_in }
)
public class ArtistScreen extends Screen implements HasParent<GalleryScreen> {

    final LocalArtist artist;

    public ArtistScreen(LocalArtist artist) {
        this.artist = artist;
    }

    @Override
    public String getName() {
        return super.getName() + artist.name;
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
        public BasePresenter providePresenter(Presenter p) {
            return p;
        }

    }

    @Singleton
    public static class Presenter extends BasePresenter {
        final OverflowHandlers.LocalArtists artistsOverflowHandler;
        final LocalArtist artist;
        final LocalArtistProfileLoader loader;

        @Inject
        public Presenter(ActionBarOwner actionBarOwner,
                         ArtworkRequestManager requestor,
                         AppPreferences settings,
                         OverflowHandlers.LocalArtists artistsOverflowHandler,
                         LocalArtist artist,
                         LocalArtistProfileLoader loader) {
            super(actionBarOwner, requestor, settings);
            this.artistsOverflowHandler = artistsOverflowHandler;
            this.artist = artist;
            this.loader = loader;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            setupActionbar();
            requestor.newArtistRequest(getView().getHero(), null,
                    new ArtInfo(artist.name, null, null), ArtworkType.LARGE);
            load();
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

        void load() {
            if (isSubscribed(loaderSubscription)) loaderSubscription.unsubscribe();
            loader.setSortOrder(settings.getString(AppPreferences.ARTIST_ALBUM_SORT_ORDER, SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z));
            loaderSubscription = loader.getListObservable().subscribe(new SimpleObserver<List<Object>>() {
                @Override
                public void onNext(List<Object> objects) {
                    if (getView() != null) {
                        getView().getAdapter().addAll(objects);
                    }
                }
            });
        }

        void setNewSortOrder(String sortOrder) {
            settings.putString(AppPreferences.ARTIST_ALBUM_SORT_ORDER, sortOrder);
            loader.reset();
            if (getView() != null) {
                getView().getAdapter().clear();
                getView().prepareRefresh();
            }
            load();
        }

        void setupActionbar() {
            actionBarOwner.setConfig(
                    new ActionBarOwner.Config.Builder(getCommonConfig())
                            .setMenuConfig(
                                    new ActionBarOwner.MenuConfig.Builder()
                                            .withMenus(R.menu.artist_album_sort_by)
                                            .withMenus(OverflowHandlers.LocalArtists.MENUS)
                                            .setActionHandler(new Func1<Integer, Boolean>() {
                                                @Override
                                                public Boolean call(Integer integer) {
                                                    switch (integer) {
                                                        case R.id.menu_sort_by_az:
                                                            setNewSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
                                                            return true;
                                                        case R.id.menu_sort_by_za:
                                                            setNewSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_Z_A);
                                                            return true;
                                                        case R.id.menu_sort_by_year:
                                                            setNewSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR);
                                                            return true;
                                                        case R.id.menu_sort_by_number_of_songs:
                                                            setNewSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_NUMBER_OF_SONGS);
                                                            return true;
                                                        default:
                                                            try {
                                                                return artistsOverflowHandler.handleClick(
                                                                        OverflowAction.valueOf(integer), artist);
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
