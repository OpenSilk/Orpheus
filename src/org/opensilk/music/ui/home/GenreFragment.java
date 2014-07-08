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

package org.opensilk.music.ui.home;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.widget.CursorAdapter;

import com.andrew.apollo.R;

import org.opensilk.music.ui.home.adapter.GenreGridAdapter;
import org.opensilk.music.ui.home.loader.GenreLoader;

/**
 * Created by drew on 6/28/14.
 */
public class GenreFragment extends BasePagerFragment {

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new GenreLoader(getActivity());
    }

    @Override
    protected CursorAdapter createAdapter() {
        if (wantGridView()) {
            return new GenreGridAdapter(getActivity(), mInjector);
        } else {
            throw new UnsupportedOperationException("Can't do list yet");
        }
    }

    @Override
    public boolean wantGridView() {
        return true;
    }

    @Override
    public int getListViewLayout() {
        return wantGridView() ? R.layout.card_staggeredgridview : R.layout.card_listview_fastscroll2;
    }
}
