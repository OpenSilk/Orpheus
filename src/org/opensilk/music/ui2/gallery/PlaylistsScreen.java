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

import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.common.widget.LetterTileDrawable;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.RxLoader;
import org.opensilk.music.ui2.profile.PlaylistScreen;
import org.opensilk.music.util.SortOrder;

import javax.inject.Inject;
import javax.inject.Singleton;

import flow.Layout;
import rx.functions.Func1;

/**
 * Created by drew on 10/19/14.
 */
@Layout(R.layout.gallery_page)
@WithModule(PlaylistsScreenModule.class)
@GalleryPageTitle(R.string.page_playlists)
public class PlaylistsScreen extends Screen {

    public static final Creator<PlaylistsScreen> CREATOR = new Creator<PlaylistsScreen>() {
        @Override
        public PlaylistsScreen createFromParcel(Parcel source) {
            PlaylistsScreen s = new PlaylistsScreen();
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public PlaylistsScreen[] newArray(int size) {
            return new PlaylistsScreen[size];
        }
    };
}
