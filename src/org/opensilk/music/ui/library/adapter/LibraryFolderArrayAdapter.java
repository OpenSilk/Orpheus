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
import org.opensilk.music.util.RemoteLibraryUtil;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 6/14/14.
 */
public class LibraryFolderArrayAdapter extends AbsLibraryArrayAdapter<Resource> {

    private String mFolderId;

    public LibraryFolderArrayAdapter(Context context, String libraryIdentity, ComponentName libraryComponent) {
        super(context, android.R.layout.simple_list_item_1, libraryIdentity, libraryComponent);
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
                                    if (folders.size() > 0) {
                                        addAll(folders);
                                    }
                                    if (songs.size() > 0) {
                                        addAll(songs);
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
}
