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
import org.opensilk.music.model.Album;
import org.opensilk.music.library.sort.AlbumSortOrder;

import java.util.Comparator;

import rx.functions.Func2;

/**
 * Created by drew on 4/26/15.
 */
public class AlbumCompare {

    public static Func2<Album, Album, Integer> func(final String s) {
        return new Func2<Album, Album, Integer>() {
            @Override
            public Integer call(Album album, Album album2) {
                return comparator(s).compare(album, album2);
            }
        };
    }

    public static Comparator<Album> comparator(String sort) {
        switch (sort) {
            case AlbumSortOrder.MOST_TRACKS:
                return new Comparator<Album>() {
                    @Override
                    public int compare(Album lhs, Album rhs) {
                        //reversed
                        int c = rhs.trackCount - lhs.trackCount;
                        if (c == 0) {
                            return ObjectUtils.compare(lhs.name, rhs.name);
                        }
                        return c;
                    }
                };
            case AlbumSortOrder.ARTIST:
                return new Comparator<Album>() {
                    @Override
                    public int compare(Album lhs, Album rhs) {
                        int c = ObjectUtils.compare(lhs.artistName, rhs.artistName);
                        if (c == 0) {
                            return ObjectUtils.compare(lhs.name, rhs.name);
                        }
                        return c;
                    }
                };
            case AlbumSortOrder.NEWEST:
                return new Comparator<Album>() {
                    @Override
                    public int compare(Album lhs, Album rhs) {
                        //reversed
                        int c = ObjectUtils.compare(rhs.date, lhs.date);
                        if (c == 0) {
                            return ObjectUtils.compare(lhs.date, rhs.date);
                        }
                        return c;
                    }
                };
            default:
                return BundleableCompare.comparator(sort);
        }
    }
}
