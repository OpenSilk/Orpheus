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

package org.opensilk.music.ui.home.adapter;

import android.content.Context;
import android.database.Cursor;

import org.opensilk.music.ui.cards.SongCard;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.silkdagger.DaggerInjector;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardCursorAdapter;

/**
 * Created by drew on 6/24/14.
 */
public class SongCardCursorAdapter extends CardCursorAdapter {

    private DaggerInjector mInjector;

    public SongCardCursorAdapter(Context context, DaggerInjector injector) {
        super(context, null, 0);
        mInjector = injector;
    }

    @Override
    protected Card getCardFromCursor(Cursor cursor) {
        SongCard card = new SongCard(getContext(), CursorHelpers.makeSongFromCursor(getContext(), cursor));
        mInjector.inject(card);
        return card;
    }

}
