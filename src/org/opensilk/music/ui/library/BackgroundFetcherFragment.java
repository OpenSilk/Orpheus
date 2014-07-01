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

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.library.FetchingProgressFragment.Action;
import org.opensilk.music.util.RemoteLibraryUtil;

import java.util.List;

/**
 * Created by drew on 7/1/14.
 */
public class BackgroundFetcherFragment extends Fragment {

    private ComponentName mLibraryComponentName;
    private String mLibraryIdentity;
    private String mFolderIdentity;
    private Action mAction;

    FetcherTask task;
    int numadded = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    class FetcherTask extends AsyncTask<Void, Void, Void> {

        final Action action;
        final Bundle bundle;
        final ListResult result;

        FetcherTask(Action action) {
            this(action, null);
        }

        FetcherTask(Action action, Bundle bundle) {
            this.action = action;
            this.bundle = bundle;
            this.result = new ListResult();
        }

        @Override
        //@DebugLog
        protected Void doInBackground(Void... params) {
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            try {
                RemoteLibraryUtil.getService(mLibraryComponentName)
                        .listSongsInFolder(mLibraryIdentity, mFolderIdentity, 5, bundle, result);
                result.waitForComplete();
            } catch (RemoteException |InterruptedException e) {
                //pass
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (isCancelled()) {
                return;
            }
            if (result.songs == null || result.songs.length == 0) {
                if (numadded == 0) {
                    Toast.makeText(getActivity(), R.string.unable_to_fetch_songs, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getResources().getQuantityString(R.plurals.NNNtrackstoqueue,
                            numadded, numadded), Toast.LENGTH_SHORT).show();
                }
//                getDialog().dismiss();
            } else {
                switch (action) {
                    case PLAY_ALL:
                        MusicUtils.playAllSongs(getActivity(), result.songs, 0, false);
                        break;
                    case SHUFFLE_ALL:
                        MusicUtils.playAllSongs(getActivity(), result.songs, 0, true);
                        break;
                    case ADD_QUEUE:
                        MusicUtils.addSongsToQueueSilent(getActivity(), result.songs);
                        break;
                }
                numadded += result.songs.length;
                if (result.paginationBundle != null) {
                    Handler h = new Handler();
                    h.post(new Runnable() {
                        @Override
                        public void run() {
//                            ((ProgressDialog) getDialog()).setMessage(getString(R.string.fetching_song_list)
//                                    + " " + getResources().getQuantityString(R.plurals.Nsongs, numadded, numadded));
                            task = new FetcherTask(Action.ADD_QUEUE, result.paginationBundle);
                            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), getResources().getQuantityString(R.plurals.NNNtrackstoqueue,
                            numadded, numadded), Toast.LENGTH_SHORT).show();
//                    getDialog().dismiss();
                }
            }
        }
    }

    class ListResult extends Result.Stub {

        Song[] songs;
        boolean done;
        Bundle paginationBundle;

        public synchronized void waitForComplete() throws InterruptedException {
            while (!done) {
                wait();
            }
        }

        @Override
        public synchronized void success(List<Bundle> items, Bundle paginationBundle) throws RemoteException {
            this.paginationBundle = paginationBundle;
            songs = new Song[items.size()];
            int ii=0;
            for (Bundle b : items) {
                try {
                    Song s = Song.BUNDLE_CREATOR.fromBundle(b);
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
