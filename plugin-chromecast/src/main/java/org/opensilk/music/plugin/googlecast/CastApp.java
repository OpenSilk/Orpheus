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

package org.opensilk.music.plugin.googlecast;

import org.opensilk.common.core.app.BaseApp;
import org.opensilk.music.renderer.googlecast.CastComponent;

/**
 * Created by drew on 10/31/15.
 */
public class CastApp extends BaseApp {
    @Override
    protected Object getRootComponent() {
        return CastComponent.FACTORY.call(this);
    }
}
