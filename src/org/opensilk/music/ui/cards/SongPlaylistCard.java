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

import org.opensilk.music.R;

import org.opensilk.music.api.model.Song;

import butterknife.ButterKnife;

/**
 * Created by drew on 6/30/14.
 */
public class SongPlaylistCard extends SongCollectionCard {

    protected View mDragHandle;
    private boolean mForLastAdded;

    public SongPlaylistCard(Context context, Song song) {
        super(context, song);
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        mDragHandle = ButterKnife.findById((View)parent.getParent().getParent(), R.id.card_drag_element);
        super.setupInnerViewElements(parent, view);
    }

    @Override
    protected void onInnerViewSetup() {
        super.onInnerViewSetup();
        if (mForLastAdded) {
            // hide drag handle on last added
            mDragHandle.setVisibility(View.GONE);
        }
    }

    @Override
    protected void cleanupViews() {
        super.cleanupViews();
        mDragHandle = null;
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        super.onCreatePopupMenu(m);
        m.getMenu().removeItem(R.id.popup_delete);
    }

    public void forLastAdded() {
        mForLastAdded= true;
    }
}
