/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.plugin.common;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.ArrayAdapter;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by drew on 7/19/14.
 */
public class LibraryArrayAdapter extends ArrayAdapter<Bundleable> {

    private static final int STEP = 30;

    private final RemoteLibrary mLibrary;
    private final LibraryInfo mLibraryInfo;

    private volatile Bundle mPaginationBundle;
    private List<Bundleable> mPendingItems;
    private AtomicBoolean mEndOfResults;

    public LibraryArrayAdapter(Context context, RemoteLibrary library, LibraryInfo libraryInfo) {
        super(context, android.R.layout.simple_list_item_1);
        mLibrary =library;
        mLibraryInfo = libraryInfo;
        mPendingItems = Collections.synchronizedList(new ArrayList<Bundleable>());
        mEndOfResults = new AtomicBoolean(false);
    }

    public void loadMore() {
        try {
            ResultCallback result = new ResultCallback();
            mLibrary.browseFolders(mLibraryInfo.libraryId, mLibraryInfo.folderId,
                    STEP, mPaginationBundle, result);
            while (!result.isComplete()) {
                try {
                    result.waitForResult();
                } catch (InterruptedException ignored) {}
            }
        } catch (RemoteException ex) {
            mPendingItems.clear();
            ex.printStackTrace();
        }
    }


    public boolean endOfResults() {
        return !mEndOfResults.get();
    }

    public void addPending() {
        if (!mPendingItems.isEmpty()) {
            addAll(mPendingItems);
            mPendingItems.clear();
        }
    }


    class ResultCallback extends Result.Stub {

        volatile boolean done;

        public boolean isComplete() {
            return done;
        }

        public synchronized void waitForResult() throws InterruptedException {
            wait();
        }

        @Override
        public synchronized void onNext(final List<Bundle> items, final Bundle paginationBundle) throws RemoteException {
            if (paginationBundle == null) {
                mEndOfResults.set(true);
            }
            mPaginationBundle = paginationBundle;
            if (!items.isEmpty()) {
                for (Bundle b : items) {
                    try {
                        mPendingItems.add(OrpheusApi.materializeBundle(b));
                    } catch (Exception ignored) {}
                }
            }
            done = true;
            notifyAll();
        }

        @Override
        public synchronized void onError(ParcelableException e) throws RemoteException {
            mPendingItems.clear();
            done = true;
            notifyAll();
        }
    }

}
