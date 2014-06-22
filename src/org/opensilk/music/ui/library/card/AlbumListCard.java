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

import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.ArtInfo;
import org.opensilk.music.artwork.ArtworkManager;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/19/14.
 */
public class AlbumListCard extends AbsListCard<Album> {

    public AlbumListCard(Context context, Album data) {
        super(context, data);
    }

    public AlbumListCard(Context context, Album data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                NavUtils.openAlbumProfile(getContext(), mData);
            }
        });
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.name);
        mCardSubTitle.setText(mData.artistName);
        mArtwork.setVisibility(View.VISIBLE);
        ArtworkManager.loadImage(new ArtInfo(mData.artistName, mData.name, mData.artworkUri), mArtwork);
    }
}
