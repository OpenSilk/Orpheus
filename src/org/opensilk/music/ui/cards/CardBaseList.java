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
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.R;

/**
 * Created by drew on 2/12/14.
 */
public abstract class CardBaseList<D> extends CardBaseThumb<D> {

    protected String mSubTitle;
    protected String mSubTitleAlt;
    protected String mExtraText;

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
        TextView v = (TextView) view.findViewById(R.id.card_main_inner_sub_title);
        if (v != null) {
            if (mSubTitle != null) {
                v.setText(mSubTitle);
            } else {
                v.setVisibility(View.GONE);
            }
        }
        TextView v1 = (TextView) view.findViewById(R.id.card_main_inner_sub_title_alt);
        if (v1 != null) {
            if (mSubTitleAlt != null) {
                v1.setText(mSubTitleAlt);
            } else {
                v1.setVisibility(View.GONE);
            }
        }
        TextView v2 = (TextView) view.findViewById(R.id.card_main_inner_extra_text);
        if (v2 != null) {
            if (mExtraText != null) {
                v2.setText(mExtraText);
            } else {
                v2.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void initHeader() {

    }

    /**
     * @return Resource id of popup menu
     */
    public abstract int getOverflowMenuId();

    /**
     * @return Listener for popup menu actions
     */
    public abstract PopupMenu.OnMenuItemClickListener getOverflowPopupMenuListener();

    public void setSubTitle(String title) {
        mSubTitle = title;
    }
}
