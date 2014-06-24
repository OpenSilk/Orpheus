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
package org.opensilk.music.ui.cards.old.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.andrew.apollo.R;

/**
 * Special CardView for use with a DragSortListView
 *
 * Created by drew on 2/13/14.
 */
public class DragSortCardView extends CardViewNoHeader {
    public DragSortCardView(Context context) {
        super(context);
    }

    public DragSortCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DragSortCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void setupListeners() {
        // Disable this... all i know is it was fucking up the dragsortlist
        //super.setupListeners();
        setupOverflowButtonListener();
    }

    @Override
    protected void setupMainView() {
        super.setupMainView();
        // Since our card is not clickable we will just transfer the cards
        // onclick listener to the main content view
        mInternalContentLayout.setBackgroundResource(R.drawable.selectable_background_orpheus);
        mInternalContentLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCard.getOnClickListener()!=null)
                    mCard.getOnClickListener().onClick(mCard,v);
            }
        });
    }
}
