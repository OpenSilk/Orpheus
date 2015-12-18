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
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.music.ui3.MusicActivityComponent;

import java.util.List;

import mortar.MortarScope;

/**
 * Created by drew on 12/17/15.
 */
@WithComponentFactory(PlaylistProviderSelectScreen.Factory.class)
public class PlaylistProviderSelectScreen extends Screen {

    final List<Uri> tracks;

    public PlaylistProviderSelectScreen(List<Uri> tracks) {
        this.tracks = tracks;
    }

    public static class Factory extends ComponentFactory<PlaylistProviderSelectScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, PlaylistProviderSelectScreen screen) {
            MusicActivityComponent cmp = DaggerService.getDaggerComponent(parentScope);
            return PlaylistProviderSelectScreenComponent.FACTORY.call(cmp, screen);
        }
    }
}
