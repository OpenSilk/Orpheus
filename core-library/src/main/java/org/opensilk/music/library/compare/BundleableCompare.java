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
import org.opensilk.music.core.spi.Bundleable;
import org.opensilk.music.library.sort.BundleableSortOrder;

import java.util.Comparator;

/**
 * Created by drew on 4/26/15.
 */
public class BundleableCompare {
    public static <T extends Bundleable> Comparator<T> comparator(String sort) {
        switch (sort) {
            case BundleableSortOrder.Z_A:
                return new Comparator<T>() {
                    @Override
                    public int compare(T lhs, T rhs) {
                        //reversed;
                        return ObjectUtils.compare(rhs.getName(), lhs.getName());
                    }
                };
            case BundleableSortOrder.A_Z:
            default:
                return new Comparator<T>() {
                    @Override
                    public int compare(T lhs, T rhs) {
                        return ObjectUtils.compare(lhs.getName(), rhs.getName());
                    }
                };
        }
    }
}
