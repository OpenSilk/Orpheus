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

import org.opensilk.music.library.sort.FolderSortOrder;
import org.opensilk.music.model.Folder;

import java.util.Comparator;

import rx.functions.Func2;

/**
 * Created by drew on 4/26/15.
 */
public class FolderCompare {
    public static Func2<Folder, Folder, Integer> func(final String sort) {
        return new Func2<Folder, Folder, Integer>() {
            @Override
            public Integer call(Folder folder, Folder folder2) {
                return comparator(sort).compare(folder, folder2);
            }
        };
    }

    public static Comparator<Folder> comparator(String sort) {
        switch (sort) {
            case FolderSortOrder.MOST_CHILDREN:
                return new Comparator<Folder>() {
                    @Override
                    public int compare(Folder lhs, Folder rhs) {
                        //Reversed
                        int c = rhs.getChildCount() - lhs.getChildCount();
                        if (c == 0) {
                            return BundleableCompare.compareNameAZ(lhs, rhs);
                        }
                        return c;
                    }
                };
            case FolderSortOrder.NEWEST:
                return new Comparator<Folder>() {
                    @Override
                    public int compare(Folder lhs, Folder rhs) {
                        //Z-A
                        int c = BundleableCompare.compareZA(lhs.getDateModified(), rhs.getDateModified());
                        if (c == 0) {
                            return BundleableCompare.compareNameAZ(lhs, rhs);
                        }
                        return c;
                    }
                };
            default:
                return BundleableCompare.comparator(sort);
        }
    }
}
