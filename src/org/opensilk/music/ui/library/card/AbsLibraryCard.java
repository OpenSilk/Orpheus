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

package org.opensilk.music.ui.library.card;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.R;

import org.opensilk.music.api.model.spi.Bundleable;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/19/14.
 */
public abstract class AbsLibraryCard<D extends Bundleable> extends Card {

    protected final D mData;

    @InjectView(R.id.card_title)
    protected TextView mCardTitle;
    @InjectView(R.id.card_subtitle)
    protected TextView mCardSubTitle;

    protected boolean mForGrid;

    public AbsLibraryCard(Context context, D data) {
        this(context, data, R.layout.library_folder_listcard_inner);
    }

    public AbsLibraryCard(Context context, D data, int innerLayout) {
        super(context, innerLayout);
        mData = data;
        init();
    }

    protected abstract void init();
    protected abstract void onInnerViewSetup();
    protected abstract void onCreatePopupMenu(PopupMenu m);
    protected abstract int getListLayout();
    protected abstract int getGridLayout();


    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        ButterKnife.inject(this, view);
        onInnerViewSetup();
    }

    @OnClick(R.id.card_overflow_button)
    public void onOverflowClicked(View v) {
        PopupMenu m = new PopupMenu(getContext(), v);
        onCreatePopupMenu(m);
        m.show();
    }

    public D getData() {
        return mData;
    }

    public void useGridLayout() {
        setInnerLayout(getGridLayout());
    }

    public void useListLayout() {
        setInnerLayout(getListLayout());
    }

    public boolean isGridStyle() {
        return getInnerLayout() == getGridLayout();
    }

}
