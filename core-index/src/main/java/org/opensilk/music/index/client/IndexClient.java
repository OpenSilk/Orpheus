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

package org.opensilk.music.index.client;

import android.net.Uri;

import org.opensilk.music.model.Container;

/**
 * Created by drew on 9/17/15.
 */
public interface IndexClient {
    /**
     * @param uri
     * @return True if uri or any ancestor is indexed
     */
    boolean isIndexed(Container container);

    /**
     *
     * @param uri
     * @return True if succeeded, false on error or if {@link #isIndexed(Uri)} returns true;
     */
    boolean add(Container container);

    /**
     *
     * @param uri
     * @return True if success, false on error or if {@link #isIndexed(Uri)} returns false;
     */
    boolean remove(Container container);
}
