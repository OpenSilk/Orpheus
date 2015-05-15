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

package org.opensilk.music.ui3.delete;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.library.internal.DeleteSubscriber;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.internal.ResultReceiver;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 5/14/15.
 */
@ScreenScope
public class DeleteScreenPresenter extends ViewPresenter<DeleteScreenView> {

    final Context appContext;
    final DeleteRequest request;
    final FragmentManagerOwner fm;

    boolean inprogress = false;
    boolean complete = false;
    boolean success = false;
    String error;
    Subscription loaderSubscription;

    @Inject
    public DeleteScreenPresenter(
            @ForApplication Context appContext,
            DeleteRequest request,
            FragmentManagerOwner fm
    ) {
        this.appContext = appContext;
        this.request = request;
        this.fm = fm;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        getView().showDelete(request.name);
        if (complete) {
            if (success) {
                getView().showSuccess();
            } else {
                getView().showError(error);
            }
        } else if (inprogress) {
            getView().gotoLoading();
        }
    }

    boolean dismisscalled = false;
    void dismissSelf() {
        if (!dismisscalled) {
            dismisscalled = true;
            fm.goBack();
        } else {
            Timber.e("Dismiss called more than once");
        }
    }

    void notifySuccess() {
        complete = true;
        success = true;
        if (hasView()) {
            getView().showSuccess();
        }
    }

    void notifyFailure(LibraryException e) {
        complete = true;
        success = false;
        if (e != null && e.getCause() != null) {
            error = e.getCause().getMessage();
        }
        if (hasView()) {
            getView().showError(error);
        }
    }

    void doDelete() {
        inprogress = true;
        if (request.tracksUri != null) {
            deleteTracks(request.tracksUri, request.notifyUri);
        } else if (request.trackUrisList != null) {
            deleteTracks(request.trackUrisList, request.notifyUri);
        } else if (request.callUri != null) {
            delete(request.callUri, request.notifyUri);
        } //else todo
    }

    //For folders and playlists
    void delete(Uri uri, Uri notiyUri) {
        Bundle extras = LibraryExtras.b()
                .putUri(uri)
                .putNotifyUri(notiyUri)
                .putResultReceiver(makeResultReceiver())
                .get();
        appContext.getContentResolver().call(uri, LibraryMethods.DELETE, null, extras);
    }

    //for albums and artists
    void deleteTracks(Uri uri, final Uri notifyUri) {
        if (isSubscribed(loaderSubscription)) {
            return;
        }
        final String authority = uri.getAuthority();
        final String libraryId = uri.getPathSegments().get(0);
        loaderSubscription = new BundleableLoader(appContext, uri, null).createObservable()
                .flatMap(new Func1<List<Bundleable>, Observable<Bundleable>>() {
                    @Override
                    public Observable<Bundleable> call(List<Bundleable> bundleables) {
                        return Observable.from(bundleables);
                    }
                })
                .collect(new ArrayList<Uri>(), new Action2<ArrayList<Uri>, Bundleable>() {
                    @Override
                    public void call(ArrayList<Uri> uris, Bundleable track) {
                        uris.add(LibraryUris.track(authority, libraryId, track.getIdentity()));
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ArrayList<Uri>>() {
                    @Override
                    public void call(ArrayList<Uri> uris) {
                        deleteTracks(uris, notifyUri);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        notifyFailure(new LibraryException(throwable));
                    }
                });
    }

    //usually for single tracks
    void deleteTracks(List<Uri> trackUris, Uri notifyUri) {
        final Uri uri = LibraryUris.tracks(notifyUri.getAuthority(), notifyUri.getPathSegments().get(0));
        Bundle extras = LibraryExtras.b()
                .putUri(uri)
                .putNotifyUri(notifyUri)
                .putResultReceiver(makeResultReceiver())
                .putUriList(trackUris)
                .get();
        Bundle ok = appContext.getContentResolver().call(uri, LibraryMethods.DELETE, null, extras);
        if (ok == null || !LibraryExtras.getOk(ok)) {
            notifyFailure(LibraryExtras.getCause(ok));
        }
    }

    ResultReceiver makeResultReceiver() {
        return new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == DeleteSubscriber.RESULT) {
                    if (LibraryExtras.getOk(resultData)) {
                        notifySuccess();
                    } else {
                        notifyFailure(null);
                    }
                } else if (resultCode == DeleteSubscriber.ERROR) {
                    notifyFailure(LibraryExtras.getCause(resultData));
                }
            }
        };
    }
}
