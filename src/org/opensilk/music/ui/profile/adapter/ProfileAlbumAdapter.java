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

package org.opensilk.music.ui.profile.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.cards.CardSongList;
import org.opensilk.music.ui.cards.SongCard;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.silkdagger.DaggerInjector;

/**
 * Created by drew on 2/21/14.
 */
public class ProfileAlbumAdapter extends CursorAdapter {

    private final DaggerInjector mInjector;

    public ProfileAlbumAdapter(Context context, DaggerInjector injector) {
        super(context, null, 0);
        mInjector =injector;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.profile_list, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // setup card content
        final SongCard card = new SongCard(context, CursorHelpers.makeLocalSongFromCursor(context, cursor));
        mInjector.inject(card);
        TextView title = (TextView) view.findViewById(R.id.track_info);
        title.setText(card.getData().name);
        final int position = cursor.getPosition();
        // hack for my inability to make onitemclicked work.
        View mainContent = view.findViewById(R.id.track_artist_info);
        mainContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApolloUtils.execute(false, new CommandRunner(context, new Command() {
                    @Override
                    public CharSequence execute() {
                        LocalSong[] list = MusicUtils.getLocalSongListForAlbum(context, ((LocalSong) card.getData()).albumId);
                        MusicUtils.playAllSongs(context, list, position, false);
                        return null;
                    }
                }));
            }
        });
        // init overflow
        View overflowButton = view.findViewById(R.id.overflow_button);
        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                card.onOverflowClicked(view);
            }
        });
    }

}
