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

import org.opensilk.music.api.callback.FolderBrowseResult;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Resource;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.library.card.FolderListCard;
import org.opensilk.music.ui.library.card.SongListCard;
import org.opensilk.music.util.RemoteLibraryUtil;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/14/14.
 */
public class LibraryFolderArrayAdapter extends AbsLibraryArrayAdapter<Resource> {

    private String mFolderId;

    public LibraryFolderArrayAdapter(Context context, String libraryIdentity, ComponentName libraryComponent, LoaderCallback callback) {
        super(context, libraryIdentity, libraryComponent, callback);
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
                    new FolderBrowseResult.Stub() {
                        @Override
                        @DebugLog
                        public void success(final List<Folder> folders, final List<Song> songs, final Bundle bundle) throws RemoteException {
                            ((Activity) getContext()).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (bundle == null) {
                                        mEndOfResults = true;
                                    }
                                    mPaginationBundle = bundle;
                                    mLoadingInProgress = false;
                                    List<Resource> items = new ArrayList<Resource>(folders.size() + songs.size());
                                    items.addAll(folders);
                                    items.addAll(songs);
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
                        public void failure(String reason) throws RemoteException {
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
    protected Card makeCard(Resource data) {
        if (data instanceof Folder) {
            return new FolderListCard(getContext(), (Folder) data);
        } else if (data instanceof Song) {
            return new SongListCard(getContext(), (Song) data);
        }
        throw new IllegalArgumentException("Resource must be of type Folder or Song");
    }

    public String getFolderId() {
        return mFolderId;
    }

}
