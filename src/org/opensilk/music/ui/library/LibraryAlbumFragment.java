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

package org.opensilk.music.ui.library;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;

import org.opensilk.music.ui.library.adapter.AbsLibraryArrayAdapter;
import org.opensilk.music.ui.library.adapter.LibraryAlbumArrayAdapter;

/**
 * Created by drew on 6/14/14.
 */
public class LibraryAlbumFragment extends CardListFragment implements AbsLibraryArrayAdapter.LoaderCallback {

    private ComponentName mLibraryComponentName;
    private String mLibraryIdentity;

    protected LibraryAlbumArrayAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            throw new RuntimeException("Null args");
        }
        mLibraryComponentName = getArguments().getParcelable(LibraryHomeFragment.ARG_COMPONENT);
        mLibraryIdentity = getArguments().getString(LibraryHomeFragment.ARG_IDENTITY);

        mAdapter = new LibraryAlbumArrayAdapter(getActivity(), mLibraryIdentity, mLibraryComponentName, this);
        if (savedInstanceState != null) {
            mAdapter.restoreInstanceState(savedInstanceState);
        } else {
            mAdapter.startLoad();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(mAdapter);
        if (savedInstanceState == null) {
            setListShown(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.saveInstanceState(outState);
    }

    @Override
    public void onFirstLoadComplete() {
        setListShown(true);
    }
}
