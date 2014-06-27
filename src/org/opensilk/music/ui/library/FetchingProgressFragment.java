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

package org.opensilk.music.ui.library;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.util.RemoteLibraryUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 6/26/14.
 */
public class FetchingProgressFragment extends DialogFragment {

    private ComponentName mLibraryComponentName;
    private String mLibraryIdentity;
    private String mFolderIdentity;

    FetcherTask task;

    public static FetchingProgressFragment newInstance(String libraryIdentity, ComponentName libraryComponentName, String folderId) {
        FetchingProgressFragment f = new FetchingProgressFragment();
        Bundle b = new Bundle(3);
        b.putString(HomeFragment.ARG_IDENTITY, libraryIdentity);
        b.putParcelable(HomeFragment.ARG_COMPONENT, libraryComponentName);
        b.putString(HomeFragment.ARG_FOLDER_ID, folderId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            throw new IllegalArgumentException("Null args");
        }

        mLibraryComponentName = getArguments().getParcelable(HomeFragment.ARG_COMPONENT);
        mLibraryIdentity = getArguments().getString(HomeFragment.ARG_IDENTITY);
        mFolderIdentity = getArguments().getString(HomeFragment.ARG_FOLDER_ID);

        setStyle(STYLE_NO_TITLE, 0);

        task = new FetcherTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.fetching_song_list));
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        task.cancel(true);
    }

    class FetcherTask extends AsyncTask<Void, Void, Song[]> {
        @Override
        protected Song[] doInBackground(Void... params) {
            Fetcher f = new Fetcher(getActivity(), mLibraryIdentity, mFolderIdentity, mLibraryComponentName);
            f.run();
            return f.songs;
        }

        @Override
        protected void onPostExecute(Song[] songs) {
            if (isCancelled()) {
                return;
            }
            getDialog().dismiss();
            if (songs == null || songs.length == 0) {
                Toast.makeText(getActivity(), R.string.unable_to_fetch_songs, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), getResources().getQuantityString(R.plurals.NNNtrackstoqueue,
                        songs.length, songs.length), Toast.LENGTH_SHORT).show();
            }
        }
    }

    static class Fetcher implements Runnable {
        final Context appContext;
        final String library;
        final String folder;
        final ComponentName component;
        final Bundle bundle;

        Song[] songs;

        Fetcher(Context context, String library, String folder, ComponentName compontent) {
            this(context, library, folder, compontent, null);
        }

        Fetcher(Context context, String library, String folder, ComponentName component, Bundle bundle) {
            this.appContext = context.getApplicationContext();
            this.library = library;
            this.folder = folder;
            this.component = component;
            this.bundle = bundle;
        }

        @Override
        @DebugLog
        public void run() {
            ListResult result = new ListResult();
            try {
                RemoteLibraryUtil.getService(component).listSongsInFolder(library, folder, 10, bundle, result);
                songs = result.get();
                if (songs != null && songs.length > 0) {
                    MusicUtils.addSongsToQueueSilent(appContext, songs);
                    if (result.paginationBundle != null) {
                        Timber.d("Fetching more songs from " + component.getShortClassName() + " in folder " + folder);
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Fetcher(appContext, library, folder, component, result.paginationBundle));
                    }
                }
            } catch (RemoteException|InterruptedException|ExecutionException e) {
                //pass
            }
        }

        class ListResult extends Result.Stub implements Future<Song[]> {

            Song[] songs;
            volatile boolean done;
            Bundle paginationBundle;

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
                return done;
            }

            @Override
            @DebugLog
            public Song[] get() throws InterruptedException, ExecutionException {
                while (!done) {
                    synchronized (this) {
                        wait();
                    }
                }
                if (songs == null || songs.length == 0) {
                    throw new InterruptedException();
                }
                return songs;
            }

            @Override
            public Song[] get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public synchronized void success(List<Bundle> items, Bundle paginationBundle) throws RemoteException {
                this.paginationBundle = paginationBundle;
                songs = new Song[items.size()];
                int ii=0;
                for (Bundle b : items) {
                    try {
                        Song s = Song.fromBundle(b);
                        songs[ii++] = s;
                    } catch (IllegalArgumentException ignored) { }
                }
                done = true;
                notifyAll();
            }

            @Override
            public synchronized void failure(int code, String reason) throws RemoteException {
                done = true;
                notifyAll();
            }
        }
    }
}
