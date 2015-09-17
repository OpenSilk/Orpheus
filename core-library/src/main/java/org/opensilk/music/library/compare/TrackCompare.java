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

import org.opensilk.music.library.sort.TrackSortOrder;
import org.opensilk.music.model.Track;

import java.util.Comparator;

import rx.functions.Func2;

/**
 * Created by drew on 4/26/15.
 */
public class TrackCompare {

    public static Func2<Track, Track, Integer> func(final String sort) {
        return new Func2<Track, Track, Integer>() {
            @Override
            public Integer call(Track track, Track track2) {
                return comparator(sort).compare(track, track2);
            }
        };
    }

    public static Comparator<Track> comparator(String sort) {
        switch (sort) {
            case TrackSortOrder.ARTIST:
                return new Comparator<Track>() {
                    @Override
                    public int compare(Track lhs, Track rhs) {
                        int c = BundleableCompare.compareAZ(lhs.getArtistName(), rhs.getArtistName());
                        //TODO throw albumArtist into the mix?
                        if (c == 0) {
                            return BundleableCompare.compareNameAZ(lhs, rhs);
                        }
                        return c;
                    }
                };
            case TrackSortOrder.ALBUM:
                return new Comparator<Track>() {
                    @Override
                    public int compare(Track lhs, Track rhs) {
                        int c = BundleableCompare.compareAZ(lhs.getAlbumName(), rhs.getAlbumName());
                        if (c == 0) {
                            return lhs.getTrackNumber() - rhs.getTrackNumber();
                        }
                        return c;
                    }
                };
            case TrackSortOrder.LONGEST:
                return new Comparator<Track>() {
                    @Override
                    public int compare(Track lhs, Track rhs) {
                        //reversed
                        long c = rhs.getDuration() - lhs.getDuration();
                        if (c == 0) {
                            return BundleableCompare.compareNameAZ(lhs, rhs);
                        }
                        return (int)c;
                    }
                };
            case TrackSortOrder.PLAYORDER:
                return new Comparator<Track>() {
                    @Override
                    public int compare(Track lhs, Track rhs) {
                        return lhs.getTrackNumber() - rhs.getTrackNumber();
                    }
                };
            default:
                return BundleableCompare.comparator(sort);
        }
    }
}
