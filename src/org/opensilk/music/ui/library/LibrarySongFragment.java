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
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.library.adapter.LibrarySongArrayAdapter;

/**
 * Created by drew on 6/14/14.
 */
public class LibrarySongFragment extends ListFragment {

    private ComponentName mLibraryComponentName;
    private String mLibraryIdentity;

    protected LibrarySongArrayAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            throw new RuntimeException("Null args");
        }
        mLibraryComponentName = getArguments().getParcelable(LibraryHomeFragment.ARG_COMPONENT);
        mLibraryIdentity = getArguments().getString(LibraryHomeFragment.ARG_IDENTITY);

        mAdapter = new LibrarySongArrayAdapter(getActivity(), mLibraryIdentity, mLibraryComponentName);
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
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.saveInstanceState(outState);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Song s = mAdapter.getItem(position);
        MusicUtils.playFile(getActivity(), s.dataUri);        //TODO
    }
}
