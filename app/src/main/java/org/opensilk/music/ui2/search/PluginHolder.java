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

package org.opensilk.music.ui2.search;

import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;

/**
 * Created by drew on 11/24/14.
 */
public class PluginHolder {
    public final PluginInfo pluginInfo;
    public final PluginConfig pluginConfig;
    public final LibraryInfo libraryInfo;
    public PluginHolder(PluginInfo pluginInfo, PluginConfig pluginConfig, LibraryInfo libraryInfo) {
        this.pluginInfo = pluginInfo;
        this.pluginConfig = pluginConfig;
        this.libraryInfo = libraryInfo;
    }
}
