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
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.andrew.apollo.R;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;

import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/19/14.
 */
public class ArtistLibraryCard extends AbsLibraryCard<Artist> {

    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;

    public ArtistLibraryCard(Context context, Artist data) {
        this(context, data, R.layout.library_listcard_artwork_inner);
    }

    public ArtistLibraryCard(Context context, Artist data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                //TODO
            }
        });
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.name);
        mCardSubTitle.setVisibility(View.GONE);
        mArtwork.setVisibility(View.GONE);
        ArtworkManager.loadImage(new ArtInfo(mData.name, null, null), mArtwork);

    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        m.inflate(R.menu.card_artist);
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return false;
            }
        });
    }

    @Override
    protected int getListLayout() {
        return R.layout.library_listcard_artwork_inner;
    }

    @Override
    protected int getGridLayout() {
        return R.layout.library_gridcard_artwork_inner;
    }
}
