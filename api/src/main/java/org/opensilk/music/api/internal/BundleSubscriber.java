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

package org.opensilk.music.api.internal;

import android.os.Bundle;
import android.os.RemoteException;

import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.api.spi.IBundleObserver;

import rx.Subscriber;

/**
 * Created by drew on 11/12/14.
 */
public class BundleSubscriber extends Subscriber<Bundle> {
    final static long INITIAL_REQUEST = 1;

    final IBundleObserver bundleObserver;

    public BundleSubscriber(IBundleObserver bundleObserver) {
        super();
        this.bundleObserver = bundleObserver;
    }

    @Override
    public void onStart() {
        request(INITIAL_REQUEST);
    }

    @Override
    public void onCompleted() {
        try {
            bundleObserver.onCompleted();
        } catch (RemoteException e) {
            unsubscribe();
        }
    }

    @Override
    public void onError(Throwable e) {
        try {
            if (e instanceof ParcelableException) {
                bundleObserver.onError((ParcelableException) e);
            } else {
                bundleObserver.onError(new ParcelableException(e));
            }
        } catch (RemoteException e2) {
            unsubscribe();
        }
    }

    @Override
    public void onNext(Bundle bundle) {
        try {
            bundleObserver.onNext(bundle);
        } catch (RemoteException e) {
            unsubscribe();
        }
    }

    //Expose protected method so we can poke it across process
    protected void _request(long n) {
        request(n);
    }
}
