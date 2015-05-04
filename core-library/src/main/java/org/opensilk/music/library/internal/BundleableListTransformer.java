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

package org.opensilk.music.library.internal;

import org.opensilk.music.model.spi.Bundleable;

import java.util.List;

import rx.Observable;
import rx.functions.Func2;

/**
 * Convenience class to reduce amount of typing needed to get model items back into a list
 * to pass across ipc.
 *
 * Created by drew on 5/3/15.
 */
public class BundleableListTransformer<T extends Bundleable> implements Observable.Transformer<T, List<T>> {

    final Func2<T, T, Integer> sort;

    public BundleableListTransformer(Func2<T, T, Integer> sort) {
        this.sort = sort;
    }

    @Override
    public Observable<List<T>> call(Observable<T> tObservable) {
        if (sort == null) {
            return tObservable.toList();
        }
        return tObservable.toSortedList(sort);
    }
}
