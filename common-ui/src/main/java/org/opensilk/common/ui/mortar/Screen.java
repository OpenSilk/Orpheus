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

package org.opensilk.common.ui.mortar;

/**
 * What is a screen?. (draft)
 *
 * A screen is an entry point for all views. ie if you have a screen you can make
 * the view, create any dependencies that view needs and automatically load any data
 * needed to populate the view.
 *
 * A screen...
 *  *must* be annotated with {@link Layout} and {@link WithComponentFactory}<br>
 *  may or may not be parcelable this is application dependent.<br>
 *  *must* have a globally unique name.<br>
 *  is usually inside the {@link org.opensilk.common.core.dagger2.ScreenScope}
 *  but may have a custom scope. It may never be in the {@link org.opensilk.common.core.dagger2.ActivityScope}
 *  ...etc
 *
 * Created by drew on 5/6/15.
 */
public abstract class Screen implements HasName {
    @Override
    public String getName() {
        return getClass().getName();
    }
}
