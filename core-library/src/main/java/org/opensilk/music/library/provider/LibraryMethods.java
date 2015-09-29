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

package org.opensilk.music.library.provider;

/**
 * Method parameters sent to the call
 *
 * Created by drew on 5/3/15.
 */
public interface LibraryMethods {

    /**
     * List children of container;
     */
    String LIST = "list";
    /**
     * Return whatever object this uri describes
     */
    String GET = "get";
    /**
     * Return whatever object the uri list describes
     */
    String MULTI_GET = "multi_get";
    /**
     * List children of container, returned objects need not be sorted
     */
    String SCAN = "scan";
    /**
     * Request default containers, sorting is left to discretion of library
     */
    String ROOTS = "roots";
    /**
     * Delete object described by uri
     */
    String DELETE = "delete";
    /**
     * Update item
     */
    String UPDATE_ITEM = "update";
    /**
     * Request plugin config
     * returned bundle from the call is the dematerialized {@link org.opensilk.music.library.LibraryConfig}
     */
    String CONFIG = "config";

}
