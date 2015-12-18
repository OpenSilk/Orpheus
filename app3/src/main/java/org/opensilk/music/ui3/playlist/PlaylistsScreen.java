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

import mortar.MortarScope;

/**
 * Created by drew on 5/5/15.
 */
@Layout(R.layout.bundleable_recycler_list)
@WithComponentFactory(PlaylistsScreen.Factory.class)
public class PlaylistsScreen extends Screen {

    final Uri loaderUri;
    final String title;

    public PlaylistsScreen(Uri loaderUri, String title) {
        this.loaderUri = loaderUri;
        this.title = title;
    }

    public Uri getLoaderUri() {
        return loaderUri;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String getName() {
        return super.getName() + "-" + loaderUri;
    }

    public static class Factory extends ComponentFactory<PlaylistsScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, PlaylistsScreen screen) {
            MusicActivityComponent activityComponent = DaggerService.getDaggerComponent(parentScope);
            return PlaylistsScreenComponent.FACTORY.call(activityComponent, screen);
        }
    }
}
