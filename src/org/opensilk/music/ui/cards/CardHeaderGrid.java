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
package org.opensilk.music.ui.cards;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.andrew.apollo.R;

import it.gmariotti.cardslib.library.internal.CardHeader;

/**
 * Created by drew on 2/11/14.
 */
public class CardHeaderGrid extends CardHeader {

    private String lineTwo;

    public CardHeaderGrid(Context context) {
        this(context, R.layout.card_grid_header_inner);
    }

    public CardHeaderGrid(Context context, int innerLayout) {
        super(context, innerLayout);
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        super.setupInnerViewElements(parent, view);
        if (lineTwo != null) {
            TextView t2 = (TextView) view.findViewById(R.id.card_header_inner_secondary_title);
            if (t2 != null) {
                t2.setText(lineTwo);
            }
        }
    }

    public void setLineTwo(String text) {
        lineTwo = text;
    }
}
