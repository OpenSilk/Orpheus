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
import com.andrew.apollo.loaders.ArtistLoader;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.ui.cards.CardArtistGrid;
import org.opensilk.music.ui.cards.CardArtistList;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;

import static com.andrew.apollo.utils.PreferenceUtils.ARTIST_LAYOUT;

/**
 * This class is used to display all of the artists on a user's device.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistFragment extends HomePagerBaseFragment<Artist> {

    /**
     * {@inheritDoc}
     */
    @Override
    @DebugLog
    public Loader<List<Artist>> onCreateLoader(final int id, final Bundle args) {
        return new ArtistLoader(getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @DebugLog
    public void onLoadFinished(final Loader<List<Artist>> loader, final List<Artist> data) {
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
            for (final Artist artist: data) {
                cards.add(new CardArtistList(getActivity(), artist));
            }
            CardArrayAdapter adapter = new CardArrayAdapter(getActivity(), cards);
            // Set the data behind the list
            mListView.setAdapter(adapter);
        } else {
            for (final Artist artist: data) {
                cards.add(new CardArtistGrid(getActivity(), artist));
            }
            CardGridArrayAdapter adapter = new CardGridArrayAdapter(getActivity(), cards);
            // Set the data behind the grid
            mGridView.setAdapter(adapter);
        }
    }

    protected boolean isSimpleLayout() {
        return PreferenceUtils.getInstance(getActivity()).isSimpleLayout(ARTIST_LAYOUT,
                getActivity());
    }

    protected boolean isDetailedLayout() {
        return PreferenceUtils.getInstance(getActivity()).isDetailedLayout(ARTIST_LAYOUT,
                getActivity());
    }
}
