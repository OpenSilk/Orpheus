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

import android.os.Bundle;
import android.support.v4.content.Loader;

import com.andrew.apollo.loaders.PlaylistLoader;
import com.andrew.apollo.model.Playlist;

import org.opensilk.music.ui.cards.CardPlaylistGrid;

import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;

/**
 * This class is used to display all of the playlists on a user's device.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class HomePlaylistFragment extends HomePagerBaseFragment<Playlist> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Loader<List<Playlist>> onCreateLoader(final int id, final Bundle args) {
        return new PlaylistLoader(getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(final Loader<List<Playlist>> loader, final List<Playlist> data) {
        // Check for any errors
        if (data.isEmpty()) {
            return;
        }

        ArrayList<Card> cards = new ArrayList<Card>();
        for (final Playlist playlist: data) {
            cards.add(new CardPlaylistGrid(getActivity(), playlist));
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
