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

package org.opensilk.music.ui3.profile;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
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

import mortar.MortarScope;

/**
 * Created by drew on 5/12/15.
 */
@Layout(R.layout.profile_view2)
@WithComponentFactory(TrackListScreen.Factory.class)
public class TrackListScreen extends Screen implements ProfileScreen {

    final TrackList trackList;

    public TrackListScreen(TrackList trackList) {
        this.trackList = trackList;
    }

    @Override
    public String getName() {
        return super.getName() + "-" + trackList.getUri();
    }

    @Override
    public MortarFragment getFragment(Context context) {
        return TrackListScreenFragment.ni(context, this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(trackList.toBundle());
    }

    public static final Creator<TrackListScreen> CREATOR = new Creator<TrackListScreen>() {
        @Override
        public TrackListScreen createFromParcel(Parcel source) {
            Bundle b = source.readBundle(TrackList.class.getClassLoader());
            return new TrackListScreen(TrackList.BUNDLE_CREATOR.fromBundle(b));
        }

        @Override
        public TrackListScreen[] newArray(int size) {
            return new TrackListScreen[size];
        }
    };

    public static class Factory extends ComponentFactory<TrackListScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, TrackListScreen screen) {
            MusicActivityComponent activityComponent = DaggerService.getDaggerComponent(parentScope);
            return TrackListScreenComponent.FACTORY.call(activityComponent, screen);
        }
    }
}
