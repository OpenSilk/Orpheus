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

package org.opensilk.music.library.compare;

import org.apache.commons.lang3.ObjectUtils;
import org.opensilk.music.model.Track;
import org.opensilk.music.library.sort.TrackSortOrder;

import java.util.Comparator;

/**
 * Created by drew on 4/26/15.
 */
public class TrackCompare {
    public static Comparator<Track> comparator(String sort) {
        switch (sort) {
            case TrackSortOrder.ARTIST:
                return new Comparator<Track>() {
                    @Override
                    public int compare(Track lhs, Track rhs) {
                        int c = ObjectUtils.compare(lhs.artistName, rhs.artistName);
                        //TODO throw albumArtist into the mix?
                        if (c == 0) {
                            return ObjectUtils.compare(lhs.name, rhs.name);
                        }
                        return c;
                    }
                };
            case TrackSortOrder.ALBUM:
                return new Comparator<Track>() {
                    @Override
                    public int compare(Track lhs, Track rhs) {
                        int c = ObjectUtils.compare(lhs.albumName, rhs.albumName);
                        if (c == 0) {
                            return ObjectUtils.compare(lhs.name, rhs.name);
                        }
                        return c;
                    }
                };
            case TrackSortOrder.DURATION:
                return new Comparator<Track>() {
                    @Override
                    public int compare(Track lhs, Track rhs) {
                        //reversed
                        int c = rhs.duration - lhs.duration;
                        if (c == 0) {
                            return ObjectUtils.compare(lhs.name, rhs.name);
                        }
                        return c;
                    }
                };
            default:
                return BundleableCompare.comparator(sort);
        }
    }
}
