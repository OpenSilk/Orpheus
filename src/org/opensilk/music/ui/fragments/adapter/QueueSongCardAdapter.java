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

package org.opensilk.music.ui.fragments.adapter;

import android.content.Context;

import com.andrew.apollo.model.RecentSong;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.cards.SongQueueCard;
import org.opensilk.silkdagger.DaggerInjector;

import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;

/**
 * Created by drew on 6/25/14.
 */
public class QueueSongCardAdapter extends CardArrayAdapter {

    private DaggerInjector mInjector;

    public QueueSongCardAdapter(Context context, DaggerInjector injector) {
        super(context, new ArrayList<Card>());
        mInjector = injector;
    }

    public void addSongs(List<RecentSong> songs) {
        setNotifyOnChange(false);
        for (int ii=0; ii<songs.size(); ii++) {
            SongQueueCard c = new SongQueueCard(getContext(), songs.get(ii));
            c.setId(String.valueOf(ii));
            mInjector.inject(c);
            add(c);
        }
        setNotifyOnChange(true);
        notifyDataSetChanged();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        Card c = getItem(position);
        return c.getId().hashCode();
    }
}
