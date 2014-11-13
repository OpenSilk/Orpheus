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

import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.ui.library.RemoteLibraryHelper;
import org.opensilk.silkdagger.DaggerInjector;

/**
 * Created by drew on 7/22/14.
 */
public class SearchAdapter extends FolderListArrayAdapter {

    private String query;

    public SearchAdapter(Context context, RemoteLibraryHelper library,
                         LibraryInfo libraryInfo, Callback callback,
                         DaggerInjector injector) {
        super(context, library, libraryInfo, callback, injector);
    }

    public void query(String query) {
        this.query = query;
        startLoad();
    }

    @Override
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
                    l.search(mLibraryInfo.libraryId, this.query,
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
        outState.putString("_query", query);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        query = inState.getString("_query");
    }

}
