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
import org.opensilk.music.model.Artist;
import org.opensilk.music.library.sort.ArtistSortOrder;

import java.util.Comparator;

import rx.functions.Func2;

/**
 * Created by drew on 4/26/15.
 */
public class ArtistCompare {

    public static Func2<Artist, Artist, Integer> func(final String sort) {
        return new Func2<Artist, Artist, Integer>() {
            @Override
            public Integer call(Artist artist, Artist artist2) {
                return comparator(sort).compare(artist, artist2);
            }
        };
    }

    public static Comparator<Artist> comparator(String sort) {
        switch (sort) {
            case ArtistSortOrder.MOST_TRACKS:
                return new Comparator<Artist>() {
                    @Override
                    public int compare(Artist lhs, Artist rhs) {
                        //reversed
                        int c = rhs.trackCount - lhs.trackCount;
                        if (c == 0) {
                            return ObjectUtils.compare(lhs.name, rhs.name);
                        }
                        return c;
                    }
                };
            case ArtistSortOrder.MOST_ALBUMS:
                return new Comparator<Artist>() {
                    @Override
                    public int compare(Artist lhs, Artist rhs) {
                        //reversed
                        int c = rhs.albumCount - lhs.albumCount;
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
