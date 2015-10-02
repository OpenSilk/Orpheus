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

/**
 * A Container has one or more children descending from Item or Container
 *
 * Created by drew on 9/2/15.
 */
public abstract class Container implements Model {

    protected final Uri uri;
    protected final Uri parentUri;
    protected final String name;
    protected final Metadata metadata;

    protected Container(@NonNull Uri uri, @NonNull Uri parentUri,
                        @NonNull String name, @NonNull Metadata metadata) {
        this.uri = uri;
        this.parentUri = parentUri;
        this.name = name;
        this.metadata = metadata;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    @Override
    public @NonNull Uri getUri() {
        return uri;
    }

    @Override
    public @NonNull String getSortName() {
        String disName = metadata.getString(Metadata.KEY_SORT_NAME);
        return disName != null ? disName : getName();
    }

    public @NonNull Uri getParentUri() {
        return parentUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Container container = (Container) o;
        return uri.equals(container.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }

}
