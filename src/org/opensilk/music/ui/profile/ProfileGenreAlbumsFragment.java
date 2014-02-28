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
import android.widget.ListView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.model.Genre;
import com.mobeta.android.dslv.DragSortListView;

import org.opensilk.music.adapters.ProfileAlbumListCardCursorAdapter;
import org.opensilk.music.adapters.SongListCardCursorAdapter;
import org.opensilk.music.loaders.GenreAlbumCursorLoader;

import it.gmariotti.cardslib.library.view.CardListView;

/**
 * Created by drew on 2/28/14.
 */
public class ProfileGenreAlbumsFragment extends ProfileBaseFragment<Genre> {

    private Genre mGenre;

    public static ProfileGenreAlbumsFragment newInstance(Bundle args) {
        ProfileGenreAlbumsFragment f = new ProfileGenreAlbumsFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGenre = mBundleData;
        //We have to set this manually since we arent using CardListView
        // XXX: Remove for pager
        ((ProfileAlbumListCardCursorAdapter) mAdapter).setRowLayoutId(R.layout.profile_card_list);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // For pager
//        View v = inflater.inflate(R.layout.profile_card_listview, null);
//        mListView = (CardListView) v.findViewById(android.R.id.list);
        View v = inflater.inflate(R.layout.profile_dragsort_listview, container, false);
        mListView = (ListView) v.findViewById(android.R.id.list);
        // not sortable
        ((DragSortListView) mListView).setDragEnabled(false);
        // set the adapter
        mListView.setAdapter(mAdapter);
        return v;
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new GenreAlbumCursorLoader(getActivity(), args.getLong(Config.ID));
    }

    /*
     * Implement Abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        return new ProfileAlbumListCardCursorAdapter(getActivity());
    }

    @Override
    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mBundleData.mGenreId);
        return b;
    }
}
