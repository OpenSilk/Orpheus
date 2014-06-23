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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.library.card.AlbumListCard;
import org.opensilk.music.ui.library.card.ArtistListCard;
import org.opensilk.music.ui.library.card.FolderListCard;
import org.opensilk.music.ui.library.card.SongListCard;
import org.opensilk.music.util.RemoteLibraryUtil;
import org.opensilk.silkdagger.DaggerInjector;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/14/14.
 */
public class LibraryFolderArrayAdapter extends AbsLibraryArrayAdapter {

    private String mFolderId;

    private DaggerInjector mInjector;

    public LibraryFolderArrayAdapter(Context context, String libraryIdentity, ComponentName libraryComponent, LoaderCallback callback, DaggerInjector injector) {
        super(context, libraryIdentity, libraryComponent, callback);
        mInjector = injector;
    }

    public void startLoad(String folderId) {
        mFolderId = folderId;
        startLoad();
    }

    @DebugLog
    protected void getMore() {
        try {
            mLoadingInProgress = true;
            RemoteLibraryUtil.getService(mLibraryComponent).browseFolders(mLibraryIdentity, mFolderId, STEP, mPaginationBundle,
                    new Result.Stub() {

                        @Override
                        public void success(final List<Bundle> items, final Bundle paginationBundle) throws RemoteException {
                            ((Activity) getContext()).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (paginationBundle == null) {
                                        mEndOfResults = true;
                                    }
                                    mPaginationBundle = paginationBundle;
                                    mLoadingInProgress = false;
                                    if (items.size() > 0) {
                                        addItems(items);
                                    }
                                    if (!mFirstLoadComplete && mCallback != null) {
                                        mFirstLoadComplete = true;
                                        mCallback.onFirstLoadComplete();
                                    }
                                }
                            });
                        }

                        @Override
                        @DebugLog
                        public void failure(int code, String reason) throws RemoteException {
//                            mPaginationBundle = null;
                            mLoadingInProgress = false;
                            //TODO
                        }
                    });
        } catch (RemoteException ex) {
            ex.printStackTrace();
            mLoadingInProgress = false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("folderid", mFolderId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        mFolderId = inState.getString("folderid");
    }

    @Override
    protected Card makeCard(Bundle data) {
        Class c;
        try {
            c = Class.forName(data.getString("clz"));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        if (Folder.class == c) {
            FolderListCard flc = new FolderListCard(getContext(), Folder.fromBundle(data));
            mInjector.inject(flc);
            return flc;
        } else if (Song.class == c) {
            return new SongListCard(getContext(), Song.fromBundle(data));
        } else if (Artist.class == c) {
            return new ArtistListCard(getContext(), Artist.fromBundle(data));
        } else if (Album.class == c) {
            return new AlbumListCard(getContext(), Album.fromBundle(data));
        }
        throw new IllegalArgumentException("Unknown resource class");
    }

    public String getFolderId() {
        return mFolderId;
    }

}
