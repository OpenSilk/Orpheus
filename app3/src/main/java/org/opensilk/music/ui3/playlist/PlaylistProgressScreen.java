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
import android.os.Bundle;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.music.ui3.MusicActivityComponent;

import mortar.MortarScope;

/**
 * Created by drew on 12/17/15.
 */
@WithComponentFactory(PlaylistProgressScreen.Factory.class)
public class PlaylistProgressScreen extends Screen {

    public enum Operation {
        CREATE,
        ADDTO,
        DELETE,
        UPDATE,
    }

    final Operation operation;
    final Bundle extras;

    public PlaylistProgressScreen(Operation operation, Bundle extras) {
        this.operation = operation;
        this.extras = extras;
    }

    public static class Factory extends ComponentFactory<PlaylistProgressScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, PlaylistProgressScreen screen) {
            MusicActivityComponent cmp = DaggerService.getDaggerComponent(parentScope);
            return PlaylistProgressScreenComponent.FACTORY.call(cmp, screen);
        }
    }
}
