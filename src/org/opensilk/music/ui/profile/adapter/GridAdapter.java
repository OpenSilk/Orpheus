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

package org.opensilk.music.ui.profile.adapter;

import android.content.Context;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalSongGroup;

import org.opensilk.music.ui.cards.AlbumCard;
import org.opensilk.music.ui.cards.SongGroupCard;
import org.opensilk.silkdagger.DaggerInjector;

import java.util.ArrayList;
import java.util.Collection;

import it.gmariotti.cardslib.library.extra.staggeredgrid.internal.CardGridStaggeredArrayAdapter;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 7/10/14.
 */
public class GridAdapter extends CardGridStaggeredArrayAdapter {

    private final DaggerInjector mInjector;

    public GridAdapter(Context context, DaggerInjector injector) {
        super(context, new ArrayList<Card>());
        mInjector = injector;
    }

    public void populate(Collection<Object> collection) {
        setNotifyOnChange(false);
        for (Object obj : collection) {
            if (obj instanceof LocalSongGroup) {
                SongGroupCard c = new SongGroupCard(getContext(), (LocalSongGroup) obj);
                mInjector.inject(c);
                add(c);
            } else if (obj instanceof LocalAlbum) {
                AlbumCard c = new AlbumCard(getContext(), (LocalAlbum) obj);
                c.useGridLayout();
                mInjector.inject(c);
                add(c);
            }
        }
        setNotifyOnChange(true);
        notifyDataSetChanged();
    }

}
