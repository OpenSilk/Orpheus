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

package org.opensilk.music.ui2.profile;

import android.os.Parcel;

import com.andrew.apollo.model.LocalArtist;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.music.R;
import org.opensilk.music.ui2.gallery.GalleryScreen;

import flow.HasParent;
import flow.Layout;

/**
 * Created by drew on 11/19/14.
 */
@Layout(R.layout.profile_recycler)
@WithModule(ArtistScreenModule.class)
@WithTransitions(
        forward = { R.anim.shrink_fade_out, R.anim.slide_in_child_bottom },
        backward = { R.anim.slide_out_child_bottom, R.anim.grow_fade_in }
)
public class ArtistScreen extends Screen implements HasParent<GalleryScreen> {

    final LocalArtist artist;

    public ArtistScreen(LocalArtist artist) {
        this.artist = artist;
    }

    @Override
    public String getName() {
        return super.getName() + artist.name;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(artist, flags);
        super.writeToParcel(dest, flags);
    }

    @Override
    public GalleryScreen getParent() {
        return new GalleryScreen();
    }

    public static final Creator<ArtistScreen> CREATOR = new Creator<ArtistScreen>() {
        @Override
        public ArtistScreen createFromParcel(Parcel source) {
            ArtistScreen s = new ArtistScreen(
                    source.<LocalArtist>readParcelable(LocalArtist.class.getClassLoader())
            );
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public ArtistScreen[] newArray(int size) {
            return new ArtistScreen[size];
        }
    };

}
