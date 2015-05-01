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

package org.opensilk.music.artwork;

import org.opensilk.music.model.ArtInfo;

/**
 * Created by drew on 12/20/14.
 */
public class RequestKey {
    public final ArtInfo artInfo;
    public final ArtworkType artworkType;

    public RequestKey(ArtInfo artInfo, ArtworkType artworkType) {
        this.artInfo = artInfo;
        this.artworkType = artworkType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestKey that = (RequestKey) o;

        if (artInfo != null ? !artInfo.equals(that.artInfo) : that.artInfo != null) return false;
        if (artworkType != that.artworkType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = artInfo != null ? artInfo.hashCode() : 0;
        result = 31 * result + artworkType.hashCode();
        return result;
    }

}
