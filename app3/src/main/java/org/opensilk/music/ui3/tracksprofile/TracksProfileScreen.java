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

package org.opensilk.music.ui3.tracksprofile;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Layout;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.model.TrackCollection;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.MusicActivityToolbarComponent;
import org.opensilk.music.ui3.common.BundleableScreen;
import org.opensilk.music.ui3.profile.ProfileScreen;

import mortar.MortarScope;

/**
 * Created by drew on 5/12/15.
 */
@Layout(R.layout.profile_recycler)
@WithComponentFactory(TracksProfileScreen.Factory.class)
public class TracksProfileScreen extends BundleableScreen implements ProfileScreen {

    final TrackCollection trackCollection;
    final String sortOrderPref;

    public TracksProfileScreen(LibraryConfig libraryConfig, LibraryInfo libraryInfo,
                               TrackCollection trackCollection, String sortOrderPref) {
        super(libraryConfig, libraryInfo);
        this.trackCollection = trackCollection;
        this.sortOrderPref = sortOrderPref;
    }

    @Override
    public String getName() {
        return super.getName() + "-" + trackCollection.tracksUri;
    }

    @Override
    public MortarFragment getFragment(Context context) {
        return TracksProfileScreenFragment.ni(context, this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(libraryConfig.dematerialize());
        dest.writeParcelable(libraryInfo, flags);
        dest.writeBundle(trackCollection.toBundle());
        dest.writeString(sortOrderPref);
    }

    public static final Creator<TracksProfileScreen> CREATOR = new Creator<TracksProfileScreen>() {
        @Override
        public TracksProfileScreen createFromParcel(Parcel source) {
            return new TracksProfileScreen(
                    LibraryConfig.materialize(source.readBundle()),
                    source.<LibraryInfo>readParcelable(getClass().getClassLoader()),
                    TrackCollection.BUNDLE_CREATOR.fromBundle(source.readBundle()),
                    source.readString()
            );
        }

        @Override
        public TracksProfileScreen[] newArray(int size) {
            return new TracksProfileScreen[size];
        }
    };

    public static class Factory extends ComponentFactory<TracksProfileScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, TracksProfileScreen screen) {
            MusicActivityToolbarComponent activityComponent = DaggerService.getDaggerComponent(parentScope);
            return TracksProfileScreenComponent.FACTORY.call(activityComponent, screen);
        }
    }
}
