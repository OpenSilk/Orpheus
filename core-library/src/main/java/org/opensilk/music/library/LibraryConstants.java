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

package org.opensilk.music.library;

/**
 * Created by drew on 4/28/15.
 */
public interface LibraryConstants {
    /**
     * Incremented when breaking changes made
     */
    int API_VERSION_MAJOR = 0;
    /**
     * New feature or removal or whatever. As long as major is same orpheus will still work
     */
    int API_VERSION_MINOR = 3;
    /**
     * Intent extra containing {@link String} library identity, used in multiple places
     */
    @Deprecated
    String EXTRA_LIBRARY_ID = "org.opensilk.music.library.extra.LIBRARY_ID";
    /**
     * Intent extra containing the {@link org.opensilk.music.library.LibraryInfo}
     */
    @Deprecated
    String EXTRA_LIBRARY_INFO = "org.opensilk.music.api.LIBRARY_INFO";
    /**
     * Intent extra containing the library's content authority
     */
    @Deprecated
    String EXTRA_LIBRARY_AUTHORITY = "org.opensilk.music.library.extra.LIBRARY_AUTHORITY";
    /**
     * Intent extra passed by Orpheus to plugin activities to help them determine whether to use light or dark themes
     */
    String EXTRA_WANT_LIGHT_THEME = "org.opensilk.music.library.extra.WANT_LIGHT_THEME";
}
