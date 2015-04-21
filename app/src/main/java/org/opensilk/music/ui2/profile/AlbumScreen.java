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

import com.andrew.apollo.model.LocalAlbum;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.music.R;
import org.opensilk.music.ui2.gallery.GalleryScreen;

import flow.HasParent;
import flow.Layout;

/**
 * Created by drew on 11/18/14.
 */
@Layout(R.layout.profile_recycler)
@WithModule(AlbumScreenModule.class)
@WithTransitions(
        forward = { R.anim.shrink_fade_out, R.anim.slide_in_child_bottom },
        backward = { R.anim.slide_out_child_bottom, R.anim.grow_fade_in }
)
public class AlbumScreen extends Screen implements HasParent<GalleryScreen> {

    final LocalAlbum album;

    public AlbumScreen(LocalAlbum album) {
        this.album = album;
    }

    @Override
    public String getName() {
        return super.getName() + album.name;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(album, flags);
        super.writeToParcel(dest, flags);
    }

    @Override
    public GalleryScreen getParent() {
        return new GalleryScreen();
    }

    public static final Creator<AlbumScreen> CREATOR = new Creator<AlbumScreen>() {
        @Override
        public AlbumScreen createFromParcel(Parcel source) {
            AlbumScreen s = new AlbumScreen(
                    source.<LocalAlbum>readParcelable(LocalAlbum.class.getClassLoader())
            );
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public AlbumScreen[] newArray(int size) {
            return new AlbumScreen[size];
        }
    };

}
