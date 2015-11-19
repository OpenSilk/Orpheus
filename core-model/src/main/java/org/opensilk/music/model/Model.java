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

package org.opensilk.music.model;

import android.net.Uri;
import android.support.annotation.NonNull;

import org.opensilk.bundleable.Bundleable;

/**
 * Created by drew on 9/21/15.
 */
public interface Model extends Bundleable {
    @NonNull Uri getUri();
    @NonNull Uri getParentUri();
    @NonNull String getName();
    @NonNull String getSortName();
    long getFlags();
}
