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

package org.opensilk.music.plugin.common;

/**
 * Created by drew on 7/19/14.
 */
public class PluginUtil {

    private PluginUtil() {}

    /**
     * Mangles the input to create a posix safe file name.
     * All illegal characters are replaced with '_' (underscores)
     * This could potentially replace the entire string with underscores,
     * but that would be on you for using dumbass naming conventions.
     * @param string
     * @return
     */
    public static String posixSafe(String string) {
        String s = string;
        if (string.startsWith("-")) {
            s = string.replaceFirst("-", "_");
        }
        return s.replaceAll("[^a-zA-Z0-9_\\.\\-]", "_");
    }
}
