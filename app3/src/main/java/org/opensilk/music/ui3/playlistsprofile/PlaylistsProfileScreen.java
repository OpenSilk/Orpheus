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

package org.opensilk.music.ui3.playlistsprofile;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.support.v4.app.Fragment;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Layout;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.common.BundleableScreen;
import org.opensilk.music.ui3.profile.ProfileScreen;

import mortar.MortarScope;

/**
 * Created by drew on 5/5/15.
 */
@Layout(R.layout.profile_recycler)
@WithComponentFactory(PlaylistsProfileScreen.Factory.class)
public class PlaylistsProfileScreen extends BundleableScreen implements ProfileScreen {

    final Playlist playlist;

    public PlaylistsProfileScreen(LibraryConfig libraryConfig, LibraryInfo libraryInfo, Playlist playlist) {
        super(libraryConfig, libraryInfo);
        this.playlist = playlist;
    }

    @Override
    public String getName() {
        return super.getName() + "-" + playlist.identity;
    }

    @Override
    public Fragment getFragment(Context context) {
        return PlaylistsProfileScreenFragment.ni(context, this);
    }

    @Override
    public String getTag() {
        return PlaylistsProfileScreenFragment.NAME;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(libraryConfig.dematerialize());
        dest.writeParcelable(libraryInfo, flags);
        dest.writeBundle(playlist.toBundle());
    }

    public static final Creator<PlaylistsProfileScreen> CREATOR = new Creator<PlaylistsProfileScreen>() {
        @Override
        public PlaylistsProfileScreen createFromParcel(Parcel source) {
            return new PlaylistsProfileScreen(
                    LibraryConfig.materialize(source.readBundle()),
                    source.<LibraryInfo>readParcelable(getClass().getClassLoader()),
                    Playlist.BUNDLE_CREATOR.fromBundle(source.readBundle())
            );
        }

        @Override
        public PlaylistsProfileScreen[] newArray(int size) {
            return new PlaylistsProfileScreen[size];
        }
    };

    public static class Factory extends ComponentFactory<PlaylistsProfileScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, PlaylistsProfileScreen screen) {
            MusicActivityComponent activityComponent = DaggerService.getDaggerComponent(parentScope);
            return PlaylistsProfileScreenComponent.FACTORY.call(activityComponent, screen);
        }
    }
}
