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

import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import org.opensilk.music.library.provider.LibraryExtras;

import rx.Subscriber;

/**
 * Created by drew on 5/14/15.
 */
public class DeleteSubscriber extends Subscriber<Boolean> {
    public static final String TAG = DeleteSubscriber.class.getSimpleName();

    public static final int RESULT = 1;
    public static final int ERROR = 2;

    final ResultReceiver resultReceiver;

    public DeleteSubscriber(ResultReceiver resultReceiver) {
        this.resultReceiver = resultReceiver;
    }

    @Override
    public void onCompleted() {
        //nothing
    }

    @Override
    public void onError(Throwable e) {
        if (e instanceof LibraryException) {
            resultReceiver.send(ERROR, LibraryExtras.b().putCause((LibraryException) e).get());
        } else {
            Log.w(TAG, "Wrapping " + e.getClass() + " in a LibraryException for IPC transport");
            resultReceiver.send(ERROR, LibraryExtras.b().putCause(new LibraryException(e)).get());
        }
    }

    @Override
    public void onNext(Boolean aBoolean) {
        //TODO check if false and log
        resultReceiver.send(RESULT, LibraryExtras.b().putOk(aBoolean).get());
    }
}
