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

package org.opensilk.music.ui.profile;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.model.Genre;

import org.opensilk.music.adapters.SongListCardCursorAdapter;
import org.opensilk.music.loaders.GenreSongCursorLoader;

import it.gmariotti.cardslib.library.view.CardListView;

/**
 * Created by drew on 2/28/14.
 */
public class ProfileGenreSongsFragment extends ProfileBaseFragment<Genre> {

    private Genre mGenre;

    public static ProfileGenreSongsFragment newInstance(Bundle args) {
        ProfileGenreSongsFragment f = new ProfileGenreSongsFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGenre = mBundleData;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.card_listview_fastscroll, null);
        mListView = (CardListView) v.findViewById(android.R.id.list);
        // set the adapter
        mListView.setAdapter(mAdapter);
        return v;
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new GenreSongCursorLoader(getActivity(), args.getLong(Config.ID));
    }

    /*
     * Implement Abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        return new SongListCardCursorAdapter(getActivity());
    }

    @Override
    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mBundleData.mGenreId);
        return b;
    }
}
