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

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 7/10/14.
 */
public class SongAlbumCard extends SongCard {

    private int position;
    private long albumId;

    public SongAlbumCard(Context context, Song song) {
        super(context, song);
        useSimpleLayout();
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                Command c = new Command() {
                    @Override
                    public CharSequence execute() {
                        LocalSong[] list = MusicUtils.getLocalSongListForAlbum(getContext(), albumId);
                        MusicUtils.playAllSongs(getContext(), list, position, false);
                        return null;
                    }
                };
                ApolloUtils.execute(false, new CommandRunner(getContext(), c));
            }
        });
    }
}
