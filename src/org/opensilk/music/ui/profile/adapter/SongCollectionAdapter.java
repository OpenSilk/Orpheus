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
import android.database.Cursor;
import android.net.Uri;

import org.opensilk.music.ui.cards.SongCollectionCard;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.silkdagger.DaggerInjector;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardCursorAdapter;

/**
 * Created by drew on 7/10/14.
 */
public class SongCollectionAdapter extends CardCursorAdapter {

    protected final DaggerInjector injector;
    protected final boolean useSimpleLayout;
    protected final Uri uri;
    protected final String[] projection;
    protected final String selection;
    protected final String[] selectionArgs;
    protected final String sortOrder;

    public SongCollectionAdapter(Context context, DaggerInjector mInjector, boolean useSimpleLayout,
                                 Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        super(context);
        this.injector = mInjector;
        this.useSimpleLayout = useSimpleLayout;
        this.uri = uri;
        this.projection = projection;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.sortOrder = sortOrder;
    }

    @Override
    protected Card getCardFromCursor(Cursor cursor) {
        SongCollectionCard card = new SongCollectionCard(getContext(), CursorHelpers.makeLocalSongFromCursor(getContext(), cursor));
        card.setPosition(cursor.getPosition());
        card.setQueryParams(uri, projection, selection, selectionArgs, sortOrder);
        if (useSimpleLayout) {
            card.useSimpleLayout();
        }
        injector.inject(card);
        return card;
    }
}
