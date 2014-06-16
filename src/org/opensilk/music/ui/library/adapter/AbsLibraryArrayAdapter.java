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
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.opensilk.music.api.model.Resource;

import java.util.ArrayList;

/**
 * Created by drew on 6/14/14.
 */
public abstract class AbsLibraryArrayAdapter<T extends Parcelable> extends ArrayAdapter<T> {

    public static final int STEP = 20;

    protected final String mLibraryIdentity;
    protected final ComponentName mLibraryComponent;

    protected Bundle mPaginationBundle;

    protected boolean mLoadingInProgress;
    protected boolean mEndOfResults;

    protected AbsLibraryArrayAdapter(Context context, int layout, String libraryIdentity, ComponentName libraryComponent) {
        super(context, layout);
        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException("Context must be from activity");
        }
        mLibraryIdentity = libraryIdentity;
        mLibraryComponent = libraryComponent;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        maybeLoadMore(position);
        return super.getView(position, convertView, parent);
    }

    public void startLoad() {
        mPaginationBundle = null;
        clear();
        getMore();
    }

    protected void maybeLoadMore(int position) {
        if (!mLoadingInProgress && !mEndOfResults) {
            if ((getCount() - 1) == position) {
                getMore();
            }
        }
    }

    protected abstract void getMore();

    protected abstract void onSaveInstanceState(Bundle outState);
    protected abstract void onRestoreInstanceState(Bundle inState);

    public void saveInstanceState(Bundle outState) {
        Bundle b = new Bundle();
        ArrayList<T> items = new ArrayList<>(getCount());
        for (int ii=0; ii<getCount(); ii++) {
            items.add(getItem(ii));
        }
        b.putParcelableArrayList("items", items);
        b.putBundle("pagination", mPaginationBundle);
        b.putBoolean("eor", mEndOfResults);
        onSaveInstanceState(b);
        outState.putBundle(getClass().getName(), b);
    }

    public void restoreInstanceState(Bundle inState) {
        Bundle b = inState.getBundle(getClass().getName());
        ArrayList<T> items = b.getParcelableArrayList("items");
        addAll(items);
        mPaginationBundle = b.getBundle("pagination");
        mEndOfResults = b.getBoolean("eor");
        onRestoreInstanceState(b);
    }

}
