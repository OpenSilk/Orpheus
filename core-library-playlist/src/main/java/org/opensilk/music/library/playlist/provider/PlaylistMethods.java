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

package org.opensilk.music.library.playlist.provider;

/**
 * Created by drew on 12/10/15.
 */
public interface PlaylistMethods {
    String CREATE = "playlist.create";
    String ADD_TO = "playlist.add_to";
    String REMOVE_FROM = "playlist.remove_from";
    String UPDATE = "playlist.update";
    String DELETE = "playlist.delete";
}
