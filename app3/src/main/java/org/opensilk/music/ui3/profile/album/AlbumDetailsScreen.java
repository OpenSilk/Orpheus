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

package org.opensilk.music.ui3.profile.album;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Layout;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.R;
import org.opensilk.music.model.Album;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.profile.ProfileScreen;

import mortar.MortarScope;

/**
 * Created by drew on 5/5/15.
 */
@Layout(R.layout.profile_view2)
@WithComponentFactory(AlbumDetailsScreen.Factory.class)
public class AlbumDetailsScreen extends Screen implements ProfileScreen {

    final Album album;

    public AlbumDetailsScreen(Album album) {
        this.album = album;
    }

    @Override
    public String getName() {
        return super.getName() + "-" + album.getUri();
    }

    @Override
    public MortarFragment getFragment(Context context) {
        return AlbumDetailsScreenFragment.ni(context, this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(album.toBundle());
    }

    public static final Creator<AlbumDetailsScreen> CREATOR = new Creator<AlbumDetailsScreen>() {
        @Override
        public AlbumDetailsScreen createFromParcel(Parcel source) {
            return new AlbumDetailsScreen(
                    Album.BUNDLE_CREATOR.fromBundle(source.readBundle(Album.class.getClassLoader()))
            );
        }

        @Override
        public AlbumDetailsScreen[] newArray(int size) {
            return new AlbumDetailsScreen[size];
        }
    };

    public static class Factory extends ComponentFactory<AlbumDetailsScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, AlbumDetailsScreen screen) {
            MusicActivityComponent activityComponent = DaggerService.getDaggerComponent(parentScope);
            return AlbumDetailsScreenComponent.FACTORY.call(activityComponent, screen);
        }
    }
}
