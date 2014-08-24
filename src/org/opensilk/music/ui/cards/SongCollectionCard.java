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
import android.net.Uri;
import android.view.View;

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.music.util.CursorHelpers;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 7/10/14.
 */
public class SongCollectionCard extends SongCard {

    private int position;
    private Uri uri;
    private String[] projection;
    private String selection;
    private String[] selectionArgs;
    private String sortOrder;

    public SongCollectionCard(Context context, Song song) {
        super(context, song);
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setQueryParams(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        this.uri = uri;
        this.projection = projection;
        this.selection =selection;
        this.selectionArgs = selectionArgs;
        this.sortOrder = sortOrder;
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                Command c = new Command() {
                    @Override
                    public CharSequence execute() {
                        LocalSong[] list = CursorHelpers.makeLocalSongList(getContext(), uri, projection, selection, selectionArgs, sortOrder);
                        MusicUtils.playAllSongs(getContext(), list, position, false);
                        return null;
                    }
                };
                new CommandRunner(getContext(), c).execute();
            }
        });
    }
}
