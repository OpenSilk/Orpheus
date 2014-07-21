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

package org.opensilk.music.api.callback;

import android.os.Bundle;

import java.util.List;

oneway interface Result {
    /**
     * @param items list of objects, may be any object (or combination) in the model package
     * @param paginationBundle container for any kind of pagination token you
     *              need to continue retrieving results, a null bundle is
     *              interpreted as end of results.
     */
    void success(in List<Bundle> items, in Bundle paginationBundle);
    /**
     * Stub
     */
    void failure(int code, String reason);
}