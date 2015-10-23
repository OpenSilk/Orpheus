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

package org.opensilk.music.library.client;

import android.os.RemoteException;

import org.opensilk.bundleable.BundleableListSlice;
import org.opensilk.music.library.internal.IBundleableObserver;
import org.opensilk.music.library.internal.LibraryException;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;

/**
 * Created by drew on 10/23/15.
 */
public class BundleableObserver<T> extends IBundleableObserver.Stub {
    final Subscriber<? super List<T>> subscriber;

    public BundleableObserver(Subscriber<? super List<T>> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onNext(BundleableListSlice slice) throws RemoteException {
        List<T> list = new ArrayList<>(slice.getList());
        if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(list);
        }
    }

    @Override
    public void onError(LibraryException e) throws RemoteException {
        if (!subscriber.isUnsubscribed()) {
            subscriber.onError(e);
        }
    }

    @Override
    public void onCompleted() throws RemoteException {
        if (!subscriber.isUnsubscribed()) {
            subscriber.onCompleted();
        }
    }
}
