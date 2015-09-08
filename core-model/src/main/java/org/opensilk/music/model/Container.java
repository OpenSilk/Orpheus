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
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.opensilk.music.model.spi.Bundleable;

/**
 * A Container has one or more children descending from Item or Container
 *
 * Created by drew on 9/2/15.
 */
public abstract class Container implements Bundleable {

    protected final Uri uri;
    protected final String name;
    protected final Metadata metadata;

    protected Container(@NonNull Uri uri, @NonNull String name, @NonNull Metadata metadata) {
        this.uri = uri;
        this.name = name;
        this.metadata = metadata;
    }

    @Override @Deprecated
    public String getIdentity() {
        return uri.toString();
    }

    @Override @Deprecated
    public String getName() {
        return getDisplayName();
    }

    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    public Uri getParentUri() {
        return metadata.getUri(Metadata.KEY_PARENT_URI);
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
        return name;
    }

}
