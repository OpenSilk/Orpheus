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

package org.opensilk.music.api;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.api.internal.BundleSubscriber;
import org.opensilk.music.api.internal.ISubscriptionImpl;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.spi.IBundleObserver;
import org.opensilk.music.api.spi.ISubscription;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 6/9/14.
 */
public abstract class RemoteLibraryService extends Service {

    protected abstract Config getConfig();

    /**
     * will be called by Orpheus during the activity onStop() method. If your plugin does any active
     * scanning or other persistent battery draining activity your should suspend it here
     */
    protected void pause() {
        //Stub
    }

    /**
     * opposite of pause, called during activity onStart(), reverse anything you did in pause() here
     */
    protected void resume() {
        //Stub
    }

    /**
     * Return a list of {@link org.opensilk.music.api.model.spi.Bundleable} objects
     * via the {@link org.opensilk.music.api.callback.Result} callback.
     * <p>
     * The {@link org.opensilk.music.api.callback.Result.Stub#success(java.util.List, android.os.Bundle) Result.success()}
     * lists must be non null but can be empty, pass a null Bundle to indicate end of results.
     *
     * @param libraryIdentity String representation of an available library
     * @param folderIdentity String representation of the folder to browse, null must be accepted and treated
     *                       as if the user wants the browse the root folder
     * @param maxResults int maximum number of results to return;
     * @param paginationBundle Bundle that can hold what ever information you need to continue retrieving results
     *                         null must be accepted and treated an initial starting point.
     *                         The Bundle passed here will be the same bundle passed in the
     *                         {@link org.opensilk.music.api.callback.Result.Stub#success(java.util.List, android.os.Bundle) Result.success()}
     *                         method of the previous query.
     * @param callback the {@link org.opensilk.music.api.callback.Result} callback to send back to Orpheus
     */
    protected abstract void browseFolders(@NonNull String libraryIdentity, @Nullable String folderIdentity, int maxResults,
                                          @Nullable Bundle paginationBundle, @NonNull Result callback);

    /**
     * Return a list of {@link org.opensilk.music.api.model.Song}s via the {@link org.opensilk.music.api.callback.Result} callback.
     * You should only return direct children, do not walk the file system here.
     * @see #browseFolders(String, String, int, android.os.Bundle, org.opensilk.music.api.callback.Result) for params description
     *
     * @param libraryIdentity
     * @param folderIdentity
     * @param maxResults
     * @param paginationBundle
     * @param callback
     */
    protected abstract void listSongsInFolder(@NonNull String libraryIdentity, @Nullable String folderIdentity,
                                              int maxResults, @Nullable Bundle paginationBundle, @NonNull Result callback);

    /**
     * Return a list of {@link org.opensilk.music.api.model.spi.Bundleable} objects
     * via the {@link org.opensilk.music.api.callback.Result} callback that match the query.
     * @see #browseFolders(String, String, int, android.os.Bundle, org.opensilk.music.api.callback.Result) for params description
     *
     * @param libraryIdentity
     * @param query the search query, raw user input from Search box
     * @param maxResults
     * @param paginationBundle
     * @param callback
     */
    protected abstract void search(@NonNull String libraryIdentity, @NonNull String query, int maxResults,
                                   @Nullable Bundle paginationBundle, @NonNull Result callback);

    protected abstract Observable<Bundle> browse(@NonNull String libraryIdentity, @Nullable String folderIdentity);
    protected abstract Observable<Bundle> browseSongs(@NonNull String libraryIdentity, @Nullable String folderIdentity);
    protected abstract Observable<Bundle> search(@NonNull String libraryIdentity, @NonNull String query);

    private RemoteLibrary.Stub mBinder;

    @Override
    public IBinder onBind(Intent intent) {
        Log.v("RemoteLibrary", "onBind()");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v("RemoteLibrary", "onRebind()");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("RemoteLibrary", "onUnbind()");
        return true;
    }

    @Override
    public void onCreate() {
        Log.v("RemoteLibrary", "onCreate()");
        super.onCreate();
        mBinder = new ServiceBinder(this);
    }

    @Override
    public void onDestroy() {
        Log.v("RemoteLibrary", "onDestroy()");
        super.onDestroy();
        mBinder = null;
    }

    private final static class ServiceBinder extends RemoteLibrary.Stub {
        private final RemoteLibraryService service;

        protected ServiceBinder(RemoteLibraryService service) {
            this.service = service;
        }

        @Override @Deprecated
        public int getApiVersion() throws RemoteException {
            return Config.materialize(getConfig()).apiVersion;
        }

        @Override @Deprecated
        public int getCapabilities() throws RemoteException {
            return Config.materialize(getConfig()).capabilities;
        }

        @Override @Deprecated
        public void getLibraryChooserIntent(Intent i) throws RemoteException {
            copyIntent(i, new Intent().setComponent(Config.materialize(getConfig()).pickerComponent));
        }

        @Override @Deprecated
        public void getSettingsIntent(Intent i) throws RemoteException {
            copyIntent(i, new Intent().setComponent(Config.materialize(getConfig()).settingsComponent));
        }

        @Override
        public void pause() throws RemoteException {
            service.pause();
        }

        @Override
        public void resume() throws RemoteException {
            service.resume();
        }

        @Override @Deprecated
        public void browseFolders(String libraryIdentity, String folderIdentity,
                                  int maxResults, Bundle paginationBundle, Result callback) throws RemoteException {
            sendUpdateNotice(callback);
        }

        @Override @Deprecated
        public void listSongsInFolder(String libraryIdentity, String folderIdentity,
                                      int maxResults, Bundle paginationBundle, Result callback) throws RemoteException {
            sendUpdateNotice(callback);
        }

        @Override @Deprecated
        public void search(String libraryIdentity, String query, int maxResults,
                           Bundle paginationBundle, Result callback) throws RemoteException {
            sendUpdateNotice(callback);
        }

        @Override
        public Bundle getConfig() throws RemoteException {
            return service.getConfig().dematerialize();
        }

        @Override
        public ISubscription browse(String libraryIdentity, String folderIdentity, IBundleObserver bundleObserver) throws RemoteException {
            final BundleSubscriber s = new BundleSubscriber(bundleObserver);
            service.browse(libraryIdentity, folderIdentity).subscribe(s);
            return new ISubscriptionImpl(s);
        }

        @Override
        public ISubscription browseSongs(String libraryIdentity, String folderIdentity, IBundleObserver bundleObserver) throws RemoteException {
            final BundleSubscriber s = new BundleSubscriber(bundleObserver);
            service.browseSongs(libraryIdentity, folderIdentity).subscribe(s);
            return new ISubscriptionImpl(s);
        }

        @Override
        public ISubscription search2(String libraryIdentity, String query, IBundleObserver bundleObserver) throws RemoteException {
            final BundleSubscriber s = new BundleSubscriber(bundleObserver);
            service.search(libraryIdentity, query).subscribe(s);
            return new ISubscriptionImpl(s);
        }

        //Notify user they need to update Orpheus to use the new plugins
        private void sendUpdateNotice(Result callback) throws RemoteException {
            List<Bundle> list = new ArrayList<>(1);
            list.add(new Folder.Builder().setIdentity("ident").setName("Upgrade Orpheus to use plugin").build().toBundle());
            callback.success(list, null);
        }

        private static void copyIntent(Intent i, Intent ogi) {
            // Make sure everything is copied
            int flags = Intent.FILL_IN_ACTION|Intent.FILL_IN_CATEGORIES|Intent.FILL_IN_CLIP_DATA;
            flags |= Intent.FILL_IN_COMPONENT|Intent.FILL_IN_DATA|Intent.FILL_IN_PACKAGE;
            flags |= Intent.FILL_IN_SELECTOR|Intent.FILL_IN_SOURCE_BOUNDS;
            i.fillIn(ogi,flags);
        }

    }

}
