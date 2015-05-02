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
 * Created by drew on 4/29/15.
 */
public interface LibraryCapability {
    int ALBUMS        = 1 << 1;
    int ARTISTS       = 1 << 2;
    int FOLDERSTRACKS = 1 << 3;
    int FOLDERSALL    = 1 << 4;
    int GENRES        = 1 << 5;
    int PLAYLISTS     = 1 << 6;
    int TRACKS        = 1 << 7;

    int SEARCHABLE = 1 << 8;
    int SETTINGS   = 1 << 9;
    int DELETE     = 1 << 10;
    int RENAME     = 1 << 11;
    int DOWNLOAD   = 1 << 12;
}
