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

package org.opensilk.music.ui3.profile.artist;

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
import org.opensilk.music.model.Artist;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.profile.ProfileScreen;

import mortar.MortarScope;

/**
 * Created by drew on 5/5/15.
 */
@Layout(R.layout.profile_view2)
@WithComponentFactory(ArtistDetailsScreen.Factory.class)
public class ArtistDetailsScreen extends Screen implements ProfileScreen {

    final Artist artist;

    public ArtistDetailsScreen(Artist artist) {
        this.artist = artist;
    }

    @Override
    public String getName() {
        return super.getName() + "-" + artist.getUri();
    }

    @Override
    public MortarFragment getFragment(Context context) {
        return ArtistDetailsScreenFragment.ni(context, this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(artist.toBundle());
    }

    public static final Creator<ArtistDetailsScreen> CREATOR = new Creator<ArtistDetailsScreen>() {
        @Override
        public ArtistDetailsScreen createFromParcel(Parcel source) {
            return new ArtistDetailsScreen(
                    Artist.BUNDLE_CREATOR.fromBundle(source.readBundle(Artist.class.getClassLoader()))
            );
        }

        @Override
        public ArtistDetailsScreen[] newArray(int size) {
            return new ArtistDetailsScreen[size];
        }
    };

    public static class Factory extends ComponentFactory<ArtistDetailsScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, ArtistDetailsScreen screen) {
            MusicActivityComponent activityComponent = DaggerService.getDaggerComponent(parentScope);
            return ArtistDetailsScreenComponent.FACTORY.call(activityComponent, screen);
        }
    }
}
