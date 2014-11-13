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
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.opensilk.music.api.callback.Result;

/**
 * Created by drew on 6/9/14.
 */
public abstract class RemoteLibraryService extends Service {

    /**
     * @return Config describing this library
     */
    protected abstract PluginConfig getConfig();

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
     * The {@link org.opensilk.music.api.callback.Result.Stub#onNext(java.util.List, android.os.Bundle) Result.success()}
     * lists must be non null but can be empty, pass a null Bundle to indicate end of results.
     *
     * @param libraryIdentity String representation of an available library
     * @param folderIdentity String representation of the folder to browse, null must be accepted and treated
     *                       as if the user wants the browse the root folder
     * @param maxResults int maximum number of results to return;
     * @param paginationBundle Bundle that can hold what ever information you need to continue retrieving results
     *                         null must be accepted and treated an initial starting point.
     *                         The Bundle passed here will be the same bundle passed in the
     *                         {@link org.opensilk.music.api.callback.Result.Stub#onNext(java.util.List, android.os.Bundle) Result.success()}
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

        @Override
        public Bundle getConfig() throws RemoteException {
            return service.getConfig().dematerialize();
        }

        @Override
        public void pause() throws RemoteException {
            service.pause();
        }

        @Override
        public void resume() throws RemoteException {
            service.resume();
        }

        @Override
        public void browseFolders(String libraryIdentity, String folderIdentity,
                                  int maxResults, Bundle paginationBundle, Result callback) throws RemoteException {
            service.browseFolders(libraryIdentity, folderIdentity, maxResults, paginationBundle, callback);
        }

        @Override
        public void listSongsInFolder(String libraryIdentity, String folderIdentity,
                                      int maxResults, Bundle paginationBundle, Result callback) throws RemoteException {
            service.listSongsInFolder(libraryIdentity, folderIdentity, maxResults, paginationBundle, callback);
        }

        @Override
        public void search(String libraryIdentity, String query, int maxResults,
                           Bundle paginationBundle, Result callback) throws RemoteException {
            service.search(libraryIdentity, query, maxResults, paginationBundle, callback);
        }

    }

}
