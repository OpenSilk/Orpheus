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

import org.opensilk.music.api.callback.SongQueryResult;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.library.card.SongListCard;
import org.opensilk.music.util.RemoteLibraryUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/18/14.
 */
public class LibrarySongArrayAdapter extends AbsLibraryArrayAdapter<Song> {

    public LibrarySongArrayAdapter(Context context, String libraryIdentity, ComponentName libraryComponent, LoaderCallback callback) {
        super(context, libraryIdentity, libraryComponent, callback);
    }

    @Override
    protected void getMore() {
        try {
            mLoadingInProgress = true;
            RemoteLibraryUtil.getService(mLibraryComponent).querySongs(mLibraryIdentity, STEP, mPaginationBundle,
                    new SongQueryResult.Stub() {
                        @Override
                        public void success(final List<Song> songs, final Bundle paginationBundle) throws RemoteException {
                            ((Activity) getContext()).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (paginationBundle == null) {
                                        mEndOfResults = true;
                                    }
                                    mPaginationBundle = paginationBundle;
                                    mLoadingInProgress = false;
                                    if (songs.size() > 0) {
                                        addItems(songs);
                                    }
                                    if (!mFirstLoadComplete && mCallback != null) {
                                        mFirstLoadComplete = true;
                                        mCallback.onFirstLoadComplete();
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {

    }

    @Override
    protected Card makeCard(Song data) {
        return new SongListCard(getContext(), data);
    }

}
