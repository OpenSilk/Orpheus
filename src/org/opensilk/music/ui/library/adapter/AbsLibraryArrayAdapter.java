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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * Created by drew on 6/14/14.
 */
public abstract class AbsLibraryArrayAdapter<T> extends ArrayAdapter<T> {

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
            int left = getCount() - 1 - position;
            if (left > 0 && left < 5) {
                getMore();
            }
        }
    }

    protected abstract void getMore();

}
