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

package org.opensilk.music.ui3.playlist;

import android.content.res.Resources;
import android.net.Uri;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Layout;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.music.R;
import org.opensilk.music.ui3.MusicActivityComponent;

import java.util.List;

import mortar.MortarScope;

/**
 * Created by drew on 10/24/15.
 */
@Layout(R.layout.screen_playlistchoose)
@WithComponentFactory(PlaylistChooseScreen.Factory.class)
public class PlaylistChooseScreen extends Screen {

    final Uri loaderUri;
    final int listKind;
    final List<Uri> uris;

    public interface ListKind {
        int POINTER = 1; //List is tracks uris *must query to get real list*
        int REAL = 2; //List is uris of tracks
    }

    public PlaylistChooseScreen(Uri loaderUri, int listKind, List<Uri> uris) {
        this.loaderUri = loaderUri;
        this.listKind = listKind;
        this.uris = uris;
    }

    @Override
    public String getName() {
        return super.getName() + "-" + loaderUri;
    }

    public static class Factory extends ComponentFactory<PlaylistChooseScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, PlaylistChooseScreen screen) {
            MusicActivityComponent parent = DaggerService.getDaggerComponent(parentScope);
            return PlaylistChooseScreenComponent.FACTORY.call(parent, new PlaylistChooseScreenModule(screen));
        }
    }
}
