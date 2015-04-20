/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2.gallery;

import android.os.Parcel;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.R;

import flow.Layout;

/**
 * Created by drew on 10/19/14.
 */
@Layout(R.layout.gallery_page)
@WithModule(ArtistsScreenModule.class)
@GalleryPageTitle(R.string.page_artists)
public class ArtistsScreen extends Screen {

    public static final Creator<ArtistsScreen> CREATOR = new Creator<ArtistsScreen>() {
        @Override
        public ArtistsScreen createFromParcel(Parcel source) {
            ArtistsScreen s = new ArtistsScreen();
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public ArtistsScreen[] newArray(int size) {
            return new ArtistsScreen[size];
        }
    };

}
