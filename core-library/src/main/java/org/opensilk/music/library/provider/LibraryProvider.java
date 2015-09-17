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

package org.opensilk.music.library.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.compare.FolderTrackCompare;
import org.opensilk.music.library.internal.BundleableListTransformer;
import org.opensilk.music.library.internal.BundleableSubscriber;
import org.opensilk.music.library.internal.DeleteSubscriber;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.internal.ResultReceiver;
import org.opensilk.music.library.sort.BundleableSortOrder;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.spi.Bundleable;

import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import static org.opensilk.music.library.internal.LibraryException.Kind.BAD_BINDER;
import static org.opensilk.music.library.internal.LibraryException.Kind.ILLEGAL_URI;
import static org.opensilk.music.library.internal.LibraryException.Kind.METHOD_NOT_IMPLEMENTED;
import static org.opensilk.music.library.provider.LibraryUris.M_FOLDER;
import static org.opensilk.music.library.provider.LibraryUris.M_PLAYLIST;
import static org.opensilk.music.library.provider.LibraryUris.M_TRACKS;

/**
 * Created by drew on 4/26/15.
 */
public abstract class LibraryProvider extends ContentProvider {
    public static final String TAG = LibraryProvider.class.getSimpleName();

    /**
     * Authority prefix all libraries must start with to be discoverable by orpheus
     */
    public static final String AUTHORITY_PFX = "orpheus.library.";

    /**
     * Our full authority
     */
    protected String mAuthority;

    private UriMatcher mMatcher;
    private Scheduler scheduler = Schedulers.computation();

    @Override
    public boolean onCreate() {
        mAuthority = AUTHORITY_PFX + getBaseAuthority();
        mMatcher = LibraryUris.makeMatcher(mAuthority);
        return true;
    }

    /**
     * Base authority for library appended to {@link #AUTHORITY_PFX}
     * default is package name, this is usually sufficient unless package contains
     * multiple libraries
     */
    protected String getBaseAuthority() {
        return getContext().getPackageName();
    }

    /**
     * @return This libraries config
     */
    protected abstract LibraryConfig getLibraryConfig();

    @Override
    public final Bundle call(String method, String arg, Bundle extras) {

        final LibraryExtras.Builder ok = LibraryExtras.b();
        ok.putOk(true);

        if (method == null) method = "";
        switch (method) {
            case LibraryMethods.LIST:
            case LibraryMethods.GET:
            case LibraryMethods.SCAN:
            case LibraryMethods.ROOTS: {
                extras.setClassLoader(getClass().getClassLoader());

                final IBinder binder = LibraryExtras.getBundleableObserverBinder(extras);
                if (binder == null || !binder.isBinderAlive()) {
                    //this is mostly for the null, if the binder is dead then
                    //sending them a reason is moot. but we check the binder here
                    //so we dont have to do it 50 times below and we can be pretty
                    //sure the linkToDeath will succeed
                    ok.putOk(false);
                    ok.putCause(new LibraryException(BAD_BINDER, null));
                    return ok.get();
                }

                final Uri uri = LibraryExtras.getUri(extras);
                final String sortOrder = extras.getString(LibraryExtras.SORTORDER);
                final Bundle args = LibraryExtras.b()
                        .putUri(uri)
                        .putSortOrder(sortOrder != null ? sortOrder : BundleableSortOrder.A_Z)
                        .get();

                switch (method) {
                    case LibraryMethods.LIST: {
                        listObjsInternal(uri, binder, args);
                        break;
                    }
                    case LibraryMethods.GET: {
                        getObjInternal(uri, binder, args);
                        break;
                    }
                    case LibraryMethods.SCAN: {
                        scanObjsInternal(uri, binder, args);
                        break;
                    }
                    case LibraryMethods.ROOTS: {
                        listRootsInternal(uri, binder, args);
                        break;
                    }
                }

                return ok.get();
            }
            case LibraryMethods.DELETE: {
                extras.setClassLoader(getClass().getClassLoader());

                final ResultReceiver resultReceiver = LibraryExtras.getResultReciever(extras);
                if (resultReceiver == null) {
                    ok.putOk(false).putCause(new LibraryException(BAD_BINDER, null));
                    return ok.get();
                }

                final Uri uri = LibraryExtras.getUri(extras);
                final DeleteSubscriber subscriber = new DeleteSubscriber(resultReceiver);
                final Bundle args = LibraryExtras.b()
                        .putNotifyUri(LibraryExtras.getNotifyUri(extras))
                        .get();

                deleteObjInternal(uri, subscriber, args);

                return ok.get();
            }
            case LibraryMethods.UPDATE_ITEM: {
                //TODO
                return ok.get();
            }
            case LibraryMethods.CONFIG: {
                return getLibraryConfig().dematerialize();
            }
            default: {
                Log.e(TAG, "Unknown method " + method);
                ok.putOk(false).putCause(new LibraryException(METHOD_NOT_IMPLEMENTED,
                        new UnsupportedOperationException(method)));
                return ok.get();
            }
        }
    }

    /*
     * Start internal methods.
     * You can override these if you need specialized handling.
     *
     * You don't need to override these for caching, you can just send the cached list, then hit the network
     * and once it comes in send a notify on the Uri, Orpheus will requery and you can send the updated cached list.
     */

    protected void listObjsInternal(final Uri uri, final IBinder binder, final Bundle args){
        final BundleableSubscriber<Bundleable> subscriber = new BundleableSubscriber<>(binder);
        Observable<Bundleable> o = Observable.create(
                new Observable.OnSubscribe<Bundleable>() {
                    @Override
                    public void call(Subscriber<? super Bundleable> subscriber) {
                        listObjs(uri, subscriber, args);
                    }
                })
                .subscribeOn(scheduler);
        o.compose(
                new BundleableListTransformer<Bundleable>(FolderTrackCompare.func(LibraryExtras.getSortOrder(args)))
        ).subscribe(subscriber);
    }

    protected void getObjInternal(final Uri uri, final IBinder binder, final Bundle args){
        final BundleableSubscriber<Bundleable> subscriber = new BundleableSubscriber<>(binder);
        Observable<Bundleable> o = Observable.create(
                new Observable.OnSubscribe<Bundleable>() {
                    @Override
                    public void call(Subscriber<? super Bundleable> subscriber) {
                        getObj(uri, subscriber, args);
                    }
                })
                .subscribeOn(scheduler);
        o.compose(
                new BundleableListTransformer<Bundleable>(null)
        ).subscribe(subscriber);
    }

    protected void scanObjsInternal(final Uri uri, final IBinder binder, final Bundle args){
        final BundleableSubscriber<Bundleable> subscriber = new BundleableSubscriber<>(binder);
        Observable<Bundleable> o = Observable.create(
                new Observable.OnSubscribe<Bundleable>() {
                    @Override
                    public void call(Subscriber<? super Bundleable> subscriber) {
                        scanObjs(uri, subscriber, args);
                    }
                })
                .subscribeOn(scheduler);
        o.compose(
                new BundleableListTransformer<Bundleable>(null)
        ).subscribe(subscriber);
    }

    protected void listRootsInternal(final Uri uri, final IBinder binder, final Bundle args){
        final BundleableSubscriber<Container> subscriber = new BundleableSubscriber<>(binder);
        Observable<Container> o = Observable.create(
                new Observable.OnSubscribe<Container>() {
                    @Override
                    public void call(Subscriber<? super Container> subscriber) {
                        listRoots(uri, subscriber, args);
                    }
                })
                .subscribeOn(scheduler);
        o.compose(
                new BundleableListTransformer<Container>(null)
        ).subscribe(subscriber);
    }

    /*
     * Start query stubs
     *
     * You must call subscriber.onComplete after emitting the list
     */

    protected void listObjs(Uri uri, Subscriber<? super Bundleable> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void getObj(Uri uri, Subscriber<? super Bundleable> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void scanObjs(Uri uri, Subscriber<? super Bundleable> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    protected void listRoots(Uri uri, Subscriber<? super Container> subscriber, Bundle args) {
        subscriber.onError(new UnsupportedOperationException());
    }

    /*
     * End query stubs
     */

    /*
     * Start internal delete methods
     */

    protected void deleteObjInternal(final Uri uri, final Subscriber<List<Uri>> subscriber, final Bundle args) {
        Observable.create(
                new Observable.OnSubscribe<Uri>() {
                    @Override
                    public void call(Subscriber<? super Uri> subscriber) {
                        deleteObj(uri, subscriber, args);
                    }
                })
                .subscribeOn(scheduler)
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        Uri u = LibraryExtras.getNotifyUri(args);
                        if (u != null) {
                            getContext().getContentResolver().notifyChange(u, null);
                        }
                    }
                })
                .toList()
                .subscribe(subscriber);
    }

    /*
     * End internal delete methods
     */

    /*
     * Start delete stubs
     */

    /**
     * Delete the object specified by <code>uri</code>. Emmit all uri's removed by change (ie
     * children of object)
     */
    protected void deleteObj(final Uri uri, final Subscriber<? super Uri> subscriber, final Bundle args) {
        throw new UnsupportedOperationException();
    }

    /*
     * End delete stubs
     */

    /*
     * Start abstract methods, we are 100% out-of-band and do not support any of these
     */

    @Override
    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    /*
     * End abstract methods
     */

    //Blocking observable always throws RuntimeExceptions
    private static Exception unwrapE(Exception e) {
        if (e instanceof RuntimeException) {
            Throwable c = e.getCause();
            if (c instanceof Exception) {
                return (Exception) c;
            }
        }
        return e;
    }

}
