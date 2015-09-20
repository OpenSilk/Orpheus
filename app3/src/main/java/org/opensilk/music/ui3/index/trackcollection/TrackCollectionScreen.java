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

package org.opensilk.music.ui3.index.trackcollection;

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
import org.opensilk.music.model.TrackList;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.profile.ProfileScreen;

import mortar.MortarScope;

/**
 * Created by drew on 5/12/15.
 */
@Layout(R.layout.profile_recycler)
@WithComponentFactory(TrackCollectionScreen.Factory.class)
public class TrackCollectionScreen extends Screen implements ProfileScreen {

    final TrackList trackList;
    final String sortOrderPref;

    public TrackCollectionScreen(TrackList trackList, String sortOrderPref) {
        this.trackList = trackList;
        this.sortOrderPref = sortOrderPref;
    }

    @Override
    public String getName() {
        return super.getName() + "-" + trackList.getUri();
    }

    @Override
    public MortarFragment getFragment(Context context) {
        return TrackCollectionScreenFragment.ni(context, this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(trackList.toBundle());
        dest.writeString(sortOrderPref);
    }

    public static final Creator<TrackCollectionScreen> CREATOR = new Creator<TrackCollectionScreen>() {
        @Override
        public TrackCollectionScreen createFromParcel(Parcel source) {
            return new TrackCollectionScreen(
                    TrackList.BUNDLE_CREATOR.fromBundle(source.readBundle(TrackList.class.getClassLoader())),
                    source.readString()
            );
        }

        @Override
        public TrackCollectionScreen[] newArray(int size) {
            return new TrackCollectionScreen[size];
        }
    };

    public static class Factory extends ComponentFactory<TrackCollectionScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, TrackCollectionScreen screen) {
            MusicActivityComponent activityComponent = DaggerService.getDaggerComponent(parentScope);
            return TrackCollectionScreenComponent.FACTORY.call(activityComponent, screen);
        }
    }
}
