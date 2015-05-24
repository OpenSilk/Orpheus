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

package org.opensilk.music.ui3.genresprofile;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.support.v4.app.Fragment;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Layout;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.model.Genre;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.MusicActivityToolbarComponent;
import org.opensilk.music.ui3.common.BundleableScreen;
import org.opensilk.music.ui3.profile.ProfileScreen;

import mortar.MortarScope;

/**
 * Created by drew on 5/5/15.
 */
@Layout(R.layout.profile_recycler)
@WithComponentFactory(GenresProfileScreen.Factory.class)
public class GenresProfileScreen extends BundleableScreen implements ProfileScreen {

    final Genre genre;

    public GenresProfileScreen(LibraryConfig libraryConfig, LibraryInfo libraryInfo, Genre genre) {
        super(libraryConfig, libraryInfo);
        this.genre = genre;
    }

    @Override
    public String getName() {
        return super.getName() + "-" + genre.identity;
    }

    @Override
    public MortarFragment getFragment(Context context) {
        return GenresProfileScreenFragment.ni(context, this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(libraryConfig.dematerialize());
        dest.writeParcelable(libraryInfo, flags);
        dest.writeBundle(genre.toBundle());
    }

    public static final Creator<GenresProfileScreen> CREATOR = new Creator<GenresProfileScreen>() {
        @Override
        public GenresProfileScreen createFromParcel(Parcel source) {
            return new GenresProfileScreen(
                    LibraryConfig.materialize(source.readBundle()),
                    source.<LibraryInfo>readParcelable(getClass().getClassLoader()),
                    Genre.BUNDLE_CREATOR.fromBundle(source.readBundle())
            );
        }

        @Override
        public GenresProfileScreen[] newArray(int size) {
            return new GenresProfileScreen[size];
        }
    };

    public static class Factory extends ComponentFactory<GenresProfileScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, GenresProfileScreen screen) {
            MusicActivityToolbarComponent activityComponent = DaggerService.getDaggerComponent(parentScope);
            return GenresProfileScreenComponent.FACTORY.call(activityComponent, screen);
        }
    }
}
