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

package org.opensilk.music.ui2;

/**
 * Created by drew on 4/20/15.
 */
public abstract class BaseMortarActivityBlueprint implements mortar.Blueprint {
    /**
     * Required for a race condition cause by Android when a new scope is created
     * before the old one is destroyed
     * <p>
     * https://github.com/square/mortar/issues/87#issuecomment-43849264
     */
    private final String scopeName;

    public BaseMortarActivityBlueprint(String scopeName) {
        this.scopeName = scopeName;
    }

    @Override
    public String getMortarScopeName() {
        return scopeName;
    }

}
