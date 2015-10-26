/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.ui3.profile.playlist;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.R;
import org.opensilk.music.index.model.BioSummary;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.sort.TrackSortOrder;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.MenuHandler;
import org.opensilk.music.ui3.common.MenuHandlerImpl;
import org.opensilk.music.ui3.common.PlayAllItemClickListener;
import org.opensilk.music.ui3.common.UtilsCommon;
import org.opensilk.music.ui3.profile.bio.BioScreen;
import org.opensilk.music.ui3.profile.bio.BioScreenFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class PlaylistDetailsScreenModule {
    final PlaylistDetailsScreen screen;

    public PlaylistDetailsScreenModule(PlaylistDetailsScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.playlistTracks(screen.playlist);
    }

    @Provides @Named("profile_heros")
    public Boolean provideWantMultiHeros() {
        return screen.playlist.getArtInfos().size() > 1;
    }

    @Provides @Named("profile_heros")
    public List<ArtInfo> provideHeroArtinfos() {
        return screen.playlist.getArtInfos();
    }

    @Provides @Named("profile_title")
    public String provideProfileTitle() {
        return screen.playlist.getName();
    }

    @Provides @Named("profile_subtitle")
    public String provideProfileSubTitle(@ForApplication Context context) {
        return UtilsCommon.makeLabel(context, R.plurals.Nsongs, screen.playlist.getTracksCount());
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            MenuHandler menuConfig
    ) {
        return BundleablePresenterConfig.builder()
                .setWantsGrid(false)
//                .setWantsNumberedTracks(true)
                .setItemClickListener(itemClickListener)
                .setMenuConfig(menuConfig)
                .setDefaultSortOrder(TrackSortOrder.PLAYORDER)
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new PlayAllItemClickListener();
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") Uri loaderUri, final ActivityResultsController activityResultsController) {
        return new MenuHandlerImpl(loaderUri, activityResultsController) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                return false;
            }

            @Override
            public boolean onMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                return false;
            }

            @Override
            public boolean onBuildActionMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenus(menuInflater, menu,
                        R.menu.action_save
                        );
                return true;
            }

            @Override
            public boolean onActionMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_save: {
                        List<Bundleable> tracks = presenter.getItems();
                        List<Uri> list = new ArrayList<>(tracks.size());
                        for (Bundleable b : tracks) {
                            list.add(((Track)b).getUri());
                        }
                        presenter.getIndexClient().updatePlaylist(screen.playlist.getUri(), list);
                        return true;
                    }
                    default:
                        return false;
                }
            }
        };
    }

    @Provides
    public RecyclerView.AdapterDataObserver provideObserver(final BundleablePresenter presenter) {
        return new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                if (itemCount == 1) {
                    presenter.getIndexClient().removeFromPlaylist(screen.playlist.getUri(), positionStart);
                } else {
                    Timber.e("Removing multiple items unsupported");
                }
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                if (itemCount == 1) {
                    presenter.getIndexClient().movePlaylistEntry(screen.playlist.getUri(), fromPosition, toPosition);
                } else {
                    Timber.e("Moving multiple items unsupported");
                }
            }
        };
    }

}
