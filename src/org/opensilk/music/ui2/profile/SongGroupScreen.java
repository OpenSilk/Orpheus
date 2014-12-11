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
import android.os.Parcel;

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.LocalSongGroup;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.LauncherActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui2.loader.LocalSongGroupLoader;
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
 * Created by drew on 11/19/14.
 */
@Layout(R.layout.profile_recycler)
@WithModule(SongGroupScreen.Module.class)
@WithTransitions(
        forward = { R.anim.shrink_fade_out, R.anim.slide_in_child_bottom },
        backward = { R.anim.slide_out_child_bottom, R.anim.grow_fade_in }
)
public class SongGroupScreen extends Screen implements HasParent<GalleryScreen> {

    final LocalSongGroup songGroup;

    public SongGroupScreen(LocalSongGroup songGroup) {
        this.songGroup = songGroup;
    }

    @Override
    public String getName() {
        return super.getName() + songGroup.name;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(songGroup, flags);
        super.writeToParcel(dest, flags);
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
                    ProfileAdapter.class,
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

        @Provides
        public BasePresenter providePresenter(Presenter p) {
            return p;
        }
    }

    @Singleton
    public static class Presenter extends BasePresenter {
        final OverflowHandlers.LocalSongGroups songGroupOverflowHandler;
        final LocalSongGroup songGroup;
        final LocalSongGroupLoader loader;

        @Inject
        public Presenter(ActionBarOwner actionBarOwner,
                         ArtworkRequestManager requestor,
                         AppPreferences settings,
                         OverflowHandlers.LocalSongGroups songGroupOverflowHandler,
                         LocalSongGroup songGroup,
                         LocalSongGroupLoader loader) {
            super(actionBarOwner, requestor, settings);
            this.songGroupOverflowHandler = songGroupOverflowHandler;
            this.songGroup = songGroup;
            this.loader = loader;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            setupActionBar();
            loadMultiArtwork(requestor,
                    songGroup.albumIds,
                    getView().getHero(),
                    getView().getHero2(),
                    getView().getHero3(),
                    getView().getHero4()
            );
            load();
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

        @Override
        ProfileAdapter makeAdapter(Context context) {
            return new ProfileAdapter(context);
        }

        @Override
        boolean isGrid() {
            return false;
        }

        void load() {
            if (isSubscribed(loaderSubscription)) loaderSubscription.unsubscribe();
            loader.setSortOrder(settings.getString(AppPreferences.SONG_COLLECTION_SORT_ORDER, SortOrder.SongSortOrder.SONG_ALBUM));
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
            settings.putString(AppPreferences.SONG_COLLECTION_SORT_ORDER, sortOrder);
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
                            .setMenuConfig(new ActionBarOwner.MenuConfig.Builder()
                                            .withMenus(R.menu.song_sort_by)
                                            .withMenus(OverflowHandlers.LocalSongGroups.MENUS)
                                            .setActionHandler(new Func1<Integer, Boolean>() {
                                                @Override
                                                public Boolean call(Integer integer) {
                                                    switch (integer) {
                                                        case R.id.menu_sort_by_az:
                                                            setNewSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
                                                            return true;
                                                        case R.id.menu_sort_by_za:
                                                            setNewSortOrder(SortOrder.SongSortOrder.SONG_Z_A);
                                                            return true;
                                                        case R.id.menu_sort_by_artist:
                                                            setNewSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
                                                            return true;
                                                        case R.id.menu_sort_by_album:
                                                            setNewSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
                                                            return true;
                                                        case R.id.menu_sort_by_year:
                                                            setNewSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
                                                            return true;
                                                        case R.id.menu_sort_by_duration:
                                                            setNewSortOrder(SortOrder.SongSortOrder.SONG_DURATION);
                                                            return true;
                                                        case R.id.menu_sort_by_filename:
                                                            setNewSortOrder(SortOrder.SongSortOrder.SONG_FILENAME);
                                                            return true;
                                                        case R.id.menu_sort_by_date_added:
                                                            setNewSortOrder(SortOrder.SongSortOrder.SONG_DATE);
                                                            return true;
                                                        default:
                                                            try {
                                                                return songGroupOverflowHandler.handleClick(
                                                                        OverflowAction.valueOf(integer), songGroup);
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

    public static final Creator<SongGroupScreen> CREATOR = new Creator<SongGroupScreen>() {
        @Override
        public SongGroupScreen createFromParcel(Parcel source) {
            SongGroupScreen s = new SongGroupScreen(
                    source.<LocalSongGroup>readParcelable(LocalSongGroup.class.getClassLoader())
            );
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public SongGroupScreen[] newArray(int size) {
            return new SongGroupScreen[size];
        }
    };

}
