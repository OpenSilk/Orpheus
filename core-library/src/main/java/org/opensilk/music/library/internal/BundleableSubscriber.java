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

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.bundleable.BundleableListSlice;

import java.util.List;

import rx.Subscriber;

/**
 * Proxy subscriber so regular observables can send results across ipc.
 * Users must check isUnsubscribed before calling onNext (like a good observable)
 * This class takes care of unsubsribing itself in the event of binder death
 *
 * Created by drew on 5/3/15.
 */
public class BundleableSubscriber<T extends Bundleable> extends Subscriber<List<T>> implements IBinder.DeathRecipient {
    static final String TAG = BundleableSubscriber.class.getSimpleName();

    private final IBundleableObserver wrapped;

    public BundleableSubscriber(IBinder binder) {
        this.wrapped = IBundleableObserver.Stub.asInterface(binder);
        try {
            binder.linkToDeath(this, 0);
        } catch (RemoteException e) {
            unsubscribe();
        }
    }

    @Override
    public void onCompleted() {
        try {
            wrapped.onCompleted();
        } catch (RemoteException e) {
            Log.e(TAG, "onCompleted", e);
        } finally {
            unlink();
        }
    }

    @Override
    public void onError(Throwable e) {
        try {
            if (e instanceof LibraryException) {
                wrapped.onError((LibraryException) e);
            } else {
                Log.w(TAG, "Wrapping " + e.getClass() + " in a LibraryException for IPC transport");
                wrapped.onError(LibraryException.unwrap(e));
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "onError", e2);
        } finally {
            unlink();
        }
    }

    @Override
    public void onNext(List<T> bundleables) {
        final BundleableListSlice<T> slice = new BundleableListSlice<T>(bundleables);
        try {
            wrapped.onNext(slice);
        } catch (RemoteException e) {
            unsubscribe();
            Log.e(TAG, "onNext", e);
        }
    }

    @Override
    public void binderDied() {
        unsubscribe();
    }

    private void unlink() {
        wrapped.asBinder().unlinkToDeath(this, 0);
    }
}
