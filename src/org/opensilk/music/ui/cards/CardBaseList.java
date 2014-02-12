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
 * Created by drew on 2/12/14.
 */
public abstract class CardBaseList<D> extends CardBaseThumb<D> {

    protected String mSecondTitle;

    public CardBaseList(Context context, D data) {
        super(context, data);
    }

    public CardBaseList(Context context, D data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        // Super sets title
        super.setupInnerViewElements(parent, view);
        if (mSecondTitle != null) {
            TextView v = (TextView) view.findViewById(R.id.card_main_inner_secondary_title);
            if (v != null) {
                v.setText(mSecondTitle);
            }
        }
    }

    @Override
    protected void initHeader() {
        final CardHeader header = new CardHeader(getContext());
        header.setButtonOverflowVisible(true);
        header.setPopupMenu(getHeaderMenuId(), getNewHeaderPopupMenuListener());
        addCardHeader(header);
    }

    /**
     * @return Resource id of popup menu
     */
    protected abstract int getHeaderMenuId();

    /**
     * @return Listener for popup menu actions
     */
    protected abstract CardHeader.OnClickCardHeaderPopupMenuListener getNewHeaderPopupMenuListener();

    public void setSecondTitle(String title) {
        mSecondTitle = title;
    }
}
