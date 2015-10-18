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

package org.opensilk.music.ui3.profile.bio;

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
import org.opensilk.music.index.model.BioSummary;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.profile.ProfileScreen;

import java.util.ArrayList;
import java.util.List;

import mortar.MortarScope;

/**
 * Created by drew on 10/18/15.
 */
@Layout(R.layout.screen_bio)
@WithComponentFactory(BioScreen.Factory.class)
public class BioScreen extends Screen implements ProfileScreen {

    final List<ArtInfo> artInfos;
    final BioSummary bioSummary;

    public BioScreen(List<ArtInfo> artInfos, BioSummary bioSummary) {
        this.artInfos = artInfos;
        this.bioSummary = bioSummary;
    }

    @Override
    public MortarFragment getFragment(Context context) {
        return BioScreenFragment.ni(context, this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(bioSummary.toBundle());
        dest.writeTypedList(artInfos);
    }

    public static final Creator<BioScreen> CREATOR = new Creator<BioScreen>() {
        @Override
        public BioScreen createFromParcel(Parcel source) {
            BioSummary summary = BioSummary.BUNDLE_CREATOR.fromBundle(
                    source.readBundle(BioScreen.class.getClassLoader()));
            List<ArtInfo> artInfos = new ArrayList<>();
            source.readTypedList(artInfos, ArtInfo.CREATOR);
            return new BioScreen(artInfos, summary);
        }

        @Override
        public BioScreen[] newArray(int size) {
            return new BioScreen[size];
        }
    };

    public static class Factory extends ComponentFactory<BioScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, BioScreen screen) {
            MusicActivityComponent cmp = DaggerService.getDaggerComponent(parentScope);
            return BioScreenComponent.FACTORY.call(cmp, screen);
        }
    }
}
