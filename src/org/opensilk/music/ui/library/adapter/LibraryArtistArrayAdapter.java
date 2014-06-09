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

import org.opensilk.music.api.callback.ArtistQueryResult;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.util.RemoteLibraryUtil;

import java.util.List;

/**
 * Created by drew on 6/14/14.
 */
public class LibraryArtistArrayAdapter extends AbsLibraryArrayAdapter<Artist> {


    public LibraryArtistArrayAdapter(Context context, String libraryIdentity, ComponentName libraryComponent) {
        super(context, android.R.layout.simple_list_item_1, libraryIdentity, libraryComponent);
    }

    @Override
    protected void getMore() {
        try {
            mLoadingInProgress = true;
            RemoteLibraryUtil.getService(mLibraryComponent).queryArtists(mLibraryIdentity, STEP, mPaginationBundle,
                    new ArtistQueryResult.Stub() {
                        @Override
                        public void success(final List<Artist> artists, final Bundle paginationBundle) throws RemoteException {
                            ((Activity) getContext()).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (paginationBundle == null) {
                                        mEndOfResults = true;
                                    }
                                    mPaginationBundle = paginationBundle;
                                    mLoadingInProgress = false;
                                    if (artists.size() > 0) {
                                        addAll(artists);
                                    }
                                }
                            });
                        }

                        @Override
                        public void failure(String reason) throws RemoteException {
                            mPaginationBundle = null;
                            mLoadingInProgress = false;
                            //TODO
                        }
                    });
        } catch (RemoteException e) {
            mLoadingInProgress = false;
            e.printStackTrace();
        }
    }
}
