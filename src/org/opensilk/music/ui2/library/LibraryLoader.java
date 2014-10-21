/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui2.library;

import android.os.Bundle;
import android.os.RemoteException;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/10/14.
 */
public class LibraryLoader {

    final int STEP = 30;
    final PluginConnectionManager connectionManager;
    final LibraryInfo info;

    @Inject
    public LibraryLoader(PluginConnectionManager connectionManager, LibraryInfo info) {
        this.connectionManager = connectionManager;
        this.info = info;
    }

    public Observable<Result> getObservable(Bundle previousPaginationBundle) {
        return Observable
                // FRP thing to get around using final
                .just(previousPaginationBundle)
                // transform the bundle into the observable we want
                .flatMap(new Func1<Bundle, Observable<Result>>() {
                    @Override
                    public Observable<Result> call(final Bundle bundle) {
                        // grab our connection and map in to the result
                        return connectionManager.bind(info.libraryComponent)
                                .flatMap(new Func1<RemoteLibrary, Observable<Result>>() {
                                    @Override
                                    public Observable<Result> call(RemoteLibrary remoteLibrary) {
                                        Timber.v("Flatmap Func1: called On: %s", Thread.currentThread().getName());
                                        ResultFuture resultFuture = new ResultFuture();
                                        try {
                                            remoteLibrary.browseFolders(info.libraryId, info.currentFolderId, STEP, bundle, resultFuture);
                                        } catch (RemoteException e) {
                                            // remove the library so we can try to rebind on retry
                                            connectionManager.onException(info.libraryComponent);
                                            return Observable.error(e);
                                        }
                                        // wraps our future into an Observable
                                        return Observable.from(resultFuture, Schedulers.io());
                                    }
                                });
                    }
                })
                // onNext(Result) gets called on ui thread
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static class Result {
        final List<Bundleable> items;
        final Bundle token;

        public Result(List<Bundleable> items, Bundle token) {
            this.items = items;
            this.token = token;
        }
    }

    public static class LibraryException extends Throwable {
        final int code;

        public LibraryException(String detailMessage, int code) {
            super(detailMessage);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    static class ResultFuture extends org.opensilk.music.api.callback.Result.Stub implements Future<Result> {

        private boolean mResultReceived = false;
        private LibraryException mException;
        private Result mResult;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return mResultReceived;
        }

        @Override
        public Result get() throws InterruptedException, ExecutionException {
            try {
                return doGet(null);
            } catch (TimeoutException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return doGet(TimeUnit.MILLISECONDS.convert(timeout, unit));
        }

        private synchronized Result doGet(Long timeoutMs) throws InterruptedException, ExecutionException, TimeoutException {
//            Timber.v("doGet: called on: %s", Thread.currentThread().getName());
            if (mException != null) {
                throw new ExecutionException(mException);
            }

            if (mResultReceived) {
                return mResult;
            }

            if (timeoutMs == null) {
                wait(0);
            } else if (timeoutMs > 0) {
                wait(timeoutMs);
            }

            if (mException != null) {
                throw new ExecutionException(mException);
            }

            if (!mResultReceived) {
                throw new TimeoutException();
            }

            return mResult;
        }


        @Override
        public synchronized void success(List<Bundle> items, Bundle paginationBundle) throws RemoteException {
//            Timber.v("success: called on: %s", Thread.currentThread().getName());
            mResultReceived = true;
            if (items == null || items.isEmpty()) {
                //empty list were done
                mResult = new Result(Collections.<Bundleable>emptyList(), paginationBundle);
            } else {
                ArrayList<Bundleable> bundleables = new ArrayList<>(items.size());
                for (Bundle b : items) {
                    try {
                        bundleables.add(OrpheusApi.transformBundle(b));
                    } catch (Exception e) {
                        //ignore for now
                    }
                }
                mResult = new Result(bundleables, paginationBundle);
            }
            notifyAll();
        }

        @Override
        public synchronized void failure(int code, String reason) throws RemoteException {
            mException = new LibraryException(reason, code);
            notifyAll();
        }
    }

}
