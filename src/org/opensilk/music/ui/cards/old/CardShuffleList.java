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

package org.opensilk.music.ui.cards.old;

import android.content.Context;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 4/15/14.
 */
public class CardShuffleList extends Card {

    public CardShuffleList(Context context) {
        this(context, R.layout.card_list_inner_layout_simple);
    }

    public CardShuffleList(Context context, int innerLayout) {
        super(context, innerLayout);
        init();
    }

    private void init() {
        mTitle = getContext().getString(R.string.menu_shuffle);
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                MusicUtils.shuffleAll(getContext());
            }
        });
    }

}
