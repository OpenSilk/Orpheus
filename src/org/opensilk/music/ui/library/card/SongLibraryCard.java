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
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;

import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/19/14.
 */
public class SongLibraryCard extends AbsLibraryCard<Song> {

    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;

    public SongLibraryCard(Context context, Song song) {
        this(context, song, R.layout.library_listcard_artwork_inner);
    }

    public SongLibraryCard(Context context, Song song, int innerLayout) {
        super(context, song, innerLayout);
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                MusicUtils.playFile(getContext(), mData.dataUri);
            }
        });
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.name);
        mCardSubTitle.setText(mData.artistName);
        ArtworkManager.loadImage(new ArtInfo(mData.albumArtistName, mData.albumName, mData.artworkUri), mArtwork);
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {

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
