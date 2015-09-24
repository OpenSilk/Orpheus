/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.common.core.rx;

import java.util.List;

import rx.Observable;

/**
 * Created by drew on 10/24/14.
 */
public interface RxLoader<T> {
    interface ContentChangedListener {
        void reload();
    }
    Observable<T> getObservable();
    Observable<List<T>> getListObservable();
    void addContentChangedListener(ContentChangedListener l);
    void removeContentChangedListener(ContentChangedListener l);
    void reset();
}
