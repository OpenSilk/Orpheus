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

package org.opensilk.music.model.compare;

import android.os.Build;

import org.opensilk.music.model.Track;
import org.opensilk.music.model.sort.TrackSortOrder;

import java.util.Comparator;
import java.util.List;

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
                        int c = BaseCompare.compareAZ(lhs.getArtistName(), rhs.getArtistName());
                        if (c == 0) {
                            c = BaseCompare.compareAZ(lhs.getAlbumName(), rhs.getAlbumName());
                        }
                        if (c == 0) {
                            c = compareDiscTrack(lhs, rhs);
                        }
                        return c;
                    }
                };
            case TrackSortOrder.ALBUM:
                return new Comparator<Track>() {
                    @Override
                    public int compare(Track lhs, Track rhs) {
                        int c = BaseCompare.compareAZ(lhs.getAlbumName(), rhs.getAlbumName());
                        if (c == 0) {
                            return compareDiscTrack(lhs, rhs);
                        }
                        return c;
                    }
                };
            case TrackSortOrder.LONGEST:
                return new Comparator<Track>() {
                    @Override
                    public int compare(Track lhs, Track rhs) {
                        List<Track.Res> lhsR = lhs.getResources();
                        List<Track.Res> rhsR = rhs.getResources();
                        if (!lhsR.isEmpty() && !rhsR.isEmpty()) {
                            //reversed
                            if (Build.VERSION.SDK_INT >= 19) {
                                return Long.compare(rhsR.get(0).getDuration(), lhsR.get(0).getDuration());
                            } else {
                                return Integer.compare(rhsR.get(0).getDurationS(), lhsR.get(0).getDurationS());
                            }
                        }
                        return 0;
                    }
                };
            case TrackSortOrder.PLAYORDER:
                return new Comparator<Track>() {
                    @Override
                    public int compare(Track lhs, Track rhs) {
                        return compareDiscTrack(lhs, rhs);
                    }
                };
            default:
                return BaseCompare.comparator(sort);
        }
    }

    private static int compareDiscTrack(Track lhs, Track rhs) {
        return compareDiscTrack(lhs.getDiscNumber(), lhs.getTrackNumber(),
                rhs.getDiscNumber(), rhs.getTrackNumber());
    }

    private static int compareDiscTrack(int disc1, int track1, int disc2, int track2) {
        if ((disc1 > 0 && disc2 > 0)) {
            int dc = Integer.compare(disc1, disc2);
            return dc != 0 ? dc : Integer.compare(track1, track2);
        } else {
            return Integer.compare(track1, track2);
        }
    }
}
