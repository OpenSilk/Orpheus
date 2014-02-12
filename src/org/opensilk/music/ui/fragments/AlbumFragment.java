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

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.loaders.AlbumLoader;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.ui.cards.CardAlbumGrid;
import org.opensilk.music.ui.cards.CardAlbumList;

import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;

import static com.andrew.apollo.utils.PreferenceUtils.ALBUM_LAYOUT;

/**
 * This class is used to display all of the albums on a user's device.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AlbumFragment extends HomePagerBaseFragment<Album> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Loader<List<Album>> onCreateLoader(final int id, final Bundle args) {
        return new AlbumLoader(getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(final Loader<List<Album>> loader, final List<Album> data) {
        // Check for any errors
        if (data.isEmpty()) {
            // Set the empty text
            final TextView empty = (TextView)mRootView.findViewById(R.id.empty);
            empty.setText(getString(R.string.empty_music));
            if (isSimpleLayout()) {
                mListView.setEmptyView(empty);
            } else {
                mGridView.setEmptyView(empty);
            }
            return;
        }

        ArrayList<Card> cards = new ArrayList<Card>();

        if (isSimpleLayout()) {
            for (final Album album: data) {
                cards.add(new CardAlbumList(getActivity(), album));
            }
            CardArrayAdapter adapter = new CardArrayAdapter(getActivity(), cards);
            // Set the data behind the list
            mListView.setAdapter(adapter);
        } else {
            for (final Album album: data) {
                cards.add(new CardAlbumGrid(getActivity(), album));
            }
            CardGridArrayAdapter adapter = new CardGridArrayAdapter(getActivity(), cards);
            // Set the data behind the grid
            mGridView.setAdapter(adapter);
        }

    }

    protected boolean isSimpleLayout() {
        return PreferenceUtils.getInstance(getActivity()).isSimpleLayout(ALBUM_LAYOUT,
                getActivity());
    }

    protected boolean isDetailedLayout() {
        return PreferenceUtils.getInstance(getActivity()).isDetailedLayout(ALBUM_LAYOUT,
                getActivity());
    }
}
