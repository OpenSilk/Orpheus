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
import android.os.Bundle;
import android.os.RemoteException;

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

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/14/14.
 */
public class FolderListArrayAdapter extends AbsEndlessListArrayAdapter {

    private DaggerInjector mInjector;

    public FolderListArrayAdapter(Context context,
                                  RemoteLibraryHelper library,
                                  LibraryInfo libraryInfo,
                                  Callback callback,
                                  DaggerInjector injector) {
        super(context, library, libraryInfo, callback);
        mInjector = injector;
    }

    //@DebugLog
    protected void getMore() {
        try {
            mLoadingInProgress = true;
            RemoteLibrary l = mLibrary.getService();
            if (l != null) {
                l.browseFolders(mLibraryInfo.libraryId, mLibraryInfo.currentFolderId,
                        STEP, mPaginationBundle, new ResultCallback(this));
            } //else what todo?
        } catch (RemoteException ex) {
            ex.printStackTrace();
            mLoadingInProgress = false;
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
        Bundleable b = OrpheusApi.transformBundle(data);
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
        throw new IllegalArgumentException("Unknown resource class");
    }

    protected void processResult(List<Bundle> items, Bundle paginationBundle) {
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
        public void success(final List<Bundle> items, final Bundle paginationBundle) throws RemoteException {
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
        public void failure(int code, String reason) throws RemoteException {
            //TODO
//            mPaginationBundle = null;
//            mLoadingInProgress = false;
        }
    }

}
