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
import org.opensilk.music.api.meta.LibraryInfo;

import java.lang.ref.WeakReference;

/**
 * Created by drew on 6/9/14.
 */
public abstract class RemoteLibraryService extends Service {

    /**
     * @return Bitmask of {@link org.opensilk.music.api.OrpheusApi.Ability}
     */
    protected abstract int getCapabilities();

    /**
     * Return intent for activity to allow user to choose from available libraries.
     * The activity should be of Dialog Style, and should take care of everything needed
     * to allow user to access the library, including selecting from an available library (or
     * account) and any auth/sign in required. The activity must return {@link android.app.Activity#RESULT_OK}
     * with the extra {@link OrpheusApi#EXTRA_LIBRARY_INFO} in the Intent containing the identity Orpheus will pass
     * to all subsequent calls.
     * <p>
     * Although not required, it is preferable the activity utilizes
     * {@link OrpheusApi#EXTRA_WANT_LIGHT_THEME} to style the activity to match the
     * current Orpheus theme.
     */
    protected abstract Intent getLibraryChooserIntent();

    /**
     * Return intent for settings activity. The settings activity must process the
     * {@link OrpheusApi#EXTRA_LIBRARY_ID} and only manipulate preferences concerning the
     * given identity.
     * <p>
     * Although not required, it is preferable the activity utilizes
     * {@link OrpheusApi#EXTRA_WANT_LIGHT_THEME} to style the activity to match the
     * current Orpheus theme.
     */
    protected abstract Intent getSettingsIntent();

    /**
     * will be called by orpheus during the activity onStop() method. If your plugin does any active
     * scanning or other persistend battry draining activity your should suspend it here
     */
    protected void pause() {

    }

    /**
     * opposite of pause, called during activity onStart(), reverse anything you did in pause() here
     */
    protected void resume() {

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

    /**
     * @return The users preferred library. If null Orpheus will launch the picker activity.
     *         At a minimum {@link org.opensilk.music.api.meta.LibraryInfo#libraryId} must be set
     *         it is preferable that all fields be populated. {@link org.opensilk.music.api.meta.LibraryInfo#folderId}
     *         should contain the id of the root folder
     */
    @Nullable
    protected abstract LibraryInfo getDefaultLibraryInfo();

    private RemoteLibrary.Stub mBinder;
    private Handler mHandler;
    private Runnable mShutdownTask;
    private int clientCount = 0;

    @Override
    public IBinder onBind(Intent intent) {
        Log.v("RemoteLibrary", "onBind()");
        mHandler.removeCallbacks(mShutdownTask);
        clientCount++;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v("RemoteLibrary", "onRebind()");
        super.onRebind(intent);
        mHandler.removeCallbacks(mShutdownTask);
        clientCount++;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("RemoteLibrary", "onUnbind()");
        if (--clientCount == 0) {
            mHandler.postDelayed(mShutdownTask, 60000);
        }
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new ServiceBinder(this);
        mHandler = new Handler();
        mShutdownTask = new ShutdownTask(this);
    }

    @Override
    public void onDestroy() {
        Log.v("RemoteLibrary", "onDestroy()");
        super.onDestroy();
        mBinder = null;
        mHandler = null;
        mShutdownTask = null;
    }

    private final static class ServiceBinder extends RemoteLibrary.Stub {
        private final WeakReference<RemoteLibraryService> ref;

        protected ServiceBinder(RemoteLibraryService service) {
            this.ref = new WeakReference<>(service);
        }

        @Override
        public int getApiVersion() throws RemoteException {
            return OrpheusApi.API_VERSION;
        }

        @Override
        public int getCapabilities() throws RemoteException {
            RemoteLibraryService s = ref.get();
            if (s != null) {
                return s.getCapabilities();
            }
            return 0;
        }

        /*
         * XXX intent is passed as param because it doesn't seem to work
         * if you just return an intent.
         */
        @Override
        public void getLibraryChooserIntent(Intent i) throws RemoteException {
            RemoteLibraryService s = ref.get();
            if (s != null) {
                Intent ogi = s.getLibraryChooserIntent();
                copyIntent(i, ogi);
            }
        }

        @Override
        public void getSettingsIntent(Intent i) throws RemoteException {
            RemoteLibraryService s = ref.get();
            if (s != null) {
                Intent ogi = s.getSettingsIntent();
                copyIntent(i, ogi);
            }
        }

        @Override
        public void pause() throws RemoteException {
            RemoteLibraryService s = ref.get();
            if (s != null) {
                s.pause();
            }
        }

        @Override
        public void resume() throws RemoteException {
            RemoteLibraryService s = ref.get();
            if (s != null) {
                s.resume();
            }
        }

        @Override
        public void browseFolders(String libraryIdentity, String folderIdentity, int maxResults, Bundle paginationBundle, Result callback) throws RemoteException {
            RemoteLibraryService s = ref.get();
            if (s != null) {
                s.browseFolders(libraryIdentity, folderIdentity, maxResults, paginationBundle, callback);
            }
        }

        @Override
        public void listSongsInFolder(String libraryIdentity, String folderIdentity, int maxResults, Bundle paginationBundle, Result callback) throws RemoteException {
            RemoteLibraryService s = ref.get();
            if (s != null) {
                s.listSongsInFolder(libraryIdentity, folderIdentity, maxResults, paginationBundle, callback);
            }
        }

        @Override
        public void search(String libraryIdentity, String query, int maxResults, Bundle paginationBundle, Result callback) throws RemoteException {
            RemoteLibraryService s = ref.get();
            if (s != null) {
                s.search(libraryIdentity, query, maxResults, paginationBundle, callback);
            }
        }

        public LibraryInfo getDefaultLibraryInfo() throws RemoteException {
            RemoteLibraryService s = ref.get();
            if (s != null) {
                return s.getDefaultLibraryInfo();
            }
            return null;
        }

        private static void copyIntent(Intent i, Intent ogi) {
            // Make sure everything is copied
            int flags = Intent.FILL_IN_ACTION|Intent.FILL_IN_CATEGORIES|Intent.FILL_IN_CLIP_DATA;
            flags |= Intent.FILL_IN_COMPONENT|Intent.FILL_IN_DATA|Intent.FILL_IN_PACKAGE;
            flags |= Intent.FILL_IN_SELECTOR|Intent.FILL_IN_SOURCE_BOUNDS;
            i.fillIn(ogi,flags);
        }

    }

    private static final class ShutdownTask implements Runnable {
        private final WeakReference<Service> ref;

        private ShutdownTask(Service service) {
            this.ref = new WeakReference<>(service);
        }

        @Override
        public void run() {
            Service s = ref.get();
            if (s != null) {
                s.stopSelf();
            }
        }
    }

}
