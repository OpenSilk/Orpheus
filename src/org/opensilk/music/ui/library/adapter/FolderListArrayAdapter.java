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

package org.opensilk.music.ui.library.adapter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.RemoteException;

import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.api.meta.LibraryInfo;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.ui.cards.AlbumCard;
import org.opensilk.music.ui.cards.ArtistCard;
import org.opensilk.music.ui.cards.FolderCard;
import org.opensilk.music.ui.cards.SongCard;
import org.opensilk.music.ui.library.RemoteLibraryHelper;
import org.opensilk.silkdagger.DaggerInjector;

import java.lang.ref.WeakReference;
import java.util.List;

import hugo.weaving.DebugLog;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/14/14.
 */
public class FolderListArrayAdapter extends AbsEndlessListArrayAdapter {

    private final DaggerInjector mInjector;
    protected int mRetryAttempts;

    public FolderListArrayAdapter(Context context,
                                  RemoteLibraryHelper library,
                                  LibraryInfo libraryInfo,
                                  Callback callback,
                                  DaggerInjector injector) {
        super(context, library, libraryInfo, callback);
        mInjector = injector;
        mRetryAttempts = 0;
    }

    //@DebugLog
    protected void getMore() {
        try {
            if (mRetryAttempts > 4) {
                throw new RemoteException();
            }
            mLoadingInProgress = true;
            RemoteLibrary l = mLibrary.getService();
            if (l != null) {
                //final int apiVersion = l.getApiVersion();
                //TODO api check
                {
                    l.browseFolders(mLibraryInfo.libraryId, mLibraryInfo.folderId,
                            STEP, mPaginationBundle, new ResultCallback(this));
                }
            } else {
                throw new RemoteException();
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
            if (mCallback != null) {
                mCallback.onLoadingFailure(false);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {

    }

    @Override
    protected Card makeCard(Bundle data) {
        try {
            Bundleable b = OrpheusApi.materializeBundle(data);
            if (b instanceof Folder) {
                FolderCard c = new FolderCard(getContext(), (Folder)b);
                mInjector.inject(c);
                return c;
            } else if (b instanceof Song) {
                SongCard c = new SongCard(getContext(), (Song)b);
                mInjector.inject(c);
                return c;
            } else if (b instanceof Artist) {
                ArtistCard c = new ArtistCard(getContext(), (Artist)b);
                mInjector.inject(c);
                return c;
            } else if (b instanceof Album) {
                AlbumCard c = new AlbumCard(getContext(), (Album)b);
                mInjector.inject(c);
                return c;
            }
        } catch (Exception e) {
            //fall
        }
        throw new IllegalArgumentException("Unknown resource class");
    }

    protected void processResult(List<Bundle> items, Bundle paginationBundle) {
        mRetryAttempts = 0;
        if (paginationBundle == null) {
            mEndOfResults = true;
        }
        mPaginationBundle = paginationBundle;
        mLoadingInProgress = false;
        if (getCount() > 0 && getItem(getCount()-1) == mLoadingCard) {
            remove(mLoadingCard);
        }
        if (items.size() > 0) {
            addItems(items);
        }
        if (!mFirstLoadComplete && mCallback != null) {
            mFirstLoadComplete = true;
            mCallback.onFirstLoadComplete();
        }
    }

    protected static class ResultCallback extends Result.Stub {
        private final WeakReference<FolderListArrayAdapter> adapter;

        protected ResultCallback(FolderListArrayAdapter adapter) {
            this.adapter = new WeakReference<>(adapter);
        }

        @Override
        @DebugLog
        public void onNext(final List<Bundle> items, final Bundle paginationBundle) throws RemoteException {
            final FolderListArrayAdapter a = adapter.get();
            if (a != null) {
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        a.processResult(items, paginationBundle);
                    }
                });
            }
        }

        @Override
        @DebugLog
        public void onError(ParcelableException e) throws RemoteException {
            int code = e.getCode();
            if (code == ParcelableException.RETRY) {
                final FolderListArrayAdapter a = adapter.get();
                if (a != null) {
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            a.mRetryAttempts++;
                            a.getMore();
                        }
                    }, a.mRetryAttempts > 0 ? 1000 * a.mRetryAttempts : 1000);
                }
            } else if (code == ParcelableException.NETWORK) {
                final FolderListArrayAdapter a = adapter.get();
                if (a != null) {
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ConnectivityManager cm = (ConnectivityManager) a.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo info = cm.getActiveNetworkInfo();
                            if (info != null && info.isConnectedOrConnecting()) {
                                a.mRetryAttempts++;
                                a.getMore();
                            } //else TODO
                        }
                    }, a.mRetryAttempts > 0 ? 1000 * a.mRetryAttempts : 1000);
                }
            } else {
                final int err = code;
                final FolderListArrayAdapter a = adapter.get();
                if (a != null) {
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (a.mCallback != null) {
                                a.mCallback.onLoadingFailure(err == ParcelableException.AUTH_FAILURE);
                            }
                        }
                    });
                }
            }
        }
    }

}
