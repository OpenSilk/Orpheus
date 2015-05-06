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

package org.opensilk.music.ui3.artistsprofile;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.sort.AlbumSortOrder;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.UtilsCommon;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class ArtistsProfileScreenModule {
    final ArtistsProfileScreen screen;

    public ArtistsProfileScreenModule(ArtistsProfileScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri() {
        return LibraryUris.artistAlbums(screen.libraryConfig.authority, screen.libraryInfo.libraryId, screen.libraryInfo.folderId);
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePluginPrefKey(screen.libraryConfig, AppPreferences.ARTIST_ALBUM_SORT_ORDER), AlbumSortOrder.A_Z);
    }

    @Provides @Named("presenter_wantGrid")
    public Boolean provideWantGrid(AppPreferences preferences) {
        return preferences.isGrid(preferences.makePluginPrefKey(screen.libraryConfig, AppPreferences.ALBUM_LAYOUT), AppPreferences.GRID);
    }

    @Provides @Named("profile_heros")
    public Boolean provideWantMultiHeros() {
        return false;
    }

    @Provides @Named("profile_heros")
    public List<ArtInfo> provideHeroArtinfos() {
        return Collections.singletonList(new ArtInfo(screen.artist.name, null, null));
    }

    @Provides @Named("profile_title")
    public String provideProfileTitle() {
        return screen.artist.name;
    }

    @Provides @Named("profile_subtitle")
    public String provideProfileSubTitle(@ForApplication Context context) {
        String subtitle = "";
        if (screen.artist.albumCount > 0) {
            subtitle += UtilsCommon.makeLabel(context, R.plurals.Nalbums, screen.artist.albumCount);
        }
        if (screen.artist.trackCount > 0) {
            if (!TextUtils.isEmpty(subtitle)) subtitle += ", ";
            subtitle += UtilsCommon.makeLabel(context, R.plurals.Nsongs, screen.artist.trackCount);
        }
        return subtitle;
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(BundleablePresenter presenter, Context context, Bundleable item) {
                //TODO
            }
        };
    }
}
