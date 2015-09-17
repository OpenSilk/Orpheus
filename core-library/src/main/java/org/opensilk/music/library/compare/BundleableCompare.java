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

import org.opensilk.music.library.sort.BundleableSortOrder;
import org.opensilk.music.model.spi.Bundleable;

import java.util.Comparator;

import rx.functions.Func2;

/**
 * Created by drew on 4/26/15.
 */
public class BundleableCompare {

    public static <T extends Bundleable> Func2<T, T, Integer> func(final String sort) {
        return new Func2<T, T, Integer>() {
            @Override
            public Integer call(T t, T t2) {
                return comparator(sort).compare(t, t2);
            }
        };
    }

    public static <T extends Bundleable> Comparator<T> comparator(String sort) {
        switch (sort) {
            case BundleableSortOrder.A_Z:
                return new Comparator<T>() {
                    @Override
                    public int compare(T lhs, T rhs) {
                        return compareNameAZ(lhs, rhs);
                    }
                };
            case BundleableSortOrder.Z_A:
                return new Comparator<T>() {
                    @Override
                    public int compare(T lhs, T rhs) {
                        return compareNameZA(lhs, rhs);
                    }
                };
            default:
                return new Comparator<T>() {
                    @Override
                    public int compare(T lhs, T rhs) {
                        return 0; //NoSort
                    }
                };
        }
    }

    private static final AlphanumComparator sComparator = new AlphanumComparator();

    public static <T extends Bundleable> int compareNameAZ(T lhs, T rhs) {
        return compareAZ(lhs.getName(), rhs.getName());
    }

    public static int compareAZ(String lhs, String rhs) {
        return sComparator.compare(lhs, rhs);
    }

    public static <T extends Bundleable> int compareNameZA(T lhs, T rhs) {
        return compareZA(lhs.getName(), rhs.getName());
    }

    public static int compareZA(String lhs, String rhs) {
        //reversed
        return sComparator.compare(rhs, lhs);
    }
}
