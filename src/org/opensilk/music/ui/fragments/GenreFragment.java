/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.opensilk.music.ui.fragments;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.loaders.GenreLoader;
import com.andrew.apollo.model.Genre;

import org.opensilk.music.ui.cards.CardGenreGrid;

import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;

/**
 * This class is used to display all of the genres on a user's device.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class GenreFragment extends HomePagerBaseFragment<Genre> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Loader<List<Genre>> onCreateLoader(final int id, final Bundle args) {
        return new GenreLoader(getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(final Loader<List<Genre>> loader, final List<Genre> data) {
        // Check for any errors
        if (data.isEmpty()) {
            // Set the empty text
            final TextView empty = (TextView)mRootView.findViewById(R.id.empty);
            empty.setText(getString(R.string.empty_music));
            mGridView.setEmptyView(empty);
            return;
        }

        ArrayList<Card> cards = new ArrayList<Card>();
        for (final Genre genre: data) {
            cards.add(new CardGenreGrid(getActivity(), genre));
        }
        CardGridArrayAdapter adapter = new CardGridArrayAdapter(getActivity(), cards);
        // Set the data behind the grid
        mGridView.setAdapter(adapter);
    }

    @Override
    protected boolean isSimpleLayout() {
        return false; //Just grid for now.
    }

    @Override
    protected boolean isDetailedLayout() {
        return false;
    }

}
