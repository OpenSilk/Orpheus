/*
 * Copyright (C) 2012 Andrew Neal
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

package org.opensilk.music.ui.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.andrew.apollo.R;

import org.opensilk.music.adapters.SongListCardCursorAdapter;
import org.opensilk.music.loaders.SongCursorLoader;

/**
 * This class is used to display all of the songs on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SongFragment extends HomePagerBaseCursorFragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set the empty text
        final View empty = mRootView.findViewById(android.R.id.empty);
        mListView.setEmptyView(empty);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SongCursorLoader(getActivity());
    }

    /*
     * Implement abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        return new SongListCardCursorAdapter(getActivity());
    }

    @Override
    protected boolean isSimpleLayout() {
        return true; // Were always list
    }

}
