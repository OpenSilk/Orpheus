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

import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Track;

import java.util.Comparator;

import rx.functions.Func2;

/**
 * Created by drew on 4/26/15.
 */
public class FolderTrackCompare {

    public static Func2<Model, Model, Integer> func(final String sort) {
        return new Func2<Model, Model, Integer>() {
            @Override
            public Integer call(Model bundleable, Model bundleable2) {
                return comparator(sort).compare(bundleable, bundleable2);
            }
        };
    }

    public static Comparator<Model> comparator(final String sort) {
        return new Comparator<Model>() {
            @Override
            public int compare(Model lhs, Model rhs) {
                if (lhs instanceof Folder && rhs instanceof Folder) {
                    Comparator<Folder> cf = FolderCompare.comparator(sort);
                    return cf.compare((Folder) lhs, (Folder) rhs);
                } else if (lhs instanceof Track && rhs instanceof Track) {
                    Comparator<Track> ct = TrackCompare.comparator(sort);
                    return ct.compare((Track) lhs, (Track) rhs);
                } else if (lhs instanceof Folder) {
                    return -1; //Folders higher
                } else if (rhs instanceof Track) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
    }
}
